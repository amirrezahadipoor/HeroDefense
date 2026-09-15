from __future__ import annotations

import sys
import unittest
from pathlib import Path

BLENDER_TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BLENDER_TOOLS))

from hd_pipeline.config import render_tier


class RenderTierTest(unittest.TestCase):
    def test_hero_bosses_and_trees_render_highest(self) -> None:
        self.assertEqual((3, 36), render_tier("hero", "character"))
        self.assertEqual((3, 36), render_tier("hero_ceremony", "character"))
        self.assertEqual((3, 36), render_tier("ancient_golem", "boss"))
        self.assertEqual((3, 36), render_tier("world_tree_healthy", "tree"))

    def test_everything_else_shares_the_mid_tier(self) -> None:
        self.assertEqual((2, 28), render_tier("rootling", "character"))
        self.assertEqual((2, 28), render_tier("acorn_band", "item"))
        self.assertEqual((2, 28), render_tier("ground_tile_0", "environment"))
        self.assertEqual((2, 28), render_tier("arena_backdrop", "arena"))

    def test_unknown_classes_fall_back_to_the_mid_tier(self) -> None:
        self.assertEqual((2, 28), render_tier("mystery_prop", "prop"))


if __name__ == "__main__":
    unittest.main()
