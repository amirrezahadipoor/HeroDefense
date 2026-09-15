"""Phase 29.x: shortage audit reflects current catalog state."""
import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[3]
CATALOG = ROOT / "core/src/main/java/com/amirrezahadipoor/herodefense/items/EquipmentCatalog.java"
ART_SHORTAGE_JSON = ROOT / "docs/ART_SHORTAGE.json"
ART_SHORTAGE_MD = ROOT / "docs/ART_SHORTAGE.md"

# After 29.1, 4 bows remain; after 29.2, 0. This test is dynamic — it checks that the JSON matches the catalog.
def catalog_borrowed():
    text = CATALOG.read_text(encoding="utf-8")
    borrowed = []
    call_pattern = re.compile(r'(bow|mythic)\s*\((.*?)\)', re.DOTALL)
    for m in call_pattern.finditer(text):
        kind = m.group(1)
        args_str = m.group(2)
        quoted = re.findall(r'"([^"]+)"', args_str)
        if kind == "bow" and len(quoted) >= 3:
            bid, art = quoted[0], quoted[2]
            if bid != art:
                borrowed.append((bid, art, kind))
        elif kind == "mythic" and len(quoted) >= 3:
            bid, art = quoted[0], quoted[2]
            if bid != art:
                borrowed.append((bid, art, kind))
    return sorted(borrowed)

class ArtShortageTest(unittest.TestCase):
    def test_audit_matches_catalog(self):
        data = json.loads(ART_SHORTAGE_JSON.read_text(encoding="utf-8"))
        borrowed = data.get("borrowed", [])
        catalog = catalog_borrowed()
        self.assertEqual(len(catalog), len(borrowed), f"catalog {catalog} vs audit {borrowed}")
        ids = {b["id"]: b["artId"] for b in borrowed}
        cat_ids = {b[0]: b[1] for b in catalog}
        self.assertEqual(cat_ids, ids)

    def test_markdown_is_frozen_and_covers_all(self):
        md = ART_SHORTAGE_MD.read_text(encoding="utf-8")
        self.assertIn("Phase 29.0 (frozen)", md)
        data = json.loads(ART_SHORTAGE_JSON.read_text(encoding="utf-8"))
        for b in data["borrowed"]:
            self.assertIn(f"`{b['id']}`", md)

    def test_after_29_1_only_bows_remain(self):
        # After mythic fix, only 4 bows should remain
        data = json.loads(ART_SHORTAGE_JSON.read_text(encoding="utf-8"))
        ids = {b["id"] for b in data["borrowed"]}
        self.assertEqual({"yew_shortbow","thornwood_bow","verdant_recurve","golemsbane_warbow"}, ids)
        for b in data["borrowed"]:
            self.assertEqual("bow", b["kind"])

if __name__ == "__main__":
    unittest.main()
