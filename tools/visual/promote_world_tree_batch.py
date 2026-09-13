#!/usr/bin/env python3
"""Promote only the exact hash-audited premium-v2 World Tree artifact."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]
REVIEW_DOCUMENT = "docs/art_reviews/WORLD_TREE_PREMIUM_V2_REVIEW.md"
REVIEW_DIRECTORY = REPOSITORY / "docs/art_reviews/world_tree_premium_v2"
AUDIT_PATH = REVIEW_DIRECTORY / "world_tree_audit.json"
EXPECTED = {
    "world_tree_healthy": (
        "heartwood-sanctum-healthy-v2",
        "living-heart-pulse-v2",
        {"idle": 6},
        (1536, 256),
        None,
    ),
    "world_tree_damaged": (
        "heartwood-sanctum-wounded-v2",
        "wounded-collapse-v2",
        {"idle": 6, "destroy": 10},
        (2048, 512),
        "destroy",
    ),
}
MINIMUM_UNIQUE = {
    "world_tree_healthy": {"idle": 5},
    "world_tree_damaged": {"idle": 5, "destroy": 8},
}
EXPECTED_BONES = sorted((
    "root", "trunk.lower", "trunk.upper", "crown", "branch.L", "branch.R",
    "bough.L", "bough.R", "canopy.L", "canopy.R", "heart", "debris.L", "debris.R",
))
EXPECTED_PIVOT = {"units": "normalized-bottom-left", "x": 0.5, "y": 0.06}
EXPECTED_SHEETS = {
    "world_tree_healthy_full_motion.png",
    "world_tree_damaged_full_motion.png",
    "world_tree_state_lineup.png",
    "world_tree_readability.png",
    "world_tree_destruction_timeline.png",
    "world_tree_arena_scale.png",
}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("candidate", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    source = args.candidate.resolve()
    destination = args.destination.resolve()
    if source == destination:
        raise ValueError("Candidate and destination must be different directories")

    candidate_manifest_path = resolve_inside(source, "asset_manifest.json")
    candidate_manifest = read_json(candidate_manifest_path)
    assets = candidate_manifest.get("assets", [])
    candidate_by_key = {asset["key"]: asset for asset in assets}
    if len(candidate_by_key) != len(assets) or set(candidate_by_key) != set(EXPECTED):
        raise ValueError("World Tree candidate must contain each expected state exactly once")
    expected_payload = validate_candidate_payload(source, candidate_manifest, candidate_by_key)
    audit = validate_review_evidence(
        source, destination, candidate_manifest_path, candidate_by_key
    )

    catalog_path = resolve_inside(destination, "asset_manifest.json")
    catalog = read_json(catalog_path)
    by_key = {asset["key"]: asset for asset in catalog["assets"]}
    if not set(EXPECTED) <= set(by_key):
        raise ValueError("Committed catalog is missing a World Tree state")

    for relative in sorted(expected_payload):
        target = resolve_destination(destination, relative)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source / relative, target)

    review = {
        "category": "world_tree",
        "document": REVIEW_DOCUMENT,
        "status": "accepted",
    }
    for key in EXPECTED:
        asset = candidate_by_key[key]
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
        write_json(metadata_path, metadata)

    catalog["pipelineVersion"] = max(
        int(catalog.get("pipelineVersion", 0)),
        int(candidate_manifest.get("pipelineVersion", 0)),
    )
    catalog["generatedBatch"] = "world-tree"
    catalog["assets"] = [by_key[key] for key in sorted(by_key)]
    write_json(catalog_path, catalog)
    print(
        "Promoted exactly two reviewed premium-v2 World Tree states "
        f"from audit {sha256_file(AUDIT_PATH)[:12]}"
    )


def validate_candidate_payload(
    source: Path,
    manifest: dict,
    candidate_by_key: dict[str, dict],
) -> set[Path]:
    expected_global = {
        "pipelineVersion": 3,
        "generatedBatch": "world-tree",
        "frameRate": 12,
        "renderSupersample": 2,
        "opaqueRenderSamples": 16,
        "overlayRenderSamples": 8,
        "maxAtlasPageSize": 2048,
    }
    for field, expected in expected_global.items():
        if manifest.get(field) != expected:
            raise ValueError(
                f"Candidate global contract mismatch: {field}={manifest.get(field)!r}"
            )

    expected_payload: set[Path] = set()
    for key, (revision, animation_profile, clips, dimensions, destruction_clip) in EXPECTED.items():
        width, height = dimensions
        asset = candidate_by_key[key]
        expected_fields = {
            "key": key,
            "family": "world_tree",
            "frameClass": "tree",
            "frameSize": 256,
            "sheet": f"sprites/{key}.png",
            "atlas": f"sprites/{key}.atlas",
            "sheetWidth": width,
            "sheetHeight": height,
            "pivot": EXPECTED_PIVOT,
            "alphaMode": "STRAIGHT_RGBA",
            "frameRate": 12,
            "renderSupersample": 2,
            "renderSamples": 16,
            "bones": EXPECTED_BONES,
            "rigBoneCount": 13,
            "boneAnimated": True,
            "rigged": True,
            "state": "healthy" if key.endswith("healthy") else "damaged",
            "visualQuality": "premium-v2",
            "modelRevision": revision,
            "rigProfile": "segmented-world-tree-v2",
            "animationProfile": animation_profile,
            "destructionClip": destruction_clip,
        }
        for field, expected in expected_fields.items():
            if asset.get(field) != expected:
                raise ValueError(
                    f"Candidate contract mismatch: {key} {field}={asset.get(field)!r}, "
                    f"expected {expected!r}"
                )
        if not 2_500 <= int(asset.get("triangles", 0)) <= 14_000:
            raise ValueError(f"Candidate triangle budget mismatch: {key}")
        if int(asset.get("meshParts", 0)) < 100:
            raise ValueError(f"Candidate mesh-part quality floor mismatch: {key}")
        if int(asset.get("materialCount", 0)) < 11:
            raise ValueError(f"Candidate material quality floor mismatch: {key}")
        landmarks = asset.get("silhouetteLandmarks")
        if not isinstance(landmarks, list) or len(landmarks) != 4 or not all(landmarks):
            raise ValueError(f"Candidate silhouette contract mismatch: {key}")
        if not asset.get("surfaceLanguage"):
            raise ValueError(f"Candidate surface-language metadata missing: {key}")
        expected_sheet = {
            "decodedBytes": width * height * 4,
            "file": f"sprites/{key}.png",
            "height": height,
            "width": width,
        }
        if asset.get("sheets") != [expected_sheet]:
            raise ValueError(f"Candidate atlas-page contract mismatch: {key}")
        if set(asset.get("clips", {})) != set(clips):
            raise ValueError(f"Candidate clip-name mismatch: {key}")
        for clip, count in clips.items():
            frames = sorted(asset["clips"][clip], key=lambda value: value["index"])
            if len(frames) != count or [frame["index"] for frame in frames] != list(range(count)):
                raise ValueError(f"Candidate frame contract mismatch: {key}/{clip}")
            for frame in frames:
                if frame.get("page", 0) != 0 or frame["width"] != 256 or frame["height"] != 256:
                    raise ValueError(f"Candidate frame geometry mismatch: {key}/{clip}")

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
    review_document_path = REPOSITORY / REVIEW_DOCUMENT
    if not review_document_path.is_file():
        raise FileNotFoundError(f"Missing accepted review document: {REVIEW_DOCUMENT}")
    if not AUDIT_PATH.is_file():
        raise FileNotFoundError(f"Missing World Tree audit: {AUDIT_PATH}")
    actual_sheets = {path.name for path in REVIEW_DIRECTORY.glob("*.png")}
    if actual_sheets != EXPECTED_SHEETS:
        raise ValueError(
            "World Tree review-sheet set mismatch: "
            f"missing={sorted(EXPECTED_SHEETS - actual_sheets)}, "
            f"unexpected={sorted(actual_sheets - EXPECTED_SHEETS)}"
        )

    audit = read_json(AUDIT_PATH)
    if audit.get("schemaVersion") != 1 or audit.get("batch") != "world-tree-premium-v2":
        raise ValueError("Unexpected World Tree audit contract")
    if audit.get("expectedKeys") != list(EXPECTED):
        raise ValueError("World Tree audit key order mismatch")
    expected_clips = {key: value[2] for key, value in EXPECTED.items()}
    if audit.get("frameContract") != expected_clips:
        raise ValueError("World Tree audit frame contract mismatch")
    if audit.get("minimumUniqueVisibleFrames") != MINIMUM_UNIQUE:
        raise ValueError("World Tree audit diversity contract mismatch")
    if audit.get("rigBoneNames") != EXPECTED_BONES:
        raise ValueError("World Tree audit rig contract mismatch")
    candidate_hash = sha256_file(candidate_manifest_path)
    if audit.get("candidateManifestSha256") != candidate_hash:
        raise ValueError("World Tree candidate-manifest provenance mismatch")

    audit_hash = sha256_file(AUDIT_PATH)
    review_text = review_document_path.read_text(encoding="utf-8")
    for required in ("**Decision:** ACCEPTED", audit_hash, candidate_hash):
        if required not in review_text:
            raise ValueError(f"World Tree review document is not hash-bound to {required}")

    recorded_sheets = audit.get("reviewSheets", {})
    if set(recorded_sheets) != EXPECTED_SHEETS or audit.get("reviewSheetCount") != 6:
        raise ValueError("World Tree review-sheet evidence mismatch")
    for name, record in recorded_sheets.items():
        path = REVIEW_DIRECTORY / name
        expected_record = {"bytes": path.stat().st_size, "sha256": sha256_file(path)}
        if record != expected_record:
            raise ValueError(f"World Tree review-sheet hash mismatch: {name}")

    destination_manifest_path = resolve_inside(destination, "asset_manifest.json")
    destination_manifest_hash = sha256_file(destination_manifest_path)
    baseline_state = destination_manifest_hash == audit.get("baselineManifestSha256")
    destination_by_key = {
        asset["key"]: asset for asset in read_json(destination_manifest_path)["assets"]
    }
    records = audit.get("assets", [])
    by_key = {record["key"]: record for record in records}
    if len(by_key) != len(records) or set(by_key) != set(EXPECTED):
        raise ValueError("World Tree audit asset set mismatch")

    for key, (revision, animation_profile, clips, _dimensions, _destroy) in EXPECTED.items():
        record = by_key[key]
        candidate = candidate_by_key[key]
        expected_values = {
            "modelRevision": revision,
            "rigProfile": "segmented-world-tree-v2",
            "animationProfile": animation_profile,
            "triangles": candidate["triangles"],
            "meshParts": candidate["meshParts"],
            "materialCount": candidate["materialCount"],
            "rigBoneCount": 13,
            "candidateSheetSha256": sha256_file(source / candidate["sheet"]),
            "candidateAtlasSha256": sha256_file(source / candidate["atlas"]),
            "candidateMetadataSha256": sha256_file(source / "sprites" / f"{key}.json"),
        }
        for field, expected in expected_values.items():
            if record.get(field) != expected:
                raise ValueError(f"World Tree audit provenance mismatch: {key} {field}")
        committed_sheet = destination / candidate["sheet"]
        if not committed_sheet.is_file():
            raise FileNotFoundError(committed_sheet)
        expected_destination_hash = (
            record["baselineSheetSha256"] if baseline_state
            else record["candidateSheetSha256"]
        )
        if sha256_file(committed_sheet) != expected_destination_hash:
            state = "reviewed baseline" if baseline_state else "already-promoted candidate"
            raise ValueError(f"Committed {key} does not match the {state}")
        if not baseline_state:
            destination_asset = destination_by_key.get(key, {})
            category_review = destination_asset.get("categoryReview", {})
            if destination_asset.get("reviewDocument") != REVIEW_DOCUMENT \
                    or category_review.get("status") != "accepted":
                raise ValueError(f"Destination is not an idempotently promoted catalog: {key}")
        assert_margins(record.get("minimumAlphaMargins"), key)
        clip_records = record.get("clips", {})
        for clip, count in clips.items():
            clip_record = clip_records.get(clip, {})
            if clip_record.get("frameCount") != count:
                raise ValueError(f"World Tree audit frame-count mismatch: {key}/{clip}")
            if int(clip_record.get("uniqueVisibleFrames", 0)) < MINIMUM_UNIQUE[key][clip]:
                raise ValueError(f"World Tree audit diversity mismatch: {key}/{clip}")
            assert_margins(clip_record.get("minimumAlphaMargins"), f"{key}/{clip}")

    summary = audit.get("summary", {})
    exact_summary = {
        "assetCount": 2,
        "frameCount": 22,
        "singlePageAtlasCount": 2,
        "decodedBytes": 5_767_168,
        "decodedBudgetBytes": 8_388_608,
    }
    for field, expected in exact_summary.items():
        if summary.get(field) != expected:
            raise ValueError(f"World Tree audit summary mismatch: {field}")
    assert_margins(summary.get("minimumAlphaMargins"), "World Tree batch")
    if not 2_500 <= int(summary.get("minimumTriangles", 0)) \
            <= int(summary.get("maximumTriangles", 0)) <= 14_000:
        raise ValueError("World Tree audit triangle summary mismatch")
    if int(summary.get("minimumMeshParts", 0)) < 100:
        raise ValueError("World Tree audit mesh-part summary mismatch")
    if int(summary.get("minimumMaterialCount", 0)) < 11:
        raise ValueError("World Tree audit material summary mismatch")
    continuity = audit.get("destructionContinuity", {})
    if float(continuity.get("startAlphaDifferenceRatio", 1.0)) > 0.03:
        raise ValueError("World Tree destruction start-continuity mismatch")
    if float(continuity.get("finalHoldAlphaDifferenceRatio", 1.0)) > 0.02:
        raise ValueError("World Tree destruction final-hold mismatch")
    if int(continuity.get("crownTopDropPixels", 0)) < 4:
        raise ValueError("World Tree destruction crown-drop mismatch")
    return audit


def assert_margins(value: object, label: str) -> None:
    if not isinstance(value, dict) or set(value) != {"left", "top", "right", "bottom"}:
        raise ValueError(f"Invalid {label} margin record")
    if min(int(margin) for margin in value.values()) < 4:
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
