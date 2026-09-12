from __future__ import annotations

import sys
import unittest
from pathlib import Path

BLENDER_TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BLENDER_TOOLS))

from hd_pipeline.atlas_layout import MAX_ATLAS_SIZE, plan_grid


class AtlasLayoutTest(unittest.TestCase):
    CLIPS = {"idle": 6, "attack": 8, "hit": 4, "death": 10}

    def test_character_contract_keeps_stable_clip_rows_when_bounded(self) -> None:
        pages, regions = plan_grid(self.CLIPS, 192)

        self.assertEqual([{"index": 0, "width": 1920, "height": 768}], pages)
        self.assertEqual(0, regions["idle"][0]["y"])
        self.assertEqual(576, regions["death"][9]["y"])

    def test_boss_frames_repack_below_the_hard_page_limit(self) -> None:
        pages, regions = plan_grid(self.CLIPS, 256)

        self.assertEqual([{"index": 0, "width": 2048, "height": 1024}], pages)
        self.assertEqual(28, sum(map(len, regions.values())))
        self.assertTrue(all(region["x"] + 256 <= MAX_ATLAS_SIZE
                            for frames in regions.values() for region in frames))

    def test_supersampled_batch_spills_deterministically_across_pages(self) -> None:
        pages, regions = plan_grid(self.CLIPS, 512)

        self.assertEqual(2, len(pages))
        self.assertTrue(all(page["width"] <= MAX_ATLAS_SIZE for page in pages))
        self.assertTrue(all(page["height"] <= MAX_ATLAS_SIZE for page in pages))
        self.assertEqual({0, 1}, {region["page"] for frames in regions.values() for region in frames})
        self.assertEqual(list(range(10)), [frame["index"] for frame in regions["death"]])

    def test_rejects_a_frame_larger_than_a_page(self) -> None:
        with self.assertRaises(ValueError):
            plan_grid({"idle": 1}, 4096)


if __name__ == "__main__":
    unittest.main()
