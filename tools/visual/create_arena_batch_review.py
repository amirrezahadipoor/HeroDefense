#!/usr/bin/env python3
"""Audit the exact arena batch and build deterministic premium-v2 review evidence."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageOps, ImageStat

from create_character_animation_review import (
    CharacterFrames,
    canvas_base,
    checker,
    presentation_card,
    text,
)

EXPECTED_KEYS = (
    "arena_backdrop",
    "ground_tile_0",
    "ground_tile_1",
    "ground_tile_2",
    "crystal_prop_0",
    "crystal_prop_1",
    "crystal_prop_2",
)
GROUND_IDENTITIES = ("root-path", "waystone-crossing", "moss-clearing")
CRYSTAL_IDENTITIES = ("azure-waystone-fan", "violet-moon-geode", "amber-root-lantern")
EXPECTED_PIVOT = {"units": "normalized-bottom-left", "x": 0.5, "y": 0.5}
DECODED_BUDGET = 2 * 1024 * 1024
VIEWPORT = (720, 1280)


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
    create_integrated_composition(baseline, candidate, output / "arena_integrated_composition.png")
    create_backdrop_value_sheet(candidate, audit, output / "arena_backdrop_value.png")
    create_ground_lineup(baseline, candidate, audit, output / "arena_ground_lineup.png")
    create_crystal_lineup(baseline, candidate, audit, output / "arena_crystal_lineup.png")
    create_runtime_readability(candidate, output / "arena_runtime_readability.png")
    create_depth_hierarchy(candidate, output / "arena_depth_hierarchy.png")

    sheets = sorted(output.glob("*.png"))
    audit["reviewSheets"] = {
        path.name: {"bytes": path.stat().st_size, "sha256": sha256(path)}
        for path in sheets
    }
    audit["reviewSheetCount"] = len(sheets)
    audit_path = output / "arena_audit.json"
    audit_path.write_text(json.dumps(audit, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(
        f"Audited {audit['summary']['assetCount']} arena assets and wrote "
        f"{len(sheets)} review sheets to {output}"
    )


def audit_batch(baseline: Path, candidate: Path) -> dict:
    baseline_manifest_path = baseline / "asset_manifest.json"
    candidate_manifest_path = candidate / "asset_manifest.json"
    baseline_manifest = read_json(baseline_manifest_path)
    candidate_manifest = read_json(candidate_manifest_path)
    actual_keys = sorted(asset["key"] for asset in candidate_manifest.get("assets", []))
    if actual_keys != sorted(EXPECTED_KEYS):
        raise ValueError(f"Candidate must contain exactly {EXPECTED_KEYS}; found {actual_keys}")
    expected_global = {
        "pipelineVersion": 3,
        "generatedBatch": "arena",
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

    candidate_entries = {asset["key"]: asset for asset in candidate_manifest["assets"]}
    baseline_entries = {asset["key"]: asset for asset in baseline_manifest["assets"]}
    payload = expected_payload(candidate)
    records = []
    total_decoded = 0
    minimum_margin = 10_000
    seen_hashes = set()

    for key in EXPECTED_KEYS:
        entry = candidate_entries[key]
        validate_metadata(entry, key)
        metadata_path = candidate / "environment" / f"{key}.json"
        if read_json(metadata_path) != entry:
            raise ValueError(f"{key} manifest and per-asset metadata differ")
        image_path = candidate / entry["sheet"]
        image = Image.open(image_path).convert("RGBA")
        expected_dimensions = (360, 640) if key == "arena_backdrop" else (192, 192)
        if image.size != expected_dimensions:
            raise ValueError(f"{key} is {image.size}, expected {expected_dimensions}")
        digest = sha256(image_path)
        if digest in seen_hashes:
            raise ValueError(f"{key} duplicates another arena render")
        seen_hashes.add(digest)
        decoded = image.width * image.height * 4
        total_decoded += decoded
        alpha = image.getchannel("A")
        bounds = alpha.getbbox()
        if bounds is None:
            raise ValueError(f"{key} is empty")
        record = {
            "key": key,
            "sheetSha256": digest,
            "metadataSha256": sha256(metadata_path),
            "dimensions": {"width": image.width, "height": image.height},
            "decodedBytes": decoded,
            "triangles": entry["triangles"],
            "meshParts": entry["meshParts"],
            "materialCount": entry["materialCount"],
            "modelRevision": entry["modelRevision"],
        }
        if key == "arena_backdrop":
            edge_minimum = minimum_edge_alpha(alpha)
            opaque_fraction = alpha.histogram()[255] / (image.width * image.height)
            if edge_minimum < 250 or opaque_fraction < 0.995:
                raise ValueError(
                    f"Backdrop is not full bleed: edge alpha {edge_minimum}, "
                    f"opaque fraction {opaque_fraction:.5f}"
                )
            value = ImageOps.grayscale(image.convert("RGB"))
            center_mean = ImageStat.Stat(value.crop((108, 96, 252, 576))).mean[0]
            edge = Image.new("L", image.size)
            edge.paste(value.crop((0, 0, 72, 640)), (0, 0))
            edge.paste(value.crop((288, 0, 360, 640)), (72, 0))
            edge_mean = ImageStat.Stat(edge.crop((0, 0, 144, 640))).mean[0]
            overall_mean = ImageStat.Stat(value).mean[0]
            if not (20 <= overall_mean <= 105):
                raise ValueError(f"Backdrop mean value is not restrained: {overall_mean:.2f}")
            if center_mean < edge_mean + 1.0:
                raise ValueError(
                    f"Backdrop does not preserve the clear lane: center {center_mean:.2f}, "
                    f"edge {edge_mean:.2f}"
                )
            record.update({
                "fullBleedMinimumEdgeAlpha": edge_minimum,
                "opaquePixelFraction": round(opaque_fraction, 6),
                "meanValue": round(overall_mean, 3),
                "centerLaneMeanValue": round(center_mean, 3),
                "edgeMeanValue": round(edge_mean, 3),
            })
        else:
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
            record["alphaMargins"] = margins
            baseline_entry = baseline_entries.get(key)
            if baseline_entry is None:
                raise ValueError(f"Baseline is missing {key}")
            baseline_path = baseline / baseline_entry["sheet"]
            if sha256(baseline_path) == digest:
                raise ValueError(f"{key} is byte-identical to its baseline")
            record["baselineSheetSha256"] = sha256(baseline_path)
        records.append(record)

    if total_decoded > DECODED_BUDGET:
        raise ValueError(f"Arena batch decodes to {total_decoded}, over {DECODED_BUDGET}")
    payload_hashes = {relative: sha256(candidate / relative) for relative in payload}
    return {
        "schemaVersion": 1,
        "batch": "arena-premium-v2",
        "expectedKeys": list(EXPECTED_KEYS),
        "baselineManifestSha256": sha256(baseline_manifest_path),
        "candidateManifestSha256": sha256(candidate_manifest_path),
        "candidatePayload": payload_hashes,
        "assets": records,
        "summary": {
            "assetCount": len(records),
            "staticFrameCount": len(records),
            "portraitBackdropCount": 1,
            "groundTileCount": 3,
            "crystalPropCount": 3,
            "decodedBytes": total_decoded,
            "decodedBudgetBytes": DECODED_BUDGET,
            "minimumTransparentAssetMargin": minimum_margin,
            "minimumTriangles": min(record["triangles"] for record in records),
            "maximumTriangles": max(record["triangles"] for record in records),
            "minimumMeshParts": min(record["meshParts"] for record in records),
            "minimumMaterialCount": min(record["materialCount"] for record in records),
        },
    }


def validate_metadata(entry: dict, key: str) -> None:
    if key == "arena_backdrop":
        expected = {
            "family": "environment",
            "frameClass": "arena",
            "frameSize": 360,
            "frameWidth": 360,
            "frameHeight": 640,
            "sheetWidth": 360,
            "sheetHeight": 640,
            "modelRevision": "forest-sanctuary-backdrop-v2",
            "compositionProfile": "portrait-clear-lane-v2",
            "depthBands": 5,
            "visualQuality": "premium-v2",
        }
        triangle_range = (1_000, 3_000)
        minimum_parts = 45
        minimum_materials = 8
    elif key.startswith("ground_tile_"):
        variant = int(key[-1])
        expected = {
            "family": "environment",
            "frameClass": "environment",
            "frameSize": 192,
            "frameWidth": 192,
            "frameHeight": 192,
            "sheetWidth": 192,
            "sheetHeight": 192,
            "modelRevision": "arena-ground-premium-v2",
            "groundIdentity": GROUND_IDENTITIES[variant],
            "variant": variant,
            "visualQuality": "premium-v2",
        }
        triangle_range = (300, 600)
        minimum_parts = 20
        minimum_materials = 6
    else:
        variant = int(key[-1])
        expected = {
            "family": "environment",
            "frameClass": "environment",
            "frameSize": 192,
            "frameWidth": 192,
            "frameHeight": 192,
            "sheetWidth": 192,
            "sheetHeight": 192,
            "modelRevision": "arena-crystal-premium-v2",
            "prop": CRYSTAL_IDENTITIES[variant],
            "variant": variant,
            "runtimeGlow": False,
            "visualQuality": "premium-v2",
        }
        triangle_range = (700, 2_200)
        minimum_parts = 30
        minimum_materials = 8
    for field, expected_value in expected.items():
        if entry.get(field) != expected_value:
            raise ValueError(f"{key} {field} is {entry.get(field)!r}; expected {expected_value!r}")
    exact_common = {
        "sheet": f"environment/{key}.png",
        "pivot": EXPECTED_PIVOT,
        "alphaMode": "STRAIGHT_RGBA",
        "renderSupersample": 2,
        "renderSamples": 16,
    }
    for field, expected_value in exact_common.items():
        if entry.get(field) != expected_value:
            raise ValueError(f"{key} {field} is {entry.get(field)!r}; expected {expected_value!r}")
    triangles = int(entry.get("triangles", 0))
    if not triangle_range[0] <= triangles <= triangle_range[1]:
        raise ValueError(f"{key} triangles {triangles} are outside {triangle_range}")
    if int(entry.get("meshParts", 0)) < minimum_parts:
        raise ValueError(f"{key} has too few purposeful mesh parts")
    if int(entry.get("materialCount", 0)) < minimum_materials:
        raise ValueError(f"{key} has too few coherent material groups")
    width = expected["frameWidth"]
    height = expected["frameHeight"]
    expected_sheet = [{
        "decodedBytes": width * height * 4,
        "file": f"environment/{key}.png",
        "height": height,
        "width": width,
    }]
    if entry.get("sheets") != expected_sheet:
        raise ValueError(f"{key} sheet contract mismatch")
    expected_clips = {"idle": [{
        "height": height, "index": 0, "page": 0,
        "width": width, "x": 0, "y": 0,
    }]}
    if entry.get("clips") != expected_clips:
        raise ValueError(f"{key} static clip contract mismatch")


def create_integrated_composition(baseline: Path, candidate: Path, output: Path) -> None:
    width, height = 1640, 1490
    canvas = canvas_base(width, height, "ARENA ENVIRONMENT — 720×1280 INTEGRATED COMPOSITION")
    draw = ImageDraw.Draw(canvas)
    old = compose_arena(baseline, candidate=False)
    new = compose_arena(candidate, candidate=True, actors_root=baseline)
    panels = (("ACCEPTED BASELINE", old), ("PREMIUM V2 CANDIDATE", new))
    for index, (label, scene) in enumerate(panels):
        x = 70 + index * 790
        y = 135
        canvas.paste(scene.convert("RGB"), (x, y))
        draw.rectangle((x, y, x + 720, y + 1280), outline="#728A83", width=3)
        text(draw, (x + 360, y - 20), label, 22, bold=True, anchor="ma")
    text(
        draw, (width // 2, 1450),
        "Reference viewport at 1× • Hero and World Tree remain primary • edge landmarks frame, never crowd, the combat lane",
        18, anchor="ma", color="#AFC5BE",
    )
    canvas.save(output, optimize=True)


def create_backdrop_value_sheet(candidate: Path, audit: dict, output: Path) -> None:
    image = asset_image(candidate, "arena_backdrop")
    grayscale = ImageOps.grayscale(image.convert("RGB")).convert("RGBA")
    width, height = 1540, 930
    canvas = canvas_base(width, height, "ARENA BACKDROP — NATIVE DETAIL & VALUE HIERARCHY")
    draw = ImageDraw.Draw(canvas)
    panels = (
        ("COLOR — 1× RUNTIME SOURCE", image),
        ("GRAYSCALE — CLEAR CENTER LANE", grayscale),
    )
    for index, (label, panel) in enumerate(panels):
        shown = panel.resize((450, 800), Image.Resampling.NEAREST)
        x = 170 + index * 750
        y = 105
        canvas.paste(shown.convert("RGB"), (x, y))
        draw.rectangle((x, y, x + 450, y + 800), outline="#728A83", width=3)
        text(draw, (x + 225, y - 18), label, 20, bold=True, anchor="ma")
        # Show the central 40% audit lane without obscuring the rendered evidence.
        draw.rectangle((x + 135, y + 120, x + 315, y + 720), outline="#D6AD4C", width=2)
    record = next(value for value in audit["assets"] if value["key"] == "arena_backdrop")
    text(
        draw, (width // 2, 890),
        f"mean value {record['meanValue']:.1f} • lane {record['centerLaneMeanValue']:.1f} • edges {record['edgeMeanValue']:.1f} • full-bleed alpha {record['fullBleedMinimumEdgeAlpha']}",
        18, bold=True, anchor="ma", color="#F2D58A",
    )
    canvas.save(output, optimize=True)


def create_ground_lineup(baseline: Path, candidate: Path, audit: dict, output: Path) -> None:
    width, height = 1710, 820
    canvas = canvas_base(width, height, "GROUND PATCHES — BASELINE VS PREMIUM V2")
    draw = ImageDraw.Draw(canvas)
    for variant in range(3):
        x = 55 + variant * 550
        text(draw, (x + 245, 90), GROUND_IDENTITIES[variant].replace("-", " ").upper(),
             20, bold=True, anchor="ma")
        for column, (label, root) in enumerate((("BEFORE", baseline), ("AFTER", candidate))):
            sprite = asset_image(root, f"ground_tile_{variant}")
            card = checker(230, 230)
            card.alpha_composite(sprite.resize((230, 230), Image.Resampling.LANCZOS))
            px = x + column * 255
            canvas.paste(card.convert("RGB"), (px, 125))
            text(draw, (px + 115, 382), label, 17, bold=True, anchor="ma")
        scene = Image.new("RGBA", (485, 245), (19, 37, 32, 255))
        sprite = asset_image(candidate, f"ground_tile_{variant}")
        for row in range(2):
            for column in range(3):
                patch = sprite.resize((190, 145), Image.Resampling.LANCZOS)
                scene.alpha_composite(patch, (-35 + column * 155 + row * 24, -10 + row * 90))
        canvas.paste(scene.convert("RGB"), (x, 450))
        text(draw, (x + 242, 724), "STAGGERED OVERLAP", 16, anchor="ma", color="#AFC5BE")
    summary = audit["summary"]
    text(draw, (width // 2, 780),
         f"3/3 variants • minimum transparent margin {summary['minimumTransparentAssetMargin']} px • all under 600 triangles",
         18, anchor="ma", color="#F2D58A")
    canvas.save(output, optimize=True)


def create_crystal_lineup(baseline: Path, candidate: Path, audit: dict, output: Path) -> None:
    width, height = 1710, 760
    canvas = canvas_base(width, height, "CRYSTAL LANDMARKS — CONSTRUCTION, SILHOUETTE, MATERIAL")
    draw = ImageDraw.Draw(canvas)
    for variant in range(3):
        x = 55 + variant * 550
        text(draw, (x + 245, 88), CRYSTAL_IDENTITIES[variant].replace("-", " ").upper(),
             20, bold=True, anchor="ma")
        old = asset_image(baseline, f"crystal_prop_{variant}")
        new = asset_image(candidate, f"crystal_prop_{variant}")
        for column, (label, sprite) in enumerate((("BEFORE", old), ("AFTER", new))):
            card = presentation_card(sprite, "checker", 230, 315)
            px = x + column * 255
            canvas.paste(card.convert("RGB"), (px, 125))
            text(draw, (px + 115, 468), label, 17, bold=True, anchor="ma")
        silhouette = silhouette_image(new, (485, 170))
        canvas.paste(silhouette.convert("RGB"), (x, 520))
        text(draw, (x + 242, 716), "IDENTITY AT SILHOUETTE SCALE", 15,
             anchor="ma", color="#AFC5BE")
    canvas.save(output, optimize=True)


def create_runtime_readability(candidate: Path, output: Path) -> None:
    width, height = 1580, 780
    canvas = canvas_base(width, height, "ARENA PROPS — RUNTIME SCALE READABILITY")
    draw = ImageDraw.Draw(canvas)
    backgrounds = (("DARK ARENA", (19, 37, 32)), ("LIGHT CHECK", (198, 204, 190)),
                   ("GRAYSCALE", (57, 57, 57)))
    sizes = (172, 148, 136)
    for row, (label, color) in enumerate(backgrounds):
        y = 120 + row * 205
        text(draw, (45, y + 80), label, 18, bold=True, anchor="lm")
        for variant in range(3):
            sprite = asset_image(candidate, f"crystal_prop_{variant}")
            if row == 2:
                sprite = ImageOps.grayscale(sprite).convert("RGBA")
            size = sizes[variant]
            panel = Image.new("RGBA", (380, 180), (*color, 255))
            shown = sprite.resize((size, size), Image.Resampling.LANCZOS)
            panel.alpha_composite(shown, ((380 - size) // 2, (180 - size) // 2))
            x = 270 + variant * 420
            canvas.paste(panel.convert("RGB"), (x, y))
            if row == 0:
                text(draw, (x + 190, y - 16), f"{CRYSTAL_IDENTITIES[variant]} • {size}px draw box",
                     16, anchor="ma", color="#C7D4CE")
    text(draw, (width // 2, 748), "No baked glow • crystal accents stay below Hero/Tree contrast • hue is not the only identity cue",
         18, anchor="ma", color="#F2D58A")
    canvas.save(output, optimize=True)


def create_depth_hierarchy(candidate: Path, output: Path) -> None:
    scene = compose_arena(candidate, candidate=True)
    value = ImageOps.grayscale(scene.convert("RGB"))
    blurred = value.resize((180, 320), Image.Resampling.BILINEAR).resize(VIEWPORT, Image.Resampling.BILINEAR)
    width, height = 1640, 1430
    canvas = canvas_base(width, height, "ARENA DEPTH TREATMENT — CLEAN LANE & BROAD VALUES")
    draw = ImageDraw.Draw(canvas)
    panels = (("FULL COLOR", scene.convert("RGB")), ("VALUE MASSES", blurred.convert("RGB")))
    for index, (label, panel) in enumerate(panels):
        x = 70 + index * 790
        y = 115
        canvas.paste(panel, (x, y))
        draw.rectangle((x + 162, y + 110, x + 558, y + 1160), outline="#D6AD4C", width=3)
        text(draw, (x + 360, y - 16), label, 20, bold=True, anchor="ma")
    text(draw, (width // 2, 1410),
         "Gold box marks the protected center 55% • upper props reduce in scale/value • near-edge forms remain peripheral",
         18, anchor="ma", color="#AFC5BE")
    canvas.save(output, optimize=True)


def compose_arena(root: Path, candidate: bool, actors_root: Path | None = None) -> Image.Image:
    width, height = VIEWPORT
    if candidate:
        backdrop = asset_image(root, "arena_backdrop").resize(VIEWPORT, Image.Resampling.BILINEAR)
        scene = backdrop.copy()
        for row in range(8):
            depth = row / 7
            tile_width = round(252 - depth * 42)
            tile_height = round(184 - depth * 32)
            shade = 0.96 - depth * 0.23
            for column in range(4):
                variant = (row * 2 + column) % 3
                tile = asset_image(root, f"ground_tile_{variant}")
                tile = tint(tile, shade * 0.92, shade, shade * 0.95, 0.96)
                world_paste(
                    scene, tile,
                    -46 + column * 193 + (row % 2) * 31,
                    10 + row * 143,
                    tile_width, tile_height,
                )
        placements = ((-8, 120, 172, 0), (556, 205, 164, 1),
                      (4, 820, 148, 2), (568, 884, 136, 0))
    else:
        scene = Image.new("RGBA", VIEWPORT, (24, 52, 43, 255))
        for row in range(6):
            for column in range(4):
                variant = (row * 2 + column) % 3
                world_paste(scene, asset_image(root, f"ground_tile_{variant}"),
                            -32 + column * 196 + (row % 2) * 34,
                            35 + row * 172, 224, 164)
        placements = ((18, 150, 128, 0), (574, 210, 128, 1),
                      (24, 805, 128, 2), (570, 850, 128, 0))
    for x, y, size, variant in placements:
        world_paste(scene, asset_image(root, f"crystal_prop_{variant}"), x, y, size, size)

    actor_assets = actors_root or root
    try:
        tree = CharacterFrames(actor_assets, "world_tree_healthy").frame("idle", 0)
        world_paste(scene, tree, 360 - 165, 755 - 31, 330, 330)
        hero = CharacterFrames(actor_assets, "hero").frame("idle", 0)
        world_paste(scene, hero, 360 - 96, 600 - 28, 192, 192)
        rootling = CharacterFrames(actor_assets, "rootling").frame("idle", 0)
        world_paste(scene, rootling, 135, 475, 138, 138)
        stone = CharacterFrames(actor_assets, "stone_beetle").frame("idle", 0)
        world_paste(scene, stone, 500, 425, 132, 132)
    except (FileNotFoundError, KeyError):
        pass
    return scene


def expected_payload(candidate: Path) -> list[str]:
    expected = ["asset_manifest.json"]
    for key in EXPECTED_KEYS:
        expected.extend((f"environment/{key}.json", f"environment/{key}.png"))
    actual = sorted(
        path.relative_to(candidate).as_posix()
        for path in candidate.rglob("*") if path.is_file()
    )
    if actual != sorted(expected):
        raise ValueError(f"Arena candidate payload mismatch; expected {sorted(expected)}, found {actual}")
    return sorted(expected)


def asset_image(root: Path, key: str) -> Image.Image:
    if key == "arena_backdrop":
        path = root / "environment/arena_backdrop.png"
    else:
        path = root / "environment" / f"{key}.png"
    return Image.open(path).convert("RGBA")


def world_paste(canvas: Image.Image, sprite: Image.Image, x: float, y: float,
                width: float, height: float) -> None:
    width_i, height_i = max(1, round(width)), max(1, round(height))
    shown = sprite.resize((width_i, height_i), Image.Resampling.LANCZOS)
    canvas.alpha_composite(shown, (round(x), canvas.height - round(y) - height_i))


def tint(image: Image.Image, red: float, green: float, blue: float, alpha: float) -> Image.Image:
    channels = image.split()
    values = (red, green, blue, alpha)
    adjusted = [channel.point(lambda value, factor=factor: round(value * factor))
                for channel, factor in zip(channels, values, strict=True)]
    return Image.merge("RGBA", adjusted)


def silhouette_image(sprite: Image.Image, size: tuple[int, int]) -> Image.Image:
    panel = Image.new("RGBA", size, (18, 30, 28, 255))
    alpha = sprite.getchannel("A")
    silhouette = Image.new("RGBA", sprite.size, (208, 224, 213, 0))
    silhouette.putalpha(alpha)
    shown = silhouette.resize((150, 150), Image.Resampling.LANCZOS)
    for x in (25, 167, 309):
        panel.alpha_composite(shown, (x, 10))
    return panel


def minimum_edge_alpha(alpha: Image.Image) -> int:
    values = []
    values.extend(alpha.crop((0, 0, alpha.width, 1)).tobytes())
    values.extend(alpha.crop((0, alpha.height - 1, alpha.width, alpha.height)).tobytes())
    values.extend(alpha.crop((0, 0, 1, alpha.height)).tobytes())
    values.extend(alpha.crop((alpha.width - 1, 0, alpha.width, alpha.height)).tobytes())
    return min(values)


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


if __name__ == "__main__":
    main()
