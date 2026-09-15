#!/usr/bin/env python3
"""Promote the exact audited and accepted 40-asset premium equipment batch."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from collections import Counter
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]
CATALOG_SOURCE = REPOSITORY / "tools/blender/equipment_visuals.json"
REVIEW_DOCUMENT = "docs/art_reviews/EQUIPMENT_PREMIUM_V2_REVIEW.md"
REVIEW_DIRECTORY = REPOSITORY / "docs/art_reviews/equipment_premium_v2"
AUDIT_PATH = REVIEW_DIRECTORY / "equipment_alignment_audit.json"
PILOT_REVIEW = "docs/art_reviews/PREMIUM_V2_PILOT_REVIEW.md"
EXPECTED_REVIEW_SHEETS = {
    *(f"equipment_icons_{tier}.png" for tier in ("common", "uncommon", "rare", "legendary")),
    *(f"equipment_composites_{page}.png" for page in range(1, 6)),
}
EXPECTED_FRAME_COUNTS = {"idle": 6, "attack": 8, "hit": 4, "death": 10}
MAX_TRIANGLES = 900


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("candidate", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    source = args.candidate.resolve()
    destination = args.destination.resolve()
    if source == destination:
        raise ValueError("Candidate and destination must be different directories")

    catalog_items = json.loads(CATALOG_SOURCE.read_text(encoding="utf-8"))["items"]
    expected_by_id = {item["id"]: item for item in catalog_items}
    if len(catalog_items) != 40 or len(expected_by_id) != len(catalog_items):
        raise ValueError("Equipment promotion requires exactly 40 unique catalog items")
    expected_keys = {f"equipment_{item_id}" for item_id in expected_by_id}

    manifest_path = source / "asset_manifest.json"
    candidate_manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    candidate_assets = candidate_manifest["assets"]
    candidate_by_key = {asset["key"]: asset for asset in candidate_assets}
    if len(candidate_by_key) != len(candidate_assets):
        raise ValueError("Duplicate premium equipment candidate key")
    if set(candidate_by_key) != expected_keys:
        raise ValueError(
            f"Premium equipment key mismatch: {sorted(set(candidate_by_key) ^ expected_keys)}"
        )

    expected_payload = validate_candidate_payload(source, expected_by_id, candidate_by_key)
    review_path = REPOSITORY / REVIEW_DOCUMENT
    if not review_path.is_file():
        raise FileNotFoundError(f"Missing accepted equipment review: {REVIEW_DOCUMENT}")
    validate_review_evidence(
        expected_by_id,
        source,
        destination,
        manifest_path,
        candidate_by_key,
    )

    catalog_path = destination / "asset_manifest.json"
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    by_key = {asset["key"]: asset for asset in catalog["assets"]}
    missing_destination_keys = expected_keys - set(by_key)
    if missing_destination_keys:
        raise ValueError(
            f"Committed catalog is missing equipment keys: {sorted(missing_destination_keys)}"
        )

    for relative in sorted(expected_payload):
        target = resolve_destination(destination, relative)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source / relative, target)

    review = {
        "category": "equipment",
        "document": REVIEW_DOCUMENT,
        "status": "accepted",
    }
    for key in sorted(expected_keys):
        asset = candidate_by_key[key]
        previous = by_key[key]
        if previous.get("pilotReviewDocument") == PILOT_REVIEW \
                or previous.get("reviewDocument") == PILOT_REVIEW:
            asset["pilotReviewDocument"] = PILOT_REVIEW
        asset.update(
            {
                "modelRevision": "equipment-premium-v2",
                "rigProfile": "hero-socket-v2",
                "reviewDocument": REVIEW_DOCUMENT,
                "categoryReview": review,
            }
        )
        by_key[key] = asset

        json_path = destination / "equipment" / f"{asset['itemId']}.json"
        metadata = json.loads(json_path.read_text(encoding="utf-8"))
        metadata.update(
            {
                "modelRevision": asset["modelRevision"],
                "rigProfile": asset["rigProfile"],
                "renderSupersample": 2,
                "renderSamples": 12,
                "visualQuality": "studio-v3",
                "reviewDocument": REVIEW_DOCUMENT,
                "categoryReview": review,
            }
        )
        if "pilotReviewDocument" in asset:
            metadata["pilotReviewDocument"] = PILOT_REVIEW
        write_json(json_path, metadata)

    catalog["generatedBatch"] = "equipment"
    catalog["assets"] = [by_key[key] for key in sorted(by_key)]
    write_json(catalog_path, catalog)
    print(f"Promoted exactly {len(expected_keys)} reviewed premium-v2 equipment assets")


def validate_candidate_payload(
    source: Path,
    expected_by_id: dict[str, dict],
    candidate_by_key: dict[str, dict],
) -> set[Path]:
    expected_payload: set[Path] = set()
    for item_id, item in expected_by_id.items():
        key = f"equipment_{item_id}"
        asset = candidate_by_key[key]
        expected = {
            "key": key,
            "family": "equipment",
            "itemId": item_id,
            "slot": item["slot"],
            "visualSlot": item["visualSlot"],
            "visualKind": item.get("visualKind", item["visualSlot"]),
            "tier": item["tier"],
            "frameClass": "character",
            "frameSize": 192,
            "sheet": f"equipment/{item_id}.png",
            "atlas": f"equipment/{item_id}.atlas",
            "icon": f"icons/equipment_{item_id}.png",
            "modelRevision": "equipment-premium-v2",
            "rigProfile": "hero-socket-v2",
            "renderSupersample": 2,
            "renderSamples": 12,
            "visualQuality": "studio-v3",
            "runtimeGlow": item["tier"] in {"RARE", "LEGENDARY"},
            "boneAnimated": True,
        }
        for field, value in expected.items():
            if asset.get(field) != value:
                raise ValueError(
                    f"Candidate contract mismatch: {item_id} {field}={asset.get(field)!r}, "
                    f"expected {value!r}"
                )
        if int(asset.get("triangles", 0)) <= 0 or int(asset["triangles"]) > MAX_TRIANGLES:
            raise ValueError(f"Candidate triangle cap exceeded: {item_id}")
        if len(asset.get("sheets", [])) != 1:
            raise ValueError(f"Equipment must fit one atlas page: {item_id}")
        sheet = asset["sheets"][0]
        if sheet != {
            "decodedBytes": 1920 * 768 * 4,
            "file": expected["sheet"],
            "height": 768,
            "width": 1920,
        }:
            raise ValueError(f"Candidate sheet contract mismatch: {item_id}")
        if set(asset.get("clips", {})) != set(EXPECTED_FRAME_COUNTS):
            raise ValueError(f"Candidate clip contract mismatch: {item_id}")
        for clip, count in EXPECTED_FRAME_COUNTS.items():
            if len(asset["clips"][clip]) != count:
                raise ValueError(f"Candidate frame-count mismatch: {item_id} {clip}")

        metadata_relative = Path("equipment") / f"{item_id}.json"
        metadata_path = resolve_inside(source, metadata_relative)
        metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        if metadata != asset:
            raise ValueError(f"Candidate manifest/metadata mismatch: {item_id}")
        expected_payload.add(metadata_relative)
        for field in ("sheet", "atlas", "icon"):
            relative = safe_relative(asset[field])
            resolve_inside(source, relative)
            expected_payload.add(relative)

    actual_payload = {
        path.relative_to(source)
        for path in source.rglob("*")
        if path.is_file() and path.name != "asset_manifest.json"
    }
    if actual_payload != expected_payload:
        raise ValueError(
            "Candidate payload mismatch: "
            f"missing={sorted(expected_payload - actual_payload)}, "
            f"unexpected={sorted(actual_payload - expected_payload)}"
        )
    return expected_payload


def validate_review_evidence(
    expected_by_id: dict[str, dict],
    source: Path,
    destination: Path,
    manifest_path: Path,
    candidate_by_key: dict[str, dict],
) -> None:
    required_sheets = {REVIEW_DIRECTORY / name for name in EXPECTED_REVIEW_SHEETS}
    missing = sorted(path for path in required_sheets if not path.is_file())
    if missing:
        raise FileNotFoundError(f"Missing equipment review sheets: {missing}")
    if not AUDIT_PATH.is_file():
        raise FileNotFoundError(f"Missing equipment alignment audit: {AUDIT_PATH}")

    audit = json.loads(AUDIT_PATH.read_text(encoding="utf-8"))
    if audit.get("contract") != "premium-v2-equipment":
        raise ValueError("Unexpected equipment audit contract")
    if audit.get("catalogSha256") != sha256_file(CATALOG_SOURCE):
        raise ValueError("Equipment review catalog provenance mismatch")
    if audit.get("candidateManifestSha256") != sha256_file(manifest_path):
        raise ValueError("Equipment review candidate-manifest provenance mismatch")

    recorded_sheets = audit.get("reviewSheets", {})
    if set(recorded_sheets) != EXPECTED_REVIEW_SHEETS:
        raise ValueError("Equipment review-sheet set mismatch")
    for name, expected_hash in recorded_sheets.items():
        if sha256_file(REVIEW_DIRECTORY / name) != expected_hash:
            raise ValueError(f"Equipment review-sheet hash mismatch: {name}")

    hero_metadata_path = destination / "sprites/hero.json"
    hero_metadata = json.loads(hero_metadata_path.read_text(encoding="utf-8"))
    hero_contract = audit.get("heroContract", {})
    expected_hero = {
        "key": "hero",
        "modelRevision": hero_metadata.get("modelRevision"),
        "rigProfile": hero_metadata.get("rigProfile"),
        "frameSize": hero_metadata.get("frameSize"),
        "sheetSha256": sha256_file(resolve_inside(destination, hero_metadata["sheet"])),
    }
    if hero_contract != expected_hero:
        raise ValueError("Equipment review was not performed against the committed Hero contract")

    audit_assets = audit.get("assets", [])
    audited_by_id = {asset["id"]: asset for asset in audit_assets}
    if len(audited_by_id) != len(audit_assets) or set(audited_by_id) != set(expected_by_id):
        raise ValueError(
            f"Equipment audit key mismatch: {sorted(set(audited_by_id) ^ set(expected_by_id))}"
        )

    summary = audit.get("summary", {})
    expected_tiers = dict(sorted(Counter(item["tier"] for item in expected_by_id.values()).items()))
    expected_slots = dict(
        sorted(Counter(item["visualSlot"] for item in expected_by_id.values()).items())
    )
    exact_summary_values = {
        "assets": 40,
        "boundaryFrames": 0,
        "detachedFrames": 0,
        "frames": 1120,
        "decodedBytes": 237_404_160,
        "tierCounts": expected_tiers,
        "visualSlotCounts": expected_slots,
    }
    for field, value in exact_summary_values.items():
        if summary.get(field) != value:
            raise ValueError(f"Equipment audit summary mismatch: {field}={summary.get(field)!r}")
    if not 0 < int(summary.get("maxTriangles", 0)) <= MAX_TRIANGLES:
        raise ValueError(f"Equipment audit triangle cap mismatch: {summary.get('maxTriangles')}")
    assert_positive_margins(summary.get("minimumFrameMargins"), "batch frame")
    assert_positive_margins(summary.get("minimumIconMargins"), "batch icon")
    if float(summary.get("minimumNearHeroPixels", 0.0)) <= 0.0:
        raise ValueError("Equipment audit contains a detached batch frame")

    for item_id, item in expected_by_id.items():
        recorded = audited_by_id[item_id]
        candidate = candidate_by_key[f"equipment_{item_id}"]
        expected_unique = {
            "idle": 1 if item["visualSlot"] == "boots" else 5,
            "attack": 7,
            "hit": 3,
            "death": 9,
        }
        if recorded.get("uniqueFrames") != expected_unique:
            raise ValueError(f"Equipment motion audit mismatch: {item_id}")
        if recorded.get("frameCount") != 28:
            raise ValueError(f"Equipment frame audit mismatch: {item_id}")
        if recorded.get("triangles") != candidate["triangles"]:
            raise ValueError(f"Equipment triangle provenance mismatch: {item_id}")
        assert_positive_margins(recorded.get("minimumFrameMargins"), f"{item_id} frame")
        assert_positive_margins(recorded.get("iconMargins"), f"{item_id} icon")
        if float(recorded.get("minimumNearHeroPixels", 0.0)) <= 0.0:
            raise ValueError(f"Equipment socket detachment: {item_id}")

        paths = {
            "sheetSha256": resolve_inside(source, candidate["sheet"]),
            "iconSha256": resolve_inside(source, candidate["icon"]),
            "atlasSha256": resolve_inside(source, candidate["atlas"]),
            "metadataSha256": source / "equipment" / f"{item_id}.json",
        }
        for field, path in paths.items():
            if recorded.get(field) != sha256_file(path):
                raise ValueError(f"Equipment reviewed-payload hash mismatch: {item_id} {field}")


def assert_positive_margins(value: object, label: str) -> None:
    if not isinstance(value, dict) or set(value) != {"left", "top", "right", "bottom"}:
        raise ValueError(f"Invalid {label} margin record")
    if min(int(margin) for margin in value.values()) <= 0:
        raise ValueError(f"Non-positive {label} margin record: {value}")


def safe_relative(value: str) -> Path:
    relative = Path(value)
    if relative.is_absolute() or ".." in relative.parts:
        raise ValueError(f"Generated payload path escapes root: {value}")
    return relative


def resolve_inside(root: Path, value: str | Path) -> Path:
    relative = safe_relative(str(value))
    path = (root / relative).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError(f"Generated payload path escapes root: {value}")
    if not path.is_file():
        raise FileNotFoundError(path)
    return path


def resolve_destination(root: Path, relative: Path) -> Path:
    target = (root / relative).resolve()
    if not target.is_relative_to(root.resolve()):
        raise ValueError(f"Promotion path escapes destination: {relative}")
    return target


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
