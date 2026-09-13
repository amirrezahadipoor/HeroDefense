from __future__ import annotations

import math

import bpy

from .config import PALETTE
from .models import (
    BuiltModel,
    MATERIALS,
    add_cone,
    add_cube,
    add_cylinder_between,
    add_ico,
    add_leaf,
    add_torus,
)
from .rig import parent_to_bone


def _create_world_tree_armature() -> bpy.types.Object:
    """Create the segmented premium-v2 tree rig used by both health states."""
    armature_data = bpy.data.armatures.new("world_tree_armature_data")
    armature = bpy.data.objects.new("world_tree_armature", armature_data)
    bpy.context.collection.objects.link(armature)
    bpy.context.view_layer.objects.active = armature
    armature.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")
    bones = {}
    layout = {
        "root": ((0.0, 0.0, 0.02), (0.0, 0.0, 0.32), None),
        "trunk.lower": ((0.0, 0.0, 0.32), (0.0, 0.0, 1.72), "root"),
        "trunk.upper": ((0.0, 0.0, 1.72), (0.0, 0.0, 3.00), "trunk.lower"),
        "crown": ((0.0, 0.0, 2.72), (0.0, 0.0, 4.18), "trunk.upper"),
        "branch.L": ((-0.08, 0.0, 1.92), (-1.20, 0.02, 2.96), "trunk.upper"),
        "branch.R": ((0.08, 0.0, 1.92), (1.20, 0.02, 2.96), "trunk.upper"),
        "bough.L": ((-1.05, 0.02, 2.82), (-1.92, 0.04, 3.68), "branch.L"),
        "bough.R": ((1.05, 0.02, 2.82), (1.92, 0.04, 3.68), "branch.R"),
        "canopy.L": ((-0.70, 0.0, 3.12), (-1.12, 0.0, 4.08), "crown"),
        "canopy.R": ((0.70, 0.0, 3.12), (1.12, 0.0, 4.08), "crown"),
        "heart": ((0.0, -0.34, 1.18), (0.0, -0.34, 1.90), "trunk.lower"),
        "debris.L": ((-1.45, -0.02, 3.66), (-1.45, -0.02, 3.94), "canopy.L"),
        "debris.R": ((1.42, -0.02, 3.48), (1.42, -0.02, 3.76), "canopy.R"),
    }
    for name, (head, tail, parent) in layout.items():
        bone = armature.data.edit_bones.new(name)
        bone.head, bone.tail = head, tail
        if parent:
            bone.parent = bones[parent]
        bones[name] = bone
    bpy.ops.object.mode_set(mode="OBJECT")
    armature.select_set(False)
    return armature


def _reset_world_tree_pose(armature: bpy.types.Object) -> None:
    for bone in armature.pose.bones:
        bone.rotation_mode = "XYZ"
        bone.rotation_euler = (0.0, 0.0, 0.0)
        bone.location = (0.0, 0.0, 0.0)
        bone.scale = (1.0, 1.0, 1.0)


def _tree_key(
    armature: bpy.types.Object,
    frame: int,
    *,
    rotations: dict[str, tuple[float, float, float]] | None = None,
    locations: dict[str, tuple[float, float, float]] | None = None,
    scales: dict[str, tuple[float, float, float]] | None = None,
) -> None:
    rotations = rotations or {}
    locations = locations or {}
    scales = scales or {}
    for bone_name in set(rotations) | set(locations) | set(scales):
        bone = armature.pose.bones[bone_name]
        if bone_name in rotations:
            bone.rotation_euler = rotations[bone_name]
            bone.keyframe_insert("rotation_euler", frame=frame, group=bone_name)
        if bone_name in locations:
            bone.location = locations[bone_name]
            bone.keyframe_insert("location", frame=frame, group=bone_name)
        if bone_name in scales:
            bone.scale = scales[bone_name]
            bone.keyframe_insert("scale", frame=frame, group=bone_name)


