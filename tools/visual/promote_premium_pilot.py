#!/usr/bin/env python3
"""Promote an explicitly reviewed premium pilot into the committed asset catalog."""
from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path

EXPECTED_KEYS = {
    "hero",
    "rootling",
    "ancient_golem",
    "equipment_worldbranch",
    "equipment_crown_of_first_leaves",
    "equipment_heartwood_aegis",
    "equipment_boots_of_three_winds",
    "equipment_eternal_seed",
    "health_potion_6",
    "crystal_prop_0",
    "ui_inventory",
}
REVIEW_DOCUMENT = "docs/art_reviews/PREMIUM_V2_PILOT_REVIEW.md"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("candidate", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    source = args.candidate.resolve()
    destination = args.destination.resolve()
    candidate = json.loads((source / "asset_manifest.json").read_text(encoding="utf-8"))
    catalog_path = destination / "asset_manifest.json"
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    candidate_assets = {asset["key"]: asset for asset in candidate["assets"]}
    if set(candidate_assets) != EXPECTED_KEYS:
        raise ValueError(f"Premium pilot key mismatch: {sorted(set(candidate_assets) ^ EXPECTED_KEYS)}")

    for path in source.rglob("*"):
        if not path.is_file() or path.name == "asset_manifest.json" or "_frames" in path.parts:
            continue
        target = destination / path.relative_to(source)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, target)

    by_key = {asset["key"]: asset for asset in catalog["assets"]}
    for key, asset in candidate_assets.items():
        asset["visualQuality"] = "premium-v2"
        asset["renderSupersample"] = 2
        asset["renderSamples"] = 8 if asset["family"] == "equipment" else 16
        asset["reviewDocument"] = REVIEW_DOCUMENT
        by_key[key] = asset
        json_path = _asset_json_path(destination, asset)
        json_data = json.loads(json_path.read_text(encoding="utf-8"))
        json_data.update({
            "visualQuality": asset["visualQuality"],
            "renderSupersample": asset["renderSupersample"],
            "renderSamples": asset["renderSamples"],
            "reviewDocument": REVIEW_DOCUMENT,
        })
        _write_json(json_path, json_data)

    catalog.update({
        "pipelineVersion": 3,
        "generatedBatch": "premium-pilot",
        "renderSupersample": 2,
        "opaqueRenderSamples": 16,
        "overlayRenderSamples": 8,
    })
    catalog["assets"] = [by_key[key] for key in sorted(by_key)]
    _write_json(catalog_path, catalog)
    print(f"Promoted {len(EXPECTED_KEYS)} reviewed premium-v2 assets into {destination}")


def _asset_json_path(root: Path, asset: dict) -> Path:
    family = asset["family"]
    if family in {"hero", "enemy", "boss", "world_tree"}:
        return root / "sprites" / f"{asset['key']}.json"
    if family == "equipment":
        return root / "equipment" / f"{asset['itemId']}.json"
    return root / family / f"{asset['key']}.json"


def _write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
