#!/usr/bin/env python3
"""Audit one-shot effect strips and create deterministic review sheets."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw

from review_strips import grade_row, silhouette_view

EXPECTED_KEYS = ("vfx_impact_flash", "vfx_shockwave_ring")
EXPECTED_EFFECT = {"vfx_impact_flash": "impact_flash",
                   "vfx_shockwave_ring": "shockwave_ring"}
EXPECTED_FRAME_SIZE = 128
EXPECTED_PLAY_FRAMES = 8
EXPECTED_SHEET_SIZE = (1024, 128)
EXPECTED_MODEL_REVISION = "effects-v1"
MAX_TRIANGLES = 4000
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
        "generatedBatch": "vfx",
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
        raise ValueError(f"VFX candidate key mismatch: {sorted(by_key)}")

    audit_assets = []
    for key in EXPECTED_KEYS:
        audit_assets.append(audit_asset(candidate, manifest_path, by_key[key]))
    create_play_sheet(candidate, by_key, output / "vfx_play_strips.png")
    create_shape_sheet(candidate, by_key, output / "vfx_shape_grade.png")
    audit = {
        "schemaVersion": 1,
        "batch": "vfx",
        "candidateManifestSha256": sha256_file(manifest_path),
        "candidatePayload": payload_hashes(candidate),
        "assets": audit_assets,
        "reviewSheets": {
            "vfx_play_strips.png": record(output / "vfx_play_strips.png"),
            "vfx_shape_grade.png": record(output / "vfx_shape_grade.png"),
        },
        "reviewSheetCount": 2,
        "summary": {
            "assetCount": len(EXPECTED_KEYS),
            "playFrames": EXPECTED_PLAY_FRAMES,
            "minimumAlphaMargin": min(a["alphaMargin"] for a in audit_assets),
            "allPlayFramesUnique": all(a["uniquePlayFrames"] == EXPECTED_PLAY_FRAMES
                                       for a in audit_assets),
        },
    }
    (output / "vfx_audit.json").write_text(
        json.dumps(audit, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"Audited {len(EXPECTED_KEYS)} effects x {EXPECTED_PLAY_FRAMES} play frames")


def audit_asset(candidate: Path, manifest_path: Path, asset: dict) -> dict:
    key = asset["key"]
    expected = {
        "key": key,
        "family": "vfx",
        "builder": EXPECTED_EFFECT[key],
        "frameClass": "vfx",
        "frameSize": EXPECTED_FRAME_SIZE,
        "sheet": f"vfx/{key}.png",
        "atlas": f"vfx/{key}.atlas",
        "sheetWidth": EXPECTED_SHEET_SIZE[0],
        "sheetHeight": EXPECTED_SHEET_SIZE[1],
        "pivot": {"units": "normalized-bottom-left", "x": 0.5, "y": 0.5},
        "alphaMode": "STRAIGHT_RGBA",
        "frameRate": 12,
        "renderSupersample": 2,
        "renderSamples": 24,
        "modelRevision": EXPECTED_MODEL_REVISION,
        "effectKind": "vfx",
        "effect": EXPECTED_EFFECT[key],
        "boneAnimated": False,
        "visualQuality": "premium-v2",
    }
    for field, value in expected.items():
        if asset.get(field) != value:
            raise ValueError(f"{key}: {field} is {asset.get(field)!r}, "
                             f"expected {value!r}")
    metadata_path = resolve_inside(candidate, f"vfx/{key}.json")
    if json.loads(metadata_path.read_text(encoding="utf-8")) != asset:
        raise ValueError(f"Manifest/metadata mismatch: {key}")
    if asset.get("sheets") != [{
        "decodedBytes": EXPECTED_SHEET_SIZE[0] * EXPECTED_SHEET_SIZE[1] * 4,
        "file": f"vfx/{key}.png",
        "height": EXPECTED_SHEET_SIZE[1],
        "width": EXPECTED_SHEET_SIZE[0],
    }]:
        raise ValueError(f"{key}: atlas-page contract mismatch")
    frames = asset.get("clips", {}).get("play", [])
    if ([f["index"] for f in frames] != list(range(EXPECTED_PLAY_FRAMES))
            or any(f.get("page", 0) != 0 or f["width"] != EXPECTED_FRAME_SIZE
                   or f["height"] != EXPECTED_FRAME_SIZE for f in frames)):
        raise ValueError(f"{key}: play-strip frame contract mismatch")
    triangles = int(asset.get("triangles", 0))
    if not 0 < triangles <= MAX_TRIANGLES:
        raise ValueError(f"{key}: triangle budget mismatch {triangles}")

    sheet = Image.open(resolve_inside(candidate, asset["sheet"])).convert("RGBA")
    if sheet.size != EXPECTED_SHEET_SIZE:
        raise ValueError(f"{key}: sheet size mismatch {sheet.size}")
    hashes, margin = set(), 10_000
    for frame in frames:
        cell = sheet.crop((frame["x"], frame["y"],
                           frame["x"] + frame["width"], frame["y"] + frame["height"]))
        bbox = cell.getchannel("A").getbbox()
        if bbox is None:
            raise ValueError(f"{key}: empty play frame {frame['index']}")
        margin = min(margin, bbox[0], bbox[1],
                     cell.width - bbox[2], cell.height - bbox[3])
        hashes.add(hashlib.sha256(cell.tobytes()).hexdigest())
    if margin < MIN_ALPHA_MARGIN:
        raise ValueError(f"{key}: alpha margin {margin} is unsafe")
    if len(hashes) != EXPECTED_PLAY_FRAMES:
        raise ValueError(f"{key}: play strip is not fully animated")
    return {
        "key": key,
        "sheetSha256": sha256_file(candidate / asset["sheet"]),
        "metadataSha256": sha256_file(metadata_path),
        "triangles": triangles,
        "meshParts": asset.get("meshParts"),
        "materialCount": asset.get("materialCount"),
        "modelRevision": asset["modelRevision"],
        "alphaMargin": margin,
        "uniquePlayFrames": len(hashes),
    }


def frames_of(candidate: Path, asset: dict) -> list[Image.Image]:
    sheet = Image.open(candidate / asset["sheet"]).convert("RGBA")
    out = []
    for frame in sorted(asset["clips"]["play"], key=lambda f: f["index"]):
        out.append(sheet.crop((frame["x"], frame["y"],
                               frame["x"] + frame["width"],
                               frame["y"] + frame["height"])))
    return out


def create_play_sheet(candidate: Path, by_key: dict, output: Path) -> None:
    cols = EXPECTED_PLAY_FRAMES
    width = 40 + cols * (EXPECTED_FRAME_SIZE + 8)
    height = 60 + len(EXPECTED_KEYS) * (EXPECTED_FRAME_SIZE + 60)
    canvas = Image.new("RGB", (width, height), (16, 22, 26))
    draw = ImageDraw.Draw(canvas)
    draw.text((20, 12), "VFX PLAY STRIPS — 8 parametric frames each", fill=(255, 255, 255))
    y = 44
    for key in EXPECTED_KEYS:
        draw.text((20, y), key, fill=(175, 197, 190))
        for index, cell in enumerate(frames_of(candidate, by_key[key])):
            canvas.paste(cell, (40 + index * (EXPECTED_FRAME_SIZE + 8), y + 16), cell)
        y += EXPECTED_FRAME_SIZE + 60
    canvas.save(output, optimize=True)


def create_shape_sheet(candidate: Path, by_key: dict, output: Path) -> None:
    cols = EXPECTED_PLAY_FRAMES
    cell = 64
    width = 40 + cols * (cell + 8)
    per_effect = cell * 2 + 120
    height = 60 + len(EXPECTED_KEYS) * per_effect + 130
    canvas = Image.new("RGB", (width, height), (16, 22, 26))
    draw = ImageDraw.Draw(canvas)
    draw.text((20, 12), "VFX SHAPE + STAGE GRADE", fill=(255, 255, 255))
    y = 44
    for key in EXPECTED_KEYS:
        draw.text((20, y), f"{key} — frame / silhouette", fill=(175, 197, 190))
        for index, full in enumerate(frames_of(candidate, by_key[key])):
            small = full.resize((cell, cell), Image.LANCZOS)
            x = 40 + index * (cell + 8)
            canvas.paste(small, (x, y + 16), small)
            ghost = silhouette_view(full).resize((cell, cell), Image.LANCZOS)
            canvas.paste(ghost, (x, y + 20 + cell), ghost)
        y += per_effect
    grade = grade_row(frames_of(candidate, by_key[EXPECTED_KEYS[0]])[0])
    canvas.paste(grade.convert("RGB"), ((width - grade.width) // 2, y + 20))
    draw.text((width // 2, y), "STAGE GRADE — impact flash frame 0",
              fill=(255, 255, 255), anchor="ma")
    canvas.save(output, optimize=True)


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