def author_world_tree_actions(
    armature: bpy.types.Object,
    damaged: bool,
) -> dict[str, bpy.types.Action]:
    """Author a restrained living loop and the wounded tree's one-shot collapse."""
    armature.animation_data_create()
    actions = {}
    state = "damaged" if damaged else "healthy"

    _reset_world_tree_pose(armature)
    idle = bpy.data.actions.new(f"world_tree_{state}_idle")
    idle.use_fake_user = True
    armature.animation_data.action = idle
    resting = {
        "crown": (0.0, -0.022 if damaged else -0.010, 0.010),
        "branch.L": (0.0, 0.0, -0.018 if damaged else -0.010),
        "branch.R": (0.0, 0.0, 0.055 if damaged else 0.012),
        "bough.L": (0.0, 0.010, -0.024),
        "bough.R": (0.0, -0.010, 0.060 if damaged else 0.022),
        "canopy.L": (0.0, 0.0, -0.012),
        "canopy.R": (0.0, 0.0, 0.024 if damaged else 0.012),
    }
    breathing = {
        "crown": (0.0, 0.020 if damaged else 0.028, -0.012),
        "branch.L": (0.0, -0.012, 0.012),
        "branch.R": (0.0, 0.010, 0.025 if damaged else -0.010),
        "bough.L": (0.0, -0.012, 0.012),
        "bough.R": (0.0, 0.014, 0.038 if damaged else -0.012),
        "canopy.L": (0.0, 0.008, 0.014),
        "canopy.R": (0.0, -0.008, 0.010 if damaged else -0.014),
    }
    pulse_low = 0.92 if damaged else 0.97
    pulse_high = 1.035 if damaged else 1.075
    _tree_key(
        armature, 1, rotations=resting,
        scales={"heart": (pulse_low, pulse_low, pulse_low)},
    )
    _tree_key(
        armature, 4, rotations=breathing,
        scales={"heart": (pulse_high, pulse_high, pulse_high)},
    )
    _tree_key(
        armature, 6, rotations=resting,
        scales={"heart": (pulse_low, pulse_low, pulse_low)},
    )
    actions["idle"] = idle

    if damaged:
        _reset_world_tree_pose(armature)
        destroy = bpy.data.actions.new("world_tree_damaged_destroy")
        destroy.use_fake_user = True
        armature.animation_data.action = destroy
        # Local Y follows each tree bone's length, local Z gives the screen-readable
        # lateral hinge, and root/debris Y translation moves downward. The motion is
        # therefore a compact segmented fall rather than an unsafe axial twist.
        _tree_key(
            armature, 1,
            rotations=resting,
            scales={"heart": (0.92, 0.92, 0.92)},
        )
        _tree_key(
            armature, 2,
            rotations={
                **resting,
                "trunk.lower": (0.0, 0.0, -0.020),
                "trunk.upper": (0.0, 0.0, 0.030),
                "crown": (0.0, 0.0, -0.025),
                "branch.L": (0.0, 0.0, -0.055),
                "branch.R": (0.0, 0.0, 0.070),
            },
            locations={"root": (0.018, 0.018, 0.0)},
            scales={"heart": (1.02, 1.02, 1.02)},
        )
        _tree_key(
            armature, 3,
            rotations={
                "trunk.lower": (0.0, 0.0, 0.035),
                "trunk.upper": (0.0, 0.0, -0.055),
                "crown": (0.0, 0.0, 0.045),
                "branch.L": (0.0, 0.0, -0.095),
                "branch.R": (0.0, 0.0, 0.115),
                "bough.L": (0.0, 0.0, -0.070),
                "bough.R": (0.0, 0.0, 0.095),
            },
            locations={"root": (-0.022, -0.012, 0.0)},
            scales={"heart": (1.18, 1.18, 1.18)},
        )
        _tree_key(
            armature, 4,
            rotations={
                "trunk.lower": (0.0, 0.0, -0.025),
                "trunk.upper": (0.0, 0.0, 0.045),
                "crown": (0.0, 0.0, -0.040),
                "branch.L": (0.0, 0.0, -0.145),
                "branch.R": (0.0, 0.0, 0.165),
                "bough.L": (0.0, 0.0, -0.115),
                "bough.R": (0.0, 0.0, 0.140),
                "canopy.L": (0.0, 0.0, -0.055),
                "canopy.R": (0.0, 0.0, 0.065),
            },
            locations={"root": (0.015, 0.008, 0.0)},
            scales={"heart": (1.34, 1.34, 1.34)},
        )
        _tree_key(
            armature, 5,
            rotations={
                "trunk.lower": (0.0, 0.0, 0.035),
                "trunk.upper": (0.0, 0.0, 0.120),
                "crown": (0.0, 0.0, 0.100),
                "branch.L": (0.0, 0.0, -0.190),
                "branch.R": (0.0, 0.0, 0.215),
                "bough.L": (0.0, 0.0, -0.160),
                "bough.R": (0.0, 0.0, 0.195),
                "canopy.L": (0.0, 0.0, -0.090),
                "canopy.R": (0.0, 0.0, 0.105),
            },
            locations={
                "root": (-0.035, -0.045, 0.0),
                "debris.L": (-0.05, -0.22, 0.0),
                "debris.R": (0.06, -0.18, 0.0),
            },
            scales={
                "heart": (0.86, 0.86, 0.86),
                "canopy.L": (0.97, 0.97, 0.97),
                "canopy.R": (0.97, 0.97, 0.97),
            },
        )
        _tree_key(
            armature, 6,
            rotations={
                "trunk.lower": (0.0, 0.0, 0.065),
                "trunk.upper": (0.0, 0.0, 0.240),
                "crown": (0.0, 0.0, 0.180),
                "branch.L": (0.0, 0.0, -0.260),
                "branch.R": (0.0, 0.0, 0.285),
                "bough.L": (0.0, 0.0, -0.235),
                "bough.R": (0.0, 0.0, 0.260),
                "canopy.L": (0.0, 0.0, -0.140),
                "canopy.R": (0.0, 0.0, 0.155),
            },
            locations={
                "root": (-0.090, -0.105, 0.0),
                "debris.L": (-0.13, -0.62, 0.0),
                "debris.R": (0.15, -0.54, 0.0),
            },
            scales={
                "heart": (0.64, 0.64, 0.64),
                "canopy.L": (0.93, 0.93, 0.93),
                "canopy.R": (0.93, 0.93, 0.93),
            },
        )
        _tree_key(
            armature, 7,
            rotations={
                "trunk.lower": (0.0, 0.0, 0.095),
                "trunk.upper": (0.0, 0.0, 0.365),
                "crown": (0.0, 0.0, 0.265),
                "branch.L": (0.0, 0.0, -0.330),
                "branch.R": (0.0, 0.0, 0.355),
                "bough.L": (0.0, 0.0, -0.305),
                "bough.R": (0.0, 0.0, 0.330),
                "canopy.L": (0.0, 0.0, -0.195),
                "canopy.R": (0.0, 0.0, 0.210),
            },
            locations={
                "root": (-0.175, -0.175, 0.0),
                "debris.L": (-0.24, -1.10, 0.0),
                "debris.R": (0.28, -0.98, 0.0),
            },
            scales={
                "heart": (0.42, 0.42, 0.42),
                "canopy.L": (0.88, 0.88, 0.88),
                "canopy.R": (0.88, 0.88, 0.88),
            },
        )
        final_rotations = {
            "trunk.lower": (0.0, 0.0, 0.125),
            "trunk.upper": (0.0, 0.0, 0.485),
            "crown": (0.0, 0.0, 0.330),
            "branch.L": (0.0, 0.0, -0.400),
            "branch.R": (0.0, 0.0, 0.425),
            "bough.L": (0.0, 0.0, -0.375),
            "bough.R": (0.0, 0.0, 0.400),
            "canopy.L": (0.0, 0.0, -0.250),
            "canopy.R": (0.0, 0.0, 0.265),
        }
        final_locations = {
            "root": (-0.285, -0.255, 0.0),
            "debris.L": (-0.34, -1.62, 0.0),
            "debris.R": (0.39, -1.48, 0.0),
        }
        final_scales = {
            "heart": (0.18, 0.18, 0.18),
            "crown": (0.90, 0.90, 0.90),
            "canopy.L": (0.84, 0.84, 0.84),
            "canopy.R": (0.84, 0.84, 0.84),
        }
        _tree_key(
            armature, 8, rotations=final_rotations,
            locations=final_locations, scales=final_scales,
        )
        _tree_key(
            armature, 9, rotations=final_rotations,
            locations=final_locations, scales=final_scales,
        )
        _tree_key(
            armature, 10, rotations=final_rotations,
            locations=final_locations, scales=final_scales,
        )
        actions["destroy"] = destroy

    armature.animation_data.action = actions["idle"]
    return actions


