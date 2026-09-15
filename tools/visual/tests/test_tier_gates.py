"""Phase 28.3 gates: review/promote scripts must require the tiered engine.

New-engine candidates render mid tier at 2x/24 and top tier (hero, bosses,
trees) at 3x/32; overlays stay 2x/8. Old (2x/16) candidates must fail the
gates so 28.7 cannot promote stale output. The committed-catalog validator
deliberately still accepts both engines until 28.7 re-renders everything.
"""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

VISUAL = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(VISUAL))

import promote_ceremony_batch
import promote_ui_supplement

BATCH_GATE_FILES = (
    "create_arena_batch_review.py",
    "create_boss_batch_review.py",
    "create_enemy_batch_review.py",
    "create_ui_batch_review.py",
    "create_ui_supplement_review.py",
    "create_world_tree_batch_review.py",
    "promote_arena_batch.py",
    "promote_boss_batch.py",
    "promote_enemy_batch.py",
    "promote_ui_batch.py",
    "promote_ui_supplement.py",
    "promote_world_tree_batch.py",
    "create_vfx_batch_review.py",
    "create_projectile_batch_review.py",
    "promote_vfx_batch.py",
    "promote_projectile_batch.py",
)
MID_TIER_ASSET_FILES = (
    "create_arena_batch_review.py",
    "create_enemy_batch_review.py",
    "create_ui_batch_review.py",
    "create_ui_supplement_review.py",
    "promote_arena_batch.py",
    "promote_enemy_batch.py",
    "promote_skill_icons.py",
    "promote_ui_batch.py",
    "promote_ui_supplement.py",
    "create_vfx_batch_review.py",
    "create_projectile_batch_review.py",
    "promote_vfx_batch.py",
    "promote_projectile_batch.py",
)
TOP_TIER_ASSET_FILES = (
    "create_boss_batch_review.py",
    "create_world_tree_batch_review.py",
    "promote_boss_batch.py",
    "promote_ceremony_batch.py",
    "promote_world_tree_batch.py",
)
OVERLAY_FILES = (
    "create_equipment_batch_review.py",
    "promote_equipment_batch.py",
    "promote_equipment_overlay.py",
)


class TierGateSourceTest(unittest.TestCase):
    def test_batch_gates_require_new_floors_and_fingerprint(self) -> None:
        for name in BATCH_GATE_FILES:
            with self.subTest(script=name):
                src = (VISUAL / name).read_text(encoding="utf-8")
                # Normalize the two gate styles (`!=` checks vs floor dicts).
                norm = src.replace('get("opaqueRenderSamples") != 24',
                                   '"opaqueRenderSamples": 24')
                norm = norm.replace('get("renderTierTop")', '"renderTierTop"')
                self.assertIn('"opaqueRenderSamples": 24', norm)
                self.assertIn('"renderTierTop"', norm)

    def test_no_old_engine_pins_remain_in_gates(self) -> None:
        for path in sorted(VISUAL.glob("create_*_review.py")) + sorted(VISUAL.glob("promote_*.py")):
            with self.subTest(script=path.name):
                src = path.read_text(encoding="utf-8")
                self.assertNotIn('"renderSamples": 16', src)
                self.assertNotIn('"opaqueRenderSamples": 16', src)
                self.assertNotIn("opaqueRenderSamples\") != 16", src)
                self.assertNotIn("else 16", src)

    def test_mid_tier_asset_pins(self) -> None:
        for name in MID_TIER_ASSET_FILES:
            with self.subTest(script=name):
                src = (VISUAL / name).read_text(encoding="utf-8")
                self.assertIn('"renderSamples": 24', src)

    def test_top_tier_asset_pins(self) -> None:
        for name in TOP_TIER_ASSET_FILES:
            with self.subTest(script=name):
                src = (VISUAL / name).read_text(encoding="utf-8")
                self.assertIn('"renderSupersample": 3', src)
                self.assertIn('"renderSamples": 32', src)

    def test_overlay_pins_unchanged(self) -> None:
        for name in OVERLAY_FILES:
            with self.subTest(script=name):
                src = (VISUAL / name).read_text(encoding="utf-8")
                self.assertIn('"renderSamples": 8', src)
                self.assertNotIn('"renderSamples": 24', src)
                self.assertNotIn('"renderSamples": 32', src)

    def test_premium_pilot_preserves_candidate_tiers(self) -> None:
        src = (VISUAL / "promote_premium_pilot.py").read_text(encoding="utf-8")
        self.assertNotIn('asset["renderSupersample"]', src)
        self.assertNotIn('asset["renderSamples"]', src)
        self.assertIn('"opaqueRenderSamples": 24', src)

    def test_committed_validator_still_accepts_both_engines(self) -> None:
        # Deliberate until 28.7 re-renders the committed catalog; 28.7 raises
        # this floor to 24.
        src = (VISUAL / "validate_generated_assets.py").read_text(encoding="utf-8")
        self.assertIn('manifest.get("opaqueRenderSamples", 0) < 16', src)


