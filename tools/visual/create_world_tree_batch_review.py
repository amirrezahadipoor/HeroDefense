#!/usr/bin/env python3
"""Audit every World Tree frame and build deterministic premium-v2 review evidence."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageOps

from create_character_animation_review import (
    CharacterFrames,
    canvas_base,
    checker,
    presentation_card,
    text,
)

TREES = (
    (
        "world_tree_healthy",
        "Healthy Heartwood Sanctum",
        "heartwood-sanctum-healthy-v2",
        "living-heart-pulse-v2",
    ),
    (
        "world_tree_damaged",
        "Wounded Heartwood Sanctum",
        "heartwood-sanctum-wounded-v2",
        "wounded-collapse-v2",
    ),
)
EXPECTED_CLIPS = {
    "world_tree_healthy": {"idle": 6},
    "world_tree_damaged": {"idle": 6, "destroy": 10},
}
MINIMUM_UNIQUE = {
    "world_tree_healthy": {"idle": 5},
    "world_tree_damaged": {"idle": 5, "destroy": 8},
}
EXPECTED_BONES = sorted((
    "root",
    "trunk.lower",
    "trunk.upper",
    "crown",
    "branch.L",
    "branch.R",
    "bough.L",
    "bough.R",
    "canopy.L",
    "canopy.R",
    "heart",
    "debris.L",
    "debris.R",
))
EXPECTED_PIVOT = {"units": "normalized-bottom-left", "x": 0.5, "y": 0.06}
DECODED_BUDGET = 8 * 1024 * 1024


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
    healthy = CharacterFrames(candidate, "world_tree_healthy")
    damaged = CharacterFrames(candidate, "world_tree_damaged")
    create_motion_sheet(
        healthy,
        "HEALTHY HEARTWOOD SANCTUM — COMPLETE NATIVE-SIZE MOTION",
        ("idle",),
        output / "world_tree_healthy_full_motion.png",
    )
    create_motion_sheet(
        damaged,
        "WOUNDED HEARTWOOD SANCTUM — IDLE & COMPLETE DESTRUCTION",
        ("idle", "destroy"),
        output / "world_tree_damaged_full_motion.png",
    )
    create_state_lineup(baseline, candidate, audit, output / "world_tree_state_lineup.png")
    create_readability_sheet(baseline, candidate, output / "world_tree_readability.png")
    create_destruction_timeline(damaged, output / "world_tree_destruction_timeline.png")
    create_arena_scale_sheet(baseline, candidate, output / "world_tree_arena_scale.png")

    sheets = sorted(output.glob("*.png"))
    audit["reviewSheets"] = {
        path.name: {"bytes": path.stat().st_size, "sha256": sha256(path)}
        for path in sheets
    }
    audit["reviewSheetCount"] = len(sheets)
    audit_path = output / "world_tree_audit.json"
    audit_path.write_text(json.dumps(audit, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(
        f"Audited {audit['summary']['assetCount']} World Tree states and "
        f"{audit['summary']['frameCount']} frames; wrote {len(sheets)} review sheets to {output}"
    )


def audit_batch(baseline: Path, candidate: Path) -> dict:
    baseline_manifest_path = baseline / "asset_manifest.json"
    candidate_manifest_path = candidate / "asset_manifest.json"
    baseline_manifest = read_json(baseline_manifest_path)
    candidate_manifest = read_json(candidate_manifest_path)
    expected_keys = [record[0] for record in TREES]
    actual_keys = sorted(asset["key"] for asset in candidate_manifest.get("assets", []))
    if actual_keys != sorted(expected_keys):
        raise ValueError(f"Candidate must contain exactly {expected_keys}; found {actual_keys}")
    expected_global = {
        "pipelineVersion": 3,
        "generatedBatch": "world-tree",
        "frameRate": 12,
        "renderSupersample": 2,
        "opaqueRenderSamples": 16,
        "maxAtlasPageSize": 2048,
    }
    for field, expected in expected_global.items():
        if candidate_manifest.get(field) != expected:
            raise ValueError(
                f"Candidate {field} is {candidate_manifest.get(field)!r}; expected {expected!r}"
            )

    baseline_entries = {asset["key"]: asset for asset in baseline_manifest["assets"]}
    candidate_entries = {asset["key"]: asset for asset in candidate_manifest["assets"]}
    records = []
    total_frames = 0
    total_decoded = 0
    global_margins = {edge: 10_000 for edge in ("left", "top", "right", "bottom")}

    for key, label, revision, animation_profile in TREES:
        if key not in baseline_entries:
            raise ValueError(f"Baseline manifest is missing {key}")
        entry = candidate_entries[key]
        validate_metadata(entry, key, revision, animation_profile)
        metadata_path = candidate / "sprites" / f"{key}.json"
        if read_json(metadata_path) != entry:
            raise ValueError(f"{key} manifest and per-asset metadata differ")
        frames = CharacterFrames(candidate, key)
        clip_records = {}
        asset_margins = {edge: 10_000 for edge in global_margins}
        full_hashes = []
        alpha_boxes = {}

        for clip, expected_count in EXPECTED_CLIPS[key].items():
            regions = sorted(entry["clips"].get(clip, []), key=lambda value: value["index"])
            if len(regions) != expected_count:
                raise ValueError(f"{key}/{clip} has {len(regions)} frames, expected {expected_count}")
            if [region["index"] for region in regions] != list(range(expected_count)):
                raise ValueError(f"{key}/{clip} frame indices are not contiguous")
            clip_hashes = []
            clip_margins = {edge: 10_000 for edge in global_margins}
            for index, region in enumerate(regions):
                if region.get("page", 0) != 0:
                    raise ValueError(f"{key} must remain a single-page atlas")
                if region["width"] != 256 or region["height"] != 256:
                    raise ValueError(f"{key}/{clip}/{index} is not a native 256 px frame")
                frame = frames.frame(clip, index)
                alpha_box = frame.getchannel("A").getbbox()
                if alpha_box is None:
                    raise ValueError(f"{key}/{clip}/{index} is empty")
                left, top, right, bottom = alpha_box
                margins = {
                    "left": left,
                    "top": top,
                    "right": frame.width - right,
                    "bottom": frame.height - bottom,
                }
                if min(margins.values()) < 4:
                    raise ValueError(f"{key}/{clip}/{index} approaches a boundary: {margins}")
                alpha_boxes[f"{clip}/{index}"] = {
                    "left": left, "top": top, "right": right, "bottom": bottom,
                }
                for edge, value in margins.items():
                    clip_margins[edge] = min(clip_margins[edge], value)
                    asset_margins[edge] = min(asset_margins[edge], value)
                    global_margins[edge] = min(global_margins[edge], value)
                digest = hashlib.sha256(frame.tobytes()).hexdigest()
                clip_hashes.append(digest)
                full_hashes.append(digest)
            unique = len(set(clip_hashes))
            if unique < MINIMUM_UNIQUE[key][clip]:
                raise ValueError(f"{key}/{clip} has only {unique} unique visible frames")
            clip_records[clip] = {
                "frameCount": expected_count,
                "uniqueVisibleFrames": unique,
                "minimumAlphaMargins": clip_margins,
            }
            total_frames += expected_count

        baseline_sheet = baseline / baseline_entries[key]["sheet"]
        candidate_sheet = candidate / entry["sheet"]
        if sha256(baseline_sheet) == sha256(candidate_sheet):
            raise ValueError(f"{key} candidate is byte-identical to its baseline")
        for sheet in entry["sheets"]:
            total_decoded += int(sheet["width"]) * int(sheet["height"]) * 4
        records.append({
            "key": key,
            "label": label,
            "baselineSheetSha256": sha256(baseline_sheet),
            "candidateSheetSha256": sha256(candidate_sheet),
            "candidateAtlasSha256": sha256(candidate / entry["atlas"]),
            "candidateMetadataSha256": sha256(metadata_path),
            "modelRevision": revision,
            "rigProfile": "segmented-world-tree-v2",
            "animationProfile": animation_profile,
            "triangles": entry["triangles"],
            "meshParts": entry["meshParts"],
            "materialCount": entry["materialCount"],
            "rigBoneCount": entry["rigBoneCount"],
            "minimumAlphaMargins": asset_margins,
            "alphaBoxes": alpha_boxes,
            "clips": clip_records,
            "fullSetUniqueVisibleFrames": len(set(full_hashes)),
        })

    healthy_frames = CharacterFrames(candidate, "world_tree_healthy")
    damaged_frames = CharacterFrames(candidate, "world_tree_damaged")
    damaged_idle = damaged_frames.frame("idle", 0)
    destruction_start = damaged_frames.frame("destroy", 0)
    destruction_final = damaged_frames.frame("destroy", 9)
    if frame_digest(healthy_frames.frame("idle", 0)) == frame_digest(damaged_idle):
        raise ValueError("Healthy and damaged states are visually identical")
    if frame_digest(damaged_idle) == frame_digest(destruction_final):
        raise ValueError("Final destruction pose is visually identical to damaged idle")
    start_difference = alpha_difference_ratio(damaged_idle, destruction_start)
    if start_difference > 0.03:
        raise ValueError(
            f"Destruction does not begin from the damaged silhouette ({start_difference:.4f})"
        )
    hold_difference = alpha_difference_ratio(
        damaged_frames.frame("destroy", 8), destruction_final
    )
    if hold_difference > 0.02:
        raise ValueError(f"Final destruction hold is unstable ({hold_difference:.4f})")
    start_box = destruction_start.getchannel("A").getbbox()
    final_box = destruction_final.getchannel("A").getbbox()
    if start_box is None or final_box is None:
        raise ValueError("Destruction contains an empty endpoint")
    start_height = start_box[3] - start_box[1]
    final_height = final_box[3] - final_box[1]
    if final_box[1] < start_box[1] + 4 and final_height > start_height - 4:
        raise ValueError("Destruction does not visibly lower the Tree crown")
    if total_decoded > DECODED_BUDGET:
        raise ValueError(f"World Tree batch decodes to {total_decoded}, over {DECODED_BUDGET}")

    return {
        "schemaVersion": 1,
        "batch": "world-tree-premium-v2",
        "baselineManifestSha256": sha256(baseline_manifest_path),
        "candidateManifestSha256": sha256(candidate_manifest_path),
        "expectedKeys": expected_keys,
        "frameContract": EXPECTED_CLIPS,
        "minimumUniqueVisibleFrames": MINIMUM_UNIQUE,
        "rigBoneNames": EXPECTED_BONES,
        "assets": records,
        "destructionContinuity": {
            "startAlphaDifferenceRatio": round(start_difference, 6),
            "finalHoldAlphaDifferenceRatio": round(hold_difference, 6),
            "startOpaqueHeight": start_height,
            "finalOpaqueHeight": final_height,
            "crownTopDropPixels": final_box[1] - start_box[1],
        },
        "summary": {
            "assetCount": len(records),
            "frameCount": total_frames,
            "singlePageAtlasCount": len(records),
            "decodedBytes": total_decoded,
            "decodedBudgetBytes": DECODED_BUDGET,
            "minimumAlphaMargins": global_margins,
            "minimumTriangles": min(record["triangles"] for record in records),
            "maximumTriangles": max(record["triangles"] for record in records),
            "minimumMeshParts": min(record["meshParts"] for record in records),
            "minimumMaterialCount": min(record["materialCount"] for record in records),
        },
    }


def validate_metadata(entry: dict, key: str, revision: str, animation_profile: str) -> None:
    healthy = key == "world_tree_healthy"
    expected_dimensions = (1536, 256) if healthy else (2048, 512)
    expected = {
        "family": "world_tree",
        "frameClass": "tree",
        "frameSize": 256,
        "sheet": f"sprites/{key}.png",
        "atlas": f"sprites/{key}.atlas",
        "sheetWidth": expected_dimensions[0],
        "sheetHeight": expected_dimensions[1],
        "pivot": EXPECTED_PIVOT,
        "alphaMode": "STRAIGHT_RGBA",
        "frameRate": 12,
        "renderSupersample": 2,
        "renderSamples": 16,
        "rigBoneCount": 13,
        "bones": EXPECTED_BONES,
        "boneAnimated": True,
        "rigged": True,
        "visualQuality": "premium-v2",
        "modelRevision": revision,
        "rigProfile": "segmented-world-tree-v2",
        "animationProfile": animation_profile,
        "state": "healthy" if healthy else "damaged",
        "destructionClip": None if healthy else "destroy",
    }
    for field, value in expected.items():
        if entry.get(field) != value:
            raise ValueError(f"{key} {field} is {entry.get(field)!r}; expected {value!r}")
    if set(entry.get("clips", {})) != set(EXPECTED_CLIPS[key]):
        raise ValueError(f"{key} clip-name contract mismatch")
    if not 2_500 <= int(entry.get("triangles", 0)) <= 14_000:
        raise ValueError(f"{key} triangle count is outside the premium World Tree budget")
    if int(entry.get("meshParts", 0)) < 100:
        raise ValueError(f"{key} requires at least 100 purposeful mesh parts")
    if int(entry.get("materialCount", 0)) < 11:
        raise ValueError(f"{key} requires at least 11 coherent materials")
    landmarks = entry.get("silhouetteLandmarks")
    if not isinstance(landmarks, list) or len(landmarks) != 4 or not all(landmarks):
        raise ValueError(f"{key} must retain four locked silhouette landmarks")
    if not entry.get("surfaceLanguage"):
        raise ValueError(f"{key} is missing surface-language metadata")
    expected_sheet = {
        "decodedBytes": expected_dimensions[0] * expected_dimensions[1] * 4,
        "file": f"sprites/{key}.png",
        "height": expected_dimensions[1],
        "width": expected_dimensions[0],
    }
    if entry.get("sheets") != [expected_sheet]:
        raise ValueError(f"{key} atlas page contract mismatch")


def create_motion_sheet(
    frames: CharacterFrames,
    title: str,
    clips: tuple[str, ...],
    output: Path,
) -> None:
    frame_size = 256
    left = 175
    gap = 9
    max_frames = max(len(frames.metadata["clips"][clip]) for clip in clips)
    width = left + max_frames * (frame_size + gap) + 30
    height = 112 + len(clips) * (frame_size + 46) + 24
    canvas = canvas_base(width, height, title)
    draw = ImageDraw.Draw(canvas)
    text(
        draw,
        (width // 2, 76),
        "Every runtime pixel at 1×; checkerboards expose alpha safety and crown motion.",
        18,
        anchor="ma",
        color="#AFC5BE",
    )
    for row, clip in enumerate(clips):
        y = 108 + row * (frame_size + 46)
        count = len(frames.metadata["clips"][clip])
        text(draw, (24, y + 116), clip.upper(), 24, bold=True, anchor="lm")
        text(draw, (24, y + 148), f"{count} frames", 16, anchor="lm", color="#AFC5BE")
        for index in range(count):
            x = left + index * (frame_size + gap)
            card = checker(frame_size, frame_size)
            card.alpha_composite(frames.frame(clip, index))
            canvas.paste(card.convert("RGB"), (x, y))
            draw.rounded_rectangle((x, y, x + frame_size, y + frame_size), 8,
                                   outline="#58706A", width=2)
            text(draw, (x + 128, y + 276), f"F{index:02d}", 15,
                 anchor="ma", color="#C7D4CE")
    canvas.save(output, optimize=True)


def create_state_lineup(baseline: Path, candidate: Path, audit: dict, output: Path) -> None:
    width, height = 1920, 840
    canvas = canvas_base(width, height, "WORLD TREE — PREMIUM V2 STATE CONTINUITY")
    draw = ImageDraw.Draw(canvas)
    old_healthy = CharacterFrames(baseline, "world_tree_healthy")
    old_damaged = CharacterFrames(baseline, "world_tree_damaged")
    healthy = CharacterFrames(candidate, "world_tree_healthy")
    damaged = CharacterFrames(candidate, "world_tree_damaged")
    panels = (
        ("BASELINE HEALTHY", old_healthy.frame("idle", 0)),
        ("BASELINE DAMAGED", old_damaged.frame("idle", 0)),
        ("PREMIUM HEALTHY", healthy.frame("idle", 0)),
        ("PREMIUM WOUNDED", damaged.frame("idle", 0)),
        ("CORE FLARE", damaged.frame("destroy", 3)),
        ("FINAL FALLEN HOLD", damaged.frame("destroy", 9)),
    )
    for index, (caption, sprite) in enumerate(panels):
        x = 55 + index * 310
        y = 135
        card = presentation_card(sprite, "checker", 280, 490)
        canvas.paste(card.convert("RGB"), (x, y))
        draw.rounded_rectangle((x, y, x + 280, y + 490), 12, outline="#58706A", width=2)
        text(draw, (x + 140, y - 17), caption, 17, bold=True, anchor="ma")
    summary = audit["summary"]
    continuity = audit["destructionContinuity"]
    text(
        draw,
        (width // 2, 690),
        f"{summary['minimumTriangles']:,}–{summary['maximumTriangles']:,} triangles  •  "
        f"{summary['minimumMeshParts']}+ parts  •  {summary['minimumMaterialCount']}+ materials  •  13-bone segmented rig",
        19,
        bold=True,
        anchor="ma",
        color="#F2D58A",
    )
    text(
        draw,
        (width // 2, 730),
        f"22/22 frames audited  •  crown drops {continuity['crownTopDropPixels']} px  •  "
        f"final opaque height {continuity['finalOpaqueHeight']} px  •  one-shot final hold",
        17,
        anchor="ma",
        color="#AFC5BE",
    )
    text(
        draw,
        (width // 2, 770),
        "Healthy and wounded states retain the same roots, heart aperture, guardian boughs, and crown identity.",
        17,
        anchor="ma",
        color="#AFC5BE",
    )
    canvas.save(output, optimize=True)


def create_readability_sheet(baseline: Path, candidate: Path, output: Path) -> None:
    width, height = 1810, 1120
    canvas = canvas_base(width, height, "WORLD TREE — SILHOUETTE, MATERIAL & CONTRAST REVIEW")
    draw = ImageDraw.Draw(canvas)
    old = CharacterFrames(baseline, "world_tree_healthy")
    healthy = CharacterFrames(candidate, "world_tree_healthy")
    damaged = CharacterFrames(candidate, "world_tree_damaged")
    state_frames = {
        "healthy": healthy.frame("idle", 0),
        "wounded": damaged.frame("idle", 0),
        "fallen": damaged.frame("destroy", 9),
    }
    panels = (
        ("Baseline", old.frame("idle", 0), "checker"),
        ("Healthy", state_frames["healthy"], "checker"),
        ("Wounded", state_frames["wounded"], "checker"),
        ("Fallen", state_frames["fallen"], "checker"),
        ("Healthy grayscale", state_frames["healthy"], "grayscale"),
        ("Wounded grayscale", state_frames["wounded"], "grayscale"),
        ("Healthy silhouette", state_frames["healthy"], "silhouette"),
        ("Fallen silhouette", state_frames["fallen"], "silhouette"),
        ("Healthy / dark arena", state_frames["healthy"], "dark"),
        ("Wounded / dark arena", state_frames["wounded"], "dark"),
        ("Healthy / light arena", state_frames["healthy"], "light"),
        ("Fallen / light arena", state_frames["fallen"], "light"),
    )
    card_width, card_height = 400, 275
    for index, (caption, sprite, mode) in enumerate(panels):
        column = index % 4
        row = index // 4
        x = 65 + column * 435
        y = 118 + row * 326
        card = presentation_card(sprite, mode, card_width, card_height)
        canvas.paste(card.convert("RGB"), (x, y))
        draw.rounded_rectangle((x, y, x + card_width, y + card_height), 12,
                               outline="#58706A", width=2)
        text(draw, (x + card_width // 2, y - 15), caption, 19, bold=True, anchor="ma")
    text(
        draw,
        (width // 2, 1070),
        "Nearest-neighbor review only: no smoothing, painted cleanup, or baked post-process glow.",
        17,
        anchor="ma",
        color="#AFC5BE",
    )
    canvas.save(output, optimize=True)


def create_destruction_timeline(damaged: CharacterFrames, output: Path) -> None:
    width, height = 2090, 560
    canvas = canvas_base(width, height, "WORLD TREE — COMPLETE ONE-SHOT DESTRUCTION TIMELINE")
    draw = ImageDraw.Draw(canvas)
    captions = (
        "WOUNDED", "SHUDDER", "RECOIL", "CORE FLARE", "BREAK",
        "FOLD", "FALL", "IMPACT", "HOLD", "HOLD",
    )
    for index in range(10):
        x = 35 + index * 204
        y = 126
        card = presentation_card(damaged.frame("destroy", index), "dark", 184, 320)
        canvas.paste(card.convert("RGB"), (x, y))
        draw.rounded_rectangle((x, y, x + 184, y + 320), 10, outline="#58706A", width=2)
        text(draw, (x + 92, 106), f"F{index:02d}  {captions[index]}", 15,
             bold=True, anchor="ma", color="#F2D58A" if index in (3, 7) else "#E7E1CF")
    text(
        draw,
        (width // 2, 505),
        "Presentation time continues after simulation stop; the non-looping final frame remains held on Game Over.",
        18,
        anchor="ma",
        color="#AFC5BE",
    )
    canvas.save(output, optimize=True)


def create_arena_scale_sheet(baseline: Path, candidate: Path, output: Path) -> None:
    width, height = 2240, 690
    canvas = canvas_base(width, height, "WORLD TREE — 720×1280 REFERENCE-SCALE HERO HIERARCHY")
    draw = ImageDraw.Draw(canvas)
    hero = CharacterFrames(baseline, "hero").frame("idle", 0)
    old = CharacterFrames(baseline, "world_tree_healthy").frame("idle", 0)
    healthy = CharacterFrames(candidate, "world_tree_healthy").frame("idle", 0)
    damaged = CharacterFrames(candidate, "world_tree_damaged")
    panels = (
        ("BASELINE COMPOSITION", old),
        ("PREMIUM HEALTHY", healthy),
        ("PREMIUM WOUNDED", damaged.frame("idle", 0)),
        ("PREMIUM FALLEN", damaged.frame("destroy", 9)),
    )
    panel_width, panel_height = 520, 520
    for index, (caption, tree) in enumerate(panels):
        x = 45 + index * 545
        y = 120
        panel = Image.new("RGBA", (panel_width, panel_height), "#18332F")
        panel_draw = ImageDraw.Draw(panel)
        for band in range(7):
            color = "#263D32" if band % 2 else "#2C4537"
            panel_draw.rectangle((0, 360 + band * 24, panel_width, 384 + band * 24), fill=color)
        panel_draw.ellipse((90, 382, 430, 470), fill="#142623")
        tree_scaled = tree.resize((330, 330), Image.Resampling.NEAREST)
        hero_scaled = hero.resize((170, 170), Image.Resampling.NEAREST)
        panel.alpha_composite(tree_scaled, (95, 48))
        panel.alpha_composite(hero_scaled, (175, 286))
        canvas.paste(panel.convert("RGB"), (x, y))
        draw.rounded_rectangle((x, y, x + panel_width, y + panel_height), 12,
                               outline="#58706A", width=2)
        text(draw, (x + panel_width // 2, y - 16), caption, 18, bold=True, anchor="ma")
    text(
        draw,
        (width // 2, 662),
        "Tree sprite box 330 reference units; Hero box 170. The Hero remains the foreground combat read.",
        17,
        anchor="ma",
        color="#AFC5BE",
    )
    canvas.save(output, optimize=True)


def alpha_difference_ratio(first: Image.Image, second: Image.Image) -> float:
    first_alpha = first.getchannel("A")
    second_alpha = second.getchannel("A")
    difference = ImageChops.difference(first_alpha, second_alpha)
    changed = sum(1 for value in difference.tobytes() if value > 8)
    occupied = max(
        1,
        sum(1 for value in first_alpha.tobytes() if value > 8),
        sum(1 for value in second_alpha.tobytes() if value > 8),
    )
    return changed / occupied


def frame_digest(frame: Image.Image) -> str:
    return hashlib.sha256(frame.tobytes()).hexdigest()


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
