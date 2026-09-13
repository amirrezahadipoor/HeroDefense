#!/usr/bin/env python3
"""Promote only the exact hash-audited premium-v2 UI icon and frame artifact."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]
REVIEW_DOCUMENT = "docs/art_reviews/UI_ASSETS_PREMIUM_V2_REVIEW.md"
REVIEW_DIRECTORY = REPOSITORY / "docs/art_reviews/ui_assets_premium_v2"
AUDIT_PATH = REVIEW_DIRECTORY / "ui_audit.json"
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
EXPECTED_SHEETS = {
    "ui_icon_lineup.png",
    "ui_icon_readability.png",
    "ui_skin_state_matrix.png",
    "ui_nine_patch_stretch.png",
    "ui_state_construction.png",
    "ui_integrated_surfaces.png",
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
        raise ValueError("UI candidate must contain every expected asset exactly once")
    payload = validate_candidate(source, manifest, candidate)
    audit = validate_evidence(source, destination, manifest_path, candidate, payload)

    catalog_path = resolve_inside(destination, "asset_manifest.json")
    catalog = read_json(catalog_path)
    by_key = {asset["key"]: asset for asset in catalog["assets"]}
    if not set(ICON_KEYS) <= set(by_key):
        raise ValueError("Committed catalog is missing an existing UI icon")
    review = {
        "category": "ui_assets",
        "document": REVIEW_DOCUMENT,
        "status": "accepted",
        "auditSha256": sha256(AUDIT_PATH),
        "sourceManifestSha256": sha256(manifest_path),
    }
    for key in EXPECTED_KEYS:
        asset = json.loads(json.dumps(candidate[key]))
        source_png = resolve_inside(source, asset["sheet"])
        destination_png = resolve_destination(destination, Path(asset["sheet"]))
        destination_png.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source_png, destination_png)
        asset.update({"reviewDocument": REVIEW_DOCUMENT, "categoryReview": review})
        if key == "ui_inventory":
            asset["pilotReviewDocument"] = "docs/art_reviews/PREMIUM_V2_PILOT_REVIEW.md"
        by_key[key] = asset
        family = "icons" if key in ICON_KEYS else "ui"
        metadata = destination / family / f"{key}.json"
        metadata.parent.mkdir(parents=True, exist_ok=True)
        write_json(metadata, asset)

    catalog["pipelineVersion"] = max(int(catalog.get("pipelineVersion", 0)), 3)
    catalog["generatedBatch"] = "ui"
    catalog["assets"] = [by_key[key] for key in sorted(by_key)]
    write_json(catalog_path, catalog)
    print(f"Promoted 16 icons and 12 UI skin states from audit {sha256(AUDIT_PATH)[:12]}")


def validate_candidate(source: Path, manifest: dict, candidate: dict[str, dict]) -> set[Path]:
    expected_global = {
        "pipelineVersion": 3,
        "generatedBatch": "ui",
        "renderSupersample": 2,
        "opaqueRenderSamples": 16,
        "overlayRenderSamples": 8,
        "maxAtlasPageSize": 2048,
    }
    for field, expected in expected_global.items():
        if manifest.get(field) != expected:
            raise ValueError(f"Candidate global contract mismatch: {field}")
    payload = {Path("asset_manifest.json")}
    for key in EXPECTED_KEYS:
        asset = candidate[key]
        validate_asset(asset, key)
        family = "icons" if key in ICON_KEYS else "ui"
        metadata = Path(family) / f"{key}.json"
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
        "pivot": {"units": "normalized-bottom-left", "x": 0.5, "y": 0.5},
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
        maximum = 1_200
    else:
        remainder = key.removeprefix("ui_frame_")
        kind, state = remainder.rsplit("_", 1)
        expected.update({
            "uiSkin": kind,
            "uiState": state,
            "modelRevision": "forest-glass-nine-patch-v2",
            "ninePatchInsets": {"left": 24, "right": 24, "top": 24, "bottom": 24},
        })
        maximum = 600
        if not asset.get("stateConstruction"):
            raise ValueError(f"Candidate state construction missing: {key}")
    for field, expected_value in expected.items():
        if asset.get(field) != expected_value:
            raise ValueError(f"Candidate contract mismatch: {key} {field}")
    if not 0 < int(asset.get("triangles", 0)) <= maximum:
        raise ValueError(f"Candidate triangle budget mismatch: {key}")
    expected_page = [{
        "decodedBytes": 36_864, "file": f"{family}/{key}.png", "height": 96, "width": 96,
    }]
    if asset.get("sheets") != expected_page:
        raise ValueError(f"Candidate page contract mismatch: {key}")
    expected_clip = {"idle": [{
        "height": 96, "index": 0, "page": 0, "width": 96, "x": 0, "y": 0,
    }]}
    if asset.get("clips") != expected_clip:
        raise ValueError(f"Candidate frame contract mismatch: {key}")


def validate_evidence(
    source: Path,
    destination: Path,
    manifest_path: Path,
    candidate: dict[str, dict],
    payload: set[Path],
) -> dict:
    review_path = REPOSITORY / REVIEW_DOCUMENT
    if not review_path.is_file() or not AUDIT_PATH.is_file():
        raise FileNotFoundError("Accepted UI review document/audit is missing")
    actual_sheets = {path.name for path in REVIEW_DIRECTORY.glob("*.png")}
    if actual_sheets != EXPECTED_SHEETS:
        raise ValueError("UI review sheet set mismatch")
    audit = read_json(AUDIT_PATH)
    if audit.get("schemaVersion") != 1 or audit.get("batch") != "ui-assets-premium-v2":
        raise ValueError("Unexpected UI audit contract")
    if audit.get("expectedKeys") != list(EXPECTED_KEYS):
        raise ValueError("UI audit key order mismatch")
    candidate_hash = sha256(manifest_path)
    if audit.get("candidateManifestSha256") != candidate_hash:
        raise ValueError("UI candidate manifest provenance mismatch")
    recorded_payload = audit.get("candidatePayload", {})
    if set(recorded_payload) != {path.as_posix() for path in payload}:
        raise ValueError("UI audit payload set mismatch")
    for relative, digest in recorded_payload.items():
        if sha256(resolve_inside(source, relative)) != digest:
            raise ValueError(f"UI candidate payload changed: {relative}")
    audit_hash = sha256(AUDIT_PATH)
    review = review_path.read_text(encoding="utf-8")
    for required in ("**Decision:** ACCEPTED", audit_hash, candidate_hash):
        if required not in review:
            raise ValueError(f"UI review is not hash-bound to {required}")
    evidence = audit.get("reviewSheets", {})
    if set(evidence) != EXPECTED_SHEETS or audit.get("reviewSheetCount") != 6:
        raise ValueError("UI sheet evidence mismatch")
    for name, record in evidence.items():
        path = REVIEW_DIRECTORY / name
        if record != {"bytes": path.stat().st_size, "sha256": sha256(path)}:
            raise ValueError(f"UI review sheet changed: {name}")

    destination_manifest = resolve_inside(destination, "asset_manifest.json")
    baseline = sha256(destination_manifest) == audit.get("baselineManifestSha256")
    committed = {asset["key"]: asset for asset in read_json(destination_manifest)["assets"]}
    records = {record["key"]: record for record in audit.get("assets", [])}
    if set(records) != set(EXPECTED_KEYS):
        raise ValueError("UI audit asset set mismatch")
    for key in EXPECTED_KEYS:
        record = records[key]
        asset = candidate[key]
        family = "icons" if key in ICON_KEYS else "ui"
        exact = {
            "sheetSha256": sha256(source / asset["sheet"]),
            "metadataSha256": sha256(source / family / f"{key}.json"),
            "triangles": asset["triangles"],
            "meshParts": asset["meshParts"],
            "materialCount": asset["materialCount"],
            "modelRevision": asset["modelRevision"],
        }
        for field, expected in exact.items():
            if record.get(field) != expected:
                raise ValueError(f"UI audit provenance mismatch: {key}/{field}")
        target = destination / asset["sheet"]
        if key in FRAME_KEYS and baseline:
            if target.exists():
                raise ValueError(f"Reviewed baseline unexpectedly has {key}")
        else:
            if not target.is_file():
                raise FileNotFoundError(target)
            expected = record.get("baselineSheetSha256") if baseline else record["sheetSha256"]
            if sha256(target) != expected:
                raise ValueError(f"Destination UI state mismatch: {key}")
        if not baseline:
            accepted = committed.get(key, {}).get("categoryReview", {})
            if accepted.get("status") != "accepted" or accepted.get("auditSha256") != audit_hash:
                raise ValueError(f"Destination is not idempotently accepted: {key}")
    summary = audit.get("summary", {})
    exact_summary = {
        "assetCount": 28,
        "iconCount": 16,
        "skinCount": 12,
        "skinFamilyCount": 3,
        "stateCountPerSkin": 4,
        "decodedBytes": 1_032_192,
        "decodedBudgetBytes": 2_097_152,
    }
    for field, expected in exact_summary.items():
        if summary.get(field) != expected:
            raise ValueError(f"UI audit summary mismatch: {field}")
    if int(summary.get("minimumAlphaMargin", 0)) < 4:
        raise ValueError("UI audit alpha margin is unsafe")
    return audit


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
