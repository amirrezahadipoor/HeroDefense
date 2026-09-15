#!/usr/bin/env python3
"""Promote the exact audited and accepted VFX batch (Phase 28.4 pipes).

Executes once docs/art_reviews/VFX_BATCH_28_4_REVIEW.md accepts the batch;
art direction itself is owned by Phase 30.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]
REVIEW_DOCUMENT = "docs/art_reviews/VFX_BATCH_28_4_REVIEW.md"
REVIEW_DIRECTORY = REPOSITORY / "docs/art_reviews/vfx_batch_28_4"
AUDIT_PATH = REVIEW_DIRECTORY / "vfx_audit.json"
EXPECTED_KEYS = ("vfx_impact_flash", "vfx_shockwave_ring")
EXPECTED_EFFECT = {"vfx_impact_flash": "impact_flash",
                   "vfx_shockwave_ring": "shockwave_ring"}
EXPECTED_SHEETS = {"vfx_play_strips.png", "vfx_shape_grade.png"}
EXPECTED_PLAY_FRAMES = 8
MAX_TRIANGLES = 4000


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
        raise ValueError(f"VFX key mismatch: {sorted(candidate)}")
    payload = validate_candidate(source, manifest, candidate)
    validate_evidence(source, destination, manifest_path, candidate, payload)

    catalog_path = resolve_inside(destination, "asset_manifest.json")
    catalog = read_json(catalog_path)
    by_key = {asset["key"]: asset for asset in catalog["assets"]}
    audit_hash = sha256(AUDIT_PATH)
    review = {
        "category": "vfx",
        "document": REVIEW_DOCUMENT,
        "status": "accepted",
        "auditSha256": audit_hash,
        "sourceManifestSha256": sha256(manifest_path),
    }
    for key in EXPECTED_KEYS:
        asset = json.loads(json.dumps(candidate[key]))
        if key in by_key:
            require_idempotent(destination, by_key[key], source, asset, audit_hash)
        for relative in (asset["sheet"], asset["atlas"], f"vfx/{key}.json"):
            target = resolve_destination(destination, Path(relative))
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(resolve_inside(source, relative), target)
        asset.update({"reviewDocument": REVIEW_DOCUMENT, "categoryReview": review})
        by_key[key] = asset
    catalog["generatedBatch"] = "vfx"
    catalog["assets"] = [by_key[key] for key in sorted(by_key)]
    write_json(catalog_path, catalog)
    print(f"Promoted {len(EXPECTED_KEYS)} reviewed VFX assets")


def validate_candidate(source: Path, manifest: dict, candidate: dict) -> set[Path]:
    for field, expected in {
        "pipelineVersion": 3,
        "generatedBatch": "vfx",
        "frameRate": 12,
        "renderSupersample": 2,
        "opaqueRenderSamples": 24,
        "overlayRenderSamples": 8,
        "renderTierTop": [3, 32],
        "maxAtlasPageSize": 2048,
    }.items():
        if manifest.get(field) != expected:
            raise ValueError(f"Candidate global contract mismatch: {field}")
    payload = {Path("asset_manifest.json")}
    for key in EXPECTED_KEYS:
        validate_asset(candidate[key], key)
        for relative in (f"vfx/{key}.json", candidate[key]["sheet"],
                         candidate[key]["atlas"]):
            resolve_inside(source, relative)
            payload.add(Path(relative))
        if read_json(resolve_inside(source, f"vfx/{key}.json")) != candidate[key]:
            raise ValueError(f"Candidate manifest/metadata mismatch: {key}")
    actual = {path.relative_to(source) for path in source.rglob("*") if path.is_file()}
    if actual != payload:
        raise ValueError(
            f"Candidate payload mismatch: missing={sorted(payload - actual)}, "
            f"unexpected={sorted(actual - payload)}")
    return payload


def validate_asset(asset: dict, key: str) -> None:
    expected = {
        "key": key,
        "family": "vfx",
        "builder": EXPECTED_EFFECT[key],
        "frameClass": "vfx",
        "frameSize": 128,
        "sheet": f"vfx/{key}.png",
        "atlas": f"vfx/{key}.atlas",
        "sheetWidth": 1024,
        "sheetHeight": 128,
        "pivot": {"units": "normalized-bottom-left", "x": 0.5, "y": 0.5},
        "alphaMode": "STRAIGHT_RGBA",
        "frameRate": 12,
        "renderSupersample": 2,
        "renderSamples": 24,
        "modelRevision": "effects-v1",
        "effectKind": "vfx",
        "effect": EXPECTED_EFFECT[key],
        "boneAnimated": False,
        "visualQuality": "premium-v2",
    }
    for field, value in expected.items():
        if asset.get(field) != value:
            raise ValueError(f"Candidate contract mismatch: {key} {field}")
    if not 0 < int(asset.get("triangles", 0)) <= MAX_TRIANGLES:
        raise ValueError(f"Candidate triangle budget mismatch: {key}")
    if asset.get("sheets") != [{
        "decodedBytes": 1024 * 128 * 4, "file": f"vfx/{key}.png",
        "height": 128, "width": 1024,
    }]:
        raise ValueError(f"Candidate page contract mismatch: {key}")
    frames = asset.get("clips", {}).get("play", [])
    if ([f.get("index") for f in frames] != list(range(EXPECTED_PLAY_FRAMES))
            or any(f.get("page", 0) != 0 or f.get("width") != 128
                   or f.get("height") != 128 for f in frames)):
        raise ValueError(f"Candidate play-strip contract mismatch: {key}")


def validate_evidence(source: Path, destination: Path, manifest_path: Path,
                      candidate: dict, payload: set[Path],
                      review_path: Path | None = None,
                      audit_path: Path | None = None) -> None:
    review_path = review_path or REPOSITORY / REVIEW_DOCUMENT
    audit_path = audit_path or AUDIT_PATH
    if not review_path.is_file() or not audit_path.is_file():
        raise FileNotFoundError("Accepted VFX review document/audit is missing")
    audit = read_json(audit_path)
    if audit.get("schemaVersion") != 1 or audit.get("batch") != "vfx":
        raise ValueError("Unexpected VFX audit contract")
    candidate_hash = sha256(manifest_path)
    if audit.get("candidateManifestSha256") != candidate_hash:
        raise ValueError("VFX candidate manifest provenance mismatch")
    recorded_payload = audit.get("candidatePayload", {})
    if set(recorded_payload) != {path.as_posix() for path in payload}:
        raise ValueError("VFX audit payload set mismatch")
    for relative, digest in recorded_payload.items():
        if sha256(resolve_inside(source, relative)) != digest:
            raise ValueError(f"VFX candidate payload changed: {relative}")
    audit_hash = sha256(audit_path)
    review = review_path.read_text(encoding="utf-8")
    for required in ("**Decision:** ACCEPTED", audit_hash, candidate_hash):
        if required not in review:
            raise ValueError(f"VFX review is not hash-bound to {required}")
    evidence = audit.get("reviewSheets", {})
    if set(evidence) != EXPECTED_SHEETS or audit.get("reviewSheetCount") != 2:
        raise ValueError("VFX sheet evidence mismatch")
    for name, record in evidence.items():
        path = audit_path.parent / name
        if record != {"bytes": path.stat().st_size, "sha256": sha256(path)}:
            raise ValueError(f"VFX review sheet changed: {name}")
    records = {record["key"]: record for record in audit.get("assets", [])}
    if set(records) != set(EXPECTED_KEYS):
        raise ValueError("VFX audit asset set mismatch")
    for key in EXPECTED_KEYS:
        record, asset = records[key], candidate[key]
        for field, expected in {
            "sheetSha256": sha256(source / asset["sheet"]),
            "metadataSha256": sha256(source / f"vfx/{key}.json"),
            "triangles": asset["triangles"],
            "meshParts": asset["meshParts"],
            "materialCount": asset["materialCount"],
            "modelRevision": asset["modelRevision"],
        }.items():
            if record.get(field) != expected:
                raise ValueError(f"VFX audit provenance mismatch: {key}/{field}")
        if record.get("uniquePlayFrames") != EXPECTED_PLAY_FRAMES:
            raise ValueError(f"VFX motion audit mismatch: {key}")
        if int(record.get("alphaMargin", 0)) < 4:
            raise ValueError(f"VFX audit alpha margin is unsafe: {key}")
    summary = audit.get("summary", {})
    if summary.get("assetCount") != 2 or summary.get("playFrames") != 8:
        raise ValueError("VFX audit summary mismatch")
    if summary.get("allPlayFramesUnique") is not True:
        raise ValueError("VFX audit motion summary mismatch")


def require_idempotent(destination: Path, committed: dict, source: Path,
                       asset: dict, audit_hash: str) -> None:
    accepted = committed.get("categoryReview", {})
    if accepted.get("status") != "accepted" or accepted.get("auditSha256") != audit_hash:
        raise ValueError(f"Destination already has unreviewed {asset['key']}")
    if sha256(destination / committed["sheet"]) != sha256(source / asset["sheet"]):
        raise ValueError(f"Destination VFX diverged: {asset['key']}")


def safe_relative(value: str) -> Path:
    relative = Path(value)
    if relative.is_absolute() or ".." in relative.parts:
        raise ValueError(f"Path escapes generated root: {value}")
    return relative


def resolve_inside(root: Path, value: str | Path) -> Path:
    path = (root / safe_relative(str(value))).resolve()
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