def _ceremony_asset(key: str, supersample: int, samples: int) -> dict:
    asset = dict(promote_ceremony_batch.EXPECTED[key])
    asset.update({
        "key": key,
        "sheet": f"sprites/{key}.png",
        "atlas": f"sprites/{key}.atlas",
        "alphaMode": "STRAIGHT_RGBA",
        "frameRate": 12,
        "renderSupersample": supersample,
        "renderSamples": samples,
        "boneAnimated": True,
        "visualQuality": "premium-v2",
        "sheets": [{
            "decodedBytes": asset["sheetWidth"] * asset["sheetHeight"] * 4,
            "file": f"sprites/{key}.png",
            "height": asset["sheetHeight"],
            "width": asset["sheetWidth"],
        }],
        "clips": {
            clip: [{
                "height": asset["frameSize"], "index": i, "page": 0,
                "width": asset["frameSize"], "x": 0, "y": 0,
            } for i in range(count)]
            for clip, count in promote_ceremony_batch.CLIPS[key].items()
        },
    })
    return asset


def _potion_asset(key: str, supersample: int, samples: int) -> dict:
    return {
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
        "renderSupersample": supersample,
        "renderSamples": samples,
        "visualQuality": "premium-v2",
        "tier": int(key.rsplit("_", 1)[1]),
        "heal_icon": True,
        "potionFamily": "heartwood-elixir",
        "modelRevision": "health-potion-premium-v2",
        "tierConstruction": ["test-fixture"],
        "triangles": 600,
        "sheets": [{
            "decodedBytes": 36_864, "file": f"icons/{key}.png",
            "height": 96, "width": 96,
        }],
        "clips": {"idle": [{
            "height": 96, "index": 0, "page": 0, "width": 96, "x": 0, "y": 0,
        }]},
    }


class TierGateBehaviorTest(unittest.TestCase):
    def test_ceremony_gate_accepts_top_tier_rejects_old(self) -> None:
        promote_ceremony_batch.validate_asset(_ceremony_asset("hero_ceremony", 3, 32),
                                              "hero_ceremony")
        promote_ceremony_batch.validate_asset(_ceremony_asset("world_tree_sapling", 3, 32),
                                              "world_tree_sapling")
        with self.assertRaises(ValueError):
            promote_ceremony_batch.validate_asset(_ceremony_asset("hero_ceremony", 2, 16),
                                                  "hero_ceremony")

    def test_supplement_gate_accepts_mid_tier_rejects_old(self) -> None:
        promote_ui_supplement.validate_asset(_potion_asset("health_potion_1", 2, 24),
                                             "health_potion_1")
        with self.assertRaises(ValueError):
            promote_ui_supplement.validate_asset(_potion_asset("health_potion_1", 2, 16),
                                                 "health_potion_1")


if __name__ == "__main__":
    unittest.main()
