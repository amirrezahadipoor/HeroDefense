"""Dependency-free source contracts for the Phase 18 planting-ceremony batch."""
from __future__ import annotations

import ast
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CEREMONY = ROOT / "tools/blender/hd_pipeline/ceremony.py"
GENERATOR = ROOT / "tools/blender/generate_assets.py"
WORKFLOW = ROOT / ".github/workflows/generate-visual-assets.yml"


class CeremonySourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.ceremony = CEREMONY.read_text(encoding="utf-8")
        cls.tree = ast.parse(cls.ceremony)
        cls.generator = GENERATOR.read_text(encoding="utf-8")

    def test_clip_contract_matches_runtime(self) -> None:
        self.assertIn('"walk": 8', self.ceremony)
        self.assertIn('"plant": 10', self.ceremony)
        self.assertIn('"water": 10', self.ceremony)
        self.assertIn('"grow": 12', self.ceremony)
        self.assertIn('"idle": 6', self.ceremony)
        self.assertIn("planting-ceremony-v1", self.ceremony)
        self.assertIn("heartwood-sapling-v1", self.ceremony)

    def test_props_are_authored_on_hand_bones(self) -> None:
        for part in ("ceremony_seed", "ceremony_can_body", "ceremony_can_spout", "ceremony_can_rose"):
            self.assertIn(part, self.ceremony)
        self.assertIn('"hand.L"', self.ceremony)
        self.assertIn('"hand.R"', self.ceremony)
        functions = {node.name for node in self.tree.body if isinstance(node, ast.FunctionDef)}
        self.assertTrue({
            "build_ceremony_hero", "author_ceremony_actions", "build_sapling_tree",
            "author_sapling_actions", "stack_named_actions",
        } <= functions)

    def test_exact_ceremony_batch_is_available_in_ci(self) -> None:
        self.assertIn('args.batch in {"ceremony", "all"}', self.generator)
        self.assertIn('"ceremony_hero"', self.generator)
        self.assertIn('"world_tree_sapling"', self.generator)
        self.assertIn("- ceremony", WORKFLOW.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
