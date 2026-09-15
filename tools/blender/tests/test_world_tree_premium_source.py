"""Dependency-free source contracts for the studio-v3 World Tree batch."""
from __future__ import annotations

import ast
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ENVIRONMENT = ROOT / "tools/blender/hd_pipeline/environment.py"
GENERATOR = ROOT / "tools/blender/generate_assets.py"
WORKFLOW = ROOT / ".github/workflows/generate-visual-assets.yml"


class WorldTreePremiumSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.environment_source = ENVIRONMENT.read_text(encoding="utf-8")
        cls.environment_tree = ast.parse(cls.environment_source)
        cls.generator_source = GENERATOR.read_text(encoding="utf-8")
        cls.generator_tree = ast.parse(cls.generator_source)

    def test_both_states_declare_locked_premium_identity(self) -> None:
        required_literals = {
            "heartwood-sanctum-healthy-v2",
            "heartwood-sanctum-wounded-v2",
            "segmented-world-tree-v2",
            "living-heart-pulse-v2",
            "wounded-collapse-v2",
            "studio-v3",
            "destroy",
        }
        literals = {
            node.value
            for node in ast.walk(self.environment_tree)
            if isinstance(node, ast.Constant) and isinstance(node.value, str)
        }
        self.assertTrue(required_literals <= literals)

    def test_segmented_rig_and_landmark_geometry_are_authored(self) -> None:
        for bone in (
            "root", "trunk.lower", "trunk.upper", "crown", "branch.L", "branch.R",
            "bough.L", "bough.R", "canopy.L", "canopy.R", "heart", "debris.L", "debris.R",
        ):
            self.assertIn(f'"{bone}"', self.environment_source)
        for authored_part in (
            "tree_buttress_root_", "tree_bark_plate_", "tree_heart_core",
            "tree_heart_ring", "tree_canopy_cluster_", "tree_crown_leaf_",
            "tree_broken_bough_R", "tree_wound_rune_", "tree_falling_bark_",
        ):
            self.assertIn(authored_part, self.environment_source)
        self.assertIn("author_world_tree_actions", self.environment_source)
        self.assertGreaterEqual(self.environment_source.count("_tree_key("), 12)

    def test_exact_clip_contract_and_premium_render_metadata_are_exported(self) -> None:
        render_function = next(
            node for node in self.generator_tree.body
            if isinstance(node, ast.FunctionDef) and node.name == "render_tree_state"
        )
        source = ast.get_source_segment(self.generator_source, render_function) or ""
        self.assertIn('clip_counts = {"idle": 6}', source)
        self.assertIn('clip_counts["destroy"] = 10', source)
        for field in (
            '"frameRate": FRAME_RATE',
            '"renderSupersample": supersample',
            '"renderSamples": render_samples',
            '"meshParts": len(mesh_parts)',
            '"materialCount": len(material_names)',
            '"rigBoneCount": len(model.armature.data.bones)',
        ):
            self.assertIn(field, source)
        self.assertIn("author_world_tree_actions(model.armature, damaged)", source)

    def test_world_tree_batch_remains_dispatchable_in_ci(self) -> None:
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("- world-tree", workflow)
        self.assertIn('if args.batch in {"world-tree", "all"}', self.generator_source)
        self.assertIn("for damaged in (False, True)", self.generator_source)


if __name__ == "__main__":
    unittest.main()
