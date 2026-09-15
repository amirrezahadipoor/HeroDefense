#!/usr/bin/env python3
"""Fast source-level guards for the exact premium-v2 boss render contract."""
from __future__ import annotations

import ast
import sys
import unittest
from pathlib import Path

BLENDER_ROOT = Path(__file__).resolve().parents[1]
REPOSITORY_ROOT = BLENDER_ROOT.parents[1]
sys.path.insert(0, str(BLENDER_ROOT))

from hd_pipeline.config import BOSSES  # noqa: E402

EXPECTED = {
    "ancient_golem": (
        "heartstone-colossus-v2", "premium-heavy-humanoid-v2",
        "ancient-golem-ground-slam-v2", "ground_slam", "_author_ancient_golem_ground_slam",
    ),
    "thorn_matriarch": (
        "briar-sovereign-v2", "premium-rooted-caster-v2",
        "thorn-matriarch-thorn-cage-v2", "thorn_cage", "_author_thorn_matriarch_thorn_cage",
    ),
    "ember_wyrm": (
        "furnace-wyrm-v2", "premium-winged-wyrm-mapped-v2",
        "ember-wyrm-flame-sweep-v2", "flame_sweep", "_author_ember_wyrm_flame_sweep",
    ),
    "void_knight": (
        "abyss-champion-v2", "premium-armored-humanoid-v2",
        "void-knight-void-charge-v2", "void_charge", "_author_void_knight_void_charge",
    ),
}


class BossPremiumSourceTest(unittest.TestCase):
    def test_runtime_boss_render_set_is_exact(self) -> None:
        configured = {asset.key: asset for asset in BOSSES}
        self.assertEqual(set(EXPECTED), set(configured))
        for key, asset in configured.items():
            self.assertEqual("boss", asset.family)
            self.assertEqual(key, asset.builder)
            self.assertEqual("boss", asset.frame_class)

    def test_every_boss_builder_declares_locked_v2_identity(self) -> None:
        source = (BLENDER_ROOT / "hd_pipeline" / "models.py").read_text(encoding="utf-8")
        tree = ast.parse(source)
        functions = {node.name: node for node in tree.body if isinstance(node, ast.FunctionDef)}
        for key, provenance in EXPECTED.items():
            builder_name = f"build_{key}"
            self.assertIn(builder_name, functions)
            segment = ast.get_source_segment(source, functions[builder_name]) or ""
            self.assertIn('"visualQuality": "studio-v3"', segment)
            for value in provenance[:4]:
                self.assertIn(value, segment)
            self.assertIn('"silhouetteLandmarks"', segment)
            self.assertIn('"surfaceLanguage"', segment)

    def test_every_signature_has_a_dedicated_profile_and_attack_author(self) -> None:
        source = (BLENDER_ROOT / "hd_pipeline" / "rig.py").read_text(encoding="utf-8")
        tree = ast.parse(source)
        function_names = {node.name for node in tree.body if isinstance(node, ast.FunctionDef)}
        for key, (_revision, _rig, profile, _signature, attack_author) in EXPECTED.items():
            self.assertIn(profile, source)
            self.assertIn(attack_author, function_names)
            for clip in ("idle", "hit", "death"):
                self.assertIn(f"_author_{key}_{clip}", function_names)

    def test_exact_boss_batch_is_available_in_ci(self) -> None:
        generator = (BLENDER_ROOT / "generate_assets.py").read_text(encoding="utf-8")
        workflow = (REPOSITORY_ROOT / ".github" / "workflows" / "generate-visual-assets.yml").read_text(encoding="utf-8")
        self.assertIn('args.batch == "bosses"', generator)
        self.assertIn("for asset in BOSSES", generator)
        self.assertIn("          - bosses\n", workflow)


if __name__ == "__main__":
    unittest.main()
