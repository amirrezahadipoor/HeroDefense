"""Dependency-free guards for World Tree audit and promotion gates."""
from __future__ import annotations

import ast
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
REVIEW = ROOT / "tools/visual/create_world_tree_batch_review.py"
PROMOTION = ROOT / "tools/visual/promote_world_tree_batch.py"


class WorldTreeReviewSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.review_source = REVIEW.read_text(encoding="utf-8")
        cls.promotion_source = PROMOTION.read_text(encoding="utf-8")
        cls.review_tree = ast.parse(cls.review_source)
        cls.promotion_tree = ast.parse(cls.promotion_source)

    def test_review_is_exhaustive_and_native_size(self) -> None:
        for token in (
            '"world_tree_healthy": {"idle": 6}',
            '"world_tree_damaged": {"idle": 6, "destroy": 10}',
            'region["width"] != 256',
            'min(margins.values()) < 4',
            "alpha_difference_ratio",
            "crownTopDropPixels",
            "create_arena_scale_sheet",
            "create_destruction_timeline",
        ):
            self.assertIn(token, self.review_source)
        self.assertIn('"frameCount": total_frames', self.review_source)
        self.assertIn('"candidateManifestSha256": sha256(candidate_manifest_path)', self.review_source)

    def test_promotion_requires_exact_payload_and_hash_bound_acceptance(self) -> None:
        for token in (
            "actual_payload != expected_payload",
            "candidateManifestSha256",
            "baselineManifestSha256",
            "candidateSheetSha256",
            "candidateAtlasSha256",
            "candidateMetadataSha256",
            "reviewSheets",
            '"**Decision:** ACCEPTED"',
            "sha256_file(AUDIT_PATH)",
            "reviewDocument",
            "categoryReview",
        ):
            self.assertIn(token, self.promotion_source)
        self.assertIn("if source == destination", self.promotion_source)
        self.assertIn("resolve_destination", self.promotion_source)

    def test_review_and_promotion_lock_the_same_state_identity(self) -> None:
        required = {
            "world_tree_healthy",
            "world_tree_damaged",
            "heartwood-sanctum-healthy-v2",
            "heartwood-sanctum-wounded-v2",
            "segmented-world-tree-v2",
            "living-heart-pulse-v2",
            "wounded-collapse-v2",
            "destroy",
        }
        for tree in (self.review_tree, self.promotion_tree):
            literals = {
                node.value
                for node in ast.walk(tree)
                if isinstance(node, ast.Constant) and isinstance(node.value, str)
            }
            self.assertTrue(required <= literals)


if __name__ == "__main__":
    unittest.main()
