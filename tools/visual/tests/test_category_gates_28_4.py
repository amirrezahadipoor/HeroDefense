"""Phase 28.4: new category gates accept tier-correct candidates only."""
from __future__ import annotations

import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path

VISUAL = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(VISUAL))

import promote_equipment_overlay as overlay
import promote_projectile_batch as projectile
import promote_vfx_batch as vfx


def _vfx_asset(key: str, samples: int = 24) -> dict:
    effect = "impact_flash" if key == "vfx_impact_flash" else "shockwave_ring"
    return {
        "key": key, "family": "vfx", "builder": effect, "frameClass": "vfx",
        "frameSize": 128, "sheet": f"vfx/{key}.png", "atlas": f"vfx/{key}.atlas",
        "sheetWidth": 1024, "sheetHeight": 128,
        "pivot": {"units": "normalized-bottom-left", "x": 0.5, "y": 0.5},
        "alphaMode": "STRAIGHT_RGBA", "frameRate": 12,
        "renderSupersample": 2, "renderSamples": samples,
        "triangles": 1200, "meshParts": 7, "materialCount": 2,
        "modelRevision": "effects-v1", "effectKind": "vfx", "effect": effect,
        "boneAnimated": False, "visualQuality": "premium-v2",
        "sheets": [{"decodedBytes": 1024 * 128 * 4, "file": f"vfx/{key}.png",
                    "height": 128, "width": 1024}],
        "clips": {"play": [{"height": 128, "index": i, "page": 0, "width": 128,
                             "x": i * 128, "y": 0} for i in range(8)]},
    }


def _projectile_asset(samples: int = 24) -> dict:
    key = "projectile_arrow"
    return {
        "key": key, "family": "projectile", "frameClass": "projectile",
        "frameSize": 64, "frameWidth": 64, "frameHeight": 64,
        "sheet": f"projectile/{key}.png", "sheetWidth": 64, "sheetHeight": 64,
        "pivot": {"units": "normalized-bottom-left", "x": 0.5, "y": 0.5},
        "alphaMode": "STRAIGHT_RGBA", "renderSupersample": 2,
        "renderSamples": samples, "triangles": 300, "meshParts": 6,
        "materialCount": 4, "modelRevision": "effects-v1",
        "effectKind": "projectile", "variant": "normal", "flightAxis": "+X",
        "visualQuality": "premium-v2",
        "sheets": [{"decodedBytes": 64 * 64 * 4, "file": f"projectile/{key}.png",
                    "height": 64, "width": 64}],
        "clips": {"idle": [{"height": 64, "index": 0, "page": 0, "width": 64,
                             "x": 0, "y": 0}]},
    }


def _write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def _write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n",
                    encoding="utf-8")


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class NewCategoryAssetGateTest(unittest.TestCase):
    def test_vfx_gate(self) -> None:
        vfx.validate_asset(_vfx_asset("vfx_impact_flash"), "vfx_impact_flash")
        vfx.validate_asset(_vfx_asset("vfx_shockwave_ring"), "vfx_shockwave_ring")
        with self.assertRaises(ValueError):
            vfx.validate_asset(_vfx_asset("vfx_impact_flash", 16),
                               "vfx_impact_flash")

    def test_projectile_gate(self) -> None:
        projectile.validate_asset(_projectile_asset(), "projectile_arrow")
        with self.assertRaises(ValueError):
            projectile.validate_asset(_projectile_asset(16), "projectile_arrow")

    def test_overlay_gate_uses_production_catalog_item(self) -> None:
        items = overlay.overlay_items()
        self.assertEqual(14, len(items))
        self.assertTrue(all(item["visualSlot"] in ("boots", "weapon")
                            for item in items.values()))
        item = items["trail_boots"]
        asset = {
            "key": "equipment_trail_boots", "family": "equipment",
            "itemId": "trail_boots", "slot": item["slot"],
            "visualSlot": "boots", "visualKind": "boots", "tier": item["tier"],
            "frameClass": "character", "frameSize": 192,
            "sheet": "equipment/trail_boots.png",
            "atlas": "equipment/trail_boots.atlas",
            "icon": "icons/equipment_trail_boots.png",
            "modelRevision": "equipment-premium-v2", "rigProfile": "hero-socket-v2",
            "renderSupersample": 2, "renderSamples": 8,
            "visualQuality": "premium-v2",
            "runtimeGlow": item["tier"] in {"RARE", "LEGENDARY"},
            "boneAnimated": True, "triangles": 500,
            "sheets": [{"decodedBytes": 1920 * 768 * 4,
                        "file": "equipment/trail_boots.png",
                        "height": 768, "width": 1920}],
            "clips": {clip: [{"index": i} for i in range(count)]
                      for clip, count in
                      (("idle", 6), ("attack", 8), ("hit", 4), ("death", 10))},
        }
        overlay.validate_asset(item, asset)
        asset["renderSamples"] = 24
        with self.assertRaises(ValueError):
            overlay.validate_asset(item, asset)