def build_world_tree(damaged: bool = False) -> BuiltModel:
    """Build the premium-v2 Heartwood Sanctum in healthy or wounded form."""
    bark_deep = MATERIALS.get("tree_bark_deep", "#34241F" if not damaged else "#241C1D")
    bark_mid = MATERIALS.get("tree_bark_mid", "#654128" if not damaged else "#4B3029")
    bark_light = MATERIALS.get("tree_bark_light", "#8B6035" if not damaged else "#694536")
    bark_cut = MATERIALS.get("tree_bark_cut", "#B6864F" if not damaged else "#8A6144")
    moss = MATERIALS.get("tree_moss", "#3E7147" if not damaged else "#44543B")
    leaf_deep = MATERIALS.get("tree_leaf_deep", "#174B36" if not damaged else "#263C31")
    leaf_mid = MATERIALS.get("tree_leaf_mid", "#2D7547" if not damaged else "#526044")
    leaf_light = MATERIALS.get("tree_leaf_light", "#72B85C" if not damaged else "#7D7947")
    leaf_accent = MATERIALS.get("tree_leaf_accent", "#B1C96B" if not damaged else "#A27A48")
    heart = MATERIALS.get("tree_heart", "#55D7BC" if not damaged else "#4F968B")
    heart_light = MATERIALS.get("tree_heart_light", "#C7FFF0" if not damaged else "#9BCFC1")
    rune = MATERIALS.get("tree_rune", "#5BC7B4" if not damaged else "#D06C4C")

    armature = _create_world_tree_armature()
    objects = []

    def own(obj: bpy.types.Object, bone: str) -> bpy.types.Object:
        parent_to_bone(obj, armature, bone)
        objects.append(obj)
        return obj

    # Three interlocked trunk masses preserve a broad, shrine-like central read.
    own(add_cone("tree_trunk_base", (0.0, 0.03, 0.86), 0.84, 0.62, 1.62,
                 bark_deep, 12), "trunk.lower")
    own(add_cone("tree_trunk_heart", (0.0, 0.0, 1.72), 0.66, 0.52, 1.35,
                 bark_mid, 12), "trunk.lower")
    own(add_cone("tree_trunk_crown", (0.0, 0.02, 2.57), 0.54, 0.36, 1.30,
                 bark_deep if damaged else bark_mid, 11), "trunk.upper")

    # Radial buttress roots ground the protected landmark and keep its feet readable.
    root_points = (
        (-1.48, -0.28), (-1.08, -0.72), (-0.52, -0.92), (0.18, -0.96),
        (0.82, -0.78), (1.42, -0.34), (1.28, 0.32), (0.55, 0.62), (-0.52, 0.60),
    )
    for index, (x, y) in enumerate(root_points):
        root_obj = add_cone(
            f"tree_buttress_root_{index}", (x * 0.48, y * 0.42, 0.20),
            0.30 if index % 2 == 0 else 0.24, 0.055, 1.55,
            bark_mid if index % 3 else bark_deep, 8,
            (0.0, math.radians(72), math.atan2(y, x)),
        )
        own(root_obj, "root")
        own(add_ico(
            f"tree_root_knuckle_{index}", (x * 0.76, y * 0.66, 0.12),
            (0.28, 0.20, 0.16), bark_light if index % 2 else bark_mid, 1,
        ), "root")

    # Layered front-facing bark plates explain age and construction at gameplay size.
    for index in range(30):
        band = index % 6
        level = index // 6
        x = (-0.50 + band * 0.20) * (1.0 - level * 0.07)
        z = 0.45 + level * 0.47 + (band % 2) * 0.10
        radius = 0.69 - level * 0.055
        y = -radius - 0.035 + abs(x) * 0.09
        plate = add_ico(
            f"tree_bark_plate_{index}", (x, y, z),
            (0.16 + (index % 3) * 0.025, 0.045, 0.25 + (index % 2) * 0.05),
            bark_light if index % 4 == 0 else bark_mid, 1,
        )
        plate.rotation_euler.y = (-0.16 + (index % 5) * 0.08)
        own(plate, "trunk.lower" if z < 1.75 else "trunk.upper")

    # Two articulated branch systems form the Tree's protective upward gesture.
    branch_specs = {
        "L": ((-0.08, 0.02, 1.86), (-1.12, 0.02, 2.88), (-1.88, 0.05, 3.58)),
        "R": ((0.08, 0.02, 1.86), (1.12, 0.02, 2.88),
              (1.56 if damaged else 1.88, 0.05, 3.36 if damaged else 3.58)),
    }
    for side, (start, elbow, end) in branch_specs.items():
        own(add_cylinder_between(
            f"tree_branch_{side}_lower", start, elbow, 0.25,
            bark_mid if side == "L" else bark_light, 9,
        ), f"branch.{side}")
        own(add_ico(
            f"tree_branch_{side}_joint", elbow, (0.34, 0.28, 0.34), bark_deep, 1,
        ), f"branch.{side}")
        own(add_cylinder_between(
            f"tree_bough_{side}_main", elbow, end, 0.18,
            bark_mid if not damaged or side == "L" else bark_cut, 8,
        ), f"bough.{side}")
        sign = -1.0 if side == "L" else 1.0
        forks = (
            (end, (end[0] + 0.28 * sign, end[1] + 0.02, end[2] + 0.52)),
            (end, (end[0] - 0.18 * sign, end[1] + 0.08, end[2] + 0.44)),
            (elbow, (elbow[0] + 0.12 * sign, elbow[1] - 0.08, elbow[2] + 0.58)),
        )
        for fork_index, (fork_start, fork_end) in enumerate(forks):
            if damaged and side == "R" and fork_index == 0:
                fork_end = (fork_start[0] + 0.16 * sign, fork_start[1], fork_start[2] + 0.16)
            own(add_cylinder_between(
                f"tree_bough_{side}_fork_{fork_index}", fork_start, fork_end,
                0.105 if fork_index < 2 else 0.12,
                bark_cut if damaged and side == "R" else bark_light, 7,
            ), f"bough.{side}")
        if damaged and side == "R":
            own(add_cone(
                "tree_broken_bough_R", (end[0] + 0.08, end[1], end[2] + 0.08),
                0.15, 0.035, 0.34, bark_cut, 7, (0.0, -0.68, 0.0),
            ), "bough.R")

    # A carved heart aperture is the single high-contrast focal detail.
    own(add_ico("tree_heart_cradle", (0.0, -0.57, 1.48),
                (0.48, 0.11, 0.62), bark_deep, 2), "heart")
    own(add_torus(
        "tree_heart_ring", (0.0, -0.72, 1.48), 0.36, 0.075,
        bark_cut, (math.pi / 2, 0.0, 0.0), 14, 5,
    ), "heart")
    own(add_ico("tree_heart_core", (0.0, -0.79, 1.48),
                (0.25, 0.10, 0.38), heart, 2), "heart")
    own(add_ico("tree_heart_highlight", (-0.065, -0.89, 1.58),
                (0.075, 0.025, 0.14), heart_light, 1), "heart")
    for index, angle in enumerate((-0.76, -0.38, 0.38, 0.76)):
        own(add_cube(
            f"tree_heart_rune_{index}",
            (math.sin(angle) * 0.40, -0.79, 1.48 + math.cos(angle) * 0.49),
            (0.060, 0.030, 0.19), rune, 0.025,
            (0.0, angle * 0.28, -angle),
        ), "heart")

    # A few broad vine runs connect the shrine core to the canopy without micro-noise.
    vine_paths = (
        ((-0.56, -0.54, 0.52), (-0.70, -0.48, 1.23)),
        ((-0.70, -0.48, 1.23), (-0.55, -0.47, 1.92)),
        ((0.53, -0.53, 0.68), (0.65, -0.47, 1.36)),
        ((0.65, -0.47, 1.36), (0.51, -0.42, 2.10)),
        ((-0.38, -0.40, 2.06), (-0.85, -0.30, 2.62)),
        ((0.38, -0.40, 2.08), (0.86, -0.28, 2.62)),
    )
    for index, (start, end) in enumerate(vine_paths):
        own(add_cylinder_between(
            f"tree_vine_{index}", start, end, 0.042,
            moss if index < 4 else leaf_deep, 6,
        ), "trunk.lower" if index < 4 else "trunk.upper")

    # Faceted canopy masses establish a tiered umbrella; perimeter leaves carry rhythm.
    canopy_count = 16 if damaged else 24
    for index in range(canopy_count):
        angle = index * 2.399963229728653
        ring = 0.55 + (index % 6) * 0.20
        x = math.cos(angle) * ring * 1.28
        y = math.sin(angle) * ring * 0.42 + 0.04
        z = 3.42 + (index % 4) * 0.22 - abs(x) * 0.05
        if damaged:
            z -= 0.06 + (0.12 if x > 0.65 else 0.0)
            if index % 5 == 0:
                x -= 0.18
        material = (leaf_light if index % 5 == 0 else
                    leaf_mid if index % 3 else leaf_deep)
        own(add_ico(
            f"tree_canopy_cluster_{index}", (x, y, z),
            (0.64 + (index % 3) * 0.08, 0.50 + (index % 2) * 0.06,
             0.56 + (index % 4) * 0.045),
            material, 2,
        ), "canopy.L" if x < -0.28 else "canopy.R" if x > 0.28 else "crown")

    leaf_count = 28 if damaged else 42
    for index in range(leaf_count):
        angle = index * math.tau / leaf_count
        x = math.cos(angle) * (1.58 + (index % 3) * 0.13)
        y = -0.17 + math.sin(angle) * 0.18
        z = 3.62 + math.sin(angle) * 0.66 + (index % 2) * 0.10
        if damaged and index % 4 == 0:
            z -= 0.30
        leaf_obj = add_leaf(
            f"tree_crown_leaf_{index}", (x, y, z),
            (0.18 + (index % 2) * 0.04, 0.055, 0.34 + (index % 3) * 0.035),
            leaf_accent if index % 7 == 0 else leaf_light if index % 3 == 0 else leaf_mid,
            (0.0, -0.25 + (index % 5) * 0.12, -angle),
        )
        bone = "canopy.L" if x < 0 else "canopy.R"
        if index in {3, 17}:
            bone = "debris.L"
        elif index in {10, 24}:
            bone = "debris.R"
        own(leaf_obj, bone)

    # Wounded-state scars are broad and directional; they do not cover the core.
    if damaged:
        scars = (
            (-0.30, -0.705, 0.82, -0.32),
            (-0.14, -0.716, 1.07, 0.26),
            (0.28, -0.668, 1.92, -0.38),
            (0.40, -0.612, 2.22, 0.31),
        )
        for index, (x, y, z, tilt) in enumerate(scars):
            own(add_cube(
                f"tree_wound_rune_{index}", (x, y, z),
                (0.055, 0.028, 0.34), rune, 0.018,
                (0.0, tilt, 0.0),
            ), "trunk.lower" if z < 1.7 else "trunk.upper")
        for index, (x, z) in enumerate(((-1.46, 3.48), (1.30, 3.20), (0.96, 3.86))):
            bone = "debris.L" if x < 0 else "debris.R"
            own(add_ico(
                f"tree_falling_bark_{index}", (x, -0.05, z),
                (0.13, 0.08, 0.20), bark_cut, 1,
            ), bone)

    metadata = {
        "state": "damaged" if damaged else "healthy",
        "rigged": True,
        "visualQuality": "premium-v2",
        "modelRevision": (
            "heartwood-sanctum-wounded-v2" if damaged
            else "heartwood-sanctum-healthy-v2"
        ),
        "rigProfile": "segmented-world-tree-v2",
        "animationProfile": (
            "wounded-collapse-v2" if damaged else "living-heart-pulse-v2"
        ),
        "silhouetteLandmarks": [
            "radial buttress roots",
            "carved heart aperture",
            "paired guardian boughs",
            "tiered leaf crown",
        ],
        "surfaceLanguage": (
            "charred heartwood, broken bough, sparse wilted crown, restrained wound runes"
            if damaged else
            "layered heartwood plates, moss vines, emerald crown, restrained cyan heart"
        ),
        "destructionClip": "destroy" if damaged else None,
    }
    return BuiltModel(armature, objects, metadata)

