"""Phase 18 planting ceremony: Hero walk/plant/water clips and the second tree's growth.

The ceremony keeps the locked 25-bone Hero rig and the segmented tree rig so the
runtime can draw the new clips with the same pivots, frame sizes, and outline
contract as combat sprites. Props (seed pouch, watering can) are rigidly owned by
the Hero's hand bones so they move with the authored gestures.
"""
from __future__ import annotations

import math

import bpy

from .config import PALETTE
from .environment import (
    _create_world_tree_armature,
    _reset_world_tree_pose,
    _tree_key,
    build_world_tree,
)
from .models import (
    BuiltModel,
    MATERIALS,
    _bone_part,
    add_cone,
    add_cube,
    add_cylinder_between,
    add_ico,
    add_leaf,
    add_torus,
    build_hero,
)
from .rig import _key, _reset_pose

CEREMONY_CLIPS: dict[str, int] = {
    "walk": 8,
    "plant": 10,
    "water": 10,
}
SAPLING_CLIPS: dict[str, int] = {
    "grow": 12,
    "idle": 6,
}
CEREMONY_IDENTITY = "planting-ceremony-v1"
SAPLING_IDENTITY = "heartwood-sapling-v1"


def build_ceremony_hero() -> BuiltModel:
    """The combat Hero plus a seed pouch on the belt and a watering can in the left hand."""
    hero = build_hero()
    armature = hero.armature
    objects = list(hero.render_objects)
    wood = MATERIALS.get("hero_wood", PALETTE["wood"])
    gold = MATERIALS.get("hero_gold", PALETTE["hero_gold"], True)
    leaf = MATERIALS.get("hero_leaf", PALETTE["hero_leaf"])
    copper = MATERIALS.get("ceremony_copper", "#B5643A", True)
    copper_dark = MATERIALS.get("ceremony_copper_dark", "#6E3A22")
    seed = MATERIALS.get("ceremony_seed", "#55D7BC", True)

    def attach(obj: bpy.types.Object, bone: str) -> None:
        _bone_part(obj, armature, bone, objects)

    # Seed pouch on the right hip; the seed itself glows in the right hand.
    attach(add_ico("ceremony_seed_pouch", (0.30, -0.12, 0.66), (0.13, 0.10, 0.16), wood, 1), "pelvis")
    attach(add_torus("ceremony_pouch_ring", (0.30, -0.12, 0.80), 0.06, 0.018, gold,
                     (math.pi / 2, 0.0, 0.0)), "pelvis")
    attach(add_ico("ceremony_seed", (0.80, -0.10, 0.90), (0.055, 0.045, 0.07), seed, 1), "hand.R")
    attach(add_leaf("ceremony_seed_sprout", (0.80, -0.10, 0.99), (0.03, 0.012, 0.06), leaf), "hand.R")

    # Watering can hangs from the left hand: body, spout, handle, rose.
    attach(add_cone("ceremony_can_body", (-0.82, -0.06, 0.74), 0.16, 0.13, 0.30, copper, 10), "hand.L")
    attach(add_torus("ceremony_can_rim", (-0.82, -0.06, 0.89), 0.13, 0.02, copper_dark,
                     (0.0, 0.0, 0.0)), "hand.L")
    attach(add_cylinder_between("ceremony_can_spout", (-0.93, -0.08, 0.72), (-1.16, -0.10, 0.96),
                                0.028, copper, 7), "hand.L")
    attach(add_ico("ceremony_can_rose", (-1.17, -0.10, 0.97), (0.06, 0.06, 0.05), copper_dark, 1), "hand.L")
    attach(add_torus("ceremony_can_handle", (-0.82, -0.06, 0.95), 0.09, 0.02, copper_dark,
                     (math.pi / 2, 0.0, 0.0)), "hand.L")

    metadata = dict(hero.metadata)
    metadata.update({
        "attachment_variant": "ceremony_props",
        "modelRevision": "hero-ceremony-v1",
        "ceremonyIdentity": CEREMONY_IDENTITY,
        "props": ["seed_pouch", "seed", "watering_can"],
    })
    return BuiltModel(armature, objects, metadata)


def author_ceremony_actions(armature: bpy.types.Object, name: str) -> dict[str, bpy.types.Action]:
    """Walk loop, kneeling plant, and tilting water pour on the standard Hero rig."""
    authors = {
        "walk": _author_walk,
        "plant": _author_plant,
        "water": _author_water,
    }
    if armature.animation_data is None:
        armature.animation_data_create()
    actions: dict[str, bpy.types.Action] = {}
    for clip, count in CEREMONY_CLIPS.items():
        action = bpy.data.actions.new(f"{name}_{clip}")
        action.use_fake_user = True
        armature.animation_data.action = action
        _reset_pose(armature)
        authors[clip](armature, count)
        for curve in action.fcurves:
            for keyframe in curve.keyframe_points:
                keyframe.interpolation = "BEZIER" if clip == "walk" else "LINEAR"
        actions[clip] = action
    _reset_pose(armature)
    armature.animation_data.action = actions["walk"]
    return actions


