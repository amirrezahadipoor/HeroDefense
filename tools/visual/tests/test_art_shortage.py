"""Phase 29.0: shortage audit is frozen and covers known 6 mythics + 4 bows."""
import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[3]
CATALOG = ROOT / "core/src/main/java/com/amirrezahadipoor/herodefense/items/EquipmentCatalog.java"
ART_SHORTAGE_MD = ROOT / "docs/ART_SHORTAGE.md"
ART_SHORTAGE_JSON = ROOT / "docs/ART_SHORTAGE.json"

EXPECTED_BORROWED = {
    "yew_shortbow": "ashwood_bow",
    "thornwood_bow": "ashwood_bow",
    "verdant_recurve": "moonwood_longbow",
    "golemsbane_warbow": "starfall_bow",
    "sunfall_last_arrow": "worldbranch",
    "crown_hollow_eye": "crown_of_first_leaves",
    "bark_first_root": "heartwood_aegis",
    "windrunner_last_steps": "boots_of_three_winds",
    "verdant_oath": "echo_band",
    "emberless_core": "eternal_seed",
}

class ArtShortageTest(unittest.TestCase):
    def test_catalog_contains_expected_borrows(self):
        text = CATALOG.read_text(encoding="utf-8")
        for borrowed, art in EXPECTED_BORROWED.items():
            self.assertIn(f'"{borrowed}"', text, f"missing borrowed {borrowed}")
            # ensure the art id appears as borrow target near the borrowed id
            self.assertIn(f'"{art}"', text)

    def test_audit_lists_exactly_ten(self):
        data = json.loads(ART_SHORTAGE_JSON.read_text(encoding="utf-8"))
        borrowed = data.get("borrowed", [])
        self.assertEqual(10, len(borrowed), f"expected 10, got {len(borrowed)}")
        ids = {b["id"]: b["artId"] for b in borrowed}
        self.assertEqual(EXPECTED_BORROWED, ids)

    def test_markdown_is_frozen_and_covers_all(self):
        md = ART_SHORTAGE_MD.read_text(encoding="utf-8")
        self.assertIn("Phase 29.0 (frozen)", md)
        for borrowed in EXPECTED_BORROWED:
            self.assertIn(f"`{borrowed}`", md)

    def test_no_other_borrow_outside_expected(self):
        # The audit script is the single source of truth — if catalog gains a new borrow, test fails
        data = json.loads(ART_SHORTAGE_JSON.read_text(encoding="utf-8"))
        ids = {b["id"] for b in data["borrowed"]}
        self.assertEqual(set(EXPECTED_BORROWED.keys()), ids)

if __name__ == "__main__":
    unittest.main()
