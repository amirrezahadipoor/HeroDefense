#!/usr/bin/env python3
"""Create deterministic before/after contact sheets for the premium-v2 pilot."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from review_strips import grade_row, silhouette_view

# Studio-v3 contact-sheet mode: baseline premium-v2 vs candidate studio-v3
STUDIO_TIER_BASELINE_QUALITY = "premium-v2"
STUDIO_TIER_CANDIDATE_QUALITY = "studio-v3"

EQUIPMENT = (
    "boots_of_three_winds",
    "heartwood_aegis",
    "eternal_seed",
    "crown_of_first_leaves",
    "worldbranch",
)
FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    character_sheet(args.baseline, args.candidate, args.output / "pilot_characters.png")
    equipment_sheet(args.baseline, args.candidate, args.output / "pilot_equipment_set.png")
    supporting_sheet(args.baseline, args.candidate, args.output / "pilot_drop_prop_ui.png")
    print(f"Wrote premium pilot review sheets to {args.output}")


def character_sheet(baseline: Path, candidate: Path, output: Path) -> None:
    columns = (
        ("Baseline idle", "idle", 0, True),
        ("Premium idle", "idle", 0, False),
        ("Anticipation", "attack", 2, False),
        ("Impact", "attack", 4, False),
        ("Hit", "hit", 1, False),
        ("Death hold", "death", 8, False),
    )
    rows = (("Hero", "hero"), ("Rootling", "rootling"), ("Ancient Golem", "ancient_golem"))
    canvas = new_canvas(1930, 1270, "PREMIUM-V2 CHARACTER & ANIMATION PILOT")
    draw = ImageDraw.Draw(canvas)
    for column, (label, _, _, _) in enumerate(columns):
        text(draw, (305 + column * 242, 82), label, 22, bold=True, anchor="ma")
    text(draw, (305 + 6 * 242, 82), "Shape", 22, bold=True, anchor="ma")
    for row, (label, key) in enumerate(rows):
        y = 145 + row * 290
        text(draw, (20, y + 120), label, 20, bold=True, anchor="lm")
        for column, (_, clip, index, use_baseline) in enumerate(columns):
            root = baseline if use_baseline else candidate
            frame = animation_frame(root, "sprites", key, clip, index)
            paste_card(canvas, frame, 190 + column * 242, y, 230, 250)
        impact = animation_frame(candidate, "sprites", key, "attack", 4)
        paste_card(canvas, silhouette_view(impact), 190 + 6 * 242, y, 230, 250)
    grade = grade_row(animation_frame(candidate, "sprites", "hero", "attack", 4))
    canvas.paste(grade.convert("RGB"), ((1930 - grade.width) // 2, 1000))
    text(draw, (1930 // 2, 982), "STAGE GRADE — HERO IMPACT", 20, bold=True, anchor="ma")
    canvas.save(output, optimize=True)


def equipment_sheet(baseline: Path, candidate: Path, output: Path) -> None:
    canvas = new_canvas(1680, 1170, "VERDANT COVENANT EQUIPMENT PILOT")
    draw = ImageDraw.Draw(canvas)
    previews = (
        ("Baseline equipped", baseline, "idle", 0),
        ("Premium equipped", candidate, "idle", 0),
        ("Premium anticipation", candidate, "attack", 2),
        ("Premium impact", candidate, "attack", 4),
        ("Premium death hold", candidate, "death", 8),
    )
    for column, (label, root, clip, index) in enumerate(previews):
        frame = equipped_frame(root, clip, index)
        x = 55 + column * 315
        text(draw, (x + 145, 88), label, 22, bold=True, anchor="ma")
        paste_card(canvas, frame, x, 120, 290, 330)
    text(draw, (45, 500), "Individual runtime icons", 25, bold=True)
    for index, item in enumerate(EQUIPMENT):
        x = 55 + index * 315
        before = Image.open(baseline / "icons" / f"equipment_{item}.png").convert("RGBA")
        after = Image.open(candidate / "icons" / f"equipment_{item}.png").convert("RGBA")
        text(draw, (x + 145, 548), item.replace("_", " ").title(), 20, bold=True, anchor="ma")
        paste_card(canvas, before, x, 575, 135, 170)
        paste_card(canvas, after, x + 155, 575, 135, 170)
        text(draw, (x + 67, 760), "Before", 18, anchor="ma")
        text(draw, (x + 222, 760), "After", 18, anchor="ma")
        paste_card(canvas, silhouette_view(after), x + 85, 775, 120, 120)
        text(draw, (x + 145, 906), "Shape", 16, anchor="ma")
    pilot_after = Image.open(candidate / "icons" / f"equipment_{EQUIPMENT[0]}.png").convert("RGBA")
    grade = grade_row(pilot_after)
    canvas.paste(grade.convert("RGB"), ((1680 - grade.width) // 2, 948))
    text(draw, (1680 // 2, 930), "STAGE GRADE — FIRST PILOT ICON", 18, bold=True, anchor="ma")
    canvas.save(output, optimize=True)


def supporting_sheet(baseline: Path, candidate: Path, output: Path) -> None:
    canvas = new_canvas(1320, 1190, "DROP, ENVIRONMENT PROP & LIVE UI PILOT")
    draw = ImageDraw.Draw(canvas)
    rows = (
        ("Tier-6 potion drop", "icons", "health_potion_6.png"),
        ("Crystal arena prop", "environment", "crystal_prop_0.png"),
        ("Inventory control", "icons", "ui_inventory.png"),
    )
    text(draw, (515, 85), "Baseline", 24, bold=True, anchor="ma")
    text(draw, (900, 85), "Premium-v2", 24, bold=True, anchor="ma")
    text(draw, (1165, 85), "Shape", 24, bold=True, anchor="ma")
    for row, (label, folder, filename) in enumerate(rows):
        y = 125 + row * 275
        text(draw, (35, y + 110), label, 24, bold=True, anchor="lm")
        before = Image.open(baseline / folder / filename).convert("RGBA")
        after = Image.open(candidate / folder / filename).convert("RGBA")
        paste_card(canvas, before, 390, y, 250, 235)
        paste_card(canvas, after, 775, y, 250, 235)
        paste_card(canvas, silhouette_view(after), 1040, y, 250, 235)
    grade = grade_row(Image.open(candidate / "icons" / "health_potion_6.png").convert("RGBA"))
    canvas.paste(grade.convert("RGB"), ((1320 - grade.width) // 2, 960))
    text(draw, (1320 // 2, 942), "STAGE GRADE — POTION DROP", 18, bold=True, anchor="ma")
    text(draw, (40, 1155),
         "Checkerboards expose alpha edges; all previews retain actual runtime frame dimensions.",
         18)
    canvas.save(output, optimize=True)


def animation_frame(root: Path, folder: str, key: str, clip: str, index: int) -> Image.Image:
    data = json.loads((root / folder / f"{key}.json").read_text())
    frame = sorted(data["clips"][clip], key=lambda value: value["index"])[index]
    sheets = data.get("sheets", [{"file": data["sheet"]}])
    sheet = Image.open(root / sheets[frame.get("page", 0)]["file"]).convert("RGBA")
    return sheet.crop((frame["x"], frame["y"],
                       frame["x"] + frame["width"], frame["y"] + frame["height"]))


def equipped_frame(root: Path, clip: str, index: int) -> Image.Image:
    result = animation_frame(root, "sprites", "hero", clip, index)
    for item in EQUIPMENT:
        overlay = animation_frame(root, "equipment", item, clip, index)
        result = Image.alpha_composite(result, overlay)
    return result


def new_canvas(width: int, height: int, title: str) -> Image.Image:
    canvas = Image.new("RGB", (width, height), "#101A1B")
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((12, 12, width - 12, height - 12), 22,
                           fill="#172829", outline="#C9A94F", width=3)
    text(draw, (width // 2, 42), title, 30, bold=True, anchor="ma", color="#F2D58A")
    return canvas


def paste_card(canvas: Image.Image, sprite: Image.Image, x: int, y: int, width: int, height: int) -> None:
    card = checker(width, height)
    scale = min((width - 20) / sprite.width, (height - 20) / sprite.height)
    size = (max(1, round(sprite.width * scale)), max(1, round(sprite.height * scale)))
    preview = sprite.resize(size, Image.Resampling.NEAREST)
    card.alpha_composite(preview, ((width - size[0]) // 2, (height - size[1]) // 2))
    canvas.paste(card.convert("RGB"), (x, y))
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((x, y, x + width, y + height), 12, outline="#58706A", width=2)


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
    color: str = "#E7E1CF",
) -> None:
    path = BOLD_PATH if bold else FONT_PATH
    draw.text(position, value, fill=color, font=ImageFont.truetype(path, size), anchor=anchor)


if __name__ == "__main__":
    main()
