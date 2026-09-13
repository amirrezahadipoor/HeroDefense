from __future__ import annotations

import ast
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CONFIG = ROOT / "tools/blender/hd_pipeline/config.py"
ENVIRONMENT = ROOT / "tools/blender/hd_pipeline/environment.py"
GENERATOR = ROOT / "tools/blender/generate_assets.py"
SCENE = ROOT / "tools/blender/hd_pipeline/scene.py"
STYLE = ROOT / "docs/VISUAL_STYLE_GUIDE.md"


class ArenaPremiumSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.config = CONFIG.read_text(encoding="utf-8")
        cls.environment = ENVIRONMENT.read_text(encoding="utf-8")
        cls.generator = GENERATOR.read_text(encoding="utf-8")
        cls.scene = SCENE.read_text(encoding="utf-8")
        cls.style = STYLE.read_text(encoding="utf-8")
        cls.environment_tree = ast.parse(cls.environment)
        cls.generator_tree = ast.parse(cls.generator)

    def test_exact_arena_batch_is_exposed(self) -> None:
        self.assertIn('args.batch == "arena"', self.generator)
        self.assertIn("render_arena_environment(output, only)", self.generator)
        self.assertIn('"arena_backdrop", "environment", "arena"', self.generator)
        for family in ("ground_tile_", "crystal_prop_"):
            self.assertIn(family, self.generator)
        self.assertIn('"assetKind": "arenaBackdrop"', self.generator)

    def test_portrait_backdrop_has_an_explicit_non_square_contract(self) -> None:
        self.assertIn('"arena": (360, 640)', self.config)
        self.assertIn('"arena": 11.5', self.config)
        self.assertIn("FRAME_DIMENSIONS", self.scene)
        self.assertIn("target_width, target_height = target_size", self.scene)
        self.assertIn('"frameWidth": frame_width', self.generator)
        self.assertIn('"frameHeight": frame_height', self.generator)
        self.assertIn('outline=False', self.generator)

    def test_backdrop_is_authored_as_broad_depth_bands_with_a_clear_lane(self) -> None:
        function = self._function(self.environment_tree, "build_arena_backdrop")
        source = ast.get_source_segment(self.environment, function) or ""
        for name in (
            "arena_backplate",
            "arena_far_canopy_band",
            "arena_mid_mist_band",
            "arena_ground_band",
            "arena_near_ground_band",
            "arena_clear_combat_lane",
            "arena_sanctuary_ring",
            "arena_distant_trunk",
            "arena_edge_leaf",
        ):
            self.assertIn(name, source)
        self.assertIn('"depthBands": 5', source)
        self.assertIn('"clearLaneFraction": 0.55', source)
        self.assertIn('"visualQuality": "premium-v2"', source)
        self.assertNotIn("ShaderNodeTexNoise", source)

    def test_all_ground_variants_share_premium_construction_not_recoloring(self) -> None:
        function = self._function(self.environment_tree, "build_ground_tile")
        source = ast.get_source_segment(self.environment, function) or ""
        for landmark in (
            "ground_patch_base",
            "ground_patch_inner",
            "ground_facet_",
            "ground_waystone_",
            "ground_moss_leaf_",
            "ground_root_run_",
        ):
            self.assertIn(landmark, source)
        self.assertIn('("root-path", "waystone-crossing", "moss-clearing")', source)
        self.assertIn('"arena-ground-premium-v2"', source)
        self.assertIn("if variant not in range(3)", source)

    def test_each_crystal_has_a_named_silhouette_and_no_baked_glow(self) -> None:
        function = self._function(self.environment_tree, "build_crystal_prop")
        source = ast.get_source_segment(self.environment, function) or ""
        for identity in (
            "azure-waystone-fan",
            "violet-moon-geode",
            "amber-root-lantern",
        ):
            self.assertIn(identity, source)
        for landmark in (
            "crystal_bedrock",
            "crystal_upper_stone",
            "crystal_guard_ring",
            "crystal_shard_",
            "crystal_highlight_",
            "crystal_rune_stud_",
            "crystal_moss_leaf_",
        ):
            self.assertIn(landmark, source)
        self.assertIn('"runtimeGlow": False', source)
        self.assertIn('"arena-crystal-premium-v2"', source)
        self.assertNotIn("emission", source.lower())

    def test_static_manifest_records_reviewable_complexity_and_render_contract(self) -> None:
        function = self._function(self.generator_tree, "render_static_model")
        source = ast.get_source_segment(self.generator, function) or ""
        for field in (
            '"triangles"',
            '"meshParts"',
            '"materialCount"',
            '"renderSupersample"',
            '"renderSamples"',
        ):
            self.assertIn(field, source)
        self.assertIn("triangle_count(model.render_objects)", source)
        self.assertIn("_material_count(model.render_objects)", source)

    def test_style_guide_names_the_arena_exception_and_runtime_hierarchy(self) -> None:
        for phrase in (
            "Arena backdrop",
            "360×640",
            "portrait depth",
            "clear combat lane",
        ):
            self.assertIn(phrase, self.style)

    @staticmethod
    def _function(tree: ast.AST, name: str) -> ast.FunctionDef:
        for node in ast.walk(tree):
            if isinstance(node, ast.FunctionDef) and node.name == name:
                return node
        raise AssertionError(f"missing function {name}")


if __name__ == "__main__":
    unittest.main()
