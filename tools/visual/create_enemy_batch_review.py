#!/usr/bin/env python3
"""Audit and render exhaustive review evidence for the four regular enemies."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw

from create_character_animation_review import (
    CLIP_ORDER,
    CharacterFrames,
    canvas_base,
    create_motion_sheet,
    create_readability_sheet,
    presentation_card,
    text,
)

ENEMIES = (
    ("rootling", "Rootling", "rootling-thorn-scout-v2", "premium-humanoid-v2", "rootling-skirmisher-v2"),
    ("stonekin", "Stonekin", "stonekin-rune-bulwark-v2", "premium-heavy-humanoid-v2", "stonekin-juggernaut-v2"),
    ("gloom_wolf", "Gloom Wolf", "gloom-wolf-shadow-stalker-v2", "premium-quadruped-mapped-v2", "gloom-wolf-pouncer-v2"),
    ("fungal_brute", "Fungal Brute", "fungal-brute-spore-bruiser-v2", "premium-heavy-humanoid-v2", "fungal-brute-brawler-v2"),
)
EXPECTED_CLIPS = {"idle": 6, "attack": 8, "hit": 4, "death": 10}
MIN_UNIQUE = {"idle": 5, "attack": 7, "hit": 3, "death": 9}
EXPECTED_PIVOT = {"units": "normalized-bottom-left", "x": 0.5, "y": 0.12}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    baseline = args.baseline.resolve()
    candidate = args.candidate.resolve()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)

    audit = audit_batch(baseline, candidate)
    for key, label, *_ in ENEMIES:
        old = CharacterFrames(baseline, key)
        new = CharacterFrames(candidate, key)
        create_motion_sheet(new, label, output / f"{key}_full_motion.png")
        create_readability_sheet(old, new, label, output / f"{key}_readability.png")
    create_lineup_sheet(baseline, candidate, audit, output / "regular_enemies_lineup.png")

    sheet_paths = sorted(output.glob("*.png"))
    audit["reviewSheets"] = {
        path.name: {"sha256": sha256(path), "bytes": path.stat().st_size}
        for path in sheet_paths
    }
    audit["reviewSheetCount"] = len(sheet_paths)
    audit_path = output / "regular_enemies_audit.json"
    audit_path.write_text(json.dumps(audit, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(
        f"Audited {audit['summary']['assetCount']} enemies and "
        f"{audit['summary']['frameCount']} frames; wrote {len(sheet_paths)} review sheets to {output}"
    )


def audit_batch(baseline: Path, candidate: Path) -> dict:
    baseline_manifest_path = baseline / "asset_manifest.json"
    candidate_manifest_path = candidate / "asset_manifest.json"
    baseline_manifest = read_json(baseline_manifest_path)
    candidate_manifest = read_json(candidate_manifest_path)
    expected_keys = [record[0] for record in ENEMIES]
    candidate_keys = sorted(entry["key"] for entry in candidate_manifest["assets"])
    if candidate_keys != sorted(expected_keys):
        raise ValueError(f"Candidate must contain exactly {expected_keys}; found {candidate_keys}")
    if candidate_manifest.get("pipelineVersion", 0) < 3:
        raise ValueError("Candidate pipelineVersion must be at least 3")
    if candidate_manifest.get("frameRate") != 12:
        raise ValueError("Candidate frame rate must remain 12 fps")
    if candidate_manifest.get("renderSupersample") != 2:
        raise ValueError("Candidate must use 2x supersampling")
    if candidate_manifest.get("opaqueRenderSamples") != 16:
        raise ValueError("Candidate must use 16 opaque samples")

    required_bones = candidate_manifest.get("requiredBones", [])
    if len(required_bones) != 25 or len(set(required_bones)) != 25:
        raise ValueError("Enemy rigs must expose the locked 25-bone contract")

    baseline_entries = {entry["key"]: entry for entry in baseline_manifest["assets"]}
    candidate_entries = {entry["key"]: entry for entry in candidate_manifest["assets"]}
    records = []
    total_frames = 0
    global_margins = {side: 10_000 for side in ("left", "top", "right", "bottom")}
    total_decoded = 0

    for key, label, revision, rig_profile, animation_profile in ENEMIES:
        if key not in baseline_entries:
            raise ValueError(f"Baseline manifest is missing {key}")
        entry = candidate_entries[key]
        validate_metadata(entry, key, revision, rig_profile, animation_profile, required_bones)
        metadata_entry = read_json(candidate / "sprites" / f"{key}.json")
        if metadata_entry != entry:
            raise ValueError(f"{key} manifest and per-asset metadata differ")
        baseline_character = CharacterFrames(baseline, key)
        candidate_character = CharacterFrames(candidate, key)
        clip_records = {}
        asset_margins = {side: 10_000 for side in global_margins}
        all_hashes: list[str] = []

        for clip, expected_count in EXPECTED_CLIPS.items():
            regions = sorted(entry["clips"].get(clip, []), key=lambda value: value["index"])
            if len(regions) != expected_count:
                raise ValueError(f"{key} {clip} has {len(regions)} frames, expected {expected_count}")
            if [region["index"] for region in regions] != list(range(expected_count)):
                raise ValueError(f"{key} {clip} frame indices are not contiguous")
            frame_hashes = []
            clip_margins = {side: 10_000 for side in global_margins}
            for index, region in enumerate(regions):
                if region.get("page", 0) != 0:
                    raise ValueError(f"{key} must remain a single-page atlas")
                if region["width"] != 192 or region["height"] != 192:
                    raise ValueError(f"{key} {clip}/{index} is not a native 192 px frame")
                frame = candidate_character.frame(clip, index)
                alpha_box = frame.getchannel("A").getbbox()
                if alpha_box is None:
                    raise ValueError(f"{key} {clip}/{index} is empty")
                left, top, right, bottom = alpha_box
                margins = {
                    "left": left,
                    "top": top,
                    "right": frame.width - right,
                    "bottom": frame.height - bottom,
                }
                if min(margins.values()) < 2:
                    raise ValueError(f"{key} {clip}/{index} approaches the frame boundary: {margins}")
                for side, margin in margins.items():
                    clip_margins[side] = min(clip_margins[side], margin)
                    asset_margins[side] = min(asset_margins[side], margin)
                    global_margins[side] = min(global_margins[side], margin)
                digest = hashlib.sha256(frame.tobytes()).hexdigest()
                frame_hashes.append(digest)
                all_hashes.append(digest)
            unique_count = len(set(frame_hashes))
            if unique_count < MIN_UNIQUE[clip]:
                raise ValueError(f"{key} {clip} has only {unique_count} unique visible frames")
            clip_records[clip] = {
                "frameCount": expected_count,
                "uniqueVisibleFrames": unique_count,
                "minimumAlphaMargins": clip_margins,
            }
            total_frames += expected_count

        baseline_sheet = baseline / baseline_entries[key]["sheet"]
        candidate_sheet = candidate / entry["sheet"]
        candidate_atlas = candidate / entry["atlas"]
        candidate_metadata = candidate / "sprites" / f"{key}.json"
        baseline_hash = sha256(baseline_sheet)
        candidate_hash = sha256(candidate_sheet)
        if baseline_hash == candidate_hash:
            raise ValueError(f"{key} candidate sheet is byte-identical to the baseline")
        if len(set(all_hashes)) < 20:
            raise ValueError(f"{key} has insufficient full-set motion diversity")
        for sheet in entry["sheets"]:
            total_decoded += int(sheet["width"]) * int(sheet["height"]) * 4

        records.append({
            "key": key,
            "label": label,
            "baselineSheetSha256": baseline_hash,
            "candidateSheetSha256": candidate_hash,
            "candidateAtlasSha256": sha256(candidate_atlas),
            "candidateMetadataSha256": sha256(candidate_metadata),
            "modelRevision": revision,
            "rigProfile": rig_profile,
            "animationProfile": animation_profile,
            "triangles": entry["triangles"],
            "meshParts": entry["meshParts"],
            "materialCount": entry["materialCount"],
            "rigBoneCount": entry["rigBoneCount"],
            "minimumAlphaMargins": asset_margins,
            "clips": clip_records,
        })

    decoded_limit = 32 * 1024 * 1024
    if total_decoded > decoded_limit:
        raise ValueError(f"Enemy batch decodes to {total_decoded} bytes, over {decoded_limit}")
    return {
        "schemaVersion": 1,
        "batch": "regular-enemies-premium-v2",
        "baselineManifestSha256": sha256(baseline_manifest_path),
        "candidateManifestSha256": sha256(candidate_manifest_path),
        "expectedKeys": expected_keys,
        "frameContract": EXPECTED_CLIPS,
        "minimumUniqueVisibleFrames": MIN_UNIQUE,
        "assets": records,
        "summary": {
            "assetCount": len(records),
            "frameCount": total_frames,
            "singlePageAtlasCount": len(records),
            "decodedBytes": total_decoded,
            "decodedBudgetBytes": decoded_limit,
            "minimumAlphaMargins": global_margins,
            "minimumTriangles": min(record["triangles"] for record in records),
            "maximumTriangles": max(record["triangles"] for record in records),
            "minimumMeshParts": min(record["meshParts"] for record in records),
            "minimumMaterialCount": min(record["materialCount"] for record in records),
        },
    }


def validate_metadata(
    entry: dict,
    key: str,
    revision: str,
    rig_profile: str,
    animation_profile: str,
    required_bones: list[str],
) -> None:
    expected = {
        "family": "enemy",
        "builder": key,
        "frameClass": "character",
        "frameSize": 192,
        "sheetWidth": 1920,
        "sheetHeight": 768,
        "pivot": EXPECTED_PIVOT,
        "alphaMode": "STRAIGHT_RGBA",
        "frameRate": 12,
        "renderSupersample": 2,
        "renderSamples": 16,
        "boneAnimated": True,
        "visualQuality": "premium-v2",
        "modelRevision": revision,
        "rigProfile": rig_profile,
        "animationProfile": animation_profile,
    }
    for field, value in expected.items():
        if entry.get(field) != value:
            raise ValueError(f"{key} {field} is {entry.get(field)!r}; expected {value!r}")
    if entry.get("bones") != sorted(required_bones):
        raise ValueError(f"{key} does not match the locked rig bone names")
    if entry.get("rigBoneCount") != 25:
        raise ValueError(f"{key} must retain all 25 bones")
    if not 900 <= int(entry.get("triangles", 0)) <= 4_000:
        raise ValueError(f"{key} triangle count is outside the premium enemy budget")
    if int(entry.get("meshParts", 0)) < 32:
        raise ValueError(f"{key} requires at least 32 purposeful mesh parts")
    if int(entry.get("materialCount", 0)) < 6:
        raise ValueError(f"{key} requires at least six coherent materials")
    if len(entry.get("sheets", [])) != 1:
        raise ValueError(f"{key} must remain on one atlas page")
    if not entry.get("silhouette") or not entry.get("materialStory"):
        raise ValueError(f"{key} is missing art-direction metadata")


def create_lineup_sheet(baseline: Path, candidate: Path, audit: dict, output: Path) -> None:
    width, height = 1780, 910
    canvas = canvas_base(width, height, "REGULAR ENEMIES — PREMIUM V2 LINEUP & GAMEPLAY HIERARCHY")
    draw = ImageDraw.Draw(canvas)
    text(
        draw,
        (width // 2, 77),
        "One shared scale: baseline idle, premium idle, and premium attack impact for every regular enemy.",
        18,
        anchor="ma",
        color="#AFC5BE",
    )
    row_labels = ("BASELINE", "PREMIUM IDLE", "ATTACK IMPACT")
    for row, label in enumerate(row_labels):
        y = 115 + row * 245
        text(draw, (30, y + 96), label, 18, bold=True, anchor="lm", color="#F2D58A")
    audit_by_key = {entry["key"]: entry for entry in audit["assets"]}
    for column, (key, label, *_rest) in enumerate(ENEMIES):
        old = CharacterFrames(baseline, key)
        new = CharacterFrames(candidate, key)
        x = 185 + column * 390
        text(draw, (x + 175, 104), label.upper(), 22, bold=True, anchor="ma")
        frames = (old.frame("idle", 0), new.frame("idle", 0), new.frame("attack", 4))
        for row, sprite in enumerate(frames):
            y = 115 + row * 245
            card = presentation_card(sprite, "checker", 350, 205)
            canvas.paste(card.convert("RGB"), (x, y))
            draw.rounded_rectangle((x, y, x + 350, y + 205), 10, outline="#58706A", width=2)
        record = audit_by_key[key]
        text(
            draw,
            (x + 175, 862),
            f"{record['triangles']:,} tris  •  {record['meshParts']} parts  •  {record['materialCount']} materials  •  25-bone rig",
            15,
            anchor="ma",
            color="#AFC5BE",
        )
    canvas.save(output, optimize=True)


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


if __name__ == "__main__":
    main()
