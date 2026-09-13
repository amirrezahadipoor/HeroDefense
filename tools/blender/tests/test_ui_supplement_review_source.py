from __future__ import annotations

import ast
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
REVIEW_PATH = ROOT / "tools/visual/create_ui_supplement_review.py"
PROMOTE_PATH = ROOT / "tools/visual/promote_ui_supplement.py"


class UiSupplementReviewSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.review = REVIEW_PATH.read_text(encoding="utf-8")
        cls.promote = PROMOTE_PATH.read_text(encoding="utf-8")
        cls.review_tree = ast.parse(cls.review)
        cls.promote_tree = ast.parse(cls.promote)

    def test_exact_batch_locks_six_potions_and_two_missing_reward_semantics(self) -> None:
        for source in (self.review, self.promote):
            self.assertIn('tuple(f"health_potion_{tier}" for tier in range(1, 7))', source)
            self.assertIn('("ui_general_power", "ui_lifesteal")', source)
            self.assertIn("EXPECTED_KEYS = (*POTION_KEYS, *NEW_REWARD_KEYS)", source)
            self.assertIn("health-potion-premium-v2", source)
            self.assertIn("heartwood-control-medallion", source)
        self.assertIn("sorted(actual) != sorted(EXPECTED_KEYS)", self.review)
        self.assertIn("actual != payload", self.promote)

    def test_review_covers_before_after_runtime_value_and_integrated_use(self) -> None:
        for function in (
            "create_potion_lineup", "create_reward_lineup", "create_readability",
            "create_value_progression", "create_integrated_surface",
        ):
            self.assertIsNotNone(self._function(self.review_tree, function))
        for contract in (
            "baselineSheetSha256", "tierConstruction", "minimumAlphaMargin",
            "rewardCardIconMap", "candidatePayload", "decodedBytes",
        ):
            self.assertIn(contract, self.review)

    def test_promotion_requires_all_hash_bound_evidence_and_is_idempotent(self) -> None:
        function = self._function(self.promote_tree, "validate_evidence")
        self.assertIsNotNone(function)
        source = ast.get_source_segment(self.promote, function) or ""
        for gate in (
            '"**Decision:** ACCEPTED"', "candidateManifestSha256", "candidatePayload",
            "reviewSheets", "baselineManifestSha256", "auditSha256",
        ):
            self.assertIn(gate, source)
        self.assertIn("sha256(resolve_inside(source, relative))", source)
        self.assertIn("actual_sheets != EXPECTED_SHEETS", source)
        self.assertIn("Destination is not idempotently accepted", source)

    @staticmethod
    def _function(tree: ast.AST, name: str) -> ast.FunctionDef | None:
        for node in ast.walk(tree):
            if isinstance(node, ast.FunctionDef) and node.name == name:
                return node
        return None


if __name__ == "__main__":
    unittest.main()
