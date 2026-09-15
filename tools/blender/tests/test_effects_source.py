"""Phase 28.4: vfx / projectile / equipment-overlay categories exist end to end."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

BLENDER_TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BLENDER_TOOLS))

from hd_pipeline.atlas_layout import plan_grid
from hd_pipeline.config import (
    CAMERA_SCALE,
    CAMERA_SHIFT_Y,
    FRAME_SIZE,
    OVERLAY_VISUAL_SLOTS,
    PROJECTILES,
    VFX_ASSETS,
    VFX_CLIPS,
    render_tier,
)


class EffectCategoryConfigTest(unittest.TestCase):
    def test_new_frame_classes_have_size_and_camera(self) -> None:
        self.assertEqual(64, FRAME_SIZE["projectile"])
        self.assertEqual(128, FRAME_SIZE["vfx"])
        for frame_class in ("projectile", "vfx"):
            self.assertIn(frame_class, CAMERA_SCALE)
            self.assertIn(frame_class, CAMERA_SHIFT_Y)

    def test_new_categories_render_mid_tier(self) -> None:
        self.assertEqual((2, 24), render_tier("projectile_arrow", "projectile"))
        self.assertEqual((2, 24), render_tier("vfx_impact_flash", "vfx"))
        self.assertEqual((2, 24), render_tier("vfx_shockwave_ring", "vfx"))

    def test_proof_asset_sets(self) -> None:
        self.assertEqual(("projectile_arrow",), tuple(a.key for a in PROJECTILES))
        self.assertEqual(("vfx_impact_flash", "vfx_shockwave_ring"),
                         tuple(a.key for a in VFX_ASSETS))
        self.assertEqual({"play": 8}, VFX_CLIPS)
        self.assertEqual(frozenset({"boots", "weapon"}), OVERLAY_VISUAL_SLOTS)

    def test_vfx_strip_packs_one_bounded_page(self) -> None:
        pages, regions = plan_grid(VFX_CLIPS, FRAME_SIZE["vfx"])
        self.assertEqual([{"index": 0, "width": 1024, "height": 128}], pages)
        self.assertEqual(list(range(8)), [f["index"] for f in regions["play"]])

    def test_projectile_single_frame_packs_exact(self) -> None:
        pages, regions = plan_grid({"idle": 1}, FRAME_SIZE["projectile"])
        self.assertEqual([{"index": 0, "width": 64, "height": 64}], pages)
        self.assertEqual(0, regions["idle"][0]["page"])


class EffectCategoryWiringTest(unittest.TestCase):
    GENERATOR = (BLENDER_TOOLS / "generate_assets.py").read_text(encoding="utf-8")
    EFFECTS = (BLENDER_TOOLS / "hd_pipeline" / "effects.py").read_text(encoding="utf-8")

    def test_batches_are_selectable_and_dispatched(self) -> None:
        for batch in ('"vfx"', '"projectile"', '"equipment-overlay"'):
            self.assertIn(batch, self.GENERATOR)
        self.assertIn("render_vfx(output, args.keep_frames, only)", self.GENERATOR)
        self.assertIn("render_projectile(output, only)", self.GENERATOR)
        self.assertIn("render_equipment_overlay(args.catalog.resolve(), output, args.keep_frames, only)",
                      self.GENERATOR)

    def test_render_paths_use_shared_atlas_and_tier_plumbing(self) -> None:
        self.assertIn("render_static_model(\n            asset.key, asset.family, asset.frame_class,",
                      self.GENERATOR)
        self.assertIn('{"assetKind": "projectile", "variant": "normal"}', self.GENERATOR)
        self.assertIn("EFFECT_BUILDERS[asset.builder]", self.GENERATOR)
        self.assertIn("pack_grid(frame_paths, sheet_path, FRAME_SIZE[asset.frame_class])",
                      self.GENERATOR)
        self.assertIn("render_tier(key, asset.frame_class)", self.GENERATOR)

    def test_isolated_workers_cover_new_categories(self) -> None:
        self.assertIn('payload["kind"] == "vfx"', self.GENERATOR)
        self.assertIn('EFFECT_BUILDERS[payload["effect"]]', self.GENERATOR)
        self.assertIn('asset_kind == "projectile"', self.GENERATOR)

    def test_overlay_batch_filters_boots_and_weapon(self) -> None:
        self.assertIn("if item[\"visualSlot\"] in OVERLAY_VISUAL_SLOTS", self.GENERATOR)

    def test_effect_builders_are_defined(self) -> None:
        self.assertIn("def build_arrow(", self.EFFECTS)
        self.assertIn("def build_impact_flash(", self.EFFECTS)
        self.assertIn("def build_shockwave_ring(", self.EFFECTS)
        self.assertIn('"impact_flash": build_impact_flash', self.EFFECTS)
        self.assertIn('"shockwave_ring": build_shockwave_ring', self.EFFECTS)


if __name__ == "__main__":
    unittest.main()
