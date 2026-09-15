"""Phase 28.4 builders: projectiles and parametric one-shot combat effects."""
from __future__ import annotations

import math

from .config import PALETTE
from .models import (
    MATERIALS,
    BuiltModel,
    add_cone,
    add_cube,
    add_cylinder_between,
    add_ico,
    add_torus,
)

EFFECT_REVISION = "effects-v1"

# Camera target height; effect geometry centers here so shift-0 framing holds.
STAGE_Z = 1.15


def build_arrow(variant: str = "normal") -> BuiltModel:
    """Build one flight-ready arrow pointing +X; the engine rotates the sprite."""
    if variant != "normal":
        raise ValueError(f"Unknown arrow variant: {variant}")
    wood = MATERIALS.get("arrow_shaft", PALETTE["wood"])
    head = MATERIALS.get("arrow_head", "#C9CFD4", True)
    fin = MATERIALS.get("arrow_fin", PALETTE["hero_leaf"])
    wrap = MATERIALS.get("arrow_wrap", PALETTE["hero_gold"], True)
    objects = [
        add_cylinder_between("arrow_shaft", (-0.55, 0.0, STAGE_Z), (0.35, 0.0, STAGE_Z),
                             0.045, wood),
        add_cone("arrow_head", (0.47, 0.0, STAGE_Z), 0.11, 0.0, 0.30,
                 head, 7, (0.0, -math.pi / 2, 0.0)),
        add_cube("arrow_fin_vertical", (-0.50, 0.0, STAGE_Z), (0.17, 0.022, 0.15), fin),
        add_cube("arrow_fin_flat", (-0.50, 0.0, STAGE_Z), (0.17, 0.15, 0.022), fin),
        add_ico("arrow_wrap", (-0.38, 0.0, STAGE_Z), (0.05, 0.055, 0.055), wrap, 1),
        add_ico("arrow_knock", (-0.60, 0.0, STAGE_Z), (0.045, 0.05, 0.05), wrap, 1),
    ]
    return BuiltModel(None, objects, {
        "modelRevision": EFFECT_REVISION,
        "effectKind": "projectile",
        "variant": variant,
        "flightAxis": "+X",
    })


def _motes(name: str, count: int, progress: float, base_radius: float,
           spread: float, size: float, z: float, material) -> list:
    """Deterministic golden-angle mote ring; shared by both effects."""
    objects = []
    for index in range(count):
        angle = math.radians(index * (360.0 / count) + progress * 40.0)
        radius = base_radius + progress * spread
        scale = size * (1.0 - 0.45 * progress)
        objects.append(add_ico(
            f"{name}_{index}",
            (math.cos(angle) * radius, math.sin(angle) * radius * 0.92, z),
            (scale, scale, scale), material, 1,
        ))
    return objects


def build_impact_flash(progress: float) -> BuiltModel:
    """Warm core that flashes then collapses, ringed by gold motes."""
    if not 0.0 <= progress <= 1.0:
        raise ValueError(f"Impact progress out of range: {progress}")
    core = MATERIALS.get("flash_core", "#FFE9B8", True)
    mote = MATERIALS.get("flash_mote", PALETTE["hero_gold"], True)
    radius = 0.55 - 0.43 * progress
    objects = [add_ico("flash_core", (0.0, 0.0, STAGE_Z),
                       (radius, radius * 0.9, radius * 0.55), core, 2)]
    objects.extend(_motes("flash_mote", 6, progress, 0.30, 0.70, 0.085, STAGE_Z, mote))
    return BuiltModel(None, objects, {
        "modelRevision": EFFECT_REVISION,
        "effectKind": "vfx",
        "effect": "impact_flash",
    })


def build_shockwave_ring(progress: float) -> BuiltModel:
    """Ground shockwave ring that expands and thins as dust lifts off it."""
    if not 0.0 <= progress <= 1.0:
        raise ValueError(f"Shockwave progress out of range: {progress}")
    ring = MATERIALS.get("shockwave_ring", PALETTE["amber"], True)
    dust = MATERIALS.get("shockwave_dust", PALETTE["parchment"])
    major = 0.25 + 0.80 * progress
    minor = 0.090 - 0.045 * progress
    objects = [add_torus("shockwave_ring", (0.0, 0.0, 0.55), major, minor, ring,
                         (0.0, 0.0, 0.0), 24, 5)]
    objects.extend(_motes("shockwave_dust", 4, progress, major * 0.9, 0.25, 0.070,
                           0.55 + 0.45 * progress, dust))
    return BuiltModel(None, objects, {
        "modelRevision": EFFECT_REVISION,
        "effectKind": "vfx",
        "effect": "shockwave_ring",
    })


EFFECT_BUILDERS = {
    "impact_flash": build_impact_flash,
    "shockwave_ring": build_shockwave_ring,
}
