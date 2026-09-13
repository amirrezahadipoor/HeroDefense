#!/usr/bin/env python3
"""Source-level guards keeping boss audit and promotion contracts in lockstep."""
from __future__ import annotations

import ast
import sys
import unittest
from pathlib import Path

BLENDER_ROOT = Path(__file__).resolve().parents[1]
REPOSITORY_ROOT = BLENDER_ROOT.parents[1]
VISUAL_ROOT = REPOSITORY_ROOT / "tools" / "visual"
sys.path.insert(0, str(VISUAL_ROOT))

import create_boss_batch_review as review  # noqa: E402
import promote_boss_batch as promotion  # noqa: E402


class BossReviewSourceTest(unittest.TestCase):
    def test_review_and_promotion_identity_tables_match(self) -> None:
        review_table = {
            key: (revision, rig, animation, review.SIGNATURE_ATTACKS[key])
            for key, _label, revision, rig, animation in review.BOSSES
        }
        self.assertEqual(review_table, promotion.EXPECTED)
        self.assertEqual(
            {"ancient_golem", "thorn_matriarch", "ember_wyrm", "void_knight"},
            set(review_table),
        )

    def test_review_contract_is_exhaustive_and_native_size(self) -> None:
        self.assertEqual({"idle": 6, "attack": 8, "hit": 4, "death": 10}, review.EXPECTED_CLIPS)
        self.assertEqual({"idle": 5, "attack": 7, "hit": 3, "death": 9}, review.MIN_UNIQUE)
        self.assertEqual(9, len(promotion.EXPECTED_SHEETS))
        source = (VISUAL_ROOT / "create_boss_batch_review.py").read_text(encoding="utf-8")
        tree = ast.parse(source)
        self.assertTrue(any(
            isinstance(node, ast.FunctionDef) and node.name == "audit_batch"
            for node in tree.body
        ))
        self.assertIn('region["width"] != 256', source)
        self.assertIn('"batch": "bosses-premium-v2"', source)
        self.assertIn('decoded_limit = 48 * 1024 * 1024', source)

    def test_promotion_is_review_and_hash_gated(self) -> None:
        source = (VISUAL_ROOT / "promote_boss_batch.py").read_text(encoding="utf-8")
        for guard in (
            "validate_candidate_payload",
            "validate_review_evidence",
            "candidateManifestSha256",
            "candidateSheetSha256",
            "candidateAtlasSha256",
            "candidateMetadataSha256",
            "baselineManifestSha256",
            "pilotReviewDocument",
        ):
            self.assertIn(guard, source)
        self.assertEqual("docs/art_reviews/BOSSES_PREMIUM_V2_REVIEW.md", promotion.REVIEW_DOCUMENT)
        self.assertEqual("bosses_audit.json", promotion.AUDIT_PATH.name)


if __name__ == "__main__":
    unittest.main()
