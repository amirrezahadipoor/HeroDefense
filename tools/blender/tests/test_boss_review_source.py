#!/usr/bin/env python3
"""Source-level guards keeping boss audit and promotion contracts in lockstep."""
from __future__ import annotations

import ast
import unittest
from pathlib import Path

BLENDER_ROOT = Path(__file__).resolve().parents[1]
REPOSITORY_ROOT = BLENDER_ROOT.parents[1]
VISUAL_ROOT = REPOSITORY_ROOT / "tools" / "visual"


def literal_assignment(path: Path, name: str):
    tree = ast.parse(path.read_text(encoding="utf-8"))
    for node in tree.body:
        if isinstance(node, ast.Assign):
            if any(isinstance(target, ast.Name) and target.id == name for target in node.targets):
                return ast.literal_eval(node.value)
    raise AssertionError(f"Missing literal assignment {name} in {path}")


class BossReviewSourceTest(unittest.TestCase):
    def test_review_and_promotion_identity_tables_match(self) -> None:
        review_path = VISUAL_ROOT / "create_boss_batch_review.py"
        promotion_path = VISUAL_ROOT / "promote_boss_batch.py"
        bosses = literal_assignment(review_path, "BOSSES")
        signatures = literal_assignment(review_path, "SIGNATURE_ATTACKS")
        expected = literal_assignment(promotion_path, "EXPECTED")
        review_table = {
            key: (revision, rig, animation, signatures[key])
            for key, _label, revision, rig, animation in bosses
        }
        self.assertEqual(review_table, expected)
        self.assertEqual(
            {"ancient_golem", "thorn_matriarch", "ember_wyrm", "void_knight"},
            set(review_table),
        )

    def test_review_contract_is_exhaustive_and_native_size(self) -> None:
        path = VISUAL_ROOT / "create_boss_batch_review.py"
        source = path.read_text(encoding="utf-8")
        self.assertEqual(
            {"idle": 6, "attack": 8, "hit": 4, "death": 10},
            literal_assignment(path, "EXPECTED_CLIPS"),
        )
        self.assertEqual(
            {"idle": 5, "attack": 7, "hit": 3, "death": 9},
            literal_assignment(path, "MIN_UNIQUE"),
        )
        tree = ast.parse(source)
        self.assertTrue(any(
            isinstance(node, ast.FunctionDef) and node.name == "audit_batch"
            for node in tree.body
        ))
        self.assertIn('region["width"] != 256', source)
        self.assertIn('"batch": "bosses-premium-v2"', source)
        self.assertIn('decoded_limit = 48 * 1024 * 1024', source)

    def test_promotion_is_review_and_hash_gated(self) -> None:
        path = VISUAL_ROOT / "promote_boss_batch.py"
        source = path.read_text(encoding="utf-8")
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
        self.assertIn(
            'REVIEW_DOCUMENT = "docs/art_reviews/BOSSES_PREMIUM_V2_REVIEW.md"',
            source,
        )
        self.assertIn('AUDIT_PATH = REVIEW_DIRECTORY / "bosses_audit.json"', source)
        expected_keys = set(literal_assignment(path, "EXPECTED"))
        self.assertEqual(9, 1 + 2 * len(expected_keys))


if __name__ == "__main__":
    unittest.main()
