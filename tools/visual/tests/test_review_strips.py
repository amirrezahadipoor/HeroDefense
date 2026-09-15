from __future__ import annotations

import hashlib
import sys
import unittest
from pathlib import Path

VISUAL_TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(VISUAL_TOOLS))

from PIL import Image

from review_strips import (
    CAPTION_HEIGHT,
    STAGE_GRADES,
    appeal_row,
    apply_grade,
    grade_row,
    grayscale_view,
    halfscale_view,
    silhouette_view,
)


def sprite() -> Image.Image:
    image = Image.new("RGBA", (8, 8), (100, 150, 200, 255))
    image.putpixel((0, 0), (10, 10, 10, 255))
    image.putpixel((7, 7), (0, 0, 0, 0))
    return image


def sha256(image: Image.Image) -> str:
    return hashlib.sha256(image.tobytes()).hexdigest()


class ReviewStripsTest(unittest.TestCase):
    def test_base_and_dawn_grades_are_identity(self) -> None:
        for grade in STAGE_GRADES[:2]:
            self.assertEqual(list(sprite().getdata()), list(apply_grade(sprite(), grade).getdata()))

    def test_amber_grade_warms_and_cools_per_recipe(self) -> None:
        graded = apply_grade(sprite(), STAGE_GRADES[2])
        self.assertEqual((102, 147, 184, 255), graded.getpixel((4, 4)))

    def test_hollow_grade_darkens_then_lifts_teal_shadows(self) -> None:
        graded = apply_grade(sprite(), STAGE_GRADES[4])
        dark = graded.getpixel((0, 0))
        self.assertTrue(7 <= dark[0] <= 9)
        self.assertGreater(dark[1], round(10 * 0.90))
        self.assertGreater(dark[2], round(10 * 1.00))

    def test_grades_preserve_alpha_exactly(self) -> None:
        for grade in STAGE_GRADES:
            self.assertEqual(
                list(sprite().getchannel("A").getdata()),
                list(apply_grade(sprite(), grade).getchannel("A").getdata()),
            )

    def test_silhouette_keeps_alpha_and_inks_opaque(self) -> None:
        shape = silhouette_view(sprite())
        self.assertEqual((232, 243, 232, 255), shape.getpixel((4, 4)))
        self.assertEqual(0, shape.getpixel((7, 7))[3])

    def test_grayscale_matches_luma_and_keeps_alpha(self) -> None:
        gray = grayscale_view(sprite())
        pixel = gray.getpixel((4, 4))
        self.assertEqual(pixel[0], pixel[1])
        self.assertEqual(pixel[1], pixel[2])
        self.assertEqual(255, pixel[3])
        self.assertEqual(0, gray.getpixel((7, 7))[3])

    def test_halfscale_uses_runtime_pixels(self) -> None:
        zoom = sprite()
        zoom.putpixel((1, 1), (10, 10, 10, 255))
        half = halfscale_view(zoom)
        self.assertEqual((4, 4), half.size)
        self.assertEqual((10, 10, 10, 255), half.getpixel((0, 0)))

    def test_rows_have_stable_layout_and_bytes(self) -> None:
        appeal = appeal_row(sprite(), cell=64)
        self.assertEqual((64 * 4, 64 + CAPTION_HEIGHT), appeal.size)
        grade = grade_row(sprite(), cell=64)
        self.assertEqual((64 * 5, 64 + CAPTION_HEIGHT), grade.size)
        self.assertEqual(sha256(appeal), sha256(appeal_row(sprite(), cell=64)))
        self.assertEqual(sha256(grade), sha256(grade_row(sprite(), cell=64)))


if __name__ == "__main__":
    unittest.main()
