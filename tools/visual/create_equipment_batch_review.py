#!/usr/bin/env python3
"""Audit every equipment socket frame and create deterministic review sheets."""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont, ImageStat

from review_strips import grade_row, silhouette_view

CLIPS = ("idle", "attack", "hit", "death")
POSES = (
    ("Idle", "idle", 0),
    ("Anticipation", "attack", 2),
    ("Impact", "attack", 4),
    ("Hit", "hit", 1),
    ("Death", "death", 8),
)
TIERS = ("COMMON", "UNCOMMON", "RARE", "LEGENDARY")
EXPECTED_FRAME_COUNTS = {"idle": 6, "attack": 8, "hit": 4, "death": 10}
EXPECTED_VISUAL_QUALITY = "premium-v2"
EXPECTED_MODEL_REVISION = "equipment-premium-v2"
EXPECTED_RIG_PROFILE = "hero-socket-v2"
EXPECTED_FRAME_SIZE = 192
EXPECTED_SHEET_SIZE = (1920, 768)
EXPECTED_ICON_SIZE = (96, 96)
MAX_TRIANGLES = 900
FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("hero_root", type=Path)
    parser.add_argument("catalog", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    baseline = args.baseline.resolve()
    candidate = args.candidate.resolve()
    hero_root = args.hero_root.resolve()
    catalog_path = args.catalog.resolve()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)

    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    items = catalog["items"]
    item_ids = [item["id"] for item in items]
    if len(items) != 40 or len(set(item_ids)) != len(item_ids):
        raise ValueError("Equipment review requires exactly 40 unique catalog items")

    manifest_path = candidate / "asset_manifest.json"
    candidate_manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    candidate_assets = candidate_manifest["assets"]
    candidate_by_key = {asset["key"]: asset for asset in candidate_assets}
    if len(candidate_by_key) != len(candidate_assets):
        raise ValueError("Duplicate equipment candidate key")
    expected_keys = {f"equipment_{item_id}" for item_id in item_ids}
    if set(candidate_by_key) != expected_keys:
        raise ValueError(
            f"Equipment candidate key mismatch: {sorted(set(candidate_by_key) ^ expected_keys)}"
        )

    hero = Frames(hero_root, "sprites", "hero")
    audit = audit_batch(
        candidate,
        hero,
        items,
        candidate_by_key,
        catalog_path,
        manifest_path,
    )
    create_icon_sheets(baseline, candidate, items, output)
    create_composite_sheets(candidate, hero, items, output)
    review_sheet_names = [
        *(f"equipment_icons_{tier.lower()}.png" for tier in TIERS),
        *(f"equipment_composites_{page}.png" for page in range(1, 6)),
    ]
    audit["reviewSheets"] = {
        name: sha256_file(output / name) for name in review_sheet_names
    }
    (output / "equipment_alignment_audit.json").write_text(
        json.dumps(audit, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(f"Audited all {len(items) * 28} frames and reviewed {len(items)} atlases in {output}")


class Frames:
    def __init__(self, root: Path, folder: str, key: str) -> None:
        self.root = root.resolve()
        item_id = key.removeprefix("equipment_")
        self.metadata_path = resolve_inside(self.root, f"{folder}/{item_id}.json")
        self.metadata = json.loads(self.metadata_path.read_text(encoding="utf-8"))
        sheet_entries = self.metadata.get("sheets", [{"file": self.metadata["sheet"]}])
        self.sheet_paths = [resolve_inside(self.root, sheet["file"]) for sheet in sheet_entries]
        self.sheets = [Image.open(path).convert("RGBA") for path in self.sheet_paths]

    def frame_entries(self, clip: str) -> list[dict]:
        return sorted(self.metadata["clips"][clip], key=lambda frame: frame["index"])

    def frame(self, clip: str, index: int) -> Image.Image:
        frame = self.frame_entries(clip)[index]
        sheet = self.sheets[frame.get("page", 0)]
        return sheet.crop(
            (
                frame["x"],
                frame["y"],
                frame["x"] + frame["width"],
                frame["y"] + frame["height"],
            )
        )


def audit_batch(
    candidate: Path,
    hero: Frames,
    items: list[dict],
    candidate_by_key: dict[str, dict],
    catalog_path: Path,
    manifest_path: Path,
) -> dict:
    validate_hero_contract(hero)
    assets = []
    totals = {
        "assets": len(items),
        "frames": 0,
        "detachedFrames": 0,
        "boundaryFrames": 0,
        "maxTriangles": 0,
        "decodedBytes": 0,
        "compressedPngBytes": 0,
    }
    global_frame_margin = [10_000, 10_000, 10_000, 10_000]
    global_icon_margin = [10_000, 10_000, 10_000, 10_000]
    global_near_overlap = 10_000_000.0

    for item in items:
        item_id = item["id"]
        key = f"equipment_{item_id}"
        asset = candidate_by_key[key]
        overlay = Frames(candidate, "equipment", key)
        metadata = overlay.metadata
        if metadata != asset:
            raise ValueError(f"Manifest/metadata mismatch: {item_id}")
        validate_asset_contract(candidate, item, asset, overlay, hero)

        # Boot sockets remain planted during the chest/head-only idle loop; every
        # action still inherits the complete root/pelvis motion.
        expected_unique = {
            "idle": 1 if item["visualSlot"] == "boots" else 5,
            "attack": 7,
            "hit": 3,
            "death": 9,
        }
        min_margin = [10_000, 10_000, 10_000, 10_000]
        min_near_overlap = 10_000_000.0
        unique_by_clip = {}

        for clip in CLIPS:
            hero_frames = hero.frame_entries(clip)
            overlay_frames = overlay.frame_entries(clip)
            hero_contract = [frame_contract(frame) for frame in hero_frames]
            overlay_contract = [frame_contract(frame) for frame in overlay_frames]
            if overlay_contract != hero_contract:
                raise ValueError(f"Hero socket frame contract mismatch: {item_id} {clip}")

            hashes = set()
            for index in range(len(overlay_frames)):
                equipment_frame = overlay.frame(clip, index)
                alpha = equipment_frame.getchannel("A")
                bbox = alpha.getbbox()
                if bbox is None:
                    raise ValueError(f"Empty equipment frame: {item_id} {clip}/{index}")
                margins = (bbox[0], bbox[1], alpha.width - bbox[2], alpha.height - bbox[3])
                min_margin = [min(a, b) for a, b in zip(min_margin, margins)]
                global_frame_margin = [
                    min(a, b) for a, b in zip(global_frame_margin, margins)
                ]
                if min(margins) == 0:
                    totals["boundaryFrames"] += 1
                    raise ValueError(f"Boundary-clipped equipment: {item_id} {clip}/{index}")

                hero_alpha = hero.frame(clip, index).getchannel("A")
                nearby_hero = hero_alpha.filter(ImageFilter.MaxFilter(15))
                overlap = ImageChops.multiply(alpha, nearby_hero)
                weighted_overlap = ImageStat.Stat(overlap).sum[0] / 255.0
                min_near_overlap = min(min_near_overlap, weighted_overlap)
                global_near_overlap = min(global_near_overlap, weighted_overlap)
                if overlap.getbbox() is None:
                    totals["detachedFrames"] += 1
                    raise ValueError(f"Detached socket overlay: {item_id} {clip}/{index}")

                hashes.add(hashlib.sha256(equipment_frame.tobytes()).hexdigest())
                totals["frames"] += 1

            unique_by_clip[clip] = len(hashes)
            if len(hashes) < expected_unique[clip]:
                raise ValueError(
                    f"Insufficient visible {clip} motion for {item_id}: {len(hashes)}"
                )

        icon_path = resolve_inside(candidate, asset["icon"])
        icon = Image.open(icon_path).convert("RGBA")
        icon_bbox = icon.getchannel("A").getbbox()
        if icon.size != EXPECTED_ICON_SIZE or icon_bbox is None:
            raise ValueError(f"Invalid equipment icon: {item_id}")
        icon_margins = [
            icon_bbox[0],
            icon_bbox[1],
            icon.width - icon_bbox[2],
            icon.height - icon_bbox[3],
        ]
        global_icon_margin = [
            min(a, b) for a, b in zip(global_icon_margin, icon_margins)
        ]
        if min(icon_margins) == 0:
            raise ValueError(f"Clipped equipment icon: {item_id}")

        sheet_path = overlay.sheet_paths[0]
        atlas_path = resolve_inside(candidate, asset["atlas"])
        triangles = int(metadata["triangles"])
        totals["maxTriangles"] = max(totals["maxTriangles"], triangles)
        totals["decodedBytes"] += sheet_path_image_bytes(sheet_path) + 96 * 96 * 4
        totals["compressedPngBytes"] += sheet_path.stat().st_size + icon_path.stat().st_size
        assets.append(
            {
                "id": item_id,
                "key": key,
                "slot": item["slot"],
                "visualSlot": item["visualSlot"],
                "tier": item["tier"],
                "triangles": triangles,
                "frameCount": sum(len(metadata["clips"][clip]) for clip in CLIPS),
                "uniqueFrames": unique_by_clip,
                "minimumFrameMargins": margins_object(min_margin),
                "minimumNearHeroPixels": round(min_near_overlap, 2),
                "iconMargins": margins_object(icon_margins),
                "sheetSha256": sha256_file(sheet_path),
                "iconSha256": sha256_file(icon_path),
                "atlasSha256": sha256_file(atlas_path),
                "metadataSha256": sha256_file(overlay.metadata_path),
            }
        )

    if totals["frames"] != len(items) * sum(EXPECTED_FRAME_COUNTS.values()):
        raise AssertionError(f"Unexpected equipment frame total: {totals['frames']}")
    totals.update(
        {
            "minimumFrameMargins": margins_object(global_frame_margin),
            "minimumIconMargins": margins_object(global_icon_margin),
            "minimumNearHeroPixels": round(global_near_overlap, 2),
            "tierCounts": dict(sorted(Counter(item["tier"] for item in items).items())),
            "visualSlotCounts": dict(
                sorted(Counter(item["visualSlot"] for item in items).items())
            ),
        }
    )
    return {
        "contract": "premium-v2-equipment",
        "catalogSha256": sha256_file(catalog_path),
        "candidateManifestSha256": sha256_file(manifest_path),
        "heroContract": {
            "key": "hero",
            "modelRevision": hero.metadata["modelRevision"],
            "rigProfile": hero.metadata["rigProfile"],
            "frameSize": hero.metadata["frameSize"],
            "sheetSha256": sha256_file(hero.sheet_paths[0]),
        },
        "summary": totals,
        "assets": assets,
    }


def validate_hero_contract(hero: Frames) -> None:
    if hero.metadata.get("key") != "hero":
        raise ValueError("Socket review requires the committed Hero")
    if hero.metadata.get("visualQuality") != EXPECTED_VISUAL_QUALITY:
        raise ValueError("Socket review requires the premium-v2 Hero")
    if hero.metadata.get("modelRevision") != "hero-premium-v2-final":
        raise ValueError("Socket review requires the finalized Hero model")
    if hero.metadata.get("rigProfile") != "premium-humanoid-v2":
        raise ValueError("Socket review requires the finalized Hero rig")
    if hero.metadata.get("frameSize") != EXPECTED_FRAME_SIZE:
        raise ValueError("Unexpected Hero frame size")
    if set(hero.metadata.get("clips", {})) != set(CLIPS):
        raise ValueError("Unexpected Hero clip contract")
    for clip, count in EXPECTED_FRAME_COUNTS.items():
        if len(hero.frame_entries(clip)) != count:
            raise ValueError(f"Unexpected Hero {clip} frame count")


def validate_asset_contract(
    candidate: Path,
    item: dict,
    asset: dict,
    overlay: Frames,
    hero: Frames,
) -> None:
    item_id = item["id"]
    expected = {
        "key": f"equipment_{item_id}",
        "family": "equipment",
        "itemId": item_id,
        "slot": item["slot"],
        "visualSlot": item["visualSlot"],
        "visualKind": item.get("visualKind", item["visualSlot"]),
        "tier": item["tier"],
        "frameClass": "character",
        "frameSize": EXPECTED_FRAME_SIZE,
        "sheet": f"equipment/{item_id}.png",
        "atlas": f"equipment/{item_id}.atlas",
        "icon": f"icons/equipment_{item_id}.png",
        "modelRevision": EXPECTED_MODEL_REVISION,
        "rigProfile": EXPECTED_RIG_PROFILE,
        "renderSupersample": 2,
        "renderSamples": 8,
        "visualQuality": EXPECTED_VISUAL_QUALITY,
        "runtimeGlow": item["tier"] in {"RARE", "LEGENDARY"},
        "boneAnimated": True,
    }
    for field, value in expected.items():
        if asset.get(field) != value:
            raise ValueError(
                f"Equipment contract mismatch: {item_id} {field}={asset.get(field)!r}, "
                f"expected {value!r}"
            )
    if asset.get("pivot") != hero.metadata.get("pivot"):
        raise ValueError(f"Hero/equipment pivot mismatch: {item_id}")
    if asset.get("bones") != hero.metadata.get("bones"):
        raise ValueError(f"Hero/equipment bone contract mismatch: {item_id}")
    if set(asset.get("clips", {})) != set(CLIPS):
        raise ValueError(f"Unexpected equipment clip contract: {item_id}")
    if len(asset.get("sheets", [])) != 1:
        raise ValueError(f"Equipment should fit one bounded atlas page: {item_id}")
    sheet = asset["sheets"][0]
    if (sheet.get("width"), sheet.get("height")) != EXPECTED_SHEET_SIZE:
        raise ValueError(f"Unexpected equipment sheet dimensions: {item_id}")
    if sheet.get("file") != expected["sheet"]:
        raise ValueError(f"Unexpected equipment sheet path: {item_id}")
    if sheet.get("decodedBytes") != EXPECTED_SHEET_SIZE[0] * EXPECTED_SHEET_SIZE[1] * 4:
        raise ValueError(f"Unexpected equipment decoded-byte declaration: {item_id}")
    triangles = int(asset.get("triangles", 0))
    if triangles <= 0 or triangles > MAX_TRIANGLES:
        raise ValueError(f"Equipment triangle hard cap exceeded: {item_id} {triangles}")
    for relative in (asset["sheet"], asset["atlas"], asset["icon"]):
        resolve_inside(candidate, relative)


def frame_contract(frame: dict) -> tuple[int, int, int, int, int, int]:
    return (
        frame["index"],
        frame.get("page", 0),
        frame["x"],
        frame["y"],
        frame["width"],
        frame["height"],
    )


def margins_object(values: list[int] | tuple[int, ...]) -> dict[str, int]:
    return {
        "left": values[0],
        "top": values[1],
        "right": values[2],
        "bottom": values[3],
    }


def sheet_path_image_bytes(path: Path) -> int:
    with Image.open(path) as image:
        return image.width * image.height * 4


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def resolve_inside(root: Path, relative: str) -> Path:
    value = Path(relative)
    if value.is_absolute():
        raise ValueError(f"Generated asset path must be relative: {relative}")
    path = (root / value).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError(f"Generated asset path escapes root: {relative}")
    if not path.is_file():
        raise FileNotFoundError(path)
    return path


def create_icon_sheets(
    baseline: Path,
    candidate: Path,
    items: list[dict],
    output: Path,
) -> None:
    for tier in TIERS:
        tier_items = [item for item in items if item["tier"] == tier]
        columns = 4
        rows = (len(tier_items) + columns - 1) // columns
        width, height = 1370, 100 + rows * 435 + 255
        canvas = base_canvas(width, height, f"{tier} EQUIPMENT — BEFORE / PREMIUM-V2 ICONS")
        draw = ImageDraw.Draw(canvas)
        for index, item in enumerate(tier_items):
            column, row = index % columns, index // columns
            x, y = 30 + column * 335, 88 + row * 435
            text(draw, (x + 155, y), title(item["id"]), 18, bold=True, anchor="ma")
            before = Image.open(
                baseline / "icons" / f"equipment_{item['id']}.png"
            ).convert("RGBA")
            after = Image.open(
                candidate / "icons" / f"equipment_{item['id']}.png"
            ).convert("RGBA")
            paste_card(canvas, before, x, y + 18, 145, 175)
            paste_card(canvas, after, x + 165, y + 18, 145, 175)
            text(draw, (x + 72, y + 215), "Before", 15, anchor="ma", color="#AFC5BE")
            text(draw, (x + 237, y + 215), "Premium", 15, anchor="ma", color="#AFC5BE")
            paste_card(canvas, silhouette_view(after), x + 82, y + 232, 145, 175)
            text(draw, (x + 155, y + 422), "Shape", 15, anchor="ma", color="#AFC5BE")
        first_after = Image.open(
            candidate / "icons" / f"equipment_{tier_items[0]['id']}.png"
        ).convert("RGBA")
        grade = grade_row(first_after)
        grade_y = 100 + rows * 435 + 65
        canvas.paste(grade.convert("RGB"), ((width - grade.width) // 2, grade_y))
        text(draw, (width // 2, grade_y - 18), f"STAGE GRADE — {title(tier_items[0]['id'])}",
             18, bold=True, anchor="ma")
        canvas.save(output / f"equipment_icons_{tier.lower()}.png", optimize=True)


def create_composite_sheets(
    candidate: Path,
    hero: Frames,
    items: list[dict],
    output: Path,
) -> None:
    page_size = 8
    total_pages = (len(items) + page_size - 1) // page_size
    for page_start in range(0, len(items), page_size):
        page_items = items[page_start : page_start + page_size]
        page_number = page_start // page_size + 1
        width, height = 1590, 1320
        canvas = base_canvas(
            width,
            height,
            f"EQUIPPED SOCKET & MOTION REVIEW — PAGE {page_number}/{total_pages}",
        )
        draw = ImageDraw.Draw(canvas)
        for index, item in enumerate(page_items):
            column, row = index % 2, index // 2
            x, y = 32 + column * 770, 92 + row * 245
            text(
                draw,
                (x, y),
                f"{title(item['id'])}  ·  {item['tier'].title()}  ·  {item['slot'].title()}",
                18,
                bold=True,
            )
            overlay = Frames(candidate, "equipment", f"equipment_{item['id']}")
            shots = []
            for caption, clip, frame_index in POSES:
                shots.append((
                    caption,
                    Image.alpha_composite(
                        hero.frame(clip, frame_index),
                        overlay.frame(clip, frame_index),
                    ),
                ))
            shots.append(("Shape", silhouette_view(shots[2][1])))
            for pose_index, (caption, composite) in enumerate(shots):
                card_x = x + pose_index * 120
                paste_card(canvas, composite, card_x, y + 28, 110, 165)
                text(
                    draw,
                    (card_x + 55, y + 215),
                    caption,
                    13,
                    anchor="ma",
                    color="#AFC5BE",
                )
        first_overlay = Frames(candidate, "equipment", f"equipment_{page_items[0]['id']}")
        first_impact = Image.alpha_composite(
            hero.frame("attack", 4), first_overlay.frame("attack", 4))
        grade = grade_row(first_impact)
        canvas.paste(grade.convert("RGB"), ((width - grade.width) // 2, 1100))
        text(draw, (width // 2, 1082),
             f"STAGE GRADE — {title(page_items[0]['id'])} IMPACT", 18, bold=True, anchor="ma")
        canvas.save(output / f"equipment_composites_{page_number}.png", optimize=True)


def base_canvas(width: int, height: int, heading: str) -> Image.Image:
    canvas = Image.new("RGB", (width, height), "#101A1B")
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(
        (12, 12, width - 12, height - 12),
        22,
        fill="#172829",
        outline="#C9A94F",
        width=3,
    )
    text(draw, (width // 2, 40), heading, 28, bold=True, anchor="ma", color="#F2D58A")
    return canvas


def paste_card(
    canvas: Image.Image,
    sprite: Image.Image,
    x: int,
    y: int,
    width: int,
    height: int,
) -> None:
    card = checker(width, height)
    scale = min((width - 12) / sprite.width, (height - 12) / sprite.height)
    size = (
        max(1, round(sprite.width * scale)),
        max(1, round(sprite.height * scale)),
    )
    preview = sprite.resize(size, Image.Resampling.NEAREST)
    card.alpha_composite(preview, ((width - size[0]) // 2, (height - size[1]) // 2))
    canvas.paste(card.convert("RGB"), (x, y))
    ImageDraw.Draw(canvas).rounded_rectangle(
        (x, y, x + width, y + height),
        9,
        outline="#58706A",
        width=2,
    )


def checker(width: int, height: int) -> Image.Image:
    image = Image.new("RGBA", (width, height), "#273635")
    draw = ImageDraw.Draw(image)
    step = 12
    for y in range(0, height, step):
        for x in range(0, width, step):
            if (x // step + y // step) % 2:
                draw.rectangle(
                    (x, y, x + step - 1, y + step - 1),
                    fill="#334744",
                )
    return image


def title(item_id: str) -> str:
    return item_id.replace("_", " ").title()


def text(
    draw: ImageDraw.ImageDraw,
    position: tuple[int, int],
    value: str,
    size: int,
    *,
    bold: bool = False,
    anchor: str | None = None,
    color: str = "#E7E1CF",
) -> None:
    draw.text(
        position,
        value,
        fill=color,
        font=ImageFont.truetype(BOLD_PATH if bold else FONT_PATH, size),
        anchor=anchor,
    )


if __name__ == "__main__":
    main()
