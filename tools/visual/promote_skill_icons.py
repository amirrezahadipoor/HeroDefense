#!/usr/bin/env python3
"""Promote the reviewed Phase 17 skill-icon batch into the committed asset catalog.

Usage: promote_skill_icons.py CANDIDATE_DIR android/assets/generated

The candidate is the unpacked `hero-defense-skill-icons-sprites` CI artifact. The
script validates the render contract, writes the audit record next to the review
sheets, copies the five PNG/JSON pairs, and records the review binding in the
catalog manifest, mirroring `promote_ui_supplement.py`.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]
REVIEW_DOCUMENT = "docs/art_reviews/SKILL_ICONS_PREMIUM_V2_REVIEW.md"
REVIEW_DIRECTORY = REPOSITORY / "docs/art_reviews/skill_icons_premium_v2"
AUDIT_PATH = REVIEW_DIRECTORY / "skill_icons_audit.json"
EXPECTED_KEYS = (
    "ui_skill_chain_lightning",
    "ui_skill_multi_shot",
    "ui_skill_stun_chance",
    "ui_skill_critical_mastery",
    "ui_skill_long_range",
)
CONTACT_SHEET = "skill_icons_contact.png"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("candidate", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    source = args.candidate.resolve()
    destination = args.destination.resolve()
    if source == destination:
        raise ValueError("Candidate and destination must differ")

    manifest_path = source / "asset_manifest.json"
    manifest = read_json(manifest_path)
    assets = manifest.get("assets", [])
    candidate = {asset["key"]: asset for asset in assets}
    if len(candidate) != len(assets) or set(candidate) != set(EXPECTED_KEYS):
        raise ValueError("Skill batch must contain exactly the five skill icons")
    for key in EXPECTED_KEYS:
        validate_asset(candidate[key], key)
        if read_json(source / "icons" / f"{key}.json") != candidate[key]:
            raise ValueError(f"Candidate manifest/metadata mismatch: {key}")
        if png_size(source / "icons" / f"{key}.png") != (96, 96):
            raise ValueError(f"Candidate icon is not 96x96: {key}")

    review_path = REPOSITORY / REVIEW_DOCUMENT
    if not review_path.is_file() or not (REVIEW_DIRECTORY / CONTACT_SHEET).is_file():
        raise FileNotFoundError("Accepted review document or contact sheet is missing")

    audit = {
        "batch": "skill-icons-premium-v2",
        "candidateManifestSha256": sha256(manifest_path),
        "assets": {
            key: {"sheetSha256": sha256(source / "icons" / f"{key}.png")}
            for key in EXPECTED_KEYS
        },
        "reviewSheets": {CONTACT_SHEET: sha256(REVIEW_DIRECTORY / CONTACT_SHEET)},
    }
    write_json(AUDIT_PATH, audit)
    review = review_path.read_text(encoding="utf-8")
    if audit["candidateManifestSha256"] not in review or "**Decision:** ACCEPTED" not in review:
        raise ValueError("Review document must accept this exact candidate manifest hash")

    catalog_path = destination / "asset_manifest.json"
    catalog = read_json(catalog_path)
    by_key = {asset["key"]: asset for asset in catalog["assets"]}
    review_record = {
        "category": "ui_assets",
        "document": REVIEW_DOCUMENT,
        "status": "accepted",
        "auditSha256": sha256(AUDIT_PATH),
        "sourceManifestSha256": audit["candidateManifestSha256"],
        "scope": "phase-17-skill-icons",
    }
    for key in EXPECTED_KEYS:
        asset = json.loads(json.dumps(candidate[key]))
        shutil.copy2(source / "icons" / f"{key}.png", destination / "icons" / f"{key}.png")
        asset.update({"reviewDocument": REVIEW_DOCUMENT, "categoryReview": review_record})
        by_key[key] = asset
        write_json(destination / "icons" / f"{key}.json", asset)
    catalog["generatedBatch"] = "skill-icons"
    catalog["assets"] = [by_key[key] for key in sorted(by_key)]
    write_json(catalog_path, catalog)
    print(f"Promoted 5 skill icons from candidate {audit['candidateManifestSha256'][:12]}")


def validate_asset(asset: dict, key: str) -> None:
    expected = {
        "key": key,
        "family": "icons",
        "frameClass": "item",
        "frameSize": 96,
        "sheet": f"icons/{key}.png",
        "sheetWidth": 96,
        "sheetHeight": 96,
        "alphaMode": "STRAIGHT_RGBA",
        "renderSupersample": 2,
        "renderSamples": 24,
        "visualQuality": "premium-v2",
        "touchOnlyUI": True,
        "uiIcon": key.removeprefix("ui_"),
        "iconFamily": "heartwood-control-medallion",
        "modelRevision": "ui-control-icon-premium-v2",
    }
    for field, value in expected.items():
        if asset.get(field) != value:
            raise ValueError(f"{key}: {field} is {asset.get(field)!r}, expected {value!r}")


def png_size(path: Path) -> tuple[int, int]:
    header = path.read_bytes()[:24]
    if header[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path} is not a PNG")
    return int.from_bytes(header[16:20], "big"), int.from_bytes(header[20:24], "big")


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


if __name__ == "__main__":
    main()
