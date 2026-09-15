#!/usr/bin/env python3
"""Audit single-frame projectile sprites and create readability sheets."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw

from review_strips import grade_row, silhouette_view

EXPECTED_KEYS = ("projectile_arrow",)
EXPECTED_FRAME_SIZE = 64
EXPECTED_MODEL_REVISION = "effects-v1"
MAX_TRIANGLES = 2000
MIN_ALPHA_MARGIN = 4


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    candidate = args.candidate.resolve()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)

    manifest_path = candidate / "asset_manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    for field, expected in {
        "pipelineVersion": 3,
        "generatedBatch": "projectile",
        "frameRate": 12,
        "renderSupersample": 2,
        "opaqueRenderSamples": 24,
        "overlayRenderSamples": 8,
        "renderTierTop": [3, 32],
        "maxAtlasPageSize": 2048,
    }.items():
        if manifest.get(field) != expected:
            raise ValueError(f"Candidate {field} is {manifest.get(field)!r}; "
                             f"expected {expected!r}")
    assets = manifest["assets"]
    by_key = {asset["key"]: asset for asset in assets}
    if set(by_key) != set(EXPECTED_KEYS) or len(by_key) != len(assets):
        raise ValueError(f"Projectile candidate key mismatch: {sorted(by_key)}")

    audit = {
        "schemaVersion": 1,
        "batch": "projectile",
        "candidateManifestSha256": sha256_file(manifest_path),
        "candidatePayload": payload_hashes(candidate),
        "assets": [audit_asset(candidate, by_key[key]) for key in EXPECTED_KEYS],
        "reviewSheets": {
            "projectile_readability.png": record(
                create_readability_sheet(candidate, by_key, output)),
        },
        "reviewSheetCount": 1,
        "summary": {
            "assetCount": len(EXPECTED_KEYS),
            "minimumAlphaMargin": min(
                a["alphaMargin"] for a in [audit_asset(candidate, by_key[key])
                                           for key in EXPECTED_KEYS]),
        },
    }
    (output / "projectile_audit.json").write_text(
        json.dumps(audit, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"Audited {len(EXPECTED_KEYS)} projectile sprite")


def audit_asset(candidate: Path, asset: dict) -> dict:
    key = asset["key"]
    expected = {
        "key": key,
        "family": "projectile",
        "frameClass": "projectile",
        "frameSize": EXPECTED_FRAME_SIZE,
        "frameWidth": EXPECTED_FRAME_SIZE,
        "frameHeight": EXPECTED_FRAME_SIZE,
        "sheet": f"projectile/{key}.png",
        "sheetWidth": EXPECTED_FRAME_SIZE,
        "sheetHeight": EXPECTED_FRAME_SIZE,
        "pivot": {"units": "normalized-bottom-left", "x": 0.5, "y": 0.5},
        "alphaMode": "STRAIGHT_RGBA",
        "renderSupersample": 2,
        "renderSamples": 24,
        "modelRevision": EXPECTED_MODEL_REVISION,
        "effectKind": "projectile",
        "variant": "normal",
        "flightAxis": "+X",
        "visualQuality": "premium-v2",
    }
    for field, value in expected.items():
        if asset.get(field) != value:
            raise ValueError(f"{key}: {field} is {asset.get(field)!r}, "
                             f"expected {value!r}")
    metadata_path = resolve_inside(candidate, f"projectile/{key}.json")
    if json.loads(metadata_path.read_text(encoding="utf-8")) != asset:
        raise ValueError(f"Manifest/metadata mismatch: {key}")
    if asset.get("sheets") != [{
        "decodedBytes": EXPECTED_FRAME_SIZE * EXPECTED_FRAME_SIZE * 4,
        "file": f"projectile/{key}.png",
        "height": EXPECTED_FRAME_SIZE,
        "width": EXPECTED_FRAME_SIZE,
    }]:
        raise ValueError(f"{key}: atlas-page contract mismatch")
    if asset.get("clips") != {"idle": [{
        "height": 64, "index": 0, "page": 0, "width": 64, "x": 0, "y": 0,
    }]}:
        raise ValueError(f"{key}: single-frame contract mismatch")
    triangles = int(asset.get("triangles", 0))
    if not 0 < triangles <= MAX_TRIANGLES:
        raise ValueError(f"{key}: triangle budget mismatch {triangles}")

    sprite = Image.open(resolve_inside(candidate, asset["sheet"])).convert("RGBA")
    if sprite.size != (EXPECTED_FRAME_SIZE, EXPECTED_FRAME_SIZE):
        raise ValueError(f"{key}: sheet size mismatch {sprite.size}")
    bbox = sprite.getchannel("A").getbbox()
    if bbox is None:
        raise ValueError(f"{key}: empty sprite")
    margin = min(bbox[0], bbox[1], sprite.width - bbox[2], sprite.height - bbox[3])
    if margin < MIN_ALPHA_MARGIN:
        raise ValueError(f"{key}: alpha margin {margin} is unsafe")
    # The head tip must read as a real point at +X, not a blunt bar; flight
    # direction itself is fixed by construction and confirmed on the sheet.
    alpha = sprite.getchannel("A")
    bbox = alpha.getbbox()
    tip_column = [alpha.getpixel((bbox[2] - 1, y)) for y in range(sprite.height)]
    if sum(1 for v in tip_column if v > 0) > 3:
        raise ValueError(f"{key}: head tip is blunt")
    return {
        "key": key,
        "sheetSha256": sha256_file(candidate / asset["sheet"]),
        "metadataSha256": sha256_file(metadata_path),
        "triangles": triangles,
        "meshParts": asset.get("meshParts"),
        "materialCount": asset.get("materialCount"),
        "modelRevision": asset["modelRevision"],
        "alphaMargin": margin,
    }


def create_readability_sheet(candidate: Path, by_key: dict, output: Path) -> Path:
    sprite = Image.open(
        candidate / by_key[EXPECTED_KEYS[0]]["sheet"]).convert("RGBA")
    width, height = 760, 560
    canvas = Image.new("RGB", (width, height), (16, 22, 26))
    draw = ImageDraw.Draw(canvas)
    draw.text((20, 12), "PROJECTILE READABILITY — shaft/head/fletching must read",
              fill=(255, 255, 255))
    panels = [
        ("1x", sprite),
        ("2x NEAREST", sprite.resize((128, 128), Image.NEAREST)),
        ("4x NEAREST", sprite.resize((256, 256), Image.NEAREST)),
    ]
    x = 20
    for caption, panel in panels:
        draw.text((x, 44), caption, fill=(175, 197, 190))
        canvas.paste(panel, (x, 64), panel)
        x += panel.width + 24
    ghost = silhouette_view(sprite).resize((256, 256), Image.NEAREST)
    draw.text((20, 340), "silhouette", fill=(175, 197, 190))
    canvas.paste(ghost, (20, 360), ghost)
    grade = grade_row(sprite)
    canvas.paste(grade.convert("RGB"), (300, 360 + (256 - grade.height) // 2))
    draw.text((300, 340), "stage grade", fill=(175, 197, 190))
    path = output / "projectile_readability.png"
    canvas.save(path, optimize=True)
    return path


def resolve_inside(root: Path, relative: str) -> Path:
    path = (root / relative).resolve()
    if not path.is_relative_to(root.resolve()) or not path.is_file():
        raise FileNotFoundError(path)
    return path


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def payload_hashes(candidate: Path) -> dict[str, str]:
    return {path.relative_to(candidate).as_posix(): sha256_file(path)
            for path in sorted(candidate.rglob("*")) if path.is_file()
            and "_frames" not in path.parts}


def record(path: Path) -> dict[str, object]:
    return {"bytes": path.stat().st_size, "sha256": sha256_file(path)}


if __name__ == "__main__":
    main()