def build_ground_tile(variant: int = 0) -> BuiltModel:
    earth = MATERIALS.get("ground_earth", "#35443A")
    stone = MATERIALS.get("ground_stone", "#53605D")
    objects = []
    tile = add_cone("ground_tile", (0, 0, -0.08), 2.3, 2.3, 0.16, earth, 12)
    objects.append(tile)
    for index in range(5):
        angle = index * 2.399 + variant
        rock = add_ico(
            f"ground_rock_{index}",
            (math.cos(angle) * (0.55 + 0.22 * index), math.sin(angle) * 0.58, 0.03),
            (0.16 + 0.02 * (index % 2), 0.12, 0.08), stone,
        )
        objects.append(rock)
    return BuiltModel(None, objects, {"variant": variant, "tileable": False})


def build_crystal_prop(variant: int = 0) -> BuiltModel:
    base = MATERIALS.get("prop_stone", "#536168")
    dark_base = MATERIALS.get("prop_dark_stone", "#29383A")
    moss = MATERIALS.get("prop_moss", "#426C48")
    colors = ("#58C7D2", "#9B6FD0", "#D8953D")
    highlights = ("#BFF8F0", "#E0C7FF", "#FFE0A0")
    crystal = MATERIALS.get(f"prop_crystal_{variant}", colors[variant % 3])
    highlight = MATERIALS.get(f"prop_crystal_highlight_{variant}", highlights[variant % 3])
    objects = [
        add_ico("crystal_base", (0, 0, 0.20), (0.66, 0.50, 0.27), dark_base, 2),
        add_torus("crystal_base_ring", (0, 0, 0.27), 0.46, 0.07, base),
    ]
    shard_data = (
        (-0.34, 0.03, 0.60, 0.13, 0.86, -13),
        (-0.15, -0.02, 0.78, 0.18, 1.26, -7),
        (0.08, 0.03, 0.92, 0.22, 1.56, 3),
        (0.31, 0.07, 0.69, 0.15, 1.03, 12),
        (0.45, 0.12, 0.51, 0.10, 0.70, 18),
    )
    for index, (x, y, z, radius, depth, tilt) in enumerate(shard_data):
        objects.append(add_cone(
            f"crystal_shard_{index}", (x, y, z), radius, 0.0, depth,
            crystal, 7, (math.radians(2 * index), math.radians(tilt), 0),
        ))
        objects.append(add_cone(
            f"crystal_highlight_{index}", (x - radius * 0.24, y - radius * 0.78, z + depth * 0.08),
            radius * 0.23, 0.0, depth * 0.64, highlight, 5,
            (math.radians(2 * index), math.radians(tilt), 0),
        ))
    for index, (x, z, angle) in enumerate(((-0.46, 0.34, -0.55), (0.41, 0.34, 0.52))):
        objects.append(add_leaf(
            f"crystal_moss_leaf_{index}", (x, -0.23, z),
            (0.18, 0.045, 0.25), moss, (0, 0, angle),
        ))
    return BuiltModel(None, objects, {
        "variant": variant,
        "prop": "faceted_crystal_cluster",
        "visualQuality": "premium-v2",
    })


