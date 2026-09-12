#!/usr/bin/env python3
"""Hero Defense deterministic Blender-to-sprite entry point.

Usage:
  blender --background --factory-startup --python tools/blender/generate_assets.py -- \
      --batch pilot --output android/assets/generated
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from datetime import datetime, timezone
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import bpy  # noqa: E402

from hd_pipeline.config import (  # noqa: E402
    BLENDER_VERSION,
    BOSSES,
    CLIPS,
    FRAME_RATE,
    FRAME_SIZE,
    PALETTE,
    REGULAR_CHARACTERS,
    REQUIRED_BONES,
    RenderAsset,
)
from hd_pipeline.environment import (  # noqa: E402
    build_crystal_prop,
    build_ground_tile,
    build_potion_icon,
    build_world_tree,
)
from hd_pipeline.models import MATERIALS, add_equipment_variant, build_character, build_hero  # noqa: E402
from hd_pipeline.rig import author_standard_actions  # noqa: E402
from hd_pipeline.scene import configure_scene, pack_grid, reset_scene, triangle_count  # noqa: E402

PIPELINE_VERSION = 1
TIER_COLORS = {
    "COMMON": "#87949A",
    "UNCOMMON": "#68AD69",
    "RARE": "#4D87D8",
    "LEGENDARY": "#D6AD4C",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--batch",
        choices=("pilot", "characters", "world-tree", "equipment", "environment", "all"),
        default="pilot",
    )
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--catalog", type=Path, default=SCRIPT_DIR / "equipment_visuals.json")
    parser.add_argument("--keep-frames", action="store_true")
    parser.add_argument("--only", nargs="*", default=[])
    arguments = sys.argv[sys.argv.index("--") + 1 :] if "--" in sys.argv else []
    return parser.parse_args(arguments)


def render_character(asset: RenderAsset, output: Path, keep_frames: bool) -> dict:
    frame_root = output / "_frames" / asset.key
    _fresh_directory(frame_root)
    reset_scene()
    MATERIALS.clear()
    scene = configure_scene(asset.frame_class, frame_root)
    model = build_character(asset.builder)
    if model.armature is None:
        raise RuntimeError(f"Character {asset.key} did not create an armature")
    actions = author_standard_actions(model.armature, asset.key)

    frame_paths: dict[str, list[Path]] = {}
    for clip, count in CLIPS.items():
        model.armature.animation_data.action = actions[clip]
        frame_paths[clip] = []
        for frame in range(1, count + 1):
            scene.frame_set(frame)
            path = frame_root / f"{asset.key}_base_{clip}_{frame - 1:02d}.png"
            scene.render.filepath = str(path)
            bpy.ops.render.render(write_still=True)
            frame_paths[clip].append(path)

    sprite_directory = output / "sprites"
    sprite_directory.mkdir(parents=True, exist_ok=True)
    sheet_path = sprite_directory / f"{asset.key}.png"
    width, height, regions = pack_grid(frame_paths, sheet_path, FRAME_SIZE[asset.frame_class])
    atlas_path = sprite_directory / f"{asset.key}.atlas"
    _write_libgdx_atlas(atlas_path, sheet_path.name, width, height, regions)
    triangles = triangle_count(model.render_objects)
    entry = {
        "key": asset.key,
        "family": asset.family,
        "builder": asset.builder,
        "frameClass": asset.frame_class,
        "frameSize": FRAME_SIZE[asset.frame_class],
        "sheet": _relative(sheet_path, output),
        "atlas": _relative(atlas_path, output),
        "sheetWidth": width,
        "sheetHeight": height,
        "clips": regions,
        "frameRate": FRAME_RATE,
        "triangles": triangles,
        "armature": model.armature.name,
        "bones": sorted(bone.name for bone in model.armature.data.bones),
        "boneAnimated": True,
        **model.metadata,
    }
    _write_json(sprite_directory / f"{asset.key}.json", entry)
    if not keep_frames:
        shutil.rmtree(frame_root)
    return entry


def render_tree_state(damaged: bool, output: Path, keep_frames: bool) -> dict:
    key = "world_tree_damaged" if damaged else "world_tree_healthy"
    frame_root = output / "_frames" / key
    _fresh_directory(frame_root)
    reset_scene()
    MATERIALS.clear()
    scene = configure_scene("tree", frame_root)
    model = build_world_tree(damaged)
    frame_paths = {"idle": []}
    for frame in range(1, 7):
        scene.frame_set(frame)
        path = frame_root / f"{key}_idle_{frame - 1:02d}.png"
        scene.render.filepath = str(path)
        bpy.ops.render.render(write_still=True)
        frame_paths["idle"].append(path)
    sprite_directory = output / "sprites"
    sprite_directory.mkdir(parents=True, exist_ok=True)
    sheet_path = sprite_directory / f"{key}.png"
    width, height, regions = pack_grid(frame_paths, sheet_path, FRAME_SIZE["tree"])
    atlas_path = sprite_directory / f"{key}.atlas"
    _write_libgdx_atlas(atlas_path, sheet_path.name, width, height, regions)
    entry = {
        "key": key,
        "family": "world_tree",
        "frameClass": "tree",
        "frameSize": FRAME_SIZE["tree"],
        "sheet": _relative(sheet_path, output),
        "atlas": _relative(atlas_path, output),
        "clips": regions,
        "triangles": triangle_count(model.render_objects),
        "armature": model.armature.name,
        "bones": sorted(bone.name for bone in model.armature.data.bones),
        "boneAnimated": True,
        **model.metadata,
    }
    _write_json(sprite_directory / f"{key}.json", entry)
    if not keep_frames:
        shutil.rmtree(frame_root)
    return entry


def render_equipment(catalog_path: Path, output: Path, keep_frames: bool, only: set[str]) -> list[dict]:
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    entries = []
    for item_index, item in enumerate(catalog["items"]):
        if only and item["id"] not in only:
            continue
        key = f"equipment_{item['id']}"
        frame_root = output / "_frames" / key
        _fresh_directory(frame_root)
        reset_scene()
        MATERIALS.clear()
        scene = configure_scene("character", frame_root)
        hero = build_hero()
        actions = author_standard_actions(hero.armature, key)
        for obj in hero.render_objects:
            obj.hide_render = True
        equipment_objects = add_equipment_variant(
            hero.armature,
            item["visualSlot"],
            item_index,
            TIER_COLORS[item["tier"]],
        )
        frame_paths: dict[str, list[Path]] = {}
        for clip, count in CLIPS.items():
            hero.armature.animation_data.action = actions[clip]
            frame_paths[clip] = []
            for frame in range(1, count + 1):
                scene.frame_set(frame)
                path = frame_root / f"{key}_{clip}_{frame - 1:02d}.png"
                scene.render.filepath = str(path)
                bpy.ops.render.render(write_still=True)
                frame_paths[clip].append(path)
        sprite_directory = output / "equipment"
        sprite_directory.mkdir(parents=True, exist_ok=True)
        sheet_path = sprite_directory / f"{item['id']}.png"
        width, height, regions = pack_grid(frame_paths, sheet_path, FRAME_SIZE["character"])
        atlas_path = sprite_directory / f"{item['id']}.atlas"
        _write_libgdx_atlas(atlas_path, sheet_path.name, width, height, regions)
        entry = {
            "key": key,
            "family": "equipment",
            "itemId": item["id"],
            "slot": item["slot"],
            "visualSlot": item["visualSlot"],
            "tier": item["tier"],
            "frameClass": "character",
            "frameSize": FRAME_SIZE["character"],
            "sheet": _relative(sheet_path, output),
            "atlas": _relative(atlas_path, output),
            "clips": regions,
            "triangles": triangle_count(equipment_objects),
            "armature": hero.armature.name,
            "bones": sorted(bone.name for bone in hero.armature.data.bones),
            "boneAnimated": True,
            "runtimeGlow": item["tier"] in {"RARE", "LEGENDARY"},
        }
        _write_json(sprite_directory / f"{item['id']}.json", entry)
        entries.append(entry)
        if not keep_frames:
            shutil.rmtree(frame_root)
    return entries


def render_static_model(key: str, family: str, frame_class: str, builder, output: Path) -> dict:
    frame_root = output / "_frames" / key
    _fresh_directory(frame_root)
    reset_scene()
    MATERIALS.clear()
    scene = configure_scene(frame_class, frame_root)
    model = builder()
    scene.frame_set(1)
    path = frame_root / f"{key}_idle_00.png"
    scene.render.filepath = str(path)
    bpy.ops.render.render(write_still=True)
    target_directory = output / family
    target_directory.mkdir(parents=True, exist_ok=True)
    target = target_directory / f"{key}.png"
    shutil.copy2(path, target)
    shutil.rmtree(frame_root)
    entry = {
        "key": key,
        "family": family,
        "frameClass": frame_class,
        "frameSize": FRAME_SIZE[frame_class],
        "sheet": _relative(target, output),
        "clips": {"idle": [{"x": 0, "y": 0, "width": FRAME_SIZE[frame_class], "height": FRAME_SIZE[frame_class], "index": 0}]},
        "triangles": triangle_count(model.render_objects),
        **model.metadata,
    }
    _write_json(target_directory / f"{key}.json", entry)
    return entry


def render_environment(output: Path) -> list[dict]:
    entries = []
    for variant in range(3):
        entries.append(render_static_model(
            f"ground_tile_{variant}", "environment", "environment",
            lambda value=variant: build_ground_tile(value), output,
        ))
    for variant in range(3):
        entries.append(render_static_model(
            f"crystal_prop_{variant}", "environment", "environment",
            lambda value=variant: build_crystal_prop(value), output,
        ))
    for tier in range(1, 7):
        entries.append(render_static_model(
            f"health_potion_{tier}", "icons", "item",
            lambda value=tier: build_potion_icon(value), output,
        ))
    return entries


def main() -> None:
    args = parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    existing = _read_existing_manifest(output)
    generated: list[dict] = []
    only = set(args.only)

    if args.batch in {"pilot", "all"}:
        pilot = (REGULAR_CHARACTERS[0], REGULAR_CHARACTERS[1])
        generated.extend(render_character(asset, output, args.keep_frames) for asset in pilot if not only or asset.key in only)
    if args.batch in {"characters", "all"}:
        generated.extend(
            render_character(asset, output, args.keep_frames)
            for asset in (*REGULAR_CHARACTERS, *BOSSES)
            if not only or asset.key in only
        )
    if args.batch in {"world-tree", "all"}:
        generated.extend(render_tree_state(damaged, output, args.keep_frames) for damaged in (False, True))
    if args.batch in {"equipment", "all"}:
        generated.extend(render_equipment(args.catalog.resolve(), output, args.keep_frames, only))
    if args.batch in {"environment", "all"}:
        generated.extend(render_environment(output))

    by_key = {entry["key"]: entry for entry in existing}
    by_key.update({entry["key"]: entry for entry in generated})
    manifest = {
        "pipelineVersion": PIPELINE_VERSION,
        "blenderVersion": BLENDER_VERSION,
        "styleGuide": "docs/VISUAL_STYLE_GUIDE.md",
        "frameRate": FRAME_RATE,
        "palette": PALETTE,
        "requiredBones": list(REQUIRED_BONES),
        "generatedBatch": args.batch,
        "assets": [by_key[key] for key in sorted(by_key)],
    }
    _write_json(output / "asset_manifest.json", manifest)
    print(f"Generated {len(generated)} assets for batch '{args.batch}' at {output}")


def _write_libgdx_atlas(
    path: Path,
    image_name: str,
    width: int,
    height: int,
    regions: dict[str, list[dict[str, int]]],
) -> None:
    lines = [
        image_name,
        f"size: {width},{height}",
        "format: RGBA8888",
        "filter: Nearest,Nearest",
        "repeat: none",
    ]
    for clip, frames in regions.items():
        for frame in frames:
            lines.extend([
                f"{path.stem}_{clip}",
                "  rotate: false",
                f"  xy: {frame['x']}, {frame['y']}",
                f"  size: {frame['width']}, {frame['height']}",
                f"  orig: {frame['width']}, {frame['height']}",
                "  offset: 0, 0",
                f"  index: {frame['index']}",
            ])
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def _read_existing_manifest(output: Path) -> list[dict]:
    path = output / "asset_manifest.json"
    if not path.exists():
        return []
    try:
        return json.loads(path.read_text(encoding="utf-8")).get("assets", [])
    except (ValueError, OSError):
        return []


def _fresh_directory(path: Path) -> None:
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True, exist_ok=True)


def _write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def _relative(path: Path, root: Path) -> str:
    return path.resolve().relative_to(root.resolve()).as_posix()


if __name__ == "__main__":
    main()
