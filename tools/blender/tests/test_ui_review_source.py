from __future__ import annotations

import ast
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
REVIEW_PATH = ROOT / "tools/visual/create_ui_batch_review.py"
PROMOTE_PATH = ROOT / "tools/visual/promote_ui_batch.py"


class UiReviewSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.review = REVIEW_PATH.read_text(encoding="utf-8")
        cls.promote = PROMOTE_PATH.read_text(encoding="utf-8")
        cls.review_tree = ast.parse(cls.review)
        cls.promote_tree = ast.parse(cls.promote)

    def test_review_and_promotion_lock_sixteen_icons_and_twelve_states(self) -> None:
        for source in (self.review, self.promote):
            self.assertIn('KINDS = ("button", "panel", "slot")', source)
            self.assertIn('STATES = ("normal", "pressed", "selected", "disabled")', source)
            self.assertIn("EXPECTED_KEYS = (*ICON_KEYS, *FRAME_KEYS)", source)
            self.assertIn("ui_inventory", source)
            self.assertIn("ui_dodge", source)
        self.assertIn("actual_keys != sorted(EXPECTED_KEYS)", self.review)
        self.assertIn("actual != payload", self.promote)

    def test_review_exercises_native_icons_state_construction_and_stretching(self) -> None:
        for function in (
            "create_icon_lineup", "create_icon_readability", "create_skin_matrix",
            "create_nine_patch_stretch", "create_state_construction",
            "create_integrated_surfaces", "nine_patch",
        ):
            self.assertIsNotNone(self._function(self.review_tree, function))
        for contract in (
            "baselineSheetSha256", "stateConstruction", "minimumAlphaMargin",
            "ninePatchInsets", "decodedBytes",
        ):
            self.assertIn(contract, self.review)

    def test_promotion_requires_hash_bound_acceptance_for_every_file_and_sheet(self) -> None:
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

    @staticmethod
    def _function(tree: ast.AST, name: str) -> ast.FunctionDef | None:
        for node in ast.walk(tree):
            if isinstance(node, ast.FunctionDef) and node.name == name:
                return node
        return None


if __name__ == "__main__":
    unittest.main()