UI_ICON_KEYS = (
    "ui_health",
    "ui_wave",
    "ui_coin",
    "ui_pause",
    "ui_speed",
    "ui_inventory",
    "ui_shop",
    "ui_settings",
    "ui_restart",
    "ui_new_game",
    "ui_continue",
    "ui_close",
    "ui_strength",
    "ui_agility",
    "ui_luck",
    "ui_dodge",
)


def build_ui_icon(key: str) -> BuiltModel:
    """Build one low-poly, front-facing mobile UI symbol from locked-palette materials."""
    if key not in UI_ICON_KEYS:
        raise ValueError(f"Unknown UI icon: {key}")
    gold = MATERIALS.get("ui_gold", PALETTE["hero_gold"], True)
    green = MATERIALS.get("ui_green", PALETTE["hero_green"])
    leaf = MATERIALS.get("ui_leaf", PALETTE["hero_leaf"])
    parchment = MATERIALS.get("ui_parchment", PALETTE["parchment"])
    wood = MATERIALS.get("ui_wood", PALETTE["wood"])
    cyan = MATERIALS.get("ui_cyan", PALETTE["cyan"])
    crimson = MATERIALS.get("ui_crimson", PALETTE["crimson"])
    stone = MATERIALS.get("ui_stone", PALETTE["stone"])
    objects = []

    def cube(name, location, scale, material, rotation_y=0.0, bevel=0.04):
        obj = add_cube(name, location, scale, material, bevel)
        obj.rotation_euler.y = rotation_y
        objects.append(obj)
        return obj

    def arrow(name, x, z, material, direction=1.0, scale=1.0):
        cube(f"{name}_shaft", (x - 0.12 * direction * scale, 0, z),
             (0.58 * scale, 0.18, 0.16 * scale), material)
        tip = add_cone(
            f"{name}_tip", (x + 0.43 * direction * scale, 0, z),
            0.30 * scale, 0.0, 0.55 * scale, material, 6,
            (0, math.radians(90) * direction, 0),
        )
        objects.append(tip)

    def heart(material):
        objects.append(add_ico("heart_left", (-0.25, 0, 1.22), (0.42, 0.25, 0.42), material, 1))
        objects.append(add_ico("heart_right", (0.25, 0, 1.22), (0.42, 0.25, 0.42), material, 1))
        point = add_cone("heart_point", (0, 0, 0.86), 0.50, 0.0, 0.90, material, 8, (math.pi, 0, 0))
        objects.append(point)

    def sword(material):
        blade = add_cylinder_between("sword_blade", (-0.48, 0, 0.54), (0.48, 0, 1.55), 0.10, material, 6)
        objects.append(blade)
        cube("sword_guard", (-0.43, 0, 0.62), (0.58, 0.20, 0.10), gold, -0.79)
        cube("sword_grip", (-0.67, 0, 0.36), (0.13, 0.17, 0.45), wood, -0.79)

    def shield(material):
        objects.append(add_ico("shield_body", (0, 0, 1.0), (0.72, 0.22, 0.82), material, 2))
        cube("shield_ridge", (0, -0.25, 1.0), (0.10, 0.08, 1.05), gold)

    if key in {"ui_health"}:
        heart(crimson)
    elif key == "ui_wave":
        for index, height in enumerate((0.72, 1.0, 1.28)):
            x = -0.52 + index * 0.52
            objects.append(add_cone(f"wave_{index}", (x, 0, 0.55 + height / 2), 0.28, 0.08, height, cyan, 6))
    elif key == "ui_coin":
        objects.append(add_cone("coin", (0, 0, 1.0), 0.72, 0.72, 0.22, gold, 12, (math.pi / 2, 0, 0)))
        cube("coin_leaf", (0, -0.15, 1.0), (0.15, 0.07, 0.62), green, -0.55)
    elif key == "ui_pause":
        cube("pause_left", (-0.26, 0, 1.0), (0.30, 0.25, 1.25), parchment)
        cube("pause_right", (0.26, 0, 1.0), (0.30, 0.25, 1.25), parchment)
    elif key == "ui_speed":
        for index in range(2):
            arrow(f"arrow_{index}", -0.30 + index * 0.60, 1.0, leaf, 1.0, 0.90)
    elif key == "ui_continue":
        arrow("continue_arrow", 0.0, 1.0, leaf, 1.0, 1.12)
    elif key == "ui_inventory":
        deep = MATERIALS.get("ui_pack_deep", "#253C35")
        bright_gold = MATERIALS.get("ui_pack_bright_gold", "#F2D58A", True)
        cube("pack_shadow_body", (0, 0.04, 0.88), (1.22, 0.52, 1.08), deep, bevel=0.12)
        cube("pack_body", (0, -0.08, 0.91), (1.08, 0.42, 0.94), wood, bevel=0.10)
        cube("pack_flap", (0, -0.33, 1.30), (0.98, 0.12, 0.38), green, bevel=0.09)
        cube("pack_center_strap", (0, -0.44, 0.93), (0.16, 0.075, 0.75), parchment, bevel=0.025)
        cube("pack_buckle", (0, -0.53, 1.02), (0.25, 0.08, 0.25), gold, bevel=0.035)
        objects.append(add_leaf("pack_leaf_mark", (0, -0.63, 1.02),
                                (0.075, 0.025, 0.13), leaf))
        objects.append(add_torus("pack_handle", (0, 0.02, 1.58), 0.31, 0.065,
                                 bright_gold, (math.pi / 2, 0, 0)))
        for side, sign in (("L", -1), ("R", 1)):
            cube(f"pack_side_binding_{side}", (0.46 * sign, -0.35, 0.92),
                 (0.085, 0.06, 0.70), gold, bevel=0.02)
            for index in range(2):
                objects.append(add_ico(
                    f"pack_stud_{side}_{index}",
                    (0.46 * sign, -0.47, 0.69 + index * 0.44),
                    (0.055, 0.025, 0.055), bright_gold, 1,
                ))
    elif key == "ui_shop":
        cube("shop_body", (0, 0, 0.76), (1.20, 0.42, 0.88), wood, bevel=0.06)
        for index, material in enumerate((green, parchment, green, parchment)):
            cube(f"awning_{index}", (-0.45 + index * 0.30, -0.25, 1.43), (0.30, 0.16, 0.38), material)
        cube("shop_door", (0, -0.26, 0.64), (0.34, 0.10, 0.62), stone)
    elif key == "ui_settings":
        objects.append(add_torus("gear_ring", (0, 0, 1.0), 0.48, 0.16, stone, (math.pi / 2, 0, 0)))
        for index in range(8):
            angle = index * math.tau / 8
            cube(
                f"gear_tooth_{index}",
                (math.cos(angle) * 0.68, 0, 1.0 + math.sin(angle) * 0.68),
                (0.24, 0.20, 0.24), stone, -angle,
            )
    elif key == "ui_restart":
        objects.append(add_torus("restart_ring", (0, 0, 1.0), 0.53, 0.11, leaf, (math.pi / 2, 0, 0)))
        tip = add_cone("restart_tip", (-0.58, 0, 1.28), 0.24, 0.0, 0.48, leaf, 6, (0, -0.75, 0))
        objects.append(tip)
    elif key in {"ui_new_game", "ui_strength"}:
        sword(parchment)
        if key == "ui_new_game":
            cube("new_plus_h", (0.47, -0.18, 0.52), (0.55, 0.12, 0.12), leaf)
            cube("new_plus_v", (0.47, -0.18, 0.52), (0.12, 0.12, 0.55), leaf)
    elif key == "ui_close":
        cube("close_a", (0, 0, 1.0), (0.20, 0.24, 1.30), crimson, 0.78)
        cube("close_b", (0, 0, 1.0), (0.20, 0.24, 1.30), crimson, -0.78)
    elif key == "ui_agility":
        for side in (-1, 1):
            for index in range(3):
                cube(
                    f"wing_{side}_{index}",
                    (side * (0.25 + index * 0.22), 0, 1.15 - index * 0.18),
                    (0.42, 0.16, 0.18), leaf, side * (0.35 + index * 0.18),
                )
    elif key == "ui_luck":
        for index in range(4):
            angle = index * math.tau / 4
            objects.append(add_ico(
                f"clover_{index}",
                (math.cos(angle) * 0.34, 0, 1.02 + math.sin(angle) * 0.34),
                (0.40, 0.20, 0.40), leaf, 1,
            ))
        cube("clover_stem", (0.20, 0, 0.55), (0.12, 0.14, 0.62), green, -0.35)
    elif key == "ui_dodge":
        shield(cyan)

    return BuiltModel(None, objects, {
        "uiIcon": key.removeprefix("ui_"),
        "touchOnlyUI": True,
        "visualQuality": "premium-v2" if key == "ui_inventory" else "baseline-compatible",
    })