def _author_walk(armature: bpy.types.Object, count: int) -> None:
    # Contact-pass-contact-pass over eight frames; arms counter-swing, torso bobs twice.
    def pose(phase: float) -> dict[str, tuple[float, float, float]]:
        swing = math.sin(phase * math.tau)
        lift = max(0.0, math.sin(phase * math.tau))
        return {
            "thigh.L": (0.55 * swing, 0.0, 0.0),
            "thigh.R": (-0.55 * swing, 0.0, 0.0),
            "shin.L": (-0.55 * max(0.0, -swing), 0.0, 0.0),
            "shin.R": (-0.55 * lift, 0.0, 0.0),
            "upper_arm.L": (-0.32 * swing, -0.05, -0.08),
            "upper_arm.R": (0.32 * swing, 0.05, 0.08),
            "chest": (0.06, 0.0, 0.10 * swing),
            "head": (-0.03, 0.0, -0.06 * swing),
        }
    for frame in range(1, count + 1):
        phase = (frame - 1) / count
        bob = 0.03 * abs(math.sin(phase * math.tau))
        _key(armature, frame, pose(phase), {"root": (0.0, 0.0, bob)})


def _author_plant(armature: bpy.types.Object, count: int) -> None:
    # Stand → kneel on the right knee → press the seed into the ground → rise.
    stand = {
        "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0), "head": (0.0, 0.0, 0.0),
        "thigh.L": (0.0, 0.0, 0.0), "shin.L": (0.0, 0.0, 0.0),
        "thigh.R": (0.0, 0.0, 0.0), "shin.R": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.0, 0.0, 0.0), "forearm.R": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0), "forearm.L": (0.0, 0.0, 0.0),
    }
    kneel = {
        "pelvis": (0.18, 0.0, 0.0), "chest": (0.42, 0.0, -0.08), "head": (0.30, 0.0, 0.05),
        "thigh.L": (-1.30, 0.0, 0.0), "shin.L": (1.45, 0.0, 0.0),
        "thigh.R": (0.35, 0.0, 0.0), "shin.R": (1.60, 0.0, 0.0),
        "upper_arm.R": (0.90, 0.10, -0.25), "forearm.R": (0.55, 0.0, -0.10),
        "upper_arm.L": (0.35, -0.10, 0.15), "forearm.L": (0.30, 0.0, 0.05),
    }
    press = {
        **kneel,
        "chest": (0.62, 0.0, -0.10), "head": (0.42, 0.0, 0.06),
        "upper_arm.R": (1.35, 0.12, -0.30), "forearm.R": (0.20, 0.0, -0.05),
    }
    _key(armature, 1, stand, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 4, kneel, {"root": (0.0, 0.0, -0.42)})
    _key(armature, 6, press, {"root": (0.0, 0.0, -0.46)})
    _key(armature, 7, press, {"root": (0.0, 0.0, -0.46)})
    _key(armature, 9, kneel, {"root": (0.0, 0.0, -0.42)})
    _key(armature, count, stand, {"root": (0.0, 0.0, 0.0)})


def _author_water(armature: bpy.types.Object, count: int) -> None:
    # Raise the can two-handed, tilt it toward the seedling, hold the pour, lower.
    rest = {
        "chest": (0.0, 0.0, 0.0), "head": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0), "forearm.L": (0.0, 0.0, 0.0), "hand.L": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.0, 0.0, 0.0), "forearm.R": (0.0, 0.0, 0.0),
    }
    raised = {
        "chest": (0.12, 0.0, 0.16), "head": (0.18, 0.0, -0.10),
        "upper_arm.L": (0.95, 0.25, 0.40), "forearm.L": (0.35, 0.0, 0.30), "hand.L": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.70, -0.15, -0.55), "forearm.R": (0.80, 0.0, -0.20),
    }
    pour = {
        **raised,
        "chest": (0.20, 0.0, 0.20), "head": (0.28, 0.0, -0.12),
        "hand.L": (0.0, -0.95, 0.0),
    }
    _key(armature, 1, rest, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 3, raised, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 5, pour, {"root": (0.0, 0.0, -0.02)})
    _key(armature, 7, pour, {"root": (0.0, 0.0, -0.02)})
    _key(armature, 9, raised, {"root": (0.0, 0.0, 0.0)})
    _key(armature, count, rest, {"root": (0.0, 0.0, 0.0)})


