#!/usr/bin/env python3
"""Attach an accepted category-review record to exact generated-asset keys."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    parser.add_argument("review_document")
    parser.add_argument("category")
    parser.add_argument("keys", nargs="+")
    args = parser.parse_args()

    root = args.root.resolve()
    document = Path(args.review_document)
    if document.is_absolute() or ".." in document.parts:
        raise ValueError("Review document must be a repository-relative path")
    if not (REPOSITORY / document).is_file():
        raise FileNotFoundError(f"Missing accepted review document: {document}")
    requested = set(args.keys)
    if len(requested) != len(args.keys):
        raise ValueError("Duplicate reviewed asset key")

    manifest_path = root / "asset_manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    by_key = {asset["key"]: asset for asset in manifest["assets"]}
    missing = requested - set(by_key)
    if missing:
        raise ValueError(f"Unknown reviewed asset keys: {sorted(missing)}")

    review = {
        "category": args.category,
        "document": document.as_posix(),
        "status": "accepted",
    }
    for key in sorted(requested):
        asset = by_key[key]
        if asset.get("visualQuality") != "premium-v2":
            raise ValueError(f"Cannot accept non-premium asset: {key}")
        asset["categoryReview"] = review
        json_path = asset_json_path(root, asset)
        metadata = json.loads(json_path.read_text(encoding="utf-8"))
        metadata["categoryReview"] = review
        write_json(json_path, metadata)

    manifest["assets"] = [by_key[key] for key in sorted(by_key)]
    write_json(manifest_path, manifest)
    print(f"Recorded accepted {args.category} review for {len(requested)} assets")


def asset_json_path(root: Path, asset: dict) -> Path:
    family = asset["family"]
    if family in {"hero", "enemy", "boss", "world_tree"}:
        return root / "sprites" / f"{asset['key']}.json"
    if family == "equipment":
        return root / "equipment" / f"{asset['itemId']}.json"
    return root / family / f"{asset['key']}.json"


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
