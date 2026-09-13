#!/usr/bin/env python3
"""Audit the exact premium UI batch and generate deterministic state/readability evidence."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageOps

from create_character_animation_review import canvas_base, checker, text

ICON_KEYS = (
    "ui_health", "ui_wave", "ui_coin", "ui_pause", "ui_speed",
    "ui_inventory", "ui_shop", "ui_settings", "ui_restart",
    "ui_new_game", "ui_continue", "ui_close", "ui_strength",
    "ui_agility", "ui_luck", "ui_dodge",
)
KINDS = ("button", "panel", "slot")
STATES = ("normal", "pressed", "selected", "disabled")
FRAME_KEYS = tuple(f"ui_frame_{kind}_{state}" for kind in KINDS for state in STATES)
EXPECTED_KEYS = (*ICON_KEYS, *FRAME_KEYS)
EXPECTED_PIVOT = {"units": "normalized-bottom-left", "x": 0.5, "y": 0.5}
DECODED_BUDGET = 2 * 1024 * 1024


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
    create_icon_lineup(baseline, candidate, output / "ui_icon_lineup.png")
    create_icon_readability(candidate, output / "ui_icon_readability.png")
    create_skin_matrix(candidate, output / "ui_skin_state_matrix.png")
    create_nine_patch_stretch(candidate, output / "ui_nine_patch_stretch.png")
    create_state_construction(candidate, output / "ui_state_construction.png")
    create_integrated_surfaces(candidate, output / "ui_integrated_surfaces.png")

    sheets = sorted(output.glob("*.png"))
    audit["reviewSheets"] = {
        path.name: {"bytes": path.stat().st_size, "sha256": sha256(path)}
        for path in sheets
    }
    audit["reviewSheetCount"] = len(sheets)
    (output / "ui_audit.json").write_text(
        json.dumps(audit, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    print(f"Audited {len(EXPECTED_KEYS)} UI assets and wrote {len(sheets)} review sheets to {output}")


def audit_batch(baseline: Path, candidate: Path) -> dict:
    baseline_manifest_path = baseline / "asset_manifest.json"
    candidate_manifest_path = candidate / "asset_manifest.json"
    baseline_manifest = read_json(baseline_manifest_path)
    candidate_manifest = read_json(candidate_manifest_path)
    actual_keys = sorted(asset["key"] for asset in candidate_manifest.get("assets", []))
    if actual_keys != sorted(EXPECTED_KEYS):
        raise ValueError(f"UI candidate must contain exactly {len(EXPECTED_KEYS)} keys; found {actual_keys}")
    globals_expected = {
        "pipelineVersion": 3,
        "generatedBatch": "ui",
        "renderSupersample": 2,
        "opaqueRenderSamples": 16,
        "maxAtlasPageSize": 2048,
    }
    for field, expected in globals_expected.items():
        if candidate_manifest.get(field) != expected:
            raise ValueError(f"Candidate {field}={candidate_manifest.get(field)!r}; expected {expected!r}")

    baseline_by_key = {asset["key"]: asset for asset in baseline_manifest["assets"]}
    candidate_by_key = {asset["key"]: asset for asset in candidate_manifest["assets"]}
    payload = expected_payload(candidate)
    records = []
    hashes = set()
    total_decoded = 0
    minimum_margin = 10_000
    for key in EXPECTED_KEYS:
        entry = candidate_by_key[key]
        validate_metadata(entry, key)
        family = "icons" if key in ICON_KEYS else "ui"
        metadata_path = candidate / family / f"{key}.json"
        image_path = candidate / entry["sheet"]
        if read_json(metadata_path) != entry:
            raise ValueError(f"{key} manifest and metadata differ")
        image = Image.open(image_path).convert("RGBA")
        if image.size != (96, 96):
            raise ValueError(f"{key} changed the 96 px contract")
        digest = sha256(image_path)
        if digest in hashes:
            raise ValueError(f"{key} duplicates another UI render")
        hashes.add(digest)
        bounds = image.getchannel("A").getbbox()
        if bounds is None:
            raise ValueError(f"{key} is empty")
        left, top, right, bottom = bounds
        margins = {
            "left": left, "top": top,
            "right": image.width - right, "bottom": image.height - bottom,
        }
        if min(margins.values()) < 4:
            raise ValueError(f"{key} approaches a boundary: {margins}")
        minimum_margin = min(minimum_margin, *margins.values())
        decoded = image.width * image.height * 4
        total_decoded += decoded
        record = {
            "key": key,
            "family": family,
            "sheetSha256": digest,
            "metadataSha256": sha256(metadata_path),
            "alphaMargins": margins,
            "triangles": entry["triangles"],
            "meshParts": entry["meshParts"],
            "materialCount": entry["materialCount"],
            "decodedBytes": decoded,
            "modelRevision": entry["modelRevision"],
        }
        if key in ICON_KEYS:
            baseline_entry = baseline_by_key.get(key)
            if baseline_entry is None:
                raise ValueError(f"Baseline is missing {key}")
            baseline_path = baseline / baseline_entry["sheet"]
            if sha256(baseline_path) == digest:
                raise ValueError(f"{key} is byte-identical to baseline")
            record["baselineSheetSha256"] = sha256(baseline_path)
            record["semantic"] = entry["uiIcon"]
        else:
            record["skin"] = entry["uiSkin"]
            record["state"] = entry["uiState"]
            record["stateConstruction"] = entry["stateConstruction"]
        records.append(record)

    if total_decoded > DECODED_BUDGET:
        raise ValueError(f"UI batch decodes to {total_decoded}, over {DECODED_BUDGET}")
    return {
        "schemaVersion": 1,
        "batch": "ui-assets-premium-v2",
        "expectedKeys": list(EXPECTED_KEYS),
        "iconKeys": list(ICON_KEYS),
        "frameKeys": list(FRAME_KEYS),
        "baselineManifestSha256": sha256(baseline_manifest_path),
        "candidateManifestSha256": sha256(candidate_manifest_path),
        "candidatePayload": {relative: sha256(candidate / relative) for relative in payload},
        "assets": records,
        "summary": {
            "assetCount": len(records),
            "iconCount": len(ICON_KEYS),
            "skinCount": len(FRAME_KEYS),
            "skinFamilyCount": len(KINDS),
            "stateCountPerSkin": len(STATES),
            "decodedBytes": total_decoded,
            "decodedBudgetBytes": DECODED_BUDGET,
            "minimumAlphaMargin": minimum_margin,
            "minimumTriangles": min(record["triangles"] for record in records),
            "maximumTriangles": max(record["triangles"] for record in records),
            "minimumMeshParts": min(record["meshParts"] for record in records),
            "minimumMaterialCount": min(record["materialCount"] for record in records),
        },
    }


def validate_metadata(entry: dict, key: str) -> None:
    family = "icons" if key in ICON_KEYS else "ui"
    expected = {
        "key": key,
        "family": family,
        "frameClass": "item",
        "frameSize": 96,
        "frameWidth": 96,
        "frameHeight": 96,
        "sheet": f"{family}/{key}.png",
        "sheetWidth": 96,
        "sheetHeight": 96,
        "pivot": EXPECTED_PIVOT,
        "alphaMode": "STRAIGHT_RGBA",
        "renderSupersample": 2,
        "renderSamples": 16,
        "touchOnlyUI": True,
        "visualQuality": "premium-v2",
    }
    if key in ICON_KEYS:
        expected.update({
            "uiIcon": key.removeprefix("ui_"),
            "iconFamily": "heartwood-control-medallion",
            "modelRevision": "ui-control-icon-premium-v2",
        })
        triangle_range = (300, 1_200)
        minimum_parts, minimum_materials = 7, 5
    else:
        remainder = key.removeprefix("ui_frame_")
        kind, state = remainder.rsplit("_", 1)
        expected.update({
            "uiSkin": kind,
            "uiState": state,
            "modelRevision": "forest-glass-nine-patch-v2",
            "ninePatchInsets": {"left": 24, "right": 24, "top": 24, "bottom": 24},
        })
        triangle_range = (150, 600)
        minimum_parts, minimum_materials = 11, 4
        if not entry.get("stateConstruction"):
            raise ValueError(f"{key} lacks state-construction provenance")
    for field, expected_value in expected.items():
        if entry.get(field) != expected_value:
            raise ValueError(f"{key} {field}={entry.get(field)!r}; expected {expected_value!r}")
    triangles = int(entry.get("triangles", 0))
    if not triangle_range[0] <= triangles <= triangle_range[1]:
        raise ValueError(f"{key} triangles {triangles} outside {triangle_range}")
    if int(entry.get("meshParts", 0)) < minimum_parts:
        raise ValueError(f"{key} has too few purposeful mesh parts")
    if int(entry.get("materialCount", 0)) < minimum_materials:
        raise ValueError(f"{key} has too few coherent materials")
    expected_sheet = [{
        "decodedBytes": 36_864,
        "file": f"{family}/{key}.png",
        "height": 96,
        "width": 96,
    }]
    if entry.get("sheets") != expected_sheet:
        raise ValueError(f"{key} sheet contract mismatch")
    expected_clip = {"idle": [{
        "height": 96, "index": 0, "page": 0, "width": 96, "x": 0, "y": 0,
    }]}
    if entry.get("clips") != expected_clip:
        raise ValueError(f"{key} static-frame contract mismatch")


def create_icon_lineup(baseline: Path, candidate: Path, output: Path) -> None:
    width, height = 1900, 1040
    canvas = canvas_base(width, height, "ALL UI CONTROL ICONS — BASELINE VS HEARTWOOD MEDALLIONS")
    draw = ImageDraw.Draw(canvas)
    for index, key in enumerate(ICON_KEYS):
        column = index % 4
        row = index // 4
        x = 48 + column * 465
        y = 110 + row * 220
        text(draw, (x + 212, y - 15), key.removeprefix("ui_").replace("_", " ").upper(),
             17, bold=True, anchor="ma")
        for offset, (label, root) in enumerate((("BEFORE", baseline), ("AFTER", candidate))):
            sprite = asset_image(root, key)
            card = checker(150, 150)
            card.alpha_composite(sprite.resize((150, 150), Image.Resampling.LANCZOS))
            px = x + offset * 218
            canvas.paste(card.convert("RGB"), (px, y))
            text(draw, (px + 75, y + 174), label, 14, bold=True, anchor="ma")
    canvas.save(output, optimize=True)


def create_icon_readability(candidate: Path, output: Path) -> None:
    width, height = 1880, 800
    canvas = canvas_base(width, height, "UI ICONS — 58 PX RUNTIME, LIGHT/DARK & GRAYSCALE")
    draw = ImageDraw.Draw(canvas)
    backgrounds = (("DARK", (16, 35, 31)), ("LIGHT", (202, 206, 191)), ("VALUE", (55, 55, 55)))
    for row, (label, color) in enumerate(backgrounds):
        y = 120 + row * 205
        text(draw, (34, y + 56), label, 18, bold=True, anchor="lm")
        for index, key in enumerate(ICON_KEYS):
            sprite = asset_image(candidate, key)
            if row == 2:
                sprite = ImageOps.grayscale(sprite).convert("RGBA")
            panel = Image.new("RGBA", (98, 130), (*color, 255))
            panel.alpha_composite(sprite.resize((58, 58), Image.Resampling.LANCZOS), (20, 18))
            x = 150 + index * 106
            canvas.paste(panel.convert("RGB"), (x, y))
            if row == 0:
                text(draw, (x + 49, y + 105), key[3:].replace("_", "\n"), 11,
                     anchor="ma", color="#C7D4CE")
    canvas.save(output, optimize=True)


def create_skin_matrix(candidate: Path, output: Path) -> None:
    width, height = 1580, 1060
    canvas = canvas_base(width, height, "REUSABLE UI FRAME SKINS — COMPLETE STATE MATRIX")
    draw = ImageDraw.Draw(canvas)
    for row, kind in enumerate(KINDS):
        y = 145 + row * 290
        text(draw, (44, y + 100), kind.upper(), 24, bold=True, anchor="lm")
        for column, state in enumerate(STATES):
            x = 250 + column * 315
            sprite = asset_image(candidate, f"ui_frame_{kind}_{state}", family="ui")
            card = checker(220, 220)
            card.alpha_composite(sprite.resize((220, 220), Image.Resampling.NEAREST))
            canvas.paste(card.convert("RGB"), (x, y))
            text(draw, (x + 110, y - 18), state.upper(), 18, bold=True, anchor="ma")
    canvas.save(output, optimize=True)


def create_nine_patch_stretch(candidate: Path, output: Path) -> None:
    width, height = 1740, 1040
    canvas = canvas_base(width, height, "NINE-PATCH STRETCH — CORNERS STAY FIXED, CENTERS SCALE")
    draw = ImageDraw.Draw(canvas)
    dimensions = {"button": (520, 130), "panel": (410, 250), "slot": (560, 105)}
    for row, kind in enumerate(KINDS):
        y = 125 + row * 290
        text(draw, (40, y + 80), kind.upper(), 22, bold=True, anchor="lm")
        target_size = dimensions[kind]
        for column, state in enumerate(STATES):
            source = asset_image(candidate, f"ui_frame_{kind}_{state}", family="ui")
            shown = nine_patch(source, *target_size)
            shown.thumbnail((330, 210), Image.Resampling.LANCZOS)
            x = 225 + column * 370
            panel = Image.new("RGBA", (340, 220), (12, 27, 25, 255))
            panel.alpha_composite(shown, ((340 - shown.width) // 2, (220 - shown.height) // 2))
            canvas.paste(panel.convert("RGB"), (x, y))
            text(draw, (x + 170, y + 244), state.upper(), 15, anchor="ma", color="#C7D4CE")
    canvas.save(output, optimize=True)


def create_state_construction(candidate: Path, output: Path) -> None:
    width, height = 1700, 650
    canvas = canvas_base(width, height, "BUTTON STATE CONSTRUCTION — NOT RECOLOR-ONLY")
    draw = ImageDraw.Draw(canvas)
    captions = (
        "warm priority edge", "inset face + green notch",
        "gold corners + leaf tabs", "desaturated frame + quiet bar",
    )
    for index, (state, caption) in enumerate(zip(STATES, captions, strict=True)):
        x = 65 + index * 410
        sprite = asset_image(candidate, f"ui_frame_button_{state}", family="ui")
        shown = sprite.resize((300, 300), Image.Resampling.NEAREST)
        canvas.paste(checker(300, 300).convert("RGB"), (x, 125))
        canvas.paste(shown, (x, 125), shown)
        text(draw, (x + 150, 92), state.upper(), 21, bold=True, anchor="ma")
        text(draw, (x + 150, 470), caption, 16, anchor="ma", color="#AFC5BE")
    text(draw, (width // 2, 585), "Pressed, selected, and disabled remain legible without changing the semantic icon glyph.",
         19, bold=True, anchor="ma", color="#F2D58A")
    canvas.save(output, optimize=True)


def create_integrated_surfaces(candidate: Path, output: Path) -> None:
    width, height = 1800, 1030
    canvas = canvas_base(width, height, "PREMIUM UI ASSETS — INTEGRATED MENU, HUD & INVENTORY")
    draw = ImageDraw.Draw(canvas)
    menu = Image.new("RGBA", (520, 820), (12, 33, 30, 255))
    for index, (state, icon) in enumerate((("normal", "ui_new_game"), ("disabled", "ui_continue"), ("pressed", "ui_settings"))):
        frame = nine_patch(asset_image(candidate, f"ui_frame_button_{state}", "ui"), 420, 135)
        y = 520 - index * 180
        menu.alpha_composite(frame, (50, y))
        menu.alpha_composite(asset_image(candidate, icon).resize((88, 88), Image.Resampling.LANCZOS), (75, y + 24))
    panel = nine_patch(asset_image(candidate, "ui_frame_panel_normal", "ui"), 225, 95)
    menu.alpha_composite(panel, (270, 690))
    menu.alpha_composite(asset_image(candidate, "ui_coin").resize((58, 58), Image.Resampling.LANCZOS), (292, 710))

    hud = Image.new("RGBA", (520, 820), (18, 47, 40, 255))
    for index, (state, icon) in enumerate((("pressed", "ui_pause"), ("normal", "ui_speed"), ("selected", "ui_inventory"), ("normal", "ui_shop"))):
        frame = nine_patch(asset_image(candidate, f"ui_frame_button_{state}", "ui"), 185, 120)
        x = 50 + (index % 2) * 230
        y = 640 - (index // 2) * 165
        hud.alpha_composite(frame, (x, y))
        hud.alpha_composite(asset_image(candidate, icon).resize((72, 72), Image.Resampling.LANCZOS), (x + 20, y + 24))

    inventory = Image.new("RGBA", (520, 820), (11, 28, 27, 255))
    details = nine_patch(asset_image(candidate, "ui_frame_panel_normal", "ui"), 440, 245)
    inventory.alpha_composite(details, (40, 500))
    for index, state in enumerate(("normal", "selected", "disabled")):
        slot = nine_patch(asset_image(candidate, f"ui_frame_slot_{state}", "ui"), 440, 105)
        inventory.alpha_composite(slot, (40, 340 - index * 125))
    action = nine_patch(asset_image(candidate, "ui_frame_button_disabled", "ui"), 200, 95)
    inventory.alpha_composite(action, (280, 25))

    for index, (label, panel_image) in enumerate((("MAIN MENU", menu), ("LIVE HUD", hud), ("INVENTORY", inventory))):
        x = 65 + index * 575
        y = 125
        canvas.paste(panel_image.convert("RGB"), (x, y))
        draw.rectangle((x, y, x + 520, y + 820), outline="#728A83", width=3)
        text(draw, (x + 260, y - 18), label, 20, bold=True, anchor="ma")
    canvas.save(output, optimize=True)


def nine_patch(source: Image.Image, width: int, height: int, inset: int = 24) -> Image.Image:
    if width < inset * 2 or height < inset * 2:
        raise ValueError("Nine-patch target is smaller than fixed corners")
    result = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    sx = (0, inset, source.width - inset, source.width)
    sy = (0, inset, source.height - inset, source.height)
    dx = (0, inset, width - inset, width)
    dy = (0, inset, height - inset, height)
    for row in range(3):
        for column in range(3):
            patch = source.crop((sx[column], sy[row], sx[column + 1], sy[row + 1]))
            target_size = (dx[column + 1] - dx[column], dy[row + 1] - dy[row])
            if patch.size != target_size:
                patch = patch.resize(target_size, Image.Resampling.BILINEAR)
            result.alpha_composite(patch, (dx[column], dy[row]))
    return result


def expected_payload(candidate: Path) -> list[str]:
    expected = ["asset_manifest.json"]
    for key in ICON_KEYS:
        expected.extend((f"icons/{key}.json", f"icons/{key}.png"))
    for key in FRAME_KEYS:
        expected.extend((f"ui/{key}.json", f"ui/{key}.png"))
    actual = sorted(path.relative_to(candidate).as_posix() for path in candidate.rglob("*") if path.is_file())
    if actual != sorted(expected):
        raise ValueError(f"UI payload mismatch: expected {sorted(expected)}, found {actual}")
    return sorted(expected)


def asset_image(root: Path, key: str, family: str = "icons") -> Image.Image:
    return Image.open(root / family / f"{key}.png").convert("RGBA")


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


if __name__ == "__main__":
    main()
