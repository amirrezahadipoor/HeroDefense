#!/usr/bin/env python3
"""Audit, document, and promote the Phase 18.4 planting-ceremony render batch.

Usage: promote_ceremony_batch.py CANDIDATE_DIR android/assets/generated

The candidate is the unpacked `hero-defense-ceremony-sprites` CI artifact (batch
`ceremony`). The script validates the render contract of the two assets
(`hero_ceremony`: walk 8 / plant 10 / water 10 on the accepted Hero rig with the
seed pouch and watering can; `world_tree_sapling`: grow 12 / idle 6 on the
accepted segmented World Tree rig), decodes every frame for an exhaustive audit,
writes the review sheets and the hash-bound audit record, requires the review
document to accept this exact candidate, and only then copies the payload and
binds the review into the committed catalog. Mirrors `promote_world_tree_batch.py`.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path

from PIL import Image, ImageDraw

REPOSITORY = Path(__file__).resolve().parents[2]
REVIEW_DOCUMENT = "docs/art_reviews/CEREMONY_PREMIUM_V2_REVIEW.md"
REVIEW_DIRECTORY = REPOSITORY / "docs/art_reviews/ceremony_premium_v2"
AUDIT_PATH = REVIEW_DIRECTORY / "ceremony_audit.json"
BATCH = "ceremony-premium-v2"
EXPECTED = {
    "hero_ceremony": {
        "family": "hero",
        "frameClass": "character",
        "frameSize": 192,
        "sheetWidth": 1920,
        "sheetHeight": 576,
        "pivot": {"units": "normalized-bottom-left", "x": 0.5, "y": 0.12},
        "rigProfile": "premium-humanoid-v2",
        "rigBoneCount": 25,
        "modelRevision": "hero-ceremony-v1",
        "ceremonyIdentity": "planting-ceremony-v1",
        "attachment_variant": "ceremony_props",
        "silhouette": "premium_elf_archer",
        "props": ["seed_pouch", "seed", "watering_can"],
    },
    "world_tree_sapling": {
        "family": "world_tree",
        "frameClass": "tree",
        "frameSize": 256,
        "sheetWidth": 2048,
        "sheetHeight": 768,
        "pivot": {"units": "normalized-bottom-left", "x": 0.5, "y": 0.06},
        "rigProfile": "segmented-world-tree-v2",
        "rigBoneCount": 13,
        "modelRevision": "world-tree-sapling-v1",
        "saplingIdentity": "heartwood-sapling-v1",
        "growsFrom": "seed",
        "state": "healthy",
    },
}
CLIPS = {
    "hero_ceremony": {"walk": 8, "plant": 10, "water": 10},
    "world_tree_sapling": {"grow": 12, "idle": 6},
}
MINIMUM_UNIQUE = {
    "hero_ceremony": {"walk": 5, "plant": 6, "water": 5},
    "world_tree_sapling": {"grow": 12, "idle": 4},
}
REVIEW_SHEETS = (
    "hero_ceremony_full_motion.png",
    "world_tree_sapling_growth.png",
    "ceremony_arena_scale.png",
)
BACKDROP = (44, 62, 52, 255)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("candidate", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--audit-only", action="store_true",
                        help="write sheets + audit, do not require acceptance or copy")
    args = parser.parse_args()
    source = args.candidate.resolve()
    destination = args.destination.resolve()
    if source == destination:
        raise ValueError("Candidate and destination must differ")

    manifest_path = source / "asset_manifest.json"
    manifest = read_json(manifest_path)
    if manifest.get("generatedBatch") != "ceremony" or manifest.get("pipelineVersion") != 3:
        raise ValueError("Candidate is not a pipeline-v3 ceremony batch")
    assets = manifest.get("assets", [])
    candidate = {asset["key"]: asset for asset in assets}
    if len(candidate) != len(assets) or set(candidate) != set(EXPECTED):
        raise ValueError("Ceremony batch must contain exactly hero_ceremony and world_tree_sapling")
    payload: set[Path] = set()
    for key in EXPECTED:
        validate_asset(candidate[key], key)
        if read_json(source / "sprites" / f"{key}.json") != candidate[key]:
            raise ValueError(f"Candidate manifest/metadata mismatch: {key}")
        payload |= {Path("sprites") / f"{key}.{ext}" for ext in ("png", "atlas", "json")}
    actual = {p.relative_to(source) for p in source.rglob("*")
              if p.is_file() and p.name != "asset_manifest.json"}
    if actual != payload:
        raise ValueError(f"Unexpected candidate payload: {sorted(actual ^ payload)}")

    frames = {key: decode_frames(source, candidate[key]) for key in EXPECTED}
    REVIEW_DIRECTORY.mkdir(parents=True, exist_ok=True)
    create_motion_sheet(frames["hero_ceremony"], REVIEW_DIRECTORY / REVIEW_SHEETS[0])
    create_growth_sheet(frames["world_tree_sapling"], REVIEW_DIRECTORY / REVIEW_SHEETS[1])
    create_arena_scale_sheet(
        frames["hero_ceremony"], frames["world_tree_sapling"], destination,
        REVIEW_DIRECTORY / REVIEW_SHEETS[2],
    )

    audit = {
        "schemaVersion": 1,
        "batch": BATCH,
        "candidateManifestSha256": sha256(manifest_path),
        "frameContract": CLIPS,
        "minimumUniqueVisibleFrames": MINIMUM_UNIQUE,
        "assets": {},
        "reviewSheets": {
            name: {"bytes": (REVIEW_DIRECTORY / name).stat().st_size,
                   "sha256": sha256(REVIEW_DIRECTORY / name)}
            for name in REVIEW_SHEETS
        },
    }
    total_frames = 0
    for key in EXPECTED:
        record = {
            "candidateSheetSha256": sha256(source / "sprites" / f"{key}.png"),
            "candidateAtlasSha256": sha256(source / "sprites" / f"{key}.atlas"),
            "candidateMetadataSha256": sha256(source / "sprites" / f"{key}.json"),
            "triangles": candidate[key]["triangles"],
            "meshParts": candidate[key]["meshParts"],
            "materialCount": candidate[key]["materialCount"],
            "clips": {},
        }
        for clip, images in frames[key].items():
            digests = {frame_digest(image) for image in images}
            margins = [alpha_margins(image) for image in images]
            minimum = [min(m[i] for m in margins) for i in range(4)]
            clip_record = {
                "frameCount": len(images),
                "uniqueVisibleFrames": len(digests),
                "minimumAlphaMargins": {
                    "left": minimum[0], "top": minimum[1],
                    "right": minimum[2], "bottom": minimum[3],
                },
            }
            if len(images) != CLIPS[key][clip]:
                raise ValueError(f"{key}/{clip}: frame-count mismatch")
            if len(digests) < MINIMUM_UNIQUE[key][clip]:
                raise ValueError(f"{key}/{clip}: only {len(digests)} unique visible frames")
            if min(minimum) < 2:
                raise ValueError(f"{key}/{clip}: a frame touches the sheet boundary")
            record["clips"][clip] = clip_record
            total_frames += len(images)
        if key == "world_tree_sapling":
            heights = [visible_height(image) for image in frames[key]["grow"]]
            areas = [visible_area(image) for image in frames[key]["grow"]]
            # The ramp rises continuously, then the crown springs and settles over the last
            # three frames (authored overshoot); the settled tree must stay near its peak.
            rising = heights[:-3]
            if any(later < earlier for earlier, later in zip(rising, rising[1:])):
                raise ValueError("Sapling growth must rise monotonically before the settle")
            if any(later < earlier for earlier, later in zip(areas[:-2], areas[1:-1])):
                raise ValueError("Sapling silhouette area must grow until the settle")
            if heights[-1] < 0.85 * max(heights):
                raise ValueError("Sapling settle collapses too far below its peak")
            if heights[0] > 0.25 * heights[-1]:
                raise ValueError("Sapling growth must start from a seed-scale sprout")
            record["growthVisibleHeights"] = heights
            record["growthVisibleAreas"] = areas
            idle_first = frames[key]["idle"][0]
            record["growToIdleAlphaDifference"] = alpha_difference_ratio(
                frames[key]["grow"][-1], idle_first)
            if record["growToIdleAlphaDifference"] > 0.05:
                raise ValueError("Sapling idle must continue from the final growth frame")
        audit["assets"][key] = record
    audit["summary"] = {
        "assetCount": 2,
        "frameCount": total_frames,
        "decodedBytes": sum(a["sheets"][0]["decodedBytes"] for a in candidate.values()),
    }
    write_json(AUDIT_PATH, audit)
    print(f"Audit written: {AUDIT_PATH} ({sha256(AUDIT_PATH)[:12]})")
    if args.audit_only:
        return

    review_path = REPOSITORY / REVIEW_DOCUMENT
    if not review_path.is_file():
        raise FileNotFoundError(REVIEW_DOCUMENT)
    review = review_path.read_text(encoding="utf-8")
    for required in ("**Decision:** ACCEPTED", audit["candidateManifestSha256"], sha256(AUDIT_PATH)):
        if required not in review:
            raise ValueError(f"Review document is not hash-bound to {required}")

    catalog_path = destination / "asset_manifest.json"
    catalog = read_json(catalog_path)
    by_key = {asset["key"]: asset for asset in catalog["assets"]}
    review_record = {
        "category": "ceremony",
        "document": REVIEW_DOCUMENT,
        "status": "accepted",
        "auditSha256": sha256(AUDIT_PATH),
        "sourceManifestSha256": audit["candidateManifestSha256"],
        "scope": "phase-18-planting-ceremony",
    }
    for key in EXPECTED:
        for ext in ("png", "atlas"):
            shutil.copy2(source / "sprites" / f"{key}.{ext}", destination / "sprites" / f"{key}.{ext}")
        asset = json.loads(json.dumps(candidate[key]))
        asset.update({"reviewDocument": REVIEW_DOCUMENT, "categoryReview": review_record})
        by_key[key] = asset
        write_json(destination / "sprites" / f"{key}.json", asset)
    catalog["generatedBatch"] = "ceremony"
    catalog["assets"] = [by_key[key] for key in sorted(by_key)]
    write_json(catalog_path, catalog)
    print(f"Promoted 2 ceremony assets from candidate {audit['candidateManifestSha256'][:12]}")


def validate_asset(asset: dict, key: str) -> None:
    expected = dict(EXPECTED[key])
    expected.update({
        "key": key,
        "sheet": f"sprites/{key}.png",
        "atlas": f"sprites/{key}.atlas",
        "alphaMode": "STRAIGHT_RGBA",
        "frameRate": 12,
        "renderSupersample": 3,
        "renderSamples": 32,
        "boneAnimated": True,
        "visualQuality": "premium-v2",
    })
    for field, value in expected.items():
        if asset.get(field) != value:
            raise ValueError(f"{key}: {field} is {asset.get(field)!r}, expected {value!r}")
    width, height = asset["sheetWidth"], asset["sheetHeight"]
    if asset.get("sheets") != [{"decodedBytes": width * height * 4, "file": f"sprites/{key}.png",
                                "height": height, "width": width}]:
        raise ValueError(f"{key}: atlas-page contract mismatch")
    if set(asset.get("clips", {})) != set(CLIPS[key]):
        raise ValueError(f"{key}: clip-name mismatch {sorted(asset.get('clips', {}))}")
    size = asset["frameSize"]
    for clip, count in CLIPS[key].items():
        frames = sorted(asset["clips"][clip], key=lambda f: f["index"])
        if [f["index"] for f in frames] != list(range(count)):
            raise ValueError(f"{key}/{clip}: frame contract mismatch")
        for frame in frames:
            if frame.get("page", 0) != 0 or frame["width"] != size or frame["height"] != size:
                raise ValueError(f"{key}/{clip}: frame geometry mismatch")


def decode_frames(source: Path, asset: dict) -> dict[str, list[Image.Image]]:
    sheet = Image.open(source / asset["sheet"]).convert("RGBA")
    if sheet.size != (asset["sheetWidth"], asset["sheetHeight"]):
        raise ValueError(f"{asset['key']}: sheet size mismatch")
    result = {}
    for clip, frames in asset["clips"].items():
        result[clip] = [
            sheet.crop((f["x"], f["y"], f["x"] + f["width"], f["y"] + f["height"]))
            for f in sorted(frames, key=lambda f: f["index"])
        ]
    return result


def create_motion_sheet(frames: dict[str, list[Image.Image]], output: Path) -> None:
    size = 192
    rows = list(frames)
    width = max(len(frames[c]) for c in rows) * size + 120
    sheet = Image.new("RGBA", (width, len(rows) * (size + 24) + 16), BACKDROP)
    draw = ImageDraw.Draw(sheet)
    for row, clip in enumerate(rows):
        y = 16 + row * (size + 24)
        draw.text((12, y + size // 2), clip.upper(), fill=(231, 216, 177, 255))
        for index, frame in enumerate(frames[clip]):
            sheet.alpha_composite(frame, (120 + index * size, y))
            draw.text((120 + index * size + 4, y + 2), str(index), fill=(200, 200, 200, 255))
    sheet.convert("RGB").save(output)


def create_growth_sheet(frames: dict[str, list[Image.Image]], output: Path) -> None:
    size = 256
    grow, idle = frames["grow"], frames["idle"]
    sheet = Image.new("RGBA", (12 * size, 2 * (size + 24) + 16), BACKDROP)
    draw = ImageDraw.Draw(sheet)
    for index, frame in enumerate(grow):
        sheet.alpha_composite(frame, (index * size, 16))
        draw.text((index * size + 4, 18), f"grow {index}", fill=(200, 200, 200, 255))
    for index, frame in enumerate(idle):
        sheet.alpha_composite(frame, (index * size, 16 + size + 24))
        draw.text((index * size + 4, 18 + size + 24), f"idle {index}", fill=(200, 200, 200, 255))
    sheet.convert("RGB").save(output)


def create_arena_scale_sheet(
    hero: dict[str, list[Image.Image]],
    sapling: dict[str, list[Image.Image]],
    destination: Path,
    output: Path,
) -> None:
    """720x1280 reference composition: Heartwood, grown sapling at SECOND_TREE, Hero at STAND."""
    canvas = Image.new("RGBA", (720, 1280), BACKDROP)
    backdrop = destination / "environment" / "arena_backdrop.png"
    if backdrop.is_file():
        canvas.alpha_composite(Image.open(backdrop).convert("RGBA").resize((720, 1280)))
    tree_sheet = destination / "sprites" / "world_tree_healthy.png"
    if tree_sheet.is_file():
        tree = Image.open(tree_sheet).convert("RGBA").crop((0, 0, 256, 256)).resize((330, 330))
        canvas.alpha_composite(tree, (int(360 - 165), int(1280 - (755 - 31) - 330)))
    grown = sapling["grow"][-1].resize((258, 258))
    canvas.alpha_composite(grown, (int(578 - 129), int(1280 - (738 - 24) - 258)))
    stand_x, stand_y = 578 - 78, 738 - 38
    canvas.alpha_composite(hero["water"][5], (int(stand_x - 96), int(1280 - (stand_y - 23) - 192)))
    canvas.alpha_composite(hero["walk"][0], (int(360 - 96), int(1280 - (600 - 23) - 192)))
    canvas.convert("RGB").save(output)


def alpha_margins(image: Image.Image) -> tuple[int, int, int, int]:
    box = image.getchannel("A").getbbox()
    if box is None:
        return (image.width, image.height, image.width, image.height)
    return (box[0], box[1], image.width - box[2], image.height - box[3])


def visible_area(image: Image.Image) -> int:
    return int(sum(image.getchannel("A").point(lambda v: 255 if v > 8 else 0).histogram()[255:]))


def visible_height(image: Image.Image) -> int:
    box = image.getchannel("A").getbbox()
    return 0 if box is None else box[3] - box[1]


def alpha_difference_ratio(first: Image.Image, second: Image.Image) -> float:
    a = first.getchannel("A").point(lambda v: 255 if v > 8 else 0)
    b = second.getchannel("A").point(lambda v: 255 if v > 8 else 0)
    from PIL import ImageChops
    diff = ImageChops.difference(a, b).getbbox()
    if diff is None:
        return 0.0
    union = ImageChops.lighter(a, b)
    return sum(ImageChops.difference(a, b).histogram()[255:]) / max(1, sum(union.histogram()[255:]))


def frame_digest(frame: Image.Image) -> str:
    return hashlib.sha256(frame.tobytes()).hexdigest()


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


if __name__ == "__main__":
    main()