class NewCategoryCandidateGateTest(unittest.TestCase):
    def _candidate(self, root: Path, batch: str, assets: list[dict]) -> Path:
        manifest = {
            "pipelineVersion": 3, "generatedBatch": batch, "frameRate": 12,
            "renderSupersample": 2, "opaqueRenderSamples": 24,
            "overlayRenderSamples": 8, "renderTierTop": [3, 32],
            "maxAtlasPageSize": 2048, "assets": assets,
        }
        manifest_path = root / "asset_manifest.json"
        _write_json(manifest_path, manifest)
        for asset in assets:
            folder = asset["family"]
            _write_json(root / folder / f"{asset['key']}.json", asset)
            _write(root / asset["sheet"], b"png:" + asset["key"].encode())
            if "atlas" in asset:
                _write(root / asset["atlas"], b"atlas")
        return manifest_path

    def test_vfx_candidate_round_trip(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            assets = [_vfx_asset("vfx_impact_flash"),
                      _vfx_asset("vfx_shockwave_ring")]
            manifest_path = self._candidate(root, "vfx", assets)
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            candidate = {a["key"]: a for a in assets}
            payload = vfx.validate_candidate(root, manifest, candidate)
            self.assertEqual(7, len(payload))
            _write(root / "stray.txt", b"x")
            with self.assertRaises(ValueError):
                vfx.validate_candidate(root, manifest, candidate)

    def test_projectile_candidate_rejects_old_floors(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            assets = [_projectile_asset()]
            manifest_path = self._candidate(root, "projectile", assets)
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            candidate = {a["key"]: a for a in assets}
            vfx_ok = projectile.validate_candidate(root, manifest, candidate)
            self.assertEqual(3, len(vfx_ok))
            manifest["opaqueRenderSamples"] = 16
            with self.assertRaises(ValueError):
                projectile.validate_candidate(root, manifest, candidate)

    def test_overlay_payload_covers_subset_only(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            items = overlay.overlay_items()
            assets = []
            for item_id, item in items.items():
                asset = {
                    "key": f"equipment_{item_id}", "family": "equipment",
                    "itemId": item_id, "slot": item["slot"],
                    "visualSlot": item["visualSlot"],
                    "visualKind": item.get("visualKind", item["visualSlot"]),
                    "tier": item["tier"], "frameClass": "character",
                    "frameSize": 192, "sheet": f"equipment/{item_id}.png",
                    "atlas": f"equipment/{item_id}.atlas",
                    "icon": f"icons/equipment_{item_id}.png",
                    "modelRevision": "equipment-premium-v2",
                    "rigProfile": "hero-socket-v2", "renderSupersample": 2,
                    "renderSamples": 8, "visualQuality": "premium-v2",
                    "runtimeGlow": item["tier"] in {"RARE", "LEGENDARY"},
                    "boneAnimated": True, "triangles": 500,
                    "sheets": [{"decodedBytes": 1920 * 768 * 4,
                                "file": f"equipment/{item_id}.png",
                                "height": 768, "width": 1920}],
                    "clips": {clip: [{"index": i} for i in range(count)]
                              for clip, count in
                              (("idle", 6), ("attack", 8), ("hit", 4),
                               ("death", 10))},
                }
                assets.append(asset)
                _write_json(root / "equipment" / f"{item_id}.json", asset)
                _write(root / asset["sheet"], b"png")
                _write(root / asset["atlas"], b"atlas")
                _write(root / asset["icon"], b"icon")
            _write_json(root / "asset_manifest.json", {"assets": assets})
            payload = overlay.validate_candidate_payload(
                root, items, {a["key"]: a for a in assets})
            self.assertEqual(14 * 4, len(payload))


if __name__ == "__main__":
    unittest.main()