def build_potion_icon(tier: int) -> BuiltModel:
    glass = MATERIALS.get(f"potion_glass_{tier}", "#B9D6CF")
    liquid_colors = ("#6CCB78", "#63C8B7", "#5EA7D8", "#986BD2", "#D35F9A", "#F0B84B")
    liquid = MATERIALS.get(f"potion_liquid_{tier}", liquid_colors[tier - 1])
    cork = MATERIALS.get("potion_cork", "#8B6138")
    gold = MATERIALS.get("potion_gold", PALETTE["hero_gold"], True)
    leaf = MATERIALS.get("potion_leaf", PALETTE["hero_leaf"])
    heart = MATERIALS.get("potion_heart", "#FFF0C2", True)
    objects = [
        # The liquid owns the broad color mass; brighter facets describe thick glass.
        add_ico("potion_bottle_liquid", (0, 0, 0.62), (0.54, 0.36, 0.60), liquid, 2),
        add_ico("potion_inner_glow", (0, -0.26, 0.68), (0.30, 0.06, 0.34),
                MATERIALS.get(f"potion_glow_{tier}", "#FFDCA0", True), 1),
        add_cube("potion_glass_highlight", (-0.20, -0.34, 0.75),
                 (0.070, 0.025, 0.32), glass, 0.02),
        add_cone("potion_neck", (0, 0, 1.08), 0.21, 0.18, 0.42, glass, 10),
        add_cone("potion_cork", (0, 0, 1.34), 0.19, 0.15, 0.23, cork, 8),
        add_torus("potion_band", (0, 0, 1.19), 0.22, 0.038, gold),
        add_torus("potion_label_ring", (0, -0.36, 0.60), 0.20, 0.040,
                  gold, (math.pi / 2, 0, 0)),
        # A real three-piece heart remains readable after the 96 px icon downsample.
        add_ico("potion_heart_left", (-0.055, -0.405, 0.64), (0.075, 0.028, 0.075), heart, 1),
        add_ico("potion_heart_right", (0.055, -0.405, 0.64), (0.075, 0.028, 0.075), heart, 1),
        add_cone("potion_heart_point", (0, -0.405, 0.56), 0.105, 0.0, 0.20,
                 heart, 6, (math.pi, 0, 0)),
    ]
    if tier >= 5:
        for index, sign in enumerate((-1, 1)):
            objects.append(add_leaf(
                f"potion_collar_leaf_{index}", (0.23 * sign, -0.16, 1.22),
                (0.13, 0.045, 0.22), leaf, (0, 0, 0.62 * sign),
            ))
    if tier == 6:
        # Restrained legendary framing creates a distinct silhouette without masking
        # the liquid mass: two cradle rails, a foot ring, and a seed-like stopper cap.
        for side, sign in (("L", -1), ("R", 1)):
            objects.append(add_cylinder_between(
                f"potion_cradle_{side}", (0.37 * sign, -0.34, 0.35),
                (0.23 * sign, -0.34, 1.03), 0.035, gold, 6,
            ))
        objects.append(add_torus(
            "potion_foot_ring", (0, -0.06, 0.22), 0.34, 0.035,
            gold, (math.pi / 2, 0, 0),
        ))
        objects.append(add_leaf(
            "potion_stopper_seed", (0, -0.04, 1.50),
            (0.13, 0.07, 0.18), leaf,
        ))
    return BuiltModel(None, objects, {
        "tier": tier,
        "heal_icon": True,
        "visualQuality": "premium-v2" if tier == 6 else "baseline-compatible",
    })
