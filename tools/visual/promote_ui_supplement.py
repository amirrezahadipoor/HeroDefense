#!/usr/bin/env python3
"""Promote only the exact accepted potion/reward-card icon supplement."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]
REVIEW_DOCUMENT = "docs/art_reviews/UI_ASSETS_PREMIUM_V2_REVIEW.md"
REVIEW_DIRECTORY = REPOSITORY / "docs/art_reviews/ui_icon_supplement_premium_v2"
AUDIT_PATH = REVIEW_DIRECTORY / "ui_supplement_audit.json"
POTION_KEYS = tuple(f"health_potion_{tier}" for tier in range(1, 7))
NEW_REWARD_KEYS = ("ui_general_power", "ui_lifesteal")
EXPECTED_KEYS = (*POTION_KEYS, *NEW_REWARD_KEYS)
EXPECTED_SHEETS = {
    "ui_supplement_potions.png",
    "ui_supplement_reward_icons.png",
    "ui_supplement_readability.png",
    "ui_supplement_value_progression.png",
    "ui_supplement_integrated.png",
}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("candidate", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    source = args.candidate.resolve()
    destination = args.destination.resolve()
    if source == destination:
        raise ValueError("Candidate and destination must differ")

    manifest_path = resolve_inside(source, "asset_manifest.json")
    manifest = read_json(manifest_path)
    assets = manifest.get("assets", [])
    candidate = {asset["key"]: asset for asset in assets}
    if len(candidate) != len(assets) or set(candidate) != set(EXPECTED_KEYS):
        raise ValueError("Supplement must contain every expected asset exactly once")
    payload = validate_candidate(source, manifest, candidate)
    validate_evidence(source, destination, manifest_path, candidate, payload)

    catalog_path = resolve_inside(destination, "asset_manifest.json")
    catalog = read_json(catalog_path)
    by_key = {asset["key"]: asset for asset in catalog["assets"]}
    if not set(POTION_KEYS) <= set(by_key):
        raise ValueError("Committed catalog is missing a potion baseline")
    review = {
        "category": "ui_assets",
        "document": REVIEW_DOCUMENT,
        "status": "accepted",
        "auditSha256": sha256(AUDIT_PATH),
        "sourceManifestSha256": sha256(manifest_path),
        "scope": "potion-and-reward-card-icon-supplement",
    }
    for key in EXPECTED_KEYS:
        asset = json.loads(json.dumps(candidate[key]))
        source_png = resolve_inside(source, asset["sheet"])
        destination_png = resolve_destination(destination, Path(asset["sheet"]))
        destination_png.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source_png, destination_png)
        asset.update({"reviewDocument": REVIEW_DOCUMENT, "categoryReview": review})
        if key == "health_potion_6":
            asset["pilotReviewDocument"] = "docs/art_reviews/PREMIUM_V2_PILOT_REVIEW.md"
        by_key[key] = asset
        metadata = destination / "icons" / f"{key}.json"
        metadata.parent.mkdir(parents=True, exist_ok=True)
        write_json(metadata, asset)

    catalog["pipelineVersion"] = max(int(catalog.get("pipelineVersion", 0)), 3)
    catalog["generatedBatch"] = "ui-supplement"
    catalog["assets"] = [by_key[key] for key in sorted(by_key)]
    write_json(catalog_path, catalog)
    print(f"Promoted 6 potions and 2 reward icons from audit {sha256(AUDIT_PATH)[:12]}")


def validate_candidate(source: Path, manifest: dict, candidate: dict[str, dict]) -> set[Path]:
    for field, expected in {
        "pipelineVersion": 3,
        "generatedBatch": "ui-supplement",
        "renderSupersample": 2,
        "opaqueRenderSamples": 24,
        "renderTierTop": [3, 32],
        "overlayRenderSamples": 8,
        "maxAtlasPageSize": 2048,
    }.items():
        if manifest.get(field) != expected:
            raise ValueError(f"Candidate global contract mismatch: {field}")
    payload = {Path("asset_manifest.json")}
    for key in EXPECTED_KEYS:
        asset = candidate[key]
        validate_asset(asset, key)
        metadata = Path("icons") / f"{key}.json"
        if read_json(resolve_inside(source, metadata)) != asset:
            raise ValueError(f"Candidate manifest/metadata mismatch: {key}")
        sheet = safe_relative(asset["sheet"])
        resolve_inside(source, sheet)
        payload.update((metadata, sheet))
    actual = {path.relative_to(source) for path in source.rglob("*") if path.is_file()}
    if actual != payload:
        raise ValueError(
            f"Candidate payload mismatch: missing={sorted(payload - actual)}, "
            f"unexpected={sorted(actual - payload)}"
        )
    return payload


def validate_asset(asset: dict, key: str) -> None:
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
        "pivot": {"units": "normalized-bottom-left", "x": 0.5, "y": 0.5},
        "alphaMode": "STRAIGHT_RGBA",
        "renderSupersample": 2,
        "renderSamples": 24,
        "visualQuality": "premium-v2",
    }
    if key in POTION_KEYS:
        expected.update({
            "tier": int(key.rsplit("_", 1)[1]),
            "heal_icon": True,
            "potionFamily": "heartwood-elixir",
            "modelRevision": "health-potion-premium-v2",
        })
        maximum = 1_200
        if not asset.get("tierConstruction"):
            raise ValueError(f"Potion tier construction missing: {key}")
    else:
        expected.update({
            "touchOnlyUI": True,
            "uiIcon": key.removeprefix("ui_"),
            "iconFamily": "heartwood-control-medallion",
            "modelRevision": "ui-control-icon-premium-v2",
        })
        maximum = 1_200
    for field, expected_value in expected.items():
        if asset.get(field) != expected_value:
            raise ValueError(f"Candidate contract mismatch: {key} {field}")
    if not 0 < int(asset.get("triangles", 0)) <= maximum:
        raise ValueError(f"Candidate triangle budget mismatch: {key}")
    if asset.get("sheets") != [{
        "decodedBytes": 36_864, "file": f"icons/{key}.png", "height": 96, "width": 96,
    }]:
        raise ValueError(f"Candidate page contract mismatch: {key}")
    if asset.get("clips") != {"idle": [{
        "height": 96, "index": 0, "page": 0, "width": 96, "x": 0, "y": 0,
    }]}:
        raise ValueError(f"Candidate frame contract mismatch: {key}")


def validate_evidence(
    source: Path,
    destination: Path,
    manifest_path: Path,
    candidate: dict[str, dict],
    payload: set[Path],
) -> None:
    review_path = REPOSITORY / REVIEW_DOCUMENT
    if not review_path.is_file() or not AUDIT_PATH.is_file():
        raise FileNotFoundError("Accepted supplement review document/audit is missing")
    actual_sheets = {path.name for path in REVIEW_DIRECTORY.glob("*.png")}
    if actual_sheets != EXPECTED_SHEETS:
        raise ValueError("Supplement review sheet set mismatch")
    audit = read_json(AUDIT_PATH)
    if audit.get("schemaVersion") != 1 or audit.get("batch") != "ui-icon-supplement-premium-v2":
        raise ValueError("Unexpected supplement audit contract")
    if audit.get("expectedKeys") != list(EXPECTED_KEYS):
        raise ValueError("Supplement audit key order mismatch")
    candidate_hash = sha256(manifest_path)
    if audit.get("candidateManifestSha256") != candidate_hash:
        raise ValueError("Supplement candidate manifest provenance mismatch")
    recorded_payload = audit.get("candidatePayload", {})
    if set(recorded_payload) != {path.as_posix() for path in payload}:
        raise ValueError("Supplement audit payload set mismatch")
    for relative, digest in recorded_payload.items():
        if sha256(resolve_inside(source, relative)) != digest:
            raise ValueError(f"Supplement candidate payload changed: {relative}")
    audit_hash = sha256(AUDIT_PATH)
    review = review_path.read_text(encoding="utf-8")
    for required in ("**Decision:** ACCEPTED", audit_hash, candidate_hash):
        if required not in review:
            raise ValueError(f"Supplement review is not hash-bound to {required}")
    evidence = audit.get("reviewSheets", {})
    if set(evidence) != EXPECTED_SHEETS or audit.get("reviewSheetCount") != 5:
        raise ValueError("Supplement sheet evidence mismatch")
    for name, record in evidence.items():
        path = REVIEW_DIRECTORY / name
        if record != {"bytes": path.stat().st_size, "sha256": sha256(path)}:
            raise ValueError(f"Supplement review sheet changed: {name}")

    destination_manifest = resolve_inside(destination, "asset_manifest.json")
    baseline = sha256(destination_manifest) == audit.get("baselineManifestSha256")
    committed = {asset["key"]: asset for asset in read_json(destination_manifest)["assets"]}
    records = {record["key"]: record for record in audit.get("assets", [])}
    if set(records) != set(EXPECTED_KEYS):
        raise ValueError("Supplement audit asset set mismatch")
    for key in EXPECTED_KEYS:
        record = records[key]
        asset = candidate[key]
        exact = {
            "sheetSha256": sha256(source / asset["sheet"]),
            "metadataSha256": sha256(source / "icons" / f"{key}.json"),
            "triangles": asset["triangles"],
            "meshParts": asset["meshParts"],
            "materialCount": asset["materialCount"],
            "modelRevision": asset["modelRevision"],
        }
        for field, expected in exact.items():
            if record.get(field) != expected:
                raise ValueError(f"Supplement audit provenance mismatch: {key}/{field}")
        target = destination / asset["sheet"]
        if baseline and key in NEW_REWARD_KEYS:
            if target.exists():
                raise ValueError(f"Reviewed baseline unexpectedly has {key}")
        else:
            if not target.is_file():
                raise FileNotFoundError(target)
            expected = record.get("baselineSheetSha256") if baseline else record["sheetSha256"]
            if sha256(target) != expected:
                raise ValueError(f"Destination supplement state mismatch: {key}")
        if not baseline:
            accepted = committed.get(key, {}).get("categoryReview", {})
            if accepted.get("status") != "accepted" or accepted.get("auditSha256") != audit_hash:
                raise ValueError(f"Destination is not idempotently accepted: {key}")
    for field, expected in {
        "assetCount": 8,
        "potionCount": 6,
        "newRewardIconCount": 2,
        "coveredRewardCardCount": 8,
        "decodedBytes": 294_912,
        "decodedBudgetBytes": 524_288,
    }.items():
        if audit.get("summary", {}).get(field) != expected:
            raise ValueError(f"Supplement audit summary mismatch: {field}")
    if int(audit.get("summary", {}).get("minimumAlphaMargin", 0)) < 4:
        raise ValueError("Supplement audit alpha margin is unsafe")


def safe_relative(value: str) -> Path:
    relative = Path(value)
    if relative.is_absolute() or ".." in relative.parts:
        raise ValueError(f"Path escapes generated root: {value}")
    return relative


def resolve_inside(root: Path, value: str | Path) -> Path:
    relative = safe_relative(str(value))
    path = (root / relative).resolve()
    if not path.is_relative_to(root.resolve()) or not path.is_file():
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


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


if __name__ == "__main__":
    main()
