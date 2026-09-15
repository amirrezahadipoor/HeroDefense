import json, pathlib, unittest, re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
CATALOG=ROOT/"core/src/main/java/com/amirrezahadipoor/herodefense/items/EquipmentCatalog.java"
MANIFEST=ROOT/"android/assets/generated/asset_manifest.json"
BOWS=["yew_shortbow","thornwood_bow","verdant_recurve","golemsbane_warbow"]
OLD={"yew_shortbow":"ashwood_bow","thornwood_bow":"ashwood_bow","verdant_recurve":"moonwood_longbow","golemsbane_warbow":"starfall_bow"}
class BowOwnArtTest(unittest.TestCase):
    def test_bows_unwired(self):
        text=CATALOG.read_text(encoding="utf-8")
        for b in BOWS:
            self.assertIn(f'bow("{b}"', text)
            # Ensure old borrow not present
            # Check that artId==id: look for pattern bow("id",...,"id")
            # Simple: count that old borrow art not in bow's art position
            # We already checked catalog has "b", not old borrow in that bow's line
            # Use regex
            import re
            pat=re.compile(r'bow\("'+b+r'"[^)]+\)')
            m=pat.search(text)
            self.assertIsNotNone(m)
            quoted=re.findall(r'"([^"]+)"', m.group(0))
            # quoted[2] is artId
            self.assertEqual(b, quoted[2], f"{b} still borrows {quoted[2]}")
    def test_manifest(self):
        manifest=json.loads(MANIFEST.read_text(encoding="utf-8"))
        keys={a["key"] for a in manifest["assets"]}
        for b in BOWS:
            self.assertIn(f"equipment_{b}", keys)
    def test_sheets_distinct(self):
        for b in BOWS:
            p=ROOT/f"android/assets/generated/equipment/{b}.png"
            old=ROOT/f"android/assets/generated/equipment/{OLD[b]}.png"
            self.assertTrue(p.exists())
            self.assertTrue(old.exists())
            self.assertNotEqual(p.read_bytes(), old.read_bytes(), f"{b} sheet copy of {OLD[b]}")
