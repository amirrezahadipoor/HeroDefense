import json, pathlib, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
MANIFEST=ROOT/"android/assets/generated/asset_manifest.json"
CATALOG=ROOT/"core/src/main/java/com/amirrezahadipoor/herodefense/items/EquipmentCatalog.java"

MYTHICS=["sunfall_last_arrow","crown_hollow_eye","bark_first_root","windrunner_last_steps","verdant_oath","emberless_core"]
OLD={"sunfall_last_arrow":"worldbranch","crown_hollow_eye":"crown_of_first_leaves","bark_first_root":"heartwood_aegis","windrunner_last_steps":"boots_of_three_winds","verdant_oath":"echo_band","emberless_core":"eternal_seed"}

class MythicOwnArtPythonTest(unittest.TestCase):
    def test_catalog_unwired(self):
        text=CATALOG.read_text(encoding="utf-8")
        for m in MYTHICS:
            # mythic def should contain artId == id, not old borrow
            self.assertIn(f'mythic("{m}"', text)
            self.assertNotIn(f'mythic("{m}", "{OLD[m]}"', text)
            # Ensure new artId is present as own id
            self.assertIn(f'"{m}")', text)

    def test_manifest_has_mythic_assets(self):
        manifest=json.loads(MANIFEST.read_text(encoding="utf-8"))
        keys={a["key"] for a in manifest["assets"]}
        for m in MYTHICS:
            self.assertIn(f"equipment_{m}", keys)
            asset=[a for a in manifest["assets"] if a["key"]==f"equipment_{m}"][0]
            self.assertEqual("MYTHIC", asset["tier"])
            self.assertEqual("studio-v3", asset["visualQuality"])
            self.assertTrue(asset["engineVersion"].startswith("33.0"))
            self.assertIn("icon", asset)

    def test_mythic_sheets_not_copied_from_borrow(self):
        for m in MYTHICS:
            myth_sheet=ROOT/f"android/assets/generated/equipment/{m}.png"
            old_sheet=ROOT/f"android/assets/generated/equipment/{OLD[m]}.png"
            self.assertTrue(myth_sheet.exists())
            self.assertTrue(old_sheet.exists())
            # They must not be byte-identical (distinct art)
            self.assertNotEqual(myth_sheet.read_bytes(), old_sheet.read_bytes(), f"{m} sheet is copy of {OLD[m]}")

if __name__=="__main__":
    unittest.main()
