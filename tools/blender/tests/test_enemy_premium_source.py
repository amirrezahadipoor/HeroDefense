#!/usr/bin/env python3
"""Fast source-level guards for the regular-enemy premium render contract."""
from __future__ import annotations

import ast
import sys
import unittest
from pathlib import Path

BLENDER_ROOT = Path(__file__).resolve().parents[1]
REPOSITORY_ROOT = BLENDER_ROOT.parents[1]
sys.path.insert(0, str(BLENDER_ROOT))

from hd_pipeline.config import REGULAR_CHARACTERS  # noqa: E402

EXPECTED = {
    "rootling": ("rootling-thorn-scout-v2", "premium-humanoid-v2", "rootling-skirmisher-v2"),
    "stonekin": ("stonekin-rune-bulwark-v2", "premium-heavy-humanoid-v2", "stonekin-juggernaut-v2"),
    "gloom_wolf": ("gloom-wolf-shadow-stalker-v2", "premium-quadruped-mapped-v2", "gloom-wolf-pouncer-v2"),
    "fungal_brute": ("fungal-brute-spore-bruiser-v2", "premium-heavy-humanoid-v2", "fungal-brute-brawler-v2"),
}


class EnemyPremiumSourceTest(unittest.TestCase):
    def test_runtime_enemy_render_set_is_exact(self) -> None:
        configured = {asset.key: asset for asset in REGULAR_CHARACTERS if asset.family == "enemy"}
        self.assertEqual(set(EXPECTED), set(configured))
        for key, asset in configured.items():
            self.assertEqual(key, asset.builder)
            self.assertEqual("character", asset.frame_class)

    def test_every_enemy_builder_declares_locked_v2_provenance(self) -> None:
        source = (BLENDER_ROOT / "hd_pipeline" / "models.py").read_text(encoding="utf-8")
        tree = ast.parse(source)
        functions = {node.name: node for node in tree.body if isinstance(node, ast.FunctionDef)}
        for key, provenance in EXPECTED.items():
            name = f"build_{key}"
            self.assertIn(name, functions)
            segment = ast.get_source_segment(source, functions[name]) or ""
            self.assertIn('"visualQuality": "premium-v2"', segment)
            for value in provenance:
                self.assertIn(value, segment)

    def test_every_enemy_profile_authors_all_four_clips(self) -> None:
        source = (BLENDER_ROOT / "hd_pipeline" / "rig.py").read_text(encoding="utf-8")
        tree = ast.parse(source)
        function_names = {node.name for node in tree.body if isinstance(node, ast.FunctionDef)}
        for key, (_revision, _rig, profile) in EXPECTED.items():
            self.assertIn(profile, source)
            prefix = "_author_" + key
            for clip in ("idle", "attack", "hit", "death"):
                self.assertIn(f"{prefix}_{clip}", function_names)

    def test_exact_enemy_batch_is_available_in_ci(self) -> None:
        generator = (BLENDER_ROOT / "generate_assets.py").read_text(encoding="utf-8")
        workflow = (REPOSITORY_ROOT / ".github" / "workflows" / "generate-visual-assets.yml").read_text(encoding="utf-8")
        self.assertIn('args.batch == "enemies"', generator)
        self.assertIn("for asset in REGULAR_CHARACTERS[1:]", generator)
        self.assertIn("          - enemies\n", workflow)


if __name__ == "__main__":
    unittest.main()
