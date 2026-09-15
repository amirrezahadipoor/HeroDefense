from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

BLENDER_VERSION = "4.2.23"
FRAME_RATE = 12
RENDER_SUPERSAMPLE = 3  # Phase 49: 2->3 for regular enemies sharper
OPAQUE_RENDER_SAMPLES = 32  # Phase 49: 28->32 for cleaner vibrant colors
# Workbench equipment overlays ignore sample counts; the number below is
# provenance-only but raised to preserve thin outline after supersampled downscale.
OVERLAY_RENDER_SAMPLES = 12  # studio-v3: 8→12 thin 1.2 interior lines need working-resolution margin
# Phase 28.3: hero, bosses, and trees render at the top tier; every other
# opaque asset shares the mid-tier floors above.
# Studio-v3 audit: 2×/28 mid, 3×/36 top confirmed sufficient for weighted 2.4/1.2 outline + rim fireflies.
# Phase 49 audit: 3×/32 mid, 4×/48 top for stunning vibrant look.
TOP_TIER_CLASSES = frozenset({"boss", "tree"})
TOP_TIER_KEY_PREFIX = "hero"
TOP_TIER_SUPERSAMPLE = 4  # Phase 49: 3->4 sharper edges, cleaner colors
TOP_TIER_SAMPLES = 48  # Phase 49: 36->48 for bloom + rim firefly free


def render_tier(asset_key: str, frame_class: str) -> tuple[int, int]:
    """(supersample, eevee_samples) for one asset; hero/bosses/trees highest."""
    if frame_class in TOP_TIER_CLASSES or asset_key == TOP_TIER_KEY_PREFIX \
            or asset_key.startswith(TOP_TIER_KEY_PREFIX + "_"):
        return (TOP_TIER_SUPERSAMPLE, TOP_TIER_SAMPLES)
    return (RENDER_SUPERSAMPLE, OPAQUE_RENDER_SAMPLES)
OUTLINE_RGBA = (0.0072, 0.0152, 0.0194, 1.0)  # linear-ish #142126
# Phase 37: colored outline per category — hero dark green, bosses dark red, enemies dark violet
OUTLINE_COLORS = {
    "hero": (0.006, 0.032, 0.016, 1.0),      # #0F2A1A dark green
    "boss": (0.08, 0.015, 0.03, 1.0),        # #3A0F1A dark red
    "enemy": (0.018, 0.012, 0.035, 1.0),     # #1A1426 dark violet
    "default": (0.0072, 0.0152, 0.0194, 1.0), # #142126 fallback
}

CAMERA_LOCATION = (6.5, -9.5, 6.2)
CAMERA_TARGET = (0.0, 0.0, 1.15)
CAMERA_SCALE = {
    "character": 3.0,
    "boss": 4.4,
    "item": 2.2,
    "tree": 6.0,
    "environment": 5.1,
    "arena": 11.5,
    "projectile": 1.5,
    "vfx": 2.9,
}
CAMERA_SHIFT_Y = {
    "character": 0.0,
    "boss": 0.06,
    "item": -0.12,
    "tree": 0.12,
    "environment": 0.0,
    "arena": 0.0,
    "projectile": 0.0,
    "vfx": 0.0,
}
FRAME_SIZE = {
    "character": 192,
    "boss": 256,
    "item": 96,
    "tree": 256,
    "environment": 384,
    # The portrait backdrop retains frameSize for manifest compatibility while its
    # explicit frameWidth/frameHeight contract is defined below.
    "arena": 720,
    "projectile": 64,
    "vfx": 128,
}
FRAME_DIMENSIONS = {
    **{key: (size, size) for key, size in FRAME_SIZE.items()},
    "arena": (720, 1280),
}

CLIPS: dict[str, int] = {
    "idle": 6,
    "attack": 8,
    "hit": 4,
    "death": 10,
}

# Phase 28.4: one-shot effect strips; the engine plays them once per trigger.
VFX_CLIPS: dict[str, int] = {
    "play": 8,
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
    "hero_green": "#2ECC71",  # Phase 34 vibrant: was #1E8A4E S0.78 V0.54 -> S0.77 V0.80 phosphorescent
    "hero_leaf": "#A8FF53",   # Phase 34 vibrant: was #8BF27A S0.50 V0.95 -> S0.67 V1.00 lime phosphor
    "hero_gold": "#FFD700",   # Phase 34 vibrant: was #E8B84B S0.68 V0.91 -> S1.00 V1.00 metallic glossy
    "skin": "#F0C9A8",
    "wood": "#70452C",
    "enemy_rust": "#B5452E",
    "enemy_violet": "#7A5CA8",
    "stone": "#8A9AA6",
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

# Phase 28.4 proof sets; Phases 29.4/30 extend the keys, not the categories.
PROJECTILES = (
    RenderAsset("projectile_arrow", "projectile", "arrow", "projectile"),
)

VFX_ASSETS = (
    RenderAsset("vfx_impact_flash", "vfx", "impact_flash", "vfx"),
    RenderAsset("vfx_shockwave_ring", "vfx", "shockwave_ring", "vfx"),
)

# Hero-worn look reduced to boots + weapon (owned artistically by Phase 29.4).
OVERLAY_VISUAL_SLOTS = frozenset({"boots", "weapon"})


def repository_root(script_file: str) -> Path:
    return Path(script_file).resolve().parents[2]
