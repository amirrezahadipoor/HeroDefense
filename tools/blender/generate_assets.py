#!/usr/bin/env python3
"""Hero Defense deterministic Blender-to-sprite entry point.

Usage:
  blender --background --factory-startup --python tools/blender/generate_assets.py -- \
      --batch pilot --output android/assets/generated
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
import traceback
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import bpy  # noqa: E402

from hd_pipeline.atlas_layout import MAX_ATLAS_SIZE  # noqa: E402
from hd_pipeline.config import (  # noqa: E402
    BLENDER_VERSION,
    BOSSES,
    CLIPS,
    FRAME_RATE,
    FRAME_SIZE,
    OPAQUE_RENDER_SAMPLES,
    OVERLAY_RENDER_SAMPLES,
    PALETTE,
    REGULAR_CHARACTERS,
    RENDER_SUPERSAMPLE,
    REQUIRED_BONES,
    RenderAsset,
)
from hd_pipeline.environment import (  # noqa: E402
    build_crystal_prop,
    build_ground_tile,
    UI_ICON_KEYS,
    build_potion_icon,
    build_ui_icon,
    build_world_tree,
)
from hd_pipeline.models import MATERIALS, add_equipment_variant, build_character, build_hero  # noqa: E402
from hd_pipeline.rig import author_standard_actions, stack_actions_for_single_render  # noqa: E402
from hd_pipeline.scene import (  # noqa: E402
    apply_alpha_outline,
    configure_equipment_overlay_renderer,
    configure_scene,
    downsample_alpha_safe,
    make_fitted_icon,
    pack_grid,
    reset_scene,
    triangle_count,
)

PIPELINE_VERSION = 3
ISOLATED_RENDERING = False
PREMIUM_PILOT_EQUIPMENT_IDS = {
    "worldbranch",
    "crown_of_first_leaves",
    "heartwood_aegis",
    "boots_of_three_winds",
    "eternal_seed",
}
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
        choices=("pilot", "premium-pilot", "characters", "world-tree", "equipment", "environment", "ui", "all"),
        default="pilot",
    )
    parser.add_argument("--output", type=Path)
    parser.add_argument("--catalog", type=Path, default=SCRIPT_DIR / "equipment_visuals.json")
    parser.add_argument("--keep-frames", action="store_true")
    parser.add_argument("--only", nargs="*", default=[])
    parser.add_argument(
        "--isolate-frames",
        action="store_true",
        help="Render each frame in a short-lived Blender child (for low-memory software GL)",
    )
    parser.add_argument("--worker-payload", type=Path, help=argparse.SUPPRESS)
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
    if ISOLATED_RENDERING:
        frame_paths = {}
        for clip, count in CLIPS.items():
            frame_paths[clip] = []
            for index in range(count):
                target = frame_root / f"{asset.key}_base_{clip}_{index:02d}.png"
                _run_frame_worker({
                    "kind": "character",
                    "builder": asset.builder,
                    "key": asset.key,
                    "frameClass": asset.frame_class,
                    "clip": clip,
                    "frame": index + 1,
                    "output": str(target),
                })
                frame_paths[clip].append(target)
    else:
        global_frames = stack_actions_for_single_render(model.armature, actions)
        frame_paths = _render_stacked_animation(
            scene,
            frame_root,
            global_frames,
            lambda clip, index: f"{asset.key}_base_{clip}_{index:02d}.png",
        )

    sprite_directory = output / "sprites"
    sprite_directory.mkdir(parents=True, exist_ok=True)
    sheet_path = sprite_directory / f"{asset.key}.png"
    pages, regions = pack_grid(frame_paths, sheet_path, FRAME_SIZE[asset.frame_class])
    atlas_path = sprite_directory / f"{asset.key}.atlas"
    _write_libgdx_atlas(atlas_path, pages, regions)
    triangles = triangle_count(model.render_objects)
    entry = {
        "key": asset.key,
        "family": asset.family,
        "builder": asset.builder,
        "frameClass": asset.frame_class,
        "frameSize": FRAME_SIZE[asset.frame_class],
        "sheet": _relative(pages[0]["path"], output),
        "sheets": _sheet_manifest(pages, output),
        "atlas": _relative(atlas_path, output),
        "sheetWidth": pages[0]["width"],
        "sheetHeight": pages[0]["height"],
        "pivot": _pivot_for(asset.frame_class),
        "alphaMode": "STRAIGHT_RGBA",
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
    if ISOLATED_RENDERING:
        frame_paths = {"idle": []}
        for index in range(6):
            target = frame_root / f"{key}_idle_{index:02d}.png"
            _run_frame_worker({
                "kind": "tree",
                "damaged": damaged,
                "frameClass": "tree",
                "frame": index + 1,
                "output": str(target),
            })
            frame_paths["idle"].append(target)
    else:
        frame_paths = _render_stacked_animation(
            scene,
            frame_root,
            {"idle": list(range(1, 7))},
            lambda clip, index: f"{key}_{clip}_{index:02d}.png",
        )
    sprite_directory = output / "sprites"
    sprite_directory.mkdir(parents=True, exist_ok=True)
    sheet_path = sprite_directory / f"{key}.png"
    pages, regions = pack_grid(frame_paths, sheet_path, FRAME_SIZE["tree"])
    atlas_path = sprite_directory / f"{key}.atlas"
    _write_libgdx_atlas(atlas_path, pages, regions)
    entry = {
        "key": key,
        "family": "world_tree",
        "frameClass": "tree",
        "frameSize": FRAME_SIZE["tree"],
        "sheet": _relative(pages[0]["path"], output),
        "sheets": _sheet_manifest(pages, output),
        "atlas": _relative(atlas_path, output),
        "sheetWidth": pages[0]["width"],
        "sheetHeight": pages[0]["height"],
        "pivot": _pivot_for("tree"),
        "alphaMode": "STRAIGHT_RGBA",
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
        configure_equipment_overlay_renderer(scene)
        hero = build_hero()
        for obj in hero.render_objects:
            bpy.data.objects.remove(obj, do_unlink=True)
        equipment_objects = add_equipment_variant(
            hero.armature,
            item["visualSlot"],
            item_index,
            TIER_COLORS[item["tier"]],
            item.get("visualKind"),
            item["id"],
            item["tier"],
        )
        actions = author_standard_actions(hero.armature, key)
        if ISOLATED_RENDERING:
            frame_paths = {}
            for clip, count in CLIPS.items():
                frame_paths[clip] = []
                for frame_index in range(count):
                    target = frame_root / f"{key}_{clip}_{frame_index:02d}.png"
                    _run_frame_worker({
                        "kind": "equipment",
                        "key": key,
                        "frameClass": "character",
                        "clip": clip,
                        "frame": frame_index + 1,
                        "output": str(target),
                        "visualSlot": item["visualSlot"],
                        "variantIndex": item_index,
                        "tierColor": TIER_COLORS[item["tier"]],
                        "visualKind": item.get("visualKind"),
                        "itemId": item["id"],
                        "tier": item["tier"],
                        "samples": OVERLAY_RENDER_SAMPLES,
                    })
                    frame_paths[clip].append(target)
        else:
            global_frames = stack_actions_for_single_render(hero.armature, actions)
            frame_paths = _render_stacked_animation(
                scene,
                frame_root,
                global_frames,
                lambda clip, index: f"{key}_{clip}_{index:02d}.png",
            )
        sprite_directory = output / "equipment"
        sprite_directory.mkdir(parents=True, exist_ok=True)
        sheet_path = sprite_directory / f"{item['id']}.png"
        pages, regions = pack_grid(frame_paths, sheet_path, FRAME_SIZE["character"])
        atlas_path = sprite_directory / f"{item['id']}.atlas"
        _write_libgdx_atlas(atlas_path, pages, regions)
        icon_directory = output / "icons"
        icon_directory.mkdir(parents=True, exist_ok=True)
        icon_path = icon_directory / f"equipment_{item['id']}.png"
        make_fitted_icon(frame_paths["idle"][0], icon_path, FRAME_SIZE["item"], 8)
        entry = {
            "key": key,
            "family": "equipment",
            "itemId": item["id"],
            "slot": item["slot"],
            "visualSlot": item["visualSlot"],
            "visualKind": item.get("visualKind", item["visualSlot"]),
            "tier": item["tier"],
            "frameClass": "character",
            "frameSize": FRAME_SIZE["character"],
            "sheet": _relative(pages[0]["path"], output),
            "sheets": _sheet_manifest(pages, output),
            "sheetWidth": pages[0]["width"],
            "sheetHeight": pages[0]["height"],
            "atlas": _relative(atlas_path, output),
            "icon": _relative(icon_path, output),
            "pivot": _pivot_for("character"),
            "alphaMode": "STRAIGHT_RGBA",
            "clips": regions,
            "triangles": triangle_count(equipment_objects),
            "armature": hero.armature.name,
            "bones": sorted(bone.name for bone in hero.armature.data.bones),
            "boneAnimated": True,
            "runtimeGlow": item["tier"] in {"RARE", "LEGENDARY"},
            "modelRevision": "equipment-premium-v2",
            "rigProfile": "hero-socket-v2",
            "renderSupersample": RENDER_SUPERSAMPLE,
            "renderSamples": OVERLAY_RENDER_SAMPLES,
            "visualQuality": "premium-v2",
        }
        _write_json(sprite_directory / f"{item['id']}.json", entry)
        entries.append(entry)
        if not keep_frames:
            shutil.rmtree(frame_root)
    return entries


def _runtime_frame_size(scene: bpy.types.Scene) -> int:
    return scene.render.resolution_x // RENDER_SUPERSAMPLE


def _outline_radius(frame_size: int) -> int:
    return 3 if frame_size >= 256 else 2


def _render_stacked_animation(
    scene: bpy.types.Scene,
    frame_root: Path,
    global_frames: dict[str, list[int]],
    target_name,
) -> dict[str, list[Path]]:
    """Render all clip frames in one engine call, then give them semantic names."""
    all_frames = [frame for frames in global_frames.values() for frame in frames]
    scene.frame_start = min(all_frames)
    scene.frame_end = max(all_frames)
    scene.render.filepath = str(frame_root / "export_")
    bpy.ops.render.render(animation=True)

    result: dict[str, list[Path]] = {}
    for clip, frames in global_frames.items():
        result[clip] = []
        for index, global_frame in enumerate(frames):
            source = frame_root / f"export_{global_frame:04d}.png"
            target = frame_root / target_name(clip, index)
            if not source.exists():
                raise RuntimeError(f"Blender did not emit expected frame: {source}")
            source.replace(target)
            frame_size = _runtime_frame_size(scene)
            downsample_alpha_safe(target, frame_size)
            if not scene.render.use_freestyle:
                apply_alpha_outline(target, _outline_radius(frame_size))
            result[clip].append(target)
    return result


def render_static_model(
    key: str,
    family: str,
    frame_class: str,
    builder,
    output: Path,
    worker_payload: dict | None = None,
) -> dict:
    frame_root = output / "_frames" / key
    _fresh_directory(frame_root)
    reset_scene()
    MATERIALS.clear()
    scene = configure_scene(frame_class, frame_root)
    model = builder()
    path = frame_root / f"{key}_idle_00.png"
    if ISOLATED_RENDERING:
        if worker_payload is None:
            raise RuntimeError(f"Isolated static render {key} requires a worker payload")
        _run_frame_worker({
            **worker_payload,
            "kind": "static",
            "frameClass": frame_class,
            "output": str(path),
        })
    else:
        scene.frame_set(1)
        scene.render.filepath = str(path)
        bpy.ops.render.render(write_still=True)
        frame_size = _runtime_frame_size(scene)
        downsample_alpha_safe(path, frame_size)
        if not scene.render.use_freestyle:
            apply_alpha_outline(path, _outline_radius(frame_size))
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
        "sheets": [{
            "file": _relative(target, output),
            "width": FRAME_SIZE[frame_class],
            "height": FRAME_SIZE[frame_class],
            "decodedBytes": FRAME_SIZE[frame_class] * FRAME_SIZE[frame_class] * 4,
        }],
        "sheetWidth": FRAME_SIZE[frame_class],
        "sheetHeight": FRAME_SIZE[frame_class],
        "pivot": _pivot_for(frame_class),
        "alphaMode": "STRAIGHT_RGBA",
        "clips": {"idle": [{
            "page": 0,
            "x": 0,
            "y": 0,
            "width": FRAME_SIZE[frame_class],
            "height": FRAME_SIZE[frame_class],
            "index": 0,
        }]},
        "triangles": triangle_count(model.render_objects),
        **model.metadata,
    }
    _write_json(target_directory / f"{key}.json", entry)
    return entry


def render_environment(output: Path, only: set[str]) -> list[dict]:
    entries = []
    for variant in range(3):
        key = f"ground_tile_{variant}"
        if not only or key in only:
            entries.append(render_static_model(
                key, "environment", "environment",
                lambda value=variant: build_ground_tile(value), output,
                {"assetKind": "ground", "variant": variant},
            ))
    for variant in range(3):
        key = f"crystal_prop_{variant}"
        if not only or key in only:
            entries.append(render_static_model(
                key, "environment", "environment",
                lambda value=variant: build_crystal_prop(value), output,
                {"assetKind": "crystal", "variant": variant},
            ))
    for tier in range(1, 7):
        key = f"health_potion_{tier}"
        if not only or key in only:
            entries.append(render_static_model(
                key, "icons", "item",
                lambda value=tier: build_potion_icon(value), output,
                {"assetKind": "potion", "tier": tier},
            ))
    return entries


def render_ui(output: Path, only: set[str]) -> list[dict]:
    entries = []
    for key in UI_ICON_KEYS:
        if not only or key in only:
            entries.append(render_static_model(
                key, "icons", "item",
                lambda value=key: build_ui_icon(value), output,
                {"assetKind": "ui", "iconKey": key},
            ))
    return entries


def _run_frame_worker(payload: dict) -> None:
    output_path = Path(payload["output"])
    output_path.parent.mkdir(parents=True, exist_ok=True)
    handle, payload_name = tempfile.mkstemp(prefix="hd-frame-", suffix=".json", dir=output_path.parent)
    os.close(handle)
    payload_path = Path(payload_name)
    payload_path.write_text(json.dumps(payload), encoding="utf-8")
    command = [
        bpy.app.binary_path,
        "--background",
        "--factory-startup",
        "--python",
        str(Path(__file__).resolve()),
        "--",
        "--worker-payload",
        str(payload_path),
    ]
    print(
        f"Rendering isolated frame: {payload.get('key', payload.get('kind'))} "
        f"{payload.get('clip', 'idle')} {payload.get('frame', 1)}",
        flush=True,
    )
    try:
        result = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    finally:
        payload_path.unlink(missing_ok=True)
    if result.returncode != 0:
        tail = "\n".join(result.stdout.splitlines()[-100:])
        raise RuntimeError(f"Frame worker failed ({result.returncode}):\n{tail}")
    if not output_path.exists():
        raise RuntimeError(f"Frame worker returned without writing {output_path}")


def _execute_frame_worker(payload_path: Path) -> None:
    payload = json.loads(payload_path.read_text(encoding="utf-8"))
    output = Path(payload["output"]).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    reset_scene()
    MATERIALS.clear()
    scene = configure_scene(payload["frameClass"], output.parent)
    if payload["kind"] == "equipment":
        configure_equipment_overlay_renderer(scene)
    if "samples" in payload:
        scene.eevee.taa_render_samples = int(payload["samples"])
        scene.eevee.taa_samples = int(payload["samples"])
    frame = int(payload.get("frame", 1))

    if payload["kind"] == "character":
        model = build_character(payload["builder"])
        actions = author_standard_actions(model.armature, payload["key"])
        model.armature.animation_data.action = actions[payload["clip"]]
    elif payload["kind"] == "equipment":
        hero = build_hero()
        for obj in hero.render_objects:
            bpy.data.objects.remove(obj, do_unlink=True)
        add_equipment_variant(
            hero.armature,
            payload["visualSlot"],
            int(payload["variantIndex"]),
            payload["tierColor"],
            payload.get("visualKind"),
            payload["itemId"],
            payload["tier"],
        )
        actions = author_standard_actions(hero.armature, payload["key"])
        hero.armature.animation_data.action = actions[payload["clip"]]
    elif payload["kind"] == "tree":
        build_world_tree(bool(payload["damaged"]))
    elif payload["kind"] == "static":
        asset_kind = payload["assetKind"]
        if asset_kind == "ground":
            build_ground_tile(int(payload["variant"]))
        elif asset_kind == "crystal":
            build_crystal_prop(int(payload["variant"]))
        elif asset_kind == "potion":
            build_potion_icon(int(payload["tier"]))
        elif asset_kind == "ui":
            build_ui_icon(payload["iconKey"])
        else:
            raise ValueError(f"Unknown static worker asset: {asset_kind}")
    else:
        raise ValueError(f"Unknown frame worker kind: {payload['kind']}")

    scene.frame_set(frame)
    scene.render.filepath = str(output)
    bpy.ops.render.render(write_still=True)
    frame_size = _runtime_frame_size(scene)
    downsample_alpha_safe(output, frame_size)
    if not scene.render.use_freestyle:
        apply_alpha_outline(output, _outline_radius(frame_size))


def main() -> None:
    global ISOLATED_RENDERING
    args = parse_args()
    if args.worker_payload:
        _execute_frame_worker(args.worker_payload.resolve())
        return
    if args.output is None:
        raise SystemExit("--output is required for batch generation")
    ISOLATED_RENDERING = args.isolate_frames
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    existing = _read_existing_manifest(output)
    generated: list[dict] = []
    only = set(args.only)

    if args.batch == "premium-pilot":
        pilot_characters = (REGULAR_CHARACTERS[0], REGULAR_CHARACTERS[1], BOSSES[0])
        generated.extend(render_character(asset, output, args.keep_frames) for asset in pilot_characters)
        generated.extend(render_equipment(
            args.catalog.resolve(), output, args.keep_frames, PREMIUM_PILOT_EQUIPMENT_IDS
        ))
        generated.extend(render_environment(output, {"health_potion_6", "crystal_prop_0"}))
        generated.extend(render_ui(output, {"ui_inventory"}))

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
        generated.extend(render_environment(output, only))
    if args.batch in {"ui", "all"}:
        generated.extend(render_ui(output, only))

    by_key = {entry["key"]: entry for entry in existing}
    by_key.update({entry["key"]: entry for entry in generated})
    manifest = {
        "pipelineVersion": PIPELINE_VERSION,
        "blenderVersion": BLENDER_VERSION,
        "styleGuide": "docs/VISUAL_STYLE_GUIDE.md",
        "frameRate": FRAME_RATE,
        "renderSupersample": RENDER_SUPERSAMPLE,
        "opaqueRenderSamples": OPAQUE_RENDER_SAMPLES,
        "overlayRenderSamples": OVERLAY_RENDER_SAMPLES,
        "maxAtlasPageSize": MAX_ATLAS_SIZE,
        "decodedCatalogBudgetBytes": 335_544_320,
        "decodedCombatResidencyBudgetBytes": 134_217_728,
        "palette": PALETTE,
        "requiredBones": list(REQUIRED_BONES),
        "generatedBatch": args.batch,
        "assets": [by_key[key] for key in sorted(by_key)],
    }
    _write_json(output / "asset_manifest.json", manifest)
    print(f"Generated {len(generated)} assets for batch '{args.batch}' at {output}")


def _write_libgdx_atlas(
    path: Path,
    pages: list[dict[str, object]],
    regions: dict[str, list[dict[str, int]]],
) -> None:
    lines: list[str] = []
    for page in pages:
        if lines:
            lines.append("")
        lines.extend([
            Path(page["path"]).name,
            f"size: {page['width']},{page['height']}",
            "format: RGBA8888",
            "filter: Nearest,Nearest",
            "repeat: none",
        ])
        for clip, frames in regions.items():
            for frame in frames:
                if frame["page"] != page["index"]:
                    continue
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


def _sheet_manifest(pages: list[dict[str, object]], output: Path) -> list[dict[str, object]]:
    return [{
        "file": _relative(Path(page["path"]), output),
        "width": page["width"],
        "height": page["height"],
        "decodedBytes": page["decodedBytes"],
    } for page in pages]


def _pivot_for(frame_class: str) -> dict[str, object]:
    values = {
        "character": (0.5, 0.12),
        "boss": (0.5, 0.12),
        "tree": (0.5, 0.06),
        "item": (0.5, 0.5),
        "environment": (0.5, 0.5),
    }
    x, y = values[frame_class]
    return {"x": x, "y": y, "units": "normalized-bottom-left"}


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
    try:
        main()
    except Exception:
        traceback.print_exc()
        raise SystemExit(1)
