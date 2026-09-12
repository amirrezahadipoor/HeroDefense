from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

BLENDER_VERSION = "4.2.23"
FRAME_RATE = 12
RENDER_SUPERSAMPLE = 2
OPAQUE_RENDER_SAMPLES = 16
OVERLAY_RENDER_SAMPLES = 8
OUTLINE_RGBA = (0.0072, 0.0152, 0.0194, 1.0)  # linear-ish #142126

CAMERA_LOCATION = (6.5, -9.5, 6.2)
CAMERA_TARGET = (0.0, 0.0, 1.15)
CAMERA_SCALE = {
    "character": 3.0,
    "boss": 4.4,
    "item": 2.2,
    "tree": 6.0,
    "environment": 5.1,
}
CAMERA_SHIFT_Y = {
    "character": 0.0,
    "boss": 0.06,
    "item": -0.12,
    "tree": 0.12,
    "environment": 0.0,
}
FRAME_SIZE = {
    "character": 192,
    "boss": 256,
    "item": 96,
    "tree": 256,
    "environment": 192,
}

CLIPS: dict[str, int] = {
    "idle": 6,
    "attack": 8,
    "hit": 4,
    "death": 10,
}

REQUIRED_BONES = (
    "root",
    "pelvis",
    "spine",
    "chest",
    "neck",
    "head",
    "upper_arm.L",
    "forearm.L",
    "hand.L",
    "upper_arm.R",
    "forearm.R",
    "hand.R",
    "thigh.L",
    "shin.L",
    "foot.L",
    "thigh.R",
    "shin.R",
    "foot.R",
    "weapon_socket",
    "helmet_socket",
    "armor_socket",
    "boot_socket.L",
    "boot_socket.R",
    "ring_socket.L",
    "ring_socket.R",
)

PALETTE = {
    "outline": "#142126",
    "hero_green": "#2E6B47",
    "hero_leaf": "#74C365",
    "hero_gold": "#D6AD4C",
    "skin": "#D9A978",
    "wood": "#70452C",
    "enemy_rust": "#9A4D36",
    "enemy_violet": "#66507E",
    "stone": "#65727A",
    "ink": "#0B1419",
    "parchment": "#E7D8B1",
    "cyan": "#5FCAD2",
    "crimson": "#B84245",
    "amber": "#E19C3B",
}


@dataclass(frozen=True)
class RenderAsset:
    key: str
    family: str
    builder: str
    frame_class: str = "character"
    variant: int = 0


REGULAR_CHARACTERS = (
    RenderAsset("hero", "hero", "hero"),
    RenderAsset("rootling", "enemy", "rootling"),
    RenderAsset("stonekin", "enemy", "stonekin"),
    RenderAsset("gloom_wolf", "enemy", "gloom_wolf"),
    RenderAsset("fungal_brute", "enemy", "fungal_brute"),
)

BOSSES = (
    RenderAsset("ancient_golem", "boss", "ancient_golem", "boss"),
    RenderAsset("thorn_matriarch", "boss", "thorn_matriarch", "boss"),
    RenderAsset("ember_wyrm", "boss", "ember_wyrm", "boss"),
    RenderAsset("void_knight", "boss", "void_knight", "boss"),
)


def repository_root(script_file: str) -> Path:
    return Path(script_file).resolve().parents[2]
