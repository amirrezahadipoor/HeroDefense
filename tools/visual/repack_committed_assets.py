#!/usr/bin/env python3
"""One-time/current-catalog migration to the bounded premium-v2 atlas contract.

Requires Pillow only when intentionally repacking already committed rendered output.
Normal deterministic generation uses Blender's image API and does not require Pillow.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from PIL import Image

REPOSITORY = Path(__file__).resolve().parents[2]
BLENDER_TOOLS = REPOSITORY / "tools" / "blender"
sys.path.insert(0, str(BLENDER_TOOLS))

from hd_pipeline.atlas_layout import MAX_ATLAS_SIZE, plan_grid

CLIP_ORDER = ("idle", "attack", "hit", "death")
PIVOTS = {
    "character": (0.5, 0.12),
    "boss": (0.5, 0.12),
    "tree": (0.5, 0.06),
    "item": (0.5, 0.5),
    "environment": (0.5, 0.5),
}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root",
        type=Path,
        default=REPOSITORY / "android" / "assets" / "generated",
    )
    args = parser.parse_args()
    root = args.root.resolve()
    manifest_path = root / "asset_manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    assets = {entry["key"]: entry for entry in manifest["assets"]}

    for entry in assets.values():
        json_path = _asset_json_path(root, entry)
        asset = json.loads(json_path.read_text(encoding="utf-8"))
        if asset.get("family") == "boss":
            _repack_boss(root, asset, json_path)
        _apply_contract(root, asset)
        _write_json(json_path, asset)
        entry.clear()
        entry.update(asset)

    # This legacy page-layout migration must never downgrade newer render provenance.
    manifest["pipelineVersion"] = max(2, int(manifest.get("pipelineVersion", 0)))
    manifest["maxAtlasPageSize"] = MAX_ATLAS_SIZE
    manifest["decodedCatalogBudgetBytes"] = 335_544_320
    manifest["decodedCombatResidencyBudgetBytes"] = 134_217_728
    manifest["assets"] = [assets[key] for key in sorted(assets)]
    _write_json(manifest_path, manifest)
    print(f"Migrated {len(assets)} assets to premium-v2 atlas metadata")


def _asset_json_path(root: Path, entry: dict) -> Path:
    family = entry["family"]
    if family in {"hero", "enemy", "boss", "world_tree"}:
        return root / "sprites" / f"{entry['key']}.json"
    if family == "equipment":
        return root / "equipment" / f"{entry['itemId']}.json"
    return root / family / f"{entry['key']}.json"


def _repack_boss(root: Path, asset: dict, json_path: Path) -> None:
    old_sheet_path = root / asset["sheet"]
    old_sheet = Image.open(old_sheet_path).convert("RGBA")
    counts = {clip: len(asset["clips"][clip]) for clip in CLIP_ORDER}
    pages, regions = plan_grid(counts, asset["frameSize"])
    if len(pages) != 1:
        raise RuntimeError("Current boss catalog should fit one bounded runtime page")
    page = pages[0]
    new_sheet = Image.new("RGBA", (page["width"], page["height"]), (0, 0, 0, 0))

    for clip in CLIP_ORDER:
        old_frames = sorted(asset["clips"][clip], key=lambda frame: frame["index"])
        for old, new in zip(old_frames, regions[clip], strict=True):
            source = old_sheet.crop((
                old["x"], old["y"],
                old["x"] + old["width"], old["y"] + old["height"],
            ))
            new_sheet.paste(source, (new["x"], new["y"]))
            if new_sheet.crop((
                new["x"], new["y"],
                new["x"] + new["width"], new["y"] + new["height"],
            )).tobytes() != source.tobytes():
                raise RuntimeError(f"Pixel mismatch while repacking {asset['key']} {clip}")

    temporary = old_sheet_path.with_suffix(".repacked.png")
    new_sheet.save(temporary, format="PNG", compress_level=9)
    temporary.replace(old_sheet_path)
    asset["clips"] = regions
    _write_atlas(root / asset["atlas"], old_sheet_path.name, page, regions)
    print(f"Pixel-exact repack: {asset['key']} {old_sheet.size} -> {new_sheet.size}")


def _apply_contract(root: Path, asset: dict) -> None:
    sheet_path = root / asset["sheet"]
    with Image.open(sheet_path) as image:
        width, height = image.size
        if image.mode != "RGBA":
            raise RuntimeError(f"Expected straight RGBA PNG: {sheet_path}")
    if width > MAX_ATLAS_SIZE or height > MAX_ATLAS_SIZE:
        raise RuntimeError(f"Oversized committed page after repack: {sheet_path} {width}x{height}")
    asset["sheetWidth"] = width
    asset["sheetHeight"] = height
    asset["sheets"] = [{
        "file": asset["sheet"],
        "width": width,
        "height": height,
        "decodedBytes": width * height * 4,
    }]
    x, y = PIVOTS[asset["frameClass"]]
    asset["pivot"] = {"x": x, "y": y, "units": "normalized-bottom-left"}
    asset["alphaMode"] = "STRAIGHT_RGBA"
    for frames in asset["clips"].values():
        for frame in frames:
            frame["page"] = frame.get("page", 0)


def _write_atlas(path: Path, image_name: str, page: dict, regions: dict) -> None:
    lines = [
        image_name,
        f"size: {page['width']},{page['height']}",
        "format: RGBA8888",
        "filter: Nearest,Nearest",
        "repeat: none",
    ]
    for clip in CLIP_ORDER:
        for frame in regions[clip]:
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


def _write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
