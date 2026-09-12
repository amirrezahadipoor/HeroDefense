#!/usr/bin/env python3
"""Create deterministic full-motion and readability sheets for one runtime character."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps

CLIP_ORDER = ("idle", "attack", "hit", "death")
FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
BACKGROUND = "#101A1B"
PANEL = "#172829"
TEXT = "#E7E1CF"
GOLD = "#F2D58A"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("key")
    parser.add_argument("label")
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    baseline = CharacterFrames(args.baseline, args.key)
    candidate = CharacterFrames(args.candidate, args.key)
    create_motion_sheet(candidate, args.label, args.output / f"{args.key}_full_motion.png")
    create_readability_sheet(
        baseline,
        candidate,
        args.label,
        args.output / f"{args.key}_readability.png",
    )
    print(f"Wrote full character review sheets to {args.output}")


class CharacterFrames:
    def __init__(self, root: Path, key: str) -> None:
        self.root = root.resolve()
        metadata_path = self.root / "sprites" / f"{key}.json"
        self.metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        self.sheets = [
            Image.open(self.root / sheet["file"]).convert("RGBA")
            for sheet in self.metadata.get("sheets", [{"file": self.metadata["sheet"]}])
        ]

    def frame(self, clip: str, index: int) -> Image.Image:
        frames = sorted(self.metadata["clips"][clip], key=lambda value: value["index"])
        frame = frames[index]
        sheet = self.sheets[frame.get("page", 0)]
        return sheet.crop((
            frame["x"],
            frame["y"],
            frame["x"] + frame["width"],
            frame["y"] + frame["height"],
        ))


def create_motion_sheet(character: CharacterFrames, label: str, output: Path) -> None:
    frame_size = int(character.metadata["frameSize"])
    gap = 10
    left = 160
    card_step = frame_size + gap
    max_frames = max(len(character.metadata["clips"][clip]) for clip in CLIP_ORDER)
    width = left + max_frames * card_step + 30
    row_step = frame_size + 42
    height = 105 + len(CLIP_ORDER) * row_step + 25
    canvas = canvas_base(width, height, f"{label.upper()} — COMPLETE NATIVE-SIZE MOTION REVIEW")
    draw = ImageDraw.Draw(canvas)
    text(draw, (width // 2, 74),
         "Every runtime frame shown at exactly 1×; checkerboards expose true alpha margins.",
         18, anchor="ma", color="#AFC5BE")

    for row, clip in enumerate(CLIP_ORDER):
        y = 100 + row * row_step
        frames = sorted(character.metadata["clips"][clip], key=lambda value: value["index"])
        text(draw, (22, y + frame_size // 2), clip.title(), 25, bold=True, anchor="lm")
        text(draw, (22, y + frame_size // 2 + 31), f"{len(frames)} frames", 16,
             anchor="lm", color="#AFC5BE")
        for index in range(len(frames)):
            x = left + index * card_step
            card = checker(frame_size, frame_size)
            card.alpha_composite(character.frame(clip, index))
            canvas.paste(card.convert("RGB"), (x, y))
            draw.rounded_rectangle((x, y, x + frame_size, y + frame_size), 8,
                                   outline="#58706A", width=2)
            text(draw, (x + frame_size // 2, y + frame_size + 17), f"F{index:02d}", 15,
                 anchor="ma", color="#C7D4CE")
    canvas.save(output, optimize=True)


def create_readability_sheet(
    baseline: CharacterFrames,
    candidate: CharacterFrames,
    label: str,
    output: Path,
) -> None:
    width, height = 1660, 860
    canvas = canvas_base(width, height, f"{label.upper()} — SILHOUETTE, MATERIAL & CONTRAST REVIEW")
    draw = ImageDraw.Draw(canvas)
    panels = (
        ("Baseline idle", baseline.frame("idle", 0), "checker"),
        ("Premium idle", candidate.frame("idle", 0), "checker"),
        ("Grayscale hierarchy", candidate.frame("idle", 0), "grayscale"),
        ("Silhouette only", candidate.frame("idle", 0), "silhouette"),
        ("Dark arena contrast", candidate.frame("idle", 0), "dark"),
        ("Light arena contrast", candidate.frame("idle", 0), "light"),
        ("Attack impact", candidate.frame("attack", 4), "checker"),
        ("Hit recoil", candidate.frame("hit", 1), "checker"),
    )
    card_width, card_height = 360, 305
    for index, (caption, sprite, mode) in enumerate(panels):
        column = index % 4
        row = index // 4
        x = 65 + column * 400
        y = 110 + row * 355
        card = presentation_card(sprite, mode, card_width, card_height)
        canvas.paste(card.convert("RGB"), (x, y))
        draw.rounded_rectangle((x, y, x + card_width, y + card_height), 12,
                               outline="#58706A", width=2)
        text(draw, (x + card_width // 2, y - 14), caption, 20, bold=True, anchor="ma")
    text(draw, (width // 2, 817),
         "Nearest-neighbor zoom preserves runtime pixels; no smoothing or painted cleanup is applied.",
         17, anchor="ma", color="#AFC5BE")
    canvas.save(output, optimize=True)


def presentation_card(sprite: Image.Image, mode: str, width: int, height: int) -> Image.Image:
    if mode == "grayscale":
        alpha = sprite.getchannel("A")
        value = ImageOps.grayscale(sprite.convert("RGB"))
        sprite = Image.merge("RGBA", (value, value, value, alpha))
    elif mode == "silhouette":
        alpha = sprite.getchannel("A")
        solid = Image.new("RGBA", sprite.size, "#E8F3E8")
        solid.putalpha(alpha)
        sprite = solid

    if mode == "dark":
        card = Image.new("RGBA", (width, height), "#203C35")
    elif mode == "light":
        card = Image.new("RGBA", (width, height), "#D8D0AF")
    else:
        card = checker(width, height)
    scale = min((width - 24) / sprite.width, (height - 24) / sprite.height)
    size = (round(sprite.width * scale), round(sprite.height * scale))
    zoomed = sprite.resize(size, Image.Resampling.NEAREST)
    card.alpha_composite(zoomed, ((width - size[0]) // 2, (height - size[1]) // 2))
    return card


def canvas_base(width: int, height: int, title: str) -> Image.Image:
    canvas = Image.new("RGB", (width, height), BACKGROUND)
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((12, 12, width - 12, height - 12), 22,
                           fill=PANEL, outline="#C9A94F", width=3)
    text(draw, (width // 2, 40), title, 29, bold=True, anchor="ma", color=GOLD)
    return canvas


def checker(width: int, height: int) -> Image.Image:
    image = Image.new("RGBA", (width, height), "#273635")
    draw = ImageDraw.Draw(image)
    step = 16
    for y in range(0, height, step):
        for x in range(0, width, step):
            if (x // step + y // step) % 2:
                draw.rectangle((x, y, x + step - 1, y + step - 1), fill="#334744")
    return image


def text(
    draw: ImageDraw.ImageDraw,
    position: tuple[int, int],
    value: str,
    size: int,
    *,
    bold: bool = False,
    anchor: str | None = None,
    color: str = TEXT,
) -> None:
    draw.text(
        position,
        value,
        fill=color,
        font=ImageFont.truetype(BOLD_PATH if bold else FONT_PATH, size),
        anchor=anchor,
    )


if __name__ == "__main__":
    main()