def build_sapling_tree() -> BuiltModel:
    """A younger, slimmer Heartwood on the same segmented rig so growth is a scale ramp."""
    tree = build_world_tree(damaged=False)
    for obj in tree.render_objects:
        if obj.name.startswith("tree_falling_bark") or obj.name.startswith("tree_broken_bough"):
            bpy.data.objects.remove(obj, do_unlink=True)
    objects = [obj for obj in tree.render_objects if obj.name in bpy.data.objects]
    metadata = dict(tree.metadata)
    metadata.update({
        "modelRevision": "world-tree-sapling-v1",
        "saplingIdentity": SAPLING_IDENTITY,
        "growsFrom": "seed",
    })
    return BuiltModel(tree.armature, objects, metadata)


def author_sapling_actions(armature: bpy.types.Object) -> dict[str, bpy.types.Action]:
    """Twelve-frame growth from a sprout (root scale 0.08) to a 0.78-scale young tree, then idle."""
    armature.animation_data_create()
    actions: dict[str, bpy.types.Action] = {}
    final = 0.78

    _reset_world_tree_pose(armature)
    grow = bpy.data.actions.new("world_tree_sapling_grow")
    grow.use_fake_user = True
    armature.animation_data.action = grow
    count = SAPLING_CLIPS["grow"]
    for frame in range(1, count + 1):
        t = (frame - 1) / (count - 1)
        eased = 1.0 - (1.0 - t) ** 3
        # Overshoot near the end so the canopy "pops" open before settling.
        overshoot = 1.0 + 0.10 * math.sin(math.pi * max(0.0, (t - 0.6) / 0.4)) if t > 0.6 else 1.0
        scale = 0.08 + (final - 0.08) * eased
        canopy = min(1.0, max(0.0, (t - 0.35) / 0.65))
        _tree_key(
            armature, frame,
            rotations={
                "branch.L": (0.0, 0.0, -0.55 * (1.0 - canopy)),
                "branch.R": (0.0, 0.0, 0.55 * (1.0 - canopy)),
                "canopy.L": (0.0, 0.0, -0.40 * (1.0 - canopy)),
                "canopy.R": (0.0, 0.0, 0.40 * (1.0 - canopy)),
            },
            scales={
                "root": (scale * overshoot, scale * overshoot, scale),
                "crown": (0.3 + 0.7 * canopy,) * 3,
                "heart": (0.6 + 0.4 * canopy,) * 3,
            },
        )
    actions["grow"] = grow

    _reset_world_tree_pose(armature)
    idle = bpy.data.actions.new("world_tree_sapling_idle")
    idle.use_fake_user = True
    armature.animation_data.action = idle
    resting = {"crown": (0.0, -0.008, 0.008), "branch.L": (0.0, 0.0, -0.012), "branch.R": (0.0, 0.0, 0.014)}
    breathing = {"crown": (0.0, 0.024, -0.010), "branch.L": (0.0, -0.010, 0.014), "branch.R": (0.0, 0.010, -0.012)}
    for frame, rot, pulse in ((1, resting, 0.97), (4, breathing, 1.07), (6, resting, 0.97)):
        _tree_key(
            armature, frame, rotations=rot,
            scales={"root": (final, final, final), "heart": (pulse, pulse, pulse)},
        )
    actions["idle"] = idle
    _reset_world_tree_pose(armature)
    armature.animation_data.action = actions["idle"]
    return actions


def stack_named_actions(
    armature: bpy.types.Object,
    actions: dict[str, bpy.types.Action],
    frame_counts: dict[str, int],
    track_name: str,
) -> dict[str, list[int]]:
    """Arrange arbitrary clips as consecutive NLA strips for a single bounded render."""
    armature.animation_data.action = None
    for track in list(armature.animation_data.nla_tracks):
        armature.animation_data.nla_tracks.remove(track)
    track = armature.animation_data.nla_tracks.new()
    track.name = track_name
    global_frame = 1
    mapping: dict[str, list[int]] = {}
    for clip, count in frame_counts.items():
        strip = track.strips.new(clip, global_frame, actions[clip])
        strip.action_frame_start = 1
        strip.action_frame_end = count
        strip.frame_start = global_frame
        strip.frame_end = global_frame + count - 1
        strip.extrapolation = "NOTHING"
        strip.blend_type = "REPLACE"
        mapping[clip] = list(range(global_frame, global_frame + count))
        global_frame += count
    return mapping
