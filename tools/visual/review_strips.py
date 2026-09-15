#!/usr/bin/env python3
"""Shared Phase-28 appeal strips for every review sheet.

Silhouette-only views and color-grade strips (style guide sections 12-13) so
appeal/color/shape regressions show before promotion, not after. All helpers
are pure PIL and deterministic: same sprite in, same pixels out.
"""
from __future__ import annotations

from PIL import Image, ImageDraw, ImageFont, ImageOps

# (label, (r, g, b) multiplier, teal shadow-lift) — guide section 12.4.
# Dawn is the ungraded reference look, identical to base by design.
STAGE_GRADES = (
    ("BASE", (1.00, 1.00, 1.00), 0.00),
    ("DAWN 1-50", (1.00, 1.00, 1.00), 0.00),
    ("AMBER 51-100", (1.02, 0.98, 0.92), 0.00),
    ("TEAL 101-150", (0.94, 1.00, 1.02), 0.00),
    ("HOLLOW 151-200", (0.86, 0.90, 1.00), 0.06),
)

INK = "#E8F3E8"
CAPTION = "#C7D4CE"
CHECKER_DARK = "#273635"
CHECKER_LIGHT = "#334744"
FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
CAPTION_HEIGHT = 22
CELL_PAD = 10


def label_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    try:
        return ImageFont.truetype(FONT_PATH, size)
    except OSError:
        return ImageFont.load_default()


def apply_grade(sprite: Image.Image, grade: tuple[str, tuple[float, float, float], float]) -> Image.Image:
    """Grade recipe from STAGE_GRADES; alpha channel is preserved exactly."""
    rgba = sprite.convert("RGBA")
    _, (rm, gm, bm), lift = grade
    red, green, blue, alpha = rgba.split()
    red = red.point(lambda v: min(255, round(v * rm)))
    green = green.point(lambda v: min(255, round(v * gm)))
    blue = blue.point(lambda v: min(255, round(v * bm)))
    graded = Image.merge("RGBA", (red, green, blue, alpha))
    if lift > 0:
        luma = ImageOps.grayscale(rgba.convert("RGB"))
        shadow = ImageOps.invert(luma).point(lambda v: round(v * lift))
        teal = Image.new("RGBA", rgba.size, (0, 255, 236, 255))
        lifted = Image.composite(teal, graded.convert("RGB"), shadow).convert("RGBA")
        lifted.putalpha(alpha)
        return lifted
    return graded


def silhouette_view(sprite: Image.Image) -> Image.Image:
    """Solid light silhouette keeping the source alpha (matches sheet style)."""
    rgba = sprite.convert("RGBA")
    solid = Image.new("RGBA", rgba.size, INK)
    solid.putalpha(rgba.getchannel("A"))
    return solid


def grayscale_view(sprite: Image.Image) -> Image.Image:
    rgba = sprite.convert("RGBA")
    value = ImageOps.grayscale(rgba.convert("RGB"))
    return Image.merge("RGBA", (value, value, value, rgba.getchannel("A")))


def halfscale_view(sprite: Image.Image) -> Image.Image:
    """50% nearest-neighbor: runtime pixels, no smoothing."""
    rgba = sprite.convert("RGBA")
    size = (max(1, rgba.width // 2), max(1, rgba.height // 2))
    return rgba.resize(size, Image.Resampling.NEAREST)


def checker(width: int, height: int) -> Image.Image:
    image = Image.new("RGBA", (width, height), CHECKER_DARK)
    draw = ImageDraw.Draw(image)
    step = 12
    for y in range(0, height, step):
        for x in range(0, width, step):
            if (x // step + y // step) % 2:
                draw.rectangle((x, y, x + step - 1, y + step - 1), fill=CHECKER_LIGHT)
    return image


def _cell(sprite: Image.Image, cell: int, caption: str) -> Image.Image:
    card = checker(cell, cell + CAPTION_HEIGHT)
    scale = min((cell - 2 * CELL_PAD) / sprite.width, (cell - 2 * CELL_PAD) / sprite.height)
    size = (max(1, round(sprite.width * scale)), max(1, round(sprite.height * scale)))
    preview = sprite.resize(size, Image.Resampling.NEAREST)
    card.alpha_composite(preview, ((cell - size[0]) // 2, (cell - size[1]) // 2))
    draw = ImageDraw.Draw(card)
    draw.text((cell // 2, cell + CAPTION_HEIGHT // 2), caption,
              fill=CAPTION, font=label_font(13), anchor="mm")
    return card


def appeal_row(sprite: Image.Image, cell: int = 144) -> Image.Image:
    """FULL | 50% | GRAY | SILHOUETTE strip for one sprite."""
    views = (
        ("FULL", sprite.convert("RGBA")),
        ("50%", halfscale_view(sprite)),
        ("GRAY", grayscale_view(sprite)),
        ("SHAPE", silhouette_view(sprite)),
    )
    strip = Image.new("RGBA", (cell * len(views), cell + CAPTION_HEIGHT), CHECKER_DARK)
    for index, (caption, view) in enumerate(views):
        strip.alpha_composite(_cell(view, cell, caption), (index * cell, 0))
    return strip


def grade_row(sprite: Image.Image, cell: int = 144) -> Image.Image:
    """BASE + four stage-grade strip (guide section 12.4)."""
    strip = Image.new("RGBA", (cell * len(STAGE_GRADES), cell + CAPTION_HEIGHT), CHECKER_DARK)
    for index, grade in enumerate(STAGE_GRADES):
        strip.alpha_composite(_cell(apply_grade(sprite, grade), cell, grade[0]), (index * cell, 0))
    return strip
