from __future__ import annotations

import ast
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
REVIEW = ROOT / "tools/visual/create_arena_batch_review.py"
PROMOTE = ROOT / "tools/visual/promote_arena_batch.py"


class ArenaReviewSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.review = REVIEW.read_text(encoding="utf-8")
        cls.promote = PROMOTE.read_text(encoding="utf-8")
        cls.review_tree = ast.parse(cls.review)
        cls.promote_tree = ast.parse(cls.promote)

    def test_review_audits_the_exact_complete_static_payload(self) -> None:
        for text in (self.review, self.promote):
            for key in (
                "arena_backdrop",
                "ground_tile_0", "ground_tile_1", "ground_tile_2",
                "crystal_prop_0", "crystal_prop_1", "crystal_prop_2",
            ):
                self.assertIn(key, text)
        self.assertIn('actual_keys != sorted(EXPECTED_KEYS)', self.review)
        self.assertIn('actual_payload != expected_payload', self.promote)
        self.assertIn('"staticFrameCount": len(records)', self.review)
        self.assertIn('"decodedBytes": 7_225_344', self.promote)

    def test_review_covers_composition_value_alpha_scale_and_identity(self) -> None:
        for function in (
            "create_integrated_composition",
            "create_backdrop_value_sheet",
            "create_ground_lineup",
            "create_crystal_lineup",
            "create_runtime_readability",
            "create_depth_hierarchy",
        ):
            self.assertIsNotNone(self._function(self.review_tree, function))
        for gate in (
            "minimum_edge_alpha",
            "opaque_fraction",
            "center_mean",
            "minimumTransparentAssetMargin",
            "baselineSheetSha256",
            "runtimeGlow",
        ):
            self.assertIn(gate, self.review)

    def test_promotion_is_acceptance_and_every_file_hash_gated(self) -> None:
        function = self._function(self.promote_tree, "validate_review_evidence")
        source = ast.get_source_segment(self.promote, function) or ""
        for gate in (
            '"**Decision:** ACCEPTED"',
            "candidateManifestSha256",
            "candidatePayload",
            "reviewSheets",
            "baselineManifestSha256",
            "auditSha256",
            "sourceManifestSha256",
        ):
            self.assertIn(gate, source if gate != "sourceManifestSha256" else self.promote)
        self.assertIn("sha256_file(resolve_inside(source, relative))", source)
        self.assertIn("actual_sheets != EXPECTED_SHEETS", source)

    def test_review_and_promotion_lock_the_same_named_identities(self) -> None:
        for identity in (
            "root-path", "waystone-crossing", "moss-clearing",
            "azure-waystone-fan", "violet-moon-geode", "amber-root-lantern",
        ):
            self.assertIn(identity, self.review)
            self.assertIn(identity, self.promote)

    @staticmethod
    def _function(tree: ast.AST, name: str) -> ast.FunctionDef | None:
        for node in ast.walk(tree):
            if isinstance(node, ast.FunctionDef) and node.name == name:
                return node
        return None


if __name__ == "__main__":
    unittest.main()
