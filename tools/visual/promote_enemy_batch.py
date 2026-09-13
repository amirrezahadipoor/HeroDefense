#!/usr/bin/env python3
"""Promote only the exact hash-audited premium-v2 regular-enemy artifact."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]
REVIEW_DOCUMENT = "docs/art_reviews/ENEMIES_PREMIUM_V2_REVIEW.md"
REVIEW_DIRECTORY = REPOSITORY / "docs/art_reviews/regular_enemies_premium_v2"
AUDIT_PATH = REVIEW_DIRECTORY / "regular_enemies_audit.json"
PILOT_REVIEW = "docs/art_reviews/PREMIUM_V2_PILOT_REVIEW.md"
EXPECTED = {
    "rootling": ("rootling-thorn-scout-v2", "premium-humanoid-v2", "rootling-skirmisher-v2"),
    "stonekin": ("stonekin-rune-bulwark-v2", "premium-heavy-humanoid-v2", "stonekin-juggernaut-v2"),
    "gloom_wolf": ("gloom-wolf-shadow-stalker-v2", "premium-quadruped-mapped-v2", "gloom-wolf-pouncer-v2"),
    "fungal_brute": ("fungal-brute-spore-bruiser-v2", "premium-heavy-humanoid-v2", "fungal-brute-brawler-v2"),
}
EXPECTED_SHEETS = {
    "regular_enemies_lineup.png",
    *(f"{key}_{suffix}.png" for key in EXPECTED for suffix in ("full_motion", "readability")),
}
EXPECTED_CLIPS = {"idle": 6, "attack": 8, "hit": 4, "death": 10}
MIN_UNIQUE = {"idle": 5, "attack": 7, "hit": 3, "death": 9}
EXPECTED_PIVOT = {"units": "normalized-bottom-left", "x": 0.5, "y": 0.12}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("candidate", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    source = args.candidate.resolve()
    destination = args.destination.resolve()
    if source == destination:
        raise ValueError("Candidate and destination must be different directories")

    candidate_manifest_path = resolve_inside(source, Path("asset_manifest.json"))
    candidate_manifest = read_json(candidate_manifest_path)
    candidate_assets = candidate_manifest.get("assets", [])
    candidate_by_key = {asset["key"]: asset for asset in candidate_assets}
    if len(candidate_by_key) != len(candidate_assets):
        raise ValueError("Duplicate regular-enemy candidate key")
    if set(candidate_by_key) != set(EXPECTED):
        raise ValueError(
            f"Regular-enemy candidate key mismatch: {sorted(set(candidate_by_key) ^ set(EXPECTED))}"
        )

    expected_payload = validate_candidate_payload(source, candidate_manifest, candidate_by_key)
    if not (REPOSITORY / REVIEW_DOCUMENT).is_file():
        raise FileNotFoundError(f"Missing accepted review document: {REVIEW_DOCUMENT}")
    audit = validate_review_evidence(source, destination, candidate_manifest_path, candidate_by_key)

    catalog_path = resolve_inside(destination, Path("asset_manifest.json"))
    catalog = read_json(catalog_path)
    by_key = {asset["key"]: asset for asset in catalog["assets"]}
    missing = set(EXPECTED) - set(by_key)
    if missing:
        raise ValueError(f"Committed catalog is missing regular enemies: {sorted(missing)}")

    for relative in sorted(expected_payload):
        target = resolve_destination(destination, relative)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source / relative, target)

    review = {
        "category": "regular-enemies",
        "document": REVIEW_DOCUMENT,
        "status": "accepted",
    }
    for key in sorted(EXPECTED):
        asset = candidate_by_key[key]
        previous = by_key[key]
        if previous.get("pilotReviewDocument") == PILOT_REVIEW \
                or previous.get("reviewDocument") == PILOT_REVIEW:
            asset["pilotReviewDocument"] = PILOT_REVIEW
        asset.update({
            "reviewDocument": REVIEW_DOCUMENT,
            "categoryReview": review,
        })
        by_key[key] = asset

        metadata_path = destination / "sprites" / f"{key}.json"
        metadata = read_json(metadata_path)
        metadata.update({
            "reviewDocument": REVIEW_DOCUMENT,
            "categoryReview": review,
        })
        if "pilotReviewDocument" in asset:
            metadata["pilotReviewDocument"] = PILOT_REVIEW
        write_json(metadata_path, metadata)

    catalog["pipelineVersion"] = max(
        int(catalog.get("pipelineVersion", 0)),
        int(candidate_manifest.get("pipelineVersion", 0)),
    )
    catalog["generatedBatch"] = "enemies"
    catalog["assets"] = [by_key[key] for key in sorted(by_key)]
    write_json(catalog_path, catalog)
    print(
        f"Promoted exactly {len(EXPECTED)} reviewed premium-v2 regular enemies "
        f"from audit {sha256_file(AUDIT_PATH)[:12]}"
    )


def validate_candidate_payload(
    source: Path,
    manifest: dict,
    candidate_by_key: dict[str, dict],
) -> set[Path]:
    exact_global = {
        "pipelineVersion": 3,
        "generatedBatch": "enemies",
        "frameRate": 12,
        "renderSupersample": 2,
        "opaqueRenderSamples": 16,
        "overlayRenderSamples": 8,
        "maxAtlasPageSize": 2048,
    }
    for field, expected in exact_global.items():
        if manifest.get(field) != expected:
            raise ValueError(
                f"Candidate global contract mismatch: {field}={manifest.get(field)!r}, expected {expected!r}"
            )
    required_bones = manifest.get("requiredBones", [])
    if len(required_bones) != 25 or len(set(required_bones)) != 25:
        raise ValueError("Candidate does not retain the locked 25-bone contract")

    expected_payload: set[Path] = set()
    for key, (revision, rig_profile, animation_profile) in EXPECTED.items():
        asset = candidate_by_key[key]
        expected_fields = {
            "key": key,
            "family": "enemy",
            "builder": key,
            "frameClass": "character",
            "frameSize": 192,
            "sheet": f"sprites/{key}.png",
            "atlas": f"sprites/{key}.atlas",
            "sheetWidth": 1920,
            "sheetHeight": 768,
            "pivot": EXPECTED_PIVOT,
            "alphaMode": "STRAIGHT_RGBA",
            "frameRate": 12,
            "renderSupersample": 2,
            "renderSamples": 16,
            "rigBoneCount": 25,
            "boneAnimated": True,
            "visualQuality": "premium-v2",
            "modelRevision": revision,
            "rigProfile": rig_profile,
            "animationProfile": animation_profile,
        }
        for field, expected in expected_fields.items():
            if asset.get(field) != expected:
                raise ValueError(
                    f"Candidate contract mismatch: {key} {field}={asset.get(field)!r}, expected {expected!r}"
                )
        if asset.get("bones") != sorted(required_bones):
            raise ValueError(f"Candidate bone-name mismatch: {key}")
        if not 900 <= int(asset.get("triangles", 0)) <= 4_000:
            raise ValueError(f"Candidate triangle budget mismatch: {key}")
        if int(asset.get("meshParts", 0)) < 32:
            raise ValueError(f"Candidate mesh-part quality floor mismatch: {key}")
        if int(asset.get("materialCount", 0)) < 6:
            raise ValueError(f"Candidate material quality floor mismatch: {key}")
        if not asset.get("silhouette") or not asset.get("materialStory"):
            raise ValueError(f"Candidate art-direction metadata missing: {key}")
        sheets = asset.get("sheets", [])
        expected_sheet = {
            "decodedBytes": 1920 * 768 * 4,
            "file": f"sprites/{key}.png",
            "height": 768,
            "width": 1920,
        }
        if sheets != [expected_sheet]:
            raise ValueError(f"Candidate atlas-page contract mismatch: {key}")
        if set(asset.get("clips", {})) != set(EXPECTED_CLIPS):
            raise ValueError(f"Candidate clip-name mismatch: {key}")
        for clip, count in EXPECTED_CLIPS.items():
            frames = sorted(asset["clips"][clip], key=lambda value: value["index"])
            if len(frames) != count or [frame["index"] for frame in frames] != list(range(count)):
                raise ValueError(f"Candidate frame contract mismatch: {key} {clip}")
            for frame in frames:
                if frame.get("page", 0) != 0 or frame["width"] != 192 or frame["height"] != 192:
                    raise ValueError(f"Candidate frame geometry mismatch: {key} {clip}")

        metadata_relative = Path("sprites") / f"{key}.json"
        metadata_path = resolve_inside(source, metadata_relative)
        if read_json(metadata_path) != asset:
            raise ValueError(f"Candidate manifest/metadata mismatch: {key}")
        expected_payload.add(metadata_relative)
        for field in ("sheet", "atlas"):
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
    source: Path,
    destination: Path,
    candidate_manifest_path: Path,
    candidate_by_key: dict[str, dict],
) -> dict:
    if not AUDIT_PATH.is_file():
        raise FileNotFoundError(f"Missing regular-enemy audit: {AUDIT_PATH}")
    actual_sheets = {path.name for path in REVIEW_DIRECTORY.glob("*.png")}
    if actual_sheets != EXPECTED_SHEETS:
        raise ValueError(
            "Regular-enemy review-sheet set mismatch: "
            f"missing={sorted(EXPECTED_SHEETS - actual_sheets)}, "
            f"unexpected={sorted(actual_sheets - EXPECTED_SHEETS)}"
        )
    audit = read_json(AUDIT_PATH)
    if audit.get("schemaVersion") != 1 or audit.get("batch") != "regular-enemies-premium-v2":
        raise ValueError("Unexpected regular-enemy audit contract")
    if audit.get("expectedKeys") != list(EXPECTED):
        raise ValueError("Regular-enemy audit key order mismatch")
    if audit.get("frameContract") != EXPECTED_CLIPS:
        raise ValueError("Regular-enemy audit frame contract mismatch")
    if audit.get("minimumUniqueVisibleFrames") != MIN_UNIQUE:
        raise ValueError("Regular-enemy audit motion-diversity contract mismatch")
    if audit.get("candidateManifestSha256") != sha256_file(candidate_manifest_path):
        raise ValueError("Regular-enemy candidate-manifest provenance mismatch")

    destination_manifest_path = resolve_inside(destination, Path("asset_manifest.json"))
    destination_manifest_hash = sha256_file(destination_manifest_path)
    baseline_state = destination_manifest_hash == audit.get("baselineManifestSha256")
    destination_by_key = {
        asset["key"]: asset for asset in read_json(destination_manifest_path)["assets"]
    }

    recorded_sheets = audit.get("reviewSheets", {})
    if set(recorded_sheets) != EXPECTED_SHEETS or audit.get("reviewSheetCount") != len(EXPECTED_SHEETS):
        raise ValueError("Regular-enemy review-sheet evidence mismatch")
    for name, record in recorded_sheets.items():
        path = REVIEW_DIRECTORY / name
        if record != {"bytes": path.stat().st_size, "sha256": sha256_file(path)}:
            raise ValueError(f"Regular-enemy review-sheet hash mismatch: {name}")

    records = audit.get("assets", [])
    by_key = {record["key"]: record for record in records}
    if len(by_key) != len(records) or set(by_key) != set(EXPECTED):
        raise ValueError("Regular-enemy audit asset set mismatch")
    for key, (revision, rig_profile, animation_profile) in EXPECTED.items():
        record = by_key[key]
        candidate = candidate_by_key[key]
        expected_values = {
            "modelRevision": revision,
            "rigProfile": rig_profile,
            "animationProfile": animation_profile,
            "triangles": candidate["triangles"],
            "meshParts": candidate["meshParts"],
            "materialCount": candidate["materialCount"],
            "rigBoneCount": 25,
            "candidateSheetSha256": sha256_file(resolve_inside(source, safe_relative(candidate["sheet"]))),
            "candidateAtlasSha256": sha256_file(resolve_inside(source, safe_relative(candidate["atlas"]))),
            "candidateMetadataSha256": sha256_file(source / "sprites" / f"{key}.json"),
        }
        for field, expected in expected_values.items():
            if record.get(field) != expected:
                raise ValueError(f"Regular-enemy audit provenance mismatch: {key} {field}")
        committed_sheet = destination / candidate["sheet"]
        if not committed_sheet.is_file():
            raise FileNotFoundError(committed_sheet)
        committed_hash = sha256_file(committed_sheet)
        expected_destination_hash = (
            record["baselineSheetSha256"] if baseline_state
            else record["candidateSheetSha256"]
        )
        if committed_hash != expected_destination_hash:
            state = "reviewed baseline" if baseline_state else "already-promoted candidate"
            raise ValueError(f"Committed {key} does not match the {state}")
        if not baseline_state:
            destination_asset = destination_by_key.get(key, {})
            category_review = destination_asset.get("categoryReview", {})
            if destination_asset.get("reviewDocument") != REVIEW_DOCUMENT \
                    or category_review.get("status") != "accepted":
                raise ValueError(
                    "Destination manifest is neither the reviewed baseline nor an "
                    f"idempotently promoted catalog: {key}"
                )
        assert_margins(record.get("minimumAlphaMargins"), f"{key} batch")
        clips = record.get("clips", {})
        for clip, count in EXPECTED_CLIPS.items():
            clip_record = clips.get(clip, {})
            if clip_record.get("frameCount") != count:
                raise ValueError(f"Regular-enemy audit frame count mismatch: {key} {clip}")
            if int(clip_record.get("uniqueVisibleFrames", 0)) < MIN_UNIQUE[clip]:
                raise ValueError(f"Regular-enemy audit motion diversity mismatch: {key} {clip}")
            assert_margins(clip_record.get("minimumAlphaMargins"), f"{key} {clip}")

    summary = audit.get("summary", {})
    exact_summary = {
        "assetCount": 4,
        "frameCount": 112,
        "singlePageAtlasCount": 4,
        "decodedBytes": 4 * 1920 * 768 * 4,
        "decodedBudgetBytes": 32 * 1024 * 1024,
    }
    for field, expected in exact_summary.items():
        if summary.get(field) != expected:
            raise ValueError(f"Regular-enemy audit summary mismatch: {field}")
    assert_margins(summary.get("minimumAlphaMargins"), "regular-enemy batch")
    if not 900 <= int(summary.get("minimumTriangles", 0)) <= int(summary.get("maximumTriangles", 0)) <= 4_000:
        raise ValueError("Regular-enemy audit triangle summary mismatch")
    if int(summary.get("minimumMeshParts", 0)) < 32:
        raise ValueError("Regular-enemy audit mesh-part summary mismatch")
    if int(summary.get("minimumMaterialCount", 0)) < 6:
        raise ValueError("Regular-enemy audit material summary mismatch")
    return audit


def assert_margins(value: object, label: str) -> None:
    if not isinstance(value, dict) or set(value) != {"left", "top", "right", "bottom"}:
        raise ValueError(f"Invalid {label} margin record")
    if min(int(margin) for margin in value.values()) < 2:
        raise ValueError(f"Unsafe {label} margin record: {value}")


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
    path = (root / relative).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError(f"Promotion path escapes destination: {relative}")
    return path


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


if __name__ == "__main__":
    main()
