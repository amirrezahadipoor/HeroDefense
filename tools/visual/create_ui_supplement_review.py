#!/usr/bin/env python3
"""Audit and render deterministic evidence for potion and reward-card icon coverage."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps

POTION_KEYS = tuple(f"health_potion_{tier}" for tier in range(1, 7))
NEW_REWARD_KEYS = ("ui_general_power", "ui_lifesteal")
EXPECTED_KEYS = (*POTION_KEYS, *NEW_REWARD_KEYS)
REWARD_ICONS = (
    ("MIGHT OF OAK", "ui_strength"),
    ("WINDSTEP", "ui_agility"),
    ("FORTUNE LEAF", "ui_luck"),
    ("FOX INSTINCT", "ui_dodge"),
    ("HEARTWOOD", "ui_health"),
    ("VERDANT POWER", "ui_general_power"),
    ("GOLDEN SAP", "ui_coin"),
    ("CRIMSON ROOT", "ui_lifesteal"),
)
EXPECTED_PIVOT = {"units": "normalized-bottom-left", "x": 0.5, "y": 0.5}
DECODED_BUDGET = 524_288
INK = (16, 36, 33)
DEEP = (8, 25, 23)
PARCHMENT = (231, 216, 177)
GOLD = (214, 173, 76)
GREEN = (53, 108, 76)


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
    create_potion_lineup(baseline, candidate, output / "ui_supplement_potions.png")
    create_reward_lineup(baseline, candidate, output / "ui_supplement_reward_icons.png")
    create_readability(baseline, candidate, output / "ui_supplement_readability.png")
    create_value_progression(candidate, output / "ui_supplement_value_progression.png")
    create_integrated_surface(baseline, candidate, output / "ui_supplement_integrated.png")

    sheets = sorted(output.glob("*.png"))
    audit["reviewSheets"] = {
        path.name: {"bytes": path.stat().st_size, "sha256": sha256(path)}
        for path in sheets
    }
    audit["reviewSheetCount"] = len(sheets)
    (output / "ui_supplement_audit.json").write_text(
        json.dumps(audit, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    print(f"Audited {len(EXPECTED_KEYS)} supplemental icons and wrote {len(sheets)} review sheets")


def audit_batch(baseline: Path, candidate: Path) -> dict:
    baseline_manifest_path = baseline / "asset_manifest.json"
    candidate_manifest_path = candidate / "asset_manifest.json"
    baseline_manifest = read_json(baseline_manifest_path)
    candidate_manifest = read_json(candidate_manifest_path)
    actual = [asset["key"] for asset in candidate_manifest.get("assets", [])]
    if sorted(actual) != sorted(EXPECTED_KEYS) or len(set(actual)) != len(EXPECTED_KEYS):
        raise ValueError(f"Supplement must contain exactly {EXPECTED_KEYS}; found {actual}")
    for field, expected in {
        "pipelineVersion": 3,
        "generatedBatch": "ui-supplement",
        "renderSupersample": 2,
        "opaqueRenderSamples": 16,
        "maxAtlasPageSize": 2048,
    }.items():
        if candidate_manifest.get(field) != expected:
            raise ValueError(f"Candidate {field} mismatch")

    baseline_by_key = {asset["key"]: asset for asset in baseline_manifest["assets"]}
    candidate_by_key = {asset["key"]: asset for asset in candidate_manifest["assets"]}
    payload = expected_payload(candidate)
    records = []
    hashes: set[str] = set()
    decoded_bytes = 0
    minimum_margin = 96
    for key in EXPECTED_KEYS:
        asset = candidate_by_key[key]
        validate_metadata(asset, key)
        metadata_path = candidate / "icons" / f"{key}.json"
        image_path = candidate / "icons" / f"{key}.png"
        if read_json(metadata_path) != asset:
            raise ValueError(f"{key} manifest and metadata differ")
        image = Image.open(image_path).convert("RGBA")
        if image.size != (96, 96):
            raise ValueError(f"{key} changed the 96 px contract")
        digest = sha256(image_path)
        if digest in hashes:
            raise ValueError(f"{key} duplicates another supplemental render")
        hashes.add(digest)
        bounds = image.getchannel("A").getbbox()
        if bounds is None:
            raise ValueError(f"{key} is empty")
        left, top, right, bottom = bounds
        margins = {
            "left": left,
            "top": top,
            "right": image.width - right,
            "bottom": image.height - bottom,
        }
        if min(margins.values()) < 4:
            raise ValueError(f"{key} approaches a boundary: {margins}")
        minimum_margin = min(minimum_margin, *margins.values())
        decoded = image.width * image.height * 4
        decoded_bytes += decoded
        baseline_asset = baseline_by_key.get(key)
        baseline_digest = None
        if baseline_asset is not None:
            baseline_path = baseline / baseline_asset["sheet"]
            baseline_digest = sha256(baseline_path)
            if baseline_digest == digest:
                raise ValueError(f"{key} is byte-identical to baseline")
        record = {
            "key": key,
            "sheetSha256": digest,
            "metadataSha256": sha256(metadata_path),
            "baselineSheetSha256": baseline_digest,
            "alphaMargins": margins,
            "decodedBytes": decoded,
            "triangles": asset["triangles"],
            "meshParts": asset["meshParts"],
            "materialCount": asset["materialCount"],
            "modelRevision": asset["modelRevision"],
        }
        if key in POTION_KEYS:
            record.update({
                "tier": asset["tier"],
                "tierConstruction": asset["tierConstruction"],
            })
        else:
            record.update({"semantic": asset["uiIcon"], "newSemantic": baseline_asset is None})
        records.append(record)

    if decoded_bytes > DECODED_BUDGET:
        raise ValueError(f"Supplement decodes to {decoded_bytes}, over {DECODED_BUDGET}")
    return {
        "schemaVersion": 1,
        "batch": "ui-icon-supplement-premium-v2",
        "expectedKeys": list(EXPECTED_KEYS),
        "potionKeys": list(POTION_KEYS),
        "newRewardIconKeys": list(NEW_REWARD_KEYS),
        "rewardCardIconMap": {title: key for title, key in REWARD_ICONS},
        "baselineManifestSha256": sha256(baseline_manifest_path),
        "candidateManifestSha256": sha256(candidate_manifest_path),
        "candidatePayload": {relative: sha256(candidate / relative) for relative in payload},
        "assets": records,
        "summary": {
            "assetCount": len(records),
            "potionCount": len(POTION_KEYS),
            "newRewardIconCount": len(NEW_REWARD_KEYS),
            "coveredRewardCardCount": len(REWARD_ICONS),
            "decodedBytes": decoded_bytes,
            "decodedBudgetBytes": DECODED_BUDGET,
            "minimumAlphaMargin": minimum_margin,
            "minimumTriangles": min(record["triangles"] for record in records),
            "maximumTriangles": max(record["triangles"] for record in records),
            "minimumMeshParts": min(record["meshParts"] for record in records),
            "minimumMaterialCount": min(record["materialCount"] for record in records),
        },
    }


def validate_metadata(asset: dict, key: str) -> None:
    expected = {
        "key": key,
        "family": "icons",
        "frameClass": "item",
        "frameSize": 96,
        "frameWidth": 96,
        "frameHeight": 96,
        "sheet": f"icons/{key}.png",
        "sheetWidth": 96,
        "sheetHeight": 96,
        "pivot": EXPECTED_PIVOT,
        "alphaMode": "STRAIGHT_RGBA",
        "renderSupersample": 2,
        "renderSamples": 16,
        "visualQuality": "premium-v2",
    }
    if key in POTION_KEYS:
        tier = int(key.rsplit("_", 1)[1])
        expected.update({
            "tier": tier,
            "heal_icon": True,
            "potionFamily": "heartwood-elixir",
            "modelRevision": "health-potion-premium-v2",
        })
        if not asset.get("tierConstruction"):
            raise ValueError(f"{key} lacks tier construction")
        triangle_range = (300, 1_200)
        minimum_parts, minimum_materials = 10, 5
    else:
        expected.update({
            "touchOnlyUI": True,
            "uiIcon": key.removeprefix("ui_"),
            "iconFamily": "heartwood-control-medallion",
            "modelRevision": "ui-control-icon-premium-v2",
        })
        triangle_range = (300, 1_200)
        minimum_parts, minimum_materials = 7, 5
    for field, expected_value in expected.items():
        if asset.get(field) != expected_value:
            raise ValueError(f"{key} {field}={asset.get(field)!r}; expected {expected_value!r}")
    triangles = int(asset.get("triangles", 0))
    if not triangle_range[0] <= triangles <= triangle_range[1]:
        raise ValueError(f"{key} triangles {triangles} outside {triangle_range}")
    if int(asset.get("meshParts", 0)) < minimum_parts:
        raise ValueError(f"{key} has too few purposeful mesh parts")
    if int(asset.get("materialCount", 0)) < minimum_materials:
        raise ValueError(f"{key} has too few coherent materials")
    if asset.get("sheets") != [{
        "decodedBytes": 36_864,
        "file": f"icons/{key}.png",
        "height": 96,
        "width": 96,
    }]:
        raise ValueError(f"{key} sheet contract mismatch")
    if asset.get("clips") != {"idle": [{
        "height": 96, "index": 0, "page": 0, "width": 96, "x": 0, "y": 0,
    }]}:
        raise ValueError(f"{key} static frame contract mismatch")


def expected_payload(candidate: Path) -> tuple[str, ...]:
    expected = ["asset_manifest.json"]
    for key in EXPECTED_KEYS:
        expected.extend((f"icons/{key}.json", f"icons/{key}.png"))
    actual = sorted(
        path.relative_to(candidate).as_posix()
        for path in candidate.rglob("*") if path.is_file()
    )
    if actual != sorted(expected):
        raise ValueError(
            f"Candidate payload mismatch: missing={sorted(set(expected) - set(actual))}, "
            f"unexpected={sorted(set(actual) - set(expected))}"
        )
    return tuple(sorted(expected))


def create_potion_lineup(baseline: Path, candidate: Path, output: Path) -> None:
    canvas = base(1900, 720, "ALL SIX HEARTWOOD ELIXIRS — BEFORE / PREMIUM-V2")
    draw = ImageDraw.Draw(canvas)
    for index, key in enumerate(POTION_KEYS):
        x = 66 + index * 305
        label = f"TIER {index + 1}"
        text(draw, (x + 120, 102), label, 23, True, "ma")
        for row, (caption, root) in enumerate((("BEFORE", baseline), ("AFTER", candidate))):
            card = checker(220, 220)
            icon = asset(root, key).resize((188, 188), Image.Resampling.LANCZOS)
            card.alpha_composite(icon, (16, 16))
            y = 132 + row * 260
            canvas.paste(card.convert("RGB"), (x, y))
            text(draw, (x + 110, y + 242), caption, 16, True, "ma")
    canvas.save(output, optimize=True)


def create_reward_lineup(baseline: Path, candidate: Path, output: Path) -> None:
    canvas = base(1900, 780, "EVERY REWARD CARD — ONE DISTINCT SEMANTIC MEDALLION")
    draw = ImageDraw.Draw(canvas)
    for index, (title, key) in enumerate(REWARD_ICONS):
        column = index % 4
        row = index // 4
        x = 100 + column * 450
        y = 135 + row * 300
        root = candidate if key in NEW_REWARD_KEYS else baseline
        medallion = asset(root, key).resize((168, 168), Image.Resampling.LANCZOS)
        card = Image.new("RGBA", (360, 220), (13, 38, 32, 255))
        border = ImageDraw.Draw(card)
        border.rounded_rectangle((2, 2, 357, 217), 16, outline=GOLD, width=4)
        card.alpha_composite(medallion, (96, 8))
        canvas.paste(card.convert("RGB"), (x, y))
        text(draw, (x + 180, y + 250), title, 17, True, "ma")
        text(draw, (x + 180, y + 276), key.removeprefix("ui_"), 14, False, "ma")
    canvas.save(output, optimize=True)


def create_readability(baseline: Path, candidate: Path, output: Path) -> None:
    entries = [(f"P{tier}", key, candidate) for tier, key in enumerate(POTION_KEYS, 1)]
    entries += [(title.split()[0], key, candidate if key in NEW_REWARD_KEYS else baseline)
                for title, key in REWARD_ICONS]
    canvas = base(1900, 790, "54 PX RUNTIME — DARK / LIGHT / GRAYSCALE READABILITY")
    draw = ImageDraw.Draw(canvas)
    row_specs = (("DARK", (12, 34, 29)), ("LIGHT", (207, 209, 193)), ("VALUE", (52, 52, 52)))
    for row, (row_name, color) in enumerate(row_specs):
        y = 135 + row * 205
        text(draw, (76, y + 70), row_name, 19, True, "lm")
        for index, (label, key, root) in enumerate(entries):
            x = 150 + index * 122
            card = Image.new("RGBA", (112, 142), color + (255,))
            icon = asset(root, key).resize((68, 68), Image.Resampling.LANCZOS)
            if row_name == "VALUE":
                icon = ImageOps.grayscale(icon).convert("RGBA")
            card.alpha_composite(icon, (22, 16))
            canvas.paste(card.convert("RGB"), (x, y))
            text(draw, (x + 56, y + 119), label, 11, False, "ma")
    canvas.save(output, optimize=True)


def create_value_progression(candidate: Path, output: Path) -> None:
    canvas = base(1900, 690, "POTION TIERS — SILHOUETTE ESCALATION WITHOUT HUE DEPENDENCE")
    draw = ImageDraw.Draw(canvas)
    constructions = (
        "clean vial", "one collar leaf", "paired leaves",
        "leaves + foot ring", "shoulder seeds", "cradle + stopper",
    )
    for index, key in enumerate(POTION_KEYS):
        x = 63 + index * 306
        raw = asset(candidate, key).resize((220, 220), Image.Resampling.LANCZOS)
        value = ImageOps.grayscale(raw).convert("RGBA")
        silhouette = raw.copy()
        alpha = silhouette.getchannel("A")
        silhouette = Image.new("RGBA", silhouette.size, (229, 220, 190, 255))
        silhouette.putalpha(alpha)
        for row, icon in enumerate((value, silhouette)):
            card = Image.new("RGBA", (240, 240), ((39, 39, 39, 255) if row == 0 else (11, 32, 29, 255)))
            card.alpha_composite(icon, (10, 10))
            canvas.paste(card.convert("RGB"), (x, 130 + row * 250))
        text(draw, (x + 120, 105), f"TIER {index + 1}", 20, True, "ma")
        text(draw, (x + 120, 625), constructions[index], 13, False, "ma")
    canvas.save(output, optimize=True)


def create_integrated_surface(baseline: Path, candidate: Path, output: Path) -> None:
    canvas = base(1900, 1000, "INTEGRATED REWARDS & PICKUPS — RESTRAINED PREMIUM HIERARCHY")
    draw = ImageDraw.Draw(canvas)
    # Three representative reward cards.
    cards = (("MIGHT OF OAK", "ui_strength", "+2 Strength"),
             ("VERDANT POWER", "ui_general_power", "+10% all damage"),
             ("CRIMSON ROOT", "ui_lifesteal", "+3% lifesteal"))
    for index, (title, key, detail) in enumerate(cards):
        x, y = 75, 150 + index * 235
        draw.rounded_rectangle((x, y, 900, y + 185), 18, fill=(15, 47, 38), outline=GOLD, width=4)
        root = candidate if key in NEW_REWARD_KEYS else baseline
        canvas.alpha_composite(asset(root, key).resize((142, 142), Image.Resampling.LANCZOS), (x + 28, y + 20))
        text(draw, (x + 200, y + 63), title, 24, True, "lm")
        text(draw, (x + 200, y + 118), detail, 20, False, "lm")
    # Ground pause, then visible homing direction into Inventory.
    text(draw, (1390, 130), "GROUND → HOMING → INVENTORY", 21, True, "ma")
    draw.rounded_rectangle((1010, 165, 1815, 820), 18, fill=(9, 30, 27), outline=(78, 116, 96), width=3)
    inventory = asset(baseline, "ui_inventory").resize((130, 130), Image.Resampling.LANCZOS)
    canvas.alpha_composite(inventory, (1610, 205))
    for index, key in enumerate(POTION_KEYS):
        start_x = 1070 + (index % 3) * 190
        start_y = 620 + (index // 3) * 120
        icon = asset(candidate, key).resize((88, 88), Image.Resampling.LANCZOS)
        canvas.alpha_composite(icon, (start_x, start_y))
        end_x, end_y = 1660, 285
        draw.line((start_x + 44, start_y + 34, end_x, end_y), fill=(102, 172, 124), width=3)
        draw.polygon(((end_x, end_y), (end_x - 17, end_y + 6), (end_x - 8, end_y + 20)), fill=GOLD)
    text(draw, (1410, 875), "Each tier rests visibly before a clear collection arc.", 18, False, "ma")
    canvas.save(output, optimize=True)


def base(width: int, height: int, title: str) -> Image.Image:
    image = Image.new("RGBA", (width, height), INK + (255,))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((13, 13, width - 14, height - 14), 22, outline=GOLD, width=3)
    text(draw, (width // 2, 55), title, 31, True, "ma")
    return image


def checker(width: int, height: int) -> Image.Image:
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    size = 16
    colors = ((41, 63, 59, 255), (55, 78, 73, 255))
    for y in range(0, height, size):
        for x in range(0, width, size):
            draw.rectangle((x, y, x + size - 1, y + size - 1), fill=colors[(x // size + y // size) % 2])
    return image


def asset(root: Path, key: str) -> Image.Image:
    manifest = read_json(root / "asset_manifest.json")
    entry = next((item for item in manifest["assets"] if item["key"] == key), None)
    if entry is None:
        raise ValueError(f"{root} lacks {key}")
    return Image.open(root / entry["sheet"]).convert("RGBA")


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    names = (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
    )
    for name in names:
        if Path(name).is_file():
            return ImageFont.truetype(name, size)
    return ImageFont.load_default()


def text(draw: ImageDraw.ImageDraw, position: tuple[int, int], value: str,
         size: int, bold: bool = False, anchor: str = "la") -> None:
    draw.text(position, value, font=font(size, bold), fill=PARCHMENT, anchor=anchor)


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


if __name__ == "__main__":
    main()
