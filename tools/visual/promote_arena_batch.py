#!/usr/bin/env python3
"""Promote only the exact hash-audited premium-v2 arena artifact."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]
REVIEW_DOCUMENT = "docs/art_reviews/ARENA_PREMIUM_V2_REVIEW.md"
REVIEW_DIRECTORY = REPOSITORY / "docs/art_reviews/arena_premium_v2"
AUDIT_PATH = REVIEW_DIRECTORY / "arena_audit.json"
EXPECTED_KEYS = (
    "arena_backdrop",
    "ground_tile_0",
    "ground_tile_1",
    "ground_tile_2",
    "crystal_prop_0",
    "crystal_prop_1",
    "crystal_prop_2",
)
EXPECTED_SHEETS = {
    "arena_integrated_composition.png",
    "arena_backdrop_value.png",
    "arena_ground_lineup.png",
    "arena_crystal_lineup.png",
    "arena_runtime_readability.png",
    "arena_depth_hierarchy.png",
}
GROUND_IDENTITIES = ("root-path", "waystone-crossing", "moss-clearing")
CRYSTAL_IDENTITIES = ("azure-waystone-fan", "violet-moon-geode", "amber-root-lantern")


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
    if len(candidate_by_key) != len(assets) or tuple(sorted(candidate_by_key)) != tuple(sorted(EXPECTED_KEYS)):
        raise ValueError("Arena candidate must contain each expected asset exactly once")
    expected_payload = validate_candidate_payload(source, candidate_manifest, candidate_by_key)
    audit = validate_review_evidence(
        source, destination, candidate_manifest_path, candidate_by_key, expected_payload
    )

    catalog_path = resolve_inside(destination, "asset_manifest.json")
    catalog = read_json(catalog_path)
    by_key = {asset["key"]: asset for asset in catalog["assets"]}
    required_existing = set(EXPECTED_KEYS) - {"arena_backdrop"}
    if not required_existing <= set(by_key):
        raise ValueError("Committed catalog is missing an existing arena asset")

    review = {
        "category": "arena_environment",
        "document": REVIEW_DOCUMENT,
        "status": "accepted",
        "auditSha256": sha256_file(AUDIT_PATH),
        "sourceManifestSha256": sha256_file(candidate_manifest_path),
    }
    for key in EXPECTED_KEYS:
        candidate_asset = json.loads(json.dumps(candidate_by_key[key]))
        source_png = resolve_inside(source, candidate_asset["sheet"])
        destination_png = resolve_destination(destination, Path(candidate_asset["sheet"]))
        destination_png.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source_png, destination_png)
        candidate_asset.update({
            "reviewDocument": REVIEW_DOCUMENT,
            "categoryReview": review,
        })
        if key == "crystal_prop_0":
            candidate_asset["pilotReviewDocument"] = (
                "docs/art_reviews/PREMIUM_V2_PILOT_REVIEW.md"
            )
        by_key[key] = candidate_asset
        metadata_path = destination / "environment" / f"{key}.json"
        metadata_path.parent.mkdir(parents=True, exist_ok=True)
        write_json(metadata_path, candidate_asset)

    catalog["pipelineVersion"] = max(
        int(catalog.get("pipelineVersion", 0)),
        int(candidate_manifest.get("pipelineVersion", 0)),
    )
    catalog["generatedBatch"] = "arena"
    catalog["assets"] = [by_key[key] for key in sorted(by_key)]
    write_json(catalog_path, catalog)
    print(
        f"Promoted exactly seven reviewed premium-v2 arena assets from audit "
        f"{sha256_file(AUDIT_PATH)[:12]}"
    )


def validate_candidate_payload(
    source: Path, manifest: dict, candidate_by_key: dict[str, dict]
) -> set[Path]:
    expected_global = {
        "pipelineVersion": 3,
        "generatedBatch": "arena",
        "frameRate": 12,
        "renderSupersample": 2,
        "opaqueRenderSamples": 16,
        "overlayRenderSamples": 8,
        "maxAtlasPageSize": 2048,
    }
    for field, expected in expected_global.items():
        if manifest.get(field) != expected:
            raise ValueError(f"Candidate global contract mismatch: {field}={manifest.get(field)!r}")

    expected_payload: set[Path] = {Path("asset_manifest.json")}
    for key in EXPECTED_KEYS:
        asset = candidate_by_key[key]
        validate_asset_contract(asset, key)
        metadata_relative = Path("environment") / f"{key}.json"
        if read_json(resolve_inside(source, metadata_relative)) != asset:
            raise ValueError(f"Candidate manifest/metadata mismatch: {key}")
        sheet_relative = safe_relative(asset["sheet"])
        resolve_inside(source, sheet_relative)
        expected_payload.update((metadata_relative, sheet_relative))

    actual_payload = {
        path.relative_to(source)
        for path in source.rglob("*") if path.is_file()
    }
    if actual_payload != expected_payload:
        raise ValueError(
            "Candidate payload mismatch: "
            f"missing={sorted(expected_payload - actual_payload)}, "
            f"unexpected={sorted(actual_payload - expected_payload)}"
        )
    return expected_payload


def validate_asset_contract(asset: dict, key: str) -> None:
    if key == "arena_backdrop":
        width, height = 720, 1280
        expected = {
            "key": key,
            "family": "environment",
            "frameClass": "arena",
            "frameSize": 720,
            "frameWidth": width,
            "frameHeight": height,
            "sheetWidth": width,
            "sheetHeight": height,
            "modelRevision": "forest-sanctuary-backdrop-v3",
            "compositionProfile": "portrait-clear-lane-v2",
            "depthBands": 5,
            "visualQuality": "premium-v2",
        }
        triangle_range = (1_000, 3_000)
        minimum_parts, minimum_materials = 45, 8
    elif key.startswith("ground_tile_"):
        variant = int(key[-1])
        width = height = 384
        expected = {
            "key": key,
            "family": "environment",
            "frameClass": "environment",
            "frameSize": 384,
            "frameWidth": width,
            "frameHeight": height,
            "sheetWidth": width,
            "sheetHeight": height,
            "modelRevision": "arena-ground-premium-v3",
            "groundIdentity": GROUND_IDENTITIES[variant],
            "variant": variant,
            "visualQuality": "premium-v2",
        }
        triangle_range = (300, 600)
        minimum_parts, minimum_materials = 20, 6
    else:
        variant = int(key[-1])
        width = height = 384
        expected = {
            "key": key,
            "family": "environment",
            "frameClass": "environment",
            "frameSize": 384,
            "frameWidth": width,
            "frameHeight": height,
            "sheetWidth": width,
            "sheetHeight": height,
            "modelRevision": "arena-crystal-premium-v2",
            "prop": CRYSTAL_IDENTITIES[variant],
            "variant": variant,
            "runtimeGlow": False,
            "visualQuality": "premium-v2",
        }
        triangle_range = (700, 2_200)
        minimum_parts, minimum_materials = 30, 8
    common = {
        "sheet": f"environment/{key}.png",
        "pivot": {"units": "normalized-bottom-left", "x": 0.5, "y": 0.5},
        "alphaMode": "STRAIGHT_RGBA",
        "renderSupersample": 2,
        "renderSamples": 16,
    }
    expected.update(common)
    for field, expected_value in expected.items():
        if asset.get(field) != expected_value:
            raise ValueError(
                f"Candidate contract mismatch: {key} {field}={asset.get(field)!r}, "
                f"expected {expected_value!r}"
            )
    triangles = int(asset.get("triangles", 0))
    if not triangle_range[0] <= triangles <= triangle_range[1]:
        raise ValueError(f"Candidate triangle budget mismatch: {key}")
    if int(asset.get("meshParts", 0)) < minimum_parts:
        raise ValueError(f"Candidate mesh-part quality floor mismatch: {key}")
    if int(asset.get("materialCount", 0)) < minimum_materials:
        raise ValueError(f"Candidate material quality floor mismatch: {key}")
    expected_sheet = [{
        "decodedBytes": width * height * 4,
        "file": f"environment/{key}.png",
        "height": height,
        "width": width,
    }]
    if asset.get("sheets") != expected_sheet:
        raise ValueError(f"Candidate page contract mismatch: {key}")
    expected_clips = {"idle": [{
        "height": height, "index": 0, "page": 0,
        "width": width, "x": 0, "y": 0,
    }]}
    if asset.get("clips") != expected_clips:
        raise ValueError(f"Candidate static-frame contract mismatch: {key}")


def validate_review_evidence(
    source: Path,
    destination: Path,
    candidate_manifest_path: Path,
    candidate_by_key: dict[str, dict],
    expected_payload: set[Path],
) -> dict:
    review_document_path = REPOSITORY / REVIEW_DOCUMENT
    if not review_document_path.is_file():
        raise FileNotFoundError(f"Missing accepted review document: {REVIEW_DOCUMENT}")
    if not AUDIT_PATH.is_file():
        raise FileNotFoundError(f"Missing arena audit: {AUDIT_PATH}")
    actual_sheets = {path.name for path in REVIEW_DIRECTORY.glob("*.png")}
    if actual_sheets != EXPECTED_SHEETS:
        raise ValueError(
            "Arena review-sheet set mismatch: "
            f"missing={sorted(EXPECTED_SHEETS - actual_sheets)}, "
            f"unexpected={sorted(actual_sheets - EXPECTED_SHEETS)}"
        )

    audit = read_json(AUDIT_PATH)
    if audit.get("schemaVersion") != 1 or audit.get("batch") != "arena-premium-v2":
        raise ValueError("Unexpected arena audit contract")
    if audit.get("expectedKeys") != list(EXPECTED_KEYS):
        raise ValueError("Arena audit key order mismatch")
    candidate_hash = sha256_file(candidate_manifest_path)
    if audit.get("candidateManifestSha256") != candidate_hash:
        raise ValueError("Arena candidate-manifest provenance mismatch")
    recorded_payload = audit.get("candidatePayload", {})
    expected_strings = {path.as_posix() for path in expected_payload}
    if set(recorded_payload) != expected_strings:
        raise ValueError("Arena audit payload-file set mismatch")
    for relative, digest in recorded_payload.items():
        if sha256_file(resolve_inside(source, relative)) != digest:
            raise ValueError(f"Arena candidate payload hash mismatch: {relative}")

    audit_hash = sha256_file(AUDIT_PATH)
    review_text = review_document_path.read_text(encoding="utf-8")
    for required in ("**Decision:** ACCEPTED", audit_hash, candidate_hash):
        if required not in review_text:
            raise ValueError(f"Arena review document is not hash-bound to {required}")
    recorded_sheets = audit.get("reviewSheets", {})
    if set(recorded_sheets) != EXPECTED_SHEETS or audit.get("reviewSheetCount") != 6:
        raise ValueError("Arena review-sheet evidence mismatch")
    for name, record in recorded_sheets.items():
        path = REVIEW_DIRECTORY / name
        expected_record = {"bytes": path.stat().st_size, "sha256": sha256_file(path)}
        if record != expected_record:
            raise ValueError(f"Arena review-sheet hash mismatch: {name}")

    destination_manifest_path = resolve_inside(destination, "asset_manifest.json")
    destination_hash = sha256_file(destination_manifest_path)
    baseline_state = destination_hash == audit.get("baselineManifestSha256")
    destination_catalog = read_json(destination_manifest_path)
    destination_by_key = {asset["key"]: asset for asset in destination_catalog["assets"]}
    records = audit.get("assets", [])
    records_by_key = {record["key"]: record for record in records}
    if len(records_by_key) != len(records) or set(records_by_key) != set(EXPECTED_KEYS):
        raise ValueError("Arena audit asset set mismatch")

    for key in EXPECTED_KEYS:
        record = records_by_key[key]
        candidate = candidate_by_key[key]
        exact = {
            "sheetSha256": sha256_file(source / candidate["sheet"]),
            "metadataSha256": sha256_file(source / "environment" / f"{key}.json"),
            "triangles": candidate["triangles"],
            "meshParts": candidate["meshParts"],
            "materialCount": candidate["materialCount"],
            "modelRevision": candidate["modelRevision"],
        }
        for field, expected in exact.items():
            if record.get(field) != expected:
                raise ValueError(f"Arena audit provenance mismatch: {key} {field}")
        destination_png = destination / candidate["sheet"]
        if key == "arena_backdrop" and baseline_state:
            if destination_png.exists():
                raise ValueError("Reviewed baseline unexpectedly already contains the backdrop")
        else:
            if not destination_png.is_file():
                raise FileNotFoundError(destination_png)
            expected_hash = record.get("baselineSheetSha256") if baseline_state else record["sheetSha256"]
            if sha256_file(destination_png) != expected_hash:
                raise ValueError(f"Destination arena state mismatch: {key}")
        if not baseline_state:
            destination_asset = destination_by_key.get(key, {})
            review = destination_asset.get("categoryReview", {})
            if destination_asset.get("reviewDocument") != REVIEW_DOCUMENT \
                    or review.get("status") != "accepted" \
                    or review.get("auditSha256") != audit_hash:
                raise ValueError(f"Destination is not an idempotently promoted arena catalog: {key}")

    summary = audit.get("summary", {})
    exact_summary = {
        "assetCount": 7,
        "staticFrameCount": 7,
        "portraitBackdropCount": 1,
        "groundTileCount": 3,
        "crystalPropCount": 3,
        "decodedBytes": 1_806_336,
        "decodedBudgetBytes": 2_097_152,
    }
    for field, expected in exact_summary.items():
        if summary.get(field) != expected:
            raise ValueError(f"Arena audit summary mismatch: {field}")
    if int(summary.get("minimumTransparentAssetMargin", 0)) < 4:
        raise ValueError("Arena audit alpha-margin summary mismatch")
    return audit


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
