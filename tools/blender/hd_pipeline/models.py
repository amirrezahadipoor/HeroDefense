from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Callable

import bpy
from mathutils import Vector

from .config import PALETTE
from .rig import BONE_LAYOUT, create_standard_armature, parent_to_bone
from .scene import add_contact_shadow, toon_material, transparent_material


@dataclass
class BuiltModel:
    armature: bpy.types.Object | None
    render_objects: list[bpy.types.Object]
    metadata: dict


class MaterialSet:
    def __init__(self) -> None:
        self._cache: dict[tuple[str, str, bool], bpy.types.Material] = {}

    def clear(self) -> None:
        self._cache.clear()

    def get(self, name: str, color_hex: str, metallic: bool = False) -> bpy.types.Material:
        key = (name, color_hex, metallic)
        if key not in self._cache:
            self._cache[key] = toon_material(name, color_hex, 1.0 if metallic else 0.0)
        return self._cache[key]


MATERIALS = MaterialSet()


def _finish(obj: bpy.types.Object, name: str, material: bpy.types.Material, scale=(1.0, 1.0, 1.0)) -> bpy.types.Object:
    obj.name = name
    obj.scale = scale
    if obj.type == "MESH":
        obj.data.materials.append(material)
        for polygon in obj.data.polygons:
            polygon.use_smooth = False
    return obj


def add_ico(name: str, location, scale, material, subdivisions: int = 1) -> bpy.types.Object:
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=subdivisions, radius=1.0, location=location)
    return _finish(bpy.context.object, name, material, scale)


def add_uv(name: str, location, scale, material) -> bpy.types.Object:
    bpy.ops.mesh.primitive_uv_sphere_add(segments=8, ring_count=4, radius=1.0, location=location)
    return _finish(bpy.context.object, name, material, scale)


def add_cube(
    name: str,
    location,
    scale,
    material,
    bevel: float = 0.0,
    rotation=(0.0, 0.0, 0.0),
) -> bpy.types.Object:
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=location, rotation=rotation)
    obj = _finish(bpy.context.object, name, material, scale)
    if bevel > 0:
        modifier = obj.modifiers.new("silhouette_bevel", "BEVEL")
        modifier.width = bevel
        modifier.segments = 1
    return obj


def add_cone(
    name: str,
    location,
    radius_bottom: float,
    radius_top: float,
    depth: float,
    material,
    vertices: int = 8,
    rotation=(0.0, 0.0, 0.0),
) -> bpy.types.Object:
    bpy.ops.mesh.primitive_cone_add(
        vertices=vertices,
        radius1=radius_bottom,
        radius2=radius_top,
        depth=depth,
        location=location,
        rotation=rotation,
    )
    return _finish(bpy.context.object, name, material)


def add_cylinder_between(name: str, start, end, radius: float, material, vertices: int = 7) -> bpy.types.Object:
    start_vec, end_vec = Vector(start), Vector(end)
    direction = end_vec - start_vec
    midpoint = (start_vec + end_vec) * 0.5
    bpy.ops.mesh.primitive_cylinder_add(vertices=vertices, radius=radius, depth=direction.length, location=midpoint)
    obj = _finish(bpy.context.object, name, material)
    obj.rotation_euler = direction.to_track_quat("Z", "Y").to_euler()
    return obj


def add_torus(
    name: str,
    location,
    major_radius: float,
    minor_radius: float,
    material,
    rotation=(0.0, 0.0, 0.0),
    major_segments: int = 12,
    minor_segments: int = 4,
) -> bpy.types.Object:
    bpy.ops.mesh.primitive_torus_add(
        major_radius=major_radius,
        minor_radius=minor_radius,
        major_segments=major_segments,
        minor_segments=minor_segments,
        location=location,
        rotation=rotation,
    )
    return _finish(bpy.context.object, name, material)


def add_leaf(
    name: str,
    location,
    scale,
    material,
    rotation=(0.0, 0.0, 0.0),
) -> bpy.types.Object:
    """Create a faceted, flattened leaf/gem silhouette with a readable center ridge."""
    leaf = add_ico(name, location, scale, material, 1)
    leaf.rotation_euler = rotation
    return leaf


# Premium boss authoring helpers intentionally sit on top of the same deterministic
# low-poly primitives as the rest of the pipeline. They make material intent and
# rigid bone ownership explicit while preserving the existing public helpers.
def make_material(
    name: str,
    color_hex: str,
    *,
    roughness: float = 0.72,
    metallic: float = 0.0,
    emission: float = 0.0,
) -> bpy.types.Material:
    del roughness, emission  # The locked toon ramp encodes these cues through color/value.
    return MATERIALS.get(name, color_hex, metallic > 0.0)


def add_ico_sphere(
    name: str,
    location,
    scale,
    material,
    *,
    subdivisions: int = 1,
    rotation=(0.0, 0.0, 0.0),
) -> bpy.types.Object:
    obj = add_ico(name, location, scale, material, subdivisions)
    obj.rotation_euler = rotation
    return obj


def add_pointed_cone(
    name: str,
    location,
    radius: float,
    depth: float,
    material,
    *,
    vertices: int = 8,
    rotation=(0.0, 0.0, 0.0),
) -> bpy.types.Object:
    return add_cone(name, location, radius, 0.0, depth, material, vertices, rotation)


def add_cylinder(
    name: str,
    location,
    radius: float,
    depth: float,
    material,
    *,
    vertices: int = 8,
    rotation=(0.0, 0.0, 0.0),
) -> bpy.types.Object:
    bpy.ops.mesh.primitive_cylinder_add(
        vertices=vertices,
        radius=radius,
        depth=depth,
        location=location,
        rotation=rotation,
    )
    return _finish(bpy.context.object, name, material)


def _tag(obj: bpy.types.Object, bone_name: str) -> bpy.types.Object:
    obj["hd_bone"] = bone_name
    return obj


def attach_meshes(armature: bpy.types.Object, objects: list[bpy.types.Object]) -> None:
    for obj in objects:
        parent_to_bone(obj, armature, str(obj["hd_bone"]))


def _bone_part(obj: bpy.types.Object, armature: bpy.types.Object, bone_name: str, objects: list[bpy.types.Object]) -> None:
    parent_to_bone(obj, armature, bone_name)
    objects.append(obj)


def _humanoid_limbs(
    armature: bpy.types.Object,
    objects: list[bpy.types.Object],
    arm_material: bpy.types.Material,
    leg_material: bpy.types.Material,
    hand_material: bpy.types.Material,
    scale: float = 1.0,
) -> None:
    for side in ("L", "R"):
        for bone_name, radius, material in (
            (f"upper_arm.{side}", 0.105 * scale, arm_material),
            (f"forearm.{side}", 0.09 * scale, arm_material),
            (f"thigh.{side}", 0.13 * scale, leg_material),
            (f"shin.{side}", 0.11 * scale, leg_material),
        ):
            head, tail, _ = BONE_LAYOUT[bone_name]
            obj = add_cylinder_between(f"{bone_name}_mesh", head, tail, radius, material)
            _bone_part(obj, armature, bone_name, objects)
        hand_head, hand_tail, _ = BONE_LAYOUT[f"hand.{side}"]
        hand = add_ico(f"hand_{side}", hand_tail, (0.12, 0.10, 0.13), hand_material)
        _bone_part(hand, armature, f"hand.{side}", objects)
        foot = add_cube(
            f"foot_{side}",
            (-0.22 if side == "L" else 0.22, -0.12, 0.075),
            (0.23, 0.34, 0.15),
            leg_material,
            0.04,
        )
        _bone_part(foot, armature, f"foot.{side}", objects)


def build_hero() -> BuiltModel:
    mats = {
        "green": MATERIALS.get("hero_green", PALETTE["hero_green"]),
        "leaf": MATERIALS.get("hero_leaf", PALETTE["hero_leaf"]),
        "deep_leaf": MATERIALS.get("hero_deep_leaf", "#174936"),
        "gold": MATERIALS.get("hero_gold", PALETTE["hero_gold"], True),
        "gold_light": MATERIALS.get("hero_gold_light", "#F2D58A", True),
        "skin": MATERIALS.get("hero_skin", PALETTE["skin"]),
        "skin_shadow": MATERIALS.get("hero_skin_shadow", "#A86F52"),
        "wood": MATERIALS.get("hero_wood", PALETTE["wood"]),
        "hair": MATERIALS.get("hero_hair", "#D8C77C"),
        "hair_shadow": MATERIALS.get("hero_hair_shadow", "#8D7142"),
        "dark": MATERIALS.get("hero_dark", "#172F2A"),
    }
    armature = create_standard_armature("hero")
    objects: list[bpy.types.Object] = []

    def attach(obj: bpy.types.Object, bone: str) -> None:
        _bone_part(obj, armature, bone, objects)

    # Layered archer silhouette: dark under-tunic, fitted green cuirass, leaf mantle.
    attach(add_cone("hero_under_tunic", (0, 0.03, 1.10), 0.42, 0.29, 0.82,
                    mats["dark"], 10), "chest")
    attach(add_cone("hero_fitted_cuirass", (0, -0.035, 1.20), 0.37, 0.25, 0.66,
                    mats["green"], 10), "chest")
    attach(add_cone("hero_leaf_cloak", (0, 0.22, 1.12), 0.51, 0.22, 0.88,
                    mats["deep_leaf"], 9), "chest")
    for index, (x, z, angle) in enumerate(((-0.28, 1.42, -0.25), (0.0, 1.48, 0.0), (0.28, 1.42, 0.25))):
        attach(add_leaf(
            f"hero_mantle_leaf_{index}", (x, -0.23, z), (0.19, 0.065, 0.34),
            mats["leaf"], (0.0, angle, angle),
        ), "chest")
    attach(add_cube("hero_cross_strap", (-0.05, -0.31, 1.21),
                    (0.13, 0.055, 0.72), mats["wood"], 0.025), "chest")
    objects[-1].rotation_euler.y = -0.43
    attach(add_torus("hero_mantle_clasp", (0.0, -0.36, 1.46), 0.105, 0.035,
                     mats["gold"], (math.pi / 2, 0.0, 0.0)), "chest")

    attach(add_cube("hero_belt", (0, -0.01, 0.78), (0.72, 0.40, 0.17),
                    mats["wood"], 0.045), "pelvis")
    attach(add_cube("hero_belt_buckle", (0, -0.235, 0.79), (0.20, 0.055, 0.20),
                    mats["gold"], 0.025), "pelvis")
    attach(add_leaf("hero_skirt_panel_left", (-0.20, -0.14, 0.61),
                    (0.22, 0.07, 0.37), mats["green"], (0, -0.10, -0.08)), "pelvis")
    attach(add_leaf("hero_skirt_panel_right", (0.20, -0.14, 0.61),
                    (0.22, 0.07, 0.37), mats["deep_leaf"], (0, 0.10, 0.08)), "pelvis")

    head = add_ico("hero_head", (0, -0.015, 1.75), (0.30, 0.26, 0.34), mats["skin"], 2)
    attach(head, "head")
    eye_material = MATERIALS.get("hero_eye", "#102129")
    for side, sign in (("L", -1), ("R", 1)):
        attach(add_ico(f"hero_eye_{side}", (0.09 * sign, -0.260, 1.80),
                       (0.034, 0.017, 0.046), eye_material, 1), "head")
        brow = add_cube(f"hero_brow_{side}", (0.09 * sign, -0.272, 1.865),
                        (0.115, 0.022, 0.025), mats["hair_shadow"], 0.008)
        brow.rotation_euler.y = 0.12 * sign
        attach(brow, "head")
    attach(add_cone("hero_nose", (0, -0.275, 1.765), 0.035, 0.008, 0.105,
                    mats["skin_shadow"], 5, (math.pi / 2, 0, 0)), "head")

    attach(add_cone("hero_hair_crown", (0, 0.08, 1.91), 0.32, 0.10, 0.46,
                    mats["hair"], 10), "head")
    for index, (x, z, angle) in enumerate(((-0.23, 1.72, -0.16), (0.23, 1.72, 0.16),
                                           (-0.13, 1.98, -0.08), (0.13, 1.98, 0.08))):
        attach(add_cone(
            f"hero_hair_lock_{index}", (x, -0.13, z), 0.075, 0.015, 0.36,
            mats["hair_shadow" if index < 2 else "hair"], 6, (0, angle, 0),
        ), "head")
    attach(add_torus("hero_circlet", (0, -0.05, 1.93), 0.29, 0.025,
                     mats["gold"], (math.pi / 2, 0, 0)), "head")
    attach(add_leaf("hero_circlet_leaf", (0, -0.315, 1.95),
                    (0.085, 0.025, 0.15), mats["gold_light"]), "head")
    for side, sign in (("L", -1), ("R", 1)):
        ear = add_cone(
            f"elf_ear_{side}", (0.31 * sign, -0.01, 1.78), 0.10, 0.012, 0.42,
            mats["skin"], 6, rotation=(0.0, math.radians(76), 0.0),
        )
        ear.rotation_euler.y *= sign
        attach(ear, "head")

    _humanoid_limbs(armature, objects, mats["green"], mats["dark"], mats["skin"])
    for side, sign in (("L", -1), ("R", 1)):
        upper_head, upper_tail, _ = BONE_LAYOUT[f"upper_arm.{side}"]
        attach(add_ico(f"hero_leaf_pauldron_{side}",
                       ((upper_head[0] + upper_tail[0]) * 0.56, -0.01, 1.39),
                       (0.24, 0.17, 0.16), mats["leaf"], 1), f"upper_arm.{side}")
        fore_head, fore_tail, _ = BONE_LAYOUT[f"forearm.{side}"]
        attach(add_cylinder_between(f"hero_bracer_{side}", fore_head, fore_tail,
                                    0.105, mats["wood"], 8), f"forearm.{side}")
        attach(add_leaf(f"hero_knee_guard_{side}", (0.22 * sign, -0.125, 0.39),
                        (0.15, 0.055, 0.19), mats["green"]), f"shin.{side}")

    return BuiltModel(armature, objects, {
        "silhouette": "premium_elf_archer",
        "attachment_variant": "equipment_neutral",
        "modelRevision": "hero-premium-v2-final",
        "rigProfile": "premium-humanoid-v2",
        "visualQuality": "premium-v2",
    })


def _add_bow(armature, objects, wood, gold) -> None:
    points = (
        (0.82, -0.10, 0.42),
        (1.06, -0.11, 0.66),
        (1.13, -0.12, 0.94),
        (1.06, -0.11, 1.22),
        (0.82, -0.09, 1.48),
    )
    for index, (start, end) in enumerate(zip(points, points[1:])):
        part = add_cylinder_between(f"starter_bow_{index}", start, end, 0.035, wood, 6)
        _bone_part(part, armature, "weapon_socket", objects)
    string = add_cylinder_between("starter_bow_string", points[0], points[-1], 0.008, gold, 5)
    _bone_part(string, armature, "weapon_socket", objects)


def _add_quiver(armature, objects, wood, gold) -> None:
    quiver = add_cone("starter_quiver", (0.28, 0.22, 1.15), 0.12, 0.16, 0.62, wood, 7, (0.18, 0.0, -0.28))
    _bone_part(quiver, armature, "chest", objects)
    for index in range(3):
        arrow = add_cylinder_between(
            f"arrow_{index}",
            (0.20 + index * 0.07, 0.20, 1.22),
            (0.29 + index * 0.07, 0.24, 1.76),
            0.012,
            gold,
            5,
        )
        _bone_part(arrow, armature, "chest", objects)


def _basic_humanoid(
    name: str,
    body_color: str,
    accent_color: str,
    head_shape: str = "ico",
    body_scale: float = 1.0,
) -> tuple[bpy.types.Object, list[bpy.types.Object], dict[str, bpy.types.Material]]:
    body = MATERIALS.get(f"{name}_body", body_color)
    accent = MATERIALS.get(f"{name}_accent", accent_color)
    dark = MATERIALS.get(f"{name}_dark", "#283036")
    armature = create_standard_armature(name, body_scale)
    objects: list[bpy.types.Object] = []
    torso = add_cone(f"{name}_torso", (0, 0, 1.13), 0.42 * body_scale, 0.30 * body_scale, 0.82 * body_scale, body, 7)
    _bone_part(torso, armature, "chest", objects)
    if head_shape == "cube":
        head = add_cube(f"{name}_head", (0, 0, 1.74), (0.52, 0.46, 0.48), body, 0.07)
    else:
        head = add_ico(f"{name}_head", (0, 0, 1.74), (0.33, 0.30, 0.34), body, 1)
    _bone_part(head, armature, "head", objects)
    _humanoid_limbs(armature, objects, body, dark, accent, body_scale)
    return armature, objects, {"body": body, "accent": accent, "dark": dark}


def build_rootling() -> BuiltModel:
    """Premium thorn-scout: wiry branch crown, bark plates, leaf mantle, and growth-ring focus."""
    mats = {
        "bark": MATERIALS.get("rootling_bark", "#744322"),
        "bark_light": MATERIALS.get("rootling_bark_light", "#A86B2D"),
        "bark_dark": MATERIALS.get("rootling_bark_dark", "#38261D"),
        "leaf": MATERIALS.get("rootling_leaf", "#6E9E43"),
        "leaf_light": MATERIALS.get("rootling_leaf_light", "#A9CB58"),
        "sap": MATERIALS.get("rootling_sap", "#F3CE73"),
    }
    armature = create_standard_armature("rootling", 0.92)
    objects: list[bpy.types.Object] = []

    def attach(obj: bpy.types.Object, bone: str) -> None:
        _bone_part(obj, armature, bone, objects)

    torso = add_cone("rootling_tapered_trunk", (0.0, 0.0, 1.05), 0.42, 0.31, 0.86, mats["bark"], 9)
    attach(torso, "spine")
    chest_plate = add_cube("rootling_split_bark_chest", (0.0, -0.30, 1.24), (0.42, 0.10, 0.42), mats["bark_dark"], 0.07)
    attach(chest_plate, "chest")
    pelvis = add_ico("rootling_knotted_hip", (0.0, 0.02, 0.72), (0.36, 0.26, 0.24), mats["bark_dark"])
    attach(pelvis, "pelvis")
    head = add_ico("rootling_carved_head", (0.0, -0.01, 1.72), (0.34, 0.29, 0.34), mats["bark"])
    attach(head, "head")
    jaw = add_cube("rootling_bark_jaw", (0.0, -0.30, 1.59), (0.28, 0.13, 0.12), mats["bark_dark"], 0.035)
    attach(jaw, "head")

    for side in (-1, 1):
        eye = add_ico(f"rootling_eye_{side}", (0.115 * side, -0.285, 1.77), (0.045, 0.025, 0.060), mats["sap"])
        attach(eye, "head")
        brow = add_leaf(
            f"rootling_brow_{side}",
            (0.115 * side, -0.305, 1.86),
            (0.15, 0.035, 0.055),
            mats["bark_dark"],
            (0.12, -0.35 * side, 0.08 * side),
        )
        attach(brow, "head")
        branch = add_cone(
            f"rootling_crown_branch_{side}",
            (0.23 * side, 0.0, 2.03),
            0.075,
            0.045,
            0.58,
            mats["bark_light"],
            7,
            (0.0, 0.34 * side, 0.0),
        )
        attach(branch, "head")
        crown_leaf = add_leaf(
            f"rootling_crown_leaf_{side}",
            (0.34 * side, -0.005, 2.25),
            (0.11, 0.055, 0.22),
            mats["leaf_light"],
            (0.18, 0.30 * side, -0.12 * side),
        )
        attach(crown_leaf, "head")

    _humanoid_limbs(armature, objects, mats["bark"], mats["bark_dark"], mats["bark_light"], 0.92)
    for side, bone in ((-1, "upper_arm.L"), (1, "upper_arm.R")):
        shoulder = add_leaf(
            f"rootling_leaf_mantle_{side}",
            (0.39 * side, -0.02, 1.38),
            (0.27, 0.12, 0.20),
            mats["leaf"],
            (0.18, 0.42 * side, 0.08 * side),
        )
        attach(shoulder, bone)
        thorn = add_cone(
            f"rootling_elbow_thorn_{side}",
            (0.57 * side, 0.02, 1.12),
            0.065,
            0.0,
            0.28,
            mats["bark_light"],
            7,
            (0.0, 0.80 * side, 0.0),
        )
        attach(thorn, f"forearm.{('L' if side < 0 else 'R')}")

    growth_ring = add_torus("rootling_growth_ring", (0.0, -0.39, 1.03), 0.17, 0.048, mats["sap"], (math.pi / 2, 0.0, 0.0))
    attach(growth_ring, "spine")
    heartwood = add_ico("rootling_heartwood", (0.0, -0.40, 1.03), (0.11, 0.035, 0.11), mats["bark_light"])
    attach(heartwood, "spine")
    for index, angle in enumerate((-0.78, 0.0, 0.78)):
        leaf = add_leaf(
            f"rootling_back_leaf_{index}",
            ((index - 1) * 0.24, 0.20, 1.28 + 0.05 * (index % 2)),
            (0.20, 0.07, 0.30),
            mats["leaf" if index != 1 else "leaf_light"],
            (0.24, angle, angle * 0.35),
        )
        attach(leaf, "chest")
    for side in (-1, 1):
        toe = add_leaf(
            f"rootling_root_toe_{side}",
            (0.22 * side, -0.30, 0.065),
            (0.20, 0.28, 0.08),
            mats["bark_dark"],
            (0.0, 0.0, 0.05 * side),
        )
        attach(toe, f"foot.{('L' if side < 0 else 'R')}")
    return BuiltModel(
        armature,
        objects,
        {
            "visualQuality": "premium-v2",
            "modelRevision": "rootling-thorn-scout-v2",
            "rigProfile": "premium-humanoid-v2",
            "animationProfile": "rootling-skirmisher-v2",
            "silhouette": "wiry thorn crown with asymmetric leaf mantle and rooted feet",
            "materialStory": "warm bark, charcoal heartwood, restrained leaf greens, and one amber sap focus",
        },
    )


def build_stonekin() -> BuiltModel:
    """Premium rune bulwark: an asymmetrical stack of chipped stone locked around a cyan core."""
    mats = {
        "basalt": MATERIALS.get("stonekin_basalt", "#34444C"),
        "slate": MATERIALS.get("stonekin_slate", "#53666D"),
        "edge": MATERIALS.get("stonekin_edge", "#82949A"),
        "deep": MATERIALS.get("stonekin_deep", "#1D292F"),
        "moss": MATERIALS.get("stonekin_moss", "#557044"),
        "rune": MATERIALS.get("stonekin_rune", "#73D2D8"),
    }
    armature = create_standard_armature("stonekin", 1.08)
    objects: list[bpy.types.Object] = []

    def attach(obj: bpy.types.Object, bone: str) -> None:
        _bone_part(obj, armature, bone, objects)

    pelvis = add_ico("stonekin_foundation", (0.0, 0.02, 0.70), (0.46, 0.32, 0.29), mats["deep"])
    attach(pelvis, "pelvis")
    torso = add_cube("stonekin_keystone_torso", (0.0, 0.01, 1.12), (0.72, 0.52, 0.75), mats["basalt"], 0.11)
    attach(torso, "spine")
    chest = add_cube("stonekin_breast_slab", (0.0, -0.31, 1.27), (0.56, 0.12, 0.42), mats["slate"], 0.055)
    attach(chest, "chest")
    left_chest = add_cube("stonekin_split_chest_left", (-0.22, -0.39, 1.30), (0.25, 0.07, 0.31), mats["edge"], 0.035)
    left_chest.rotation_euler[1] = -0.08
    attach(left_chest, "chest")
    waist = add_torus("stonekin_waist_bind", (0.0, -0.01, 0.82), 0.36, 0.065, mats["deep"])
    attach(waist, "pelvis")

    head = add_cube("stonekin_crag_head", (0.0, -0.01, 1.73), (0.48, 0.42, 0.42), mats["slate"], 0.085)
    head.rotation_euler[2] = -0.035
    attach(head, "head")
    crown = add_cube("stonekin_broken_crown", (-0.07, 0.0, 1.98), (0.40, 0.32, 0.17), mats["basalt"], 0.055)
    crown.rotation_euler[1] = -0.15
    attach(crown, "head")
    jaw = add_cube("stonekin_heavy_jaw", (0.03, -0.29, 1.58), (0.38, 0.17, 0.14), mats["deep"], 0.045)
    attach(jaw, "head")
    for side in (-1, 1):
        eye = add_cube(f"stonekin_eye_{side}", (0.105 * side, -0.235, 1.76), (0.065, 0.035, 0.035), mats["rune"], 0.012)
        eye.rotation_euler[2] = 0.08 * side
        attach(eye, "head")
        brow = add_cube(f"stonekin_brow_{side}", (0.12 * side, -0.26, 1.83), (0.17, 0.055, 0.055), mats["deep"], 0.02)
        brow.rotation_euler[2] = 0.16 * side
        attach(brow, "head")

    _humanoid_limbs(armature, objects, mats["slate"], mats["deep"], mats["edge"], 1.16)
    for side, letter in ((-1, "L"), (1, "R")):
        shoulder_scale = (0.40 if side < 0 else 0.34, 0.31, 0.31 if side < 0 else 0.26)
        shoulder = add_ico(f"stonekin_shoulder_boulder_{letter}", (0.45 * side, 0.0, 1.39), shoulder_scale, mats["edge" if side < 0 else "slate"])
        shoulder.rotation_euler[1] = 0.16 * side
        attach(shoulder, f"upper_arm.{letter}")
        pauldron = add_cube(f"stonekin_shoulder_plate_{letter}", (0.52 * side, -0.16, 1.44), (0.31, 0.13, 0.25), mats["basalt"], 0.04)
        pauldron.rotation_euler[1] = 0.18 * side
        attach(pauldron, f"upper_arm.{letter}")
        gauntlet = add_ico(f"stonekin_gauntlet_{letter}", (0.72 * side, -0.05, 1.00), (0.20, 0.17, 0.22), mats["basalt"])
        attach(gauntlet, f"forearm.{letter}")
        knuckle = add_cube(f"stonekin_knuckle_{letter}", (0.79 * side, -0.13, 0.92), (0.20, 0.14, 0.10), mats["edge"], 0.025)
        attach(knuckle, f"hand.{letter}")
        knee = add_cube(f"stonekin_knee_{letter}", (0.22 * side, -0.16, 0.38), (0.24, 0.17, 0.18), mats["slate"], 0.035)
        attach(knee, f"shin.{letter}")
        toe = add_cube(f"stonekin_slab_foot_{letter}", (0.22 * side, -0.24, 0.075), (0.31, 0.42, 0.17), mats["basalt"], 0.045)
        attach(toe, f"foot.{letter}")

    core_ring = add_torus("stonekin_rune_core_ring", (0.0, -0.405, 1.24), 0.17, 0.045, mats["rune"], (math.pi / 2, 0.0, 0.0))
    attach(core_ring, "chest")
    core = add_ico("stonekin_rune_core", (0.0, -0.42, 1.24), (0.10, 0.035, 0.14), mats["deep"])
    core.rotation_euler[2] = math.pi / 4
    attach(core, "chest")
    for index, (x, z, angle) in enumerate(((-0.16, 1.07, -0.62), (0.15, 1.08, 0.62), (0.0, 1.40, 0.0))):
        stroke = add_cube(f"stonekin_rune_stroke_{index}", (x, -0.425, z), (0.045, 0.025, 0.19), mats["rune"], 0.012)
        stroke.rotation_euler[1] = angle
        attach(stroke, "chest")
    for index, (x, z, size) in enumerate(((-0.30, 1.91, 0.12), (0.29, 1.61, 0.10), (-0.38, 0.92, 0.085))):
        chip = add_ico(f"stonekin_edge_chip_{index}", (x, -0.25, z), (size, 0.055, size * 0.72), mats["edge"])
        attach(chip, "head" if z > 1.5 else "spine")
    for index, (x, z) in enumerate(((-0.27, 1.50), (-0.20, 1.42), (0.30, 0.88))):
        moss = add_leaf(f"stonekin_moss_{index}", (x, -0.37, z), (0.11, 0.03, 0.075), mats["moss"], (0.0, 0.0, 0.35 * (-1 if x < 0 else 1)))
        attach(moss, "chest" if z > 1.0 else "spine")
    return BuiltModel(
        armature,
        objects,
        {
            "visualQuality": "premium-v2",
            "modelRevision": "stonekin-rune-bulwark-v2",
            "rigProfile": "premium-heavy-humanoid-v2",
            "animationProfile": "stonekin-juggernaut-v2",
            "silhouette": "broad asymmetrical crag shoulders, broken crown, massive fists, and slab feet",
            "materialStory": "charcoal basalt and cool slate with moss accents and one restrained cyan rune core",
        },
    )


def build_gloom_wolf() -> BuiltModel:
    """Premium shadow stalker: a low four-legged wedge with a swept tail and moonlit face mask."""
    mats = {
        "void": MATERIALS.get("gloom_wolf_void", "#211B35"),
        "fur": MATERIALS.get("gloom_wolf_fur", "#433367"),
        "fur_light": MATERIALS.get("gloom_wolf_fur_light", "#69518F"),
        "mask": MATERIALS.get("gloom_wolf_mask", "#171827"),
        "moon": MATERIALS.get("gloom_wolf_moon", "#70D8D6"),
        "fang": MATERIALS.get("gloom_wolf_fang", "#DED5B9"),
    }
    armature = create_standard_armature("gloom_wolf", 0.87)
    objects: list[bpy.types.Object] = []

    def attach(obj: bpy.types.Object, bone: str) -> None:
        _bone_part(obj, armature, bone, objects)

    haunches = add_ico("gloom_wolf_haunches", (0.0, 0.16, 0.98), (0.64, 0.46, 0.47), mats["void"])
    attach(haunches, "pelvis")
    body = add_ico("gloom_wolf_long_body", (0.0, -0.01, 1.13), (0.67, 0.43, 0.47), mats["fur"])
    body.rotation_euler[0] = -0.08
    attach(body, "spine")
    chest = add_ico("gloom_wolf_deep_chest", (0.0, -0.15, 1.31), (0.52, 0.38, 0.46), mats["fur_light"])
    attach(chest, "chest")
    ruff = add_cone("gloom_wolf_neck_ruff", (0.0, -0.06, 1.47), 0.46, 0.28, 0.43, mats["void"], 9)
    attach(ruff, "neck")
    head = add_ico("gloom_wolf_wedge_head", (0.0, -0.18, 1.64), (0.43, 0.38, 0.32), mats["fur"])
    head.rotation_euler[0] = -0.10
    attach(head, "head")
    muzzle = add_cone("gloom_wolf_muzzle", (0.0, -0.49, 1.56), 0.25, 0.14, 0.42, mats["mask"], 7, (math.pi / 2, 0.0, 0.0))
    attach(muzzle, "head")
    nose = add_ico("gloom_wolf_nose", (0.0, -0.69, 1.56), (0.13, 0.08, 0.09), mats["void"])
    attach(nose, "head")

    for side, letter in ((-1, "L"), (1, "R")):
        eye = add_leaf(f"gloom_wolf_eye_{letter}", (0.13 * side, -0.47, 1.69), (0.085, 0.028, 0.045), mats["moon"], (0.0, 0.0, 0.08 * side))
        attach(eye, "head")
        cheek = add_leaf(f"gloom_wolf_face_mask_{letter}", (0.19 * side, -0.43, 1.57), (0.18, 0.055, 0.15), mats["mask"], (0.0, 0.0, -0.18 * side))
        attach(cheek, "head")
        ear_outer = add_cone(f"gloom_wolf_ear_{letter}", (0.25 * side, -0.10, 1.96), 0.17, 0.015, 0.52, mats["void"], 6, (0.0, 0.22 * side, 0.0))
        attach(ear_outer, "head")
        ear_inner = add_leaf(f"gloom_wolf_ear_inner_{letter}", (0.25 * side, -0.30, 1.91), (0.075, 0.025, 0.18), mats["fur_light"], (0.0, 0.0, -0.10 * side))
        attach(ear_inner, "head")
        fang = add_cone(f"gloom_wolf_fang_{letter}", (0.075 * side, -0.64, 1.45), 0.035, 0.0, 0.16, mats["fang"], 6)
        attach(fang, "head")

        front_upper = add_cylinder_between(f"gloom_wolf_front_upper_{letter}", (0.37 * side, -0.08, 1.27), (0.40 * side, -0.11, 0.78), 0.115, mats["fur"])
        attach(front_upper, f"upper_arm.{letter}")
        front_lower = add_cylinder_between(f"gloom_wolf_front_lower_{letter}", (0.40 * side, -0.11, 0.78), (0.39 * side, -0.15, 0.28), 0.092, mats["void"])
        attach(front_lower, f"forearm.{letter}")
        front_paw = add_cube(f"gloom_wolf_front_paw_{letter}", (0.39 * side, -0.27, 0.16), (0.25, 0.34, 0.14), mats["void"], 0.045)
        attach(front_paw, f"hand.{letter}")
        rear_upper = add_cylinder_between(f"gloom_wolf_rear_upper_{letter}", (0.30 * side, 0.12, 0.92), (0.34 * side, 0.10, 0.49), 0.145, mats["fur"])
        attach(rear_upper, f"thigh.{letter}")
        rear_lower = add_cylinder_between(f"gloom_wolf_rear_lower_{letter}", (0.34 * side, 0.10, 0.49), (0.31 * side, -0.02, 0.19), 0.105, mats["void"])
        attach(rear_lower, f"shin.{letter}")
        rear_paw = add_cube(f"gloom_wolf_rear_paw_{letter}", (0.31 * side, -0.15, 0.11), (0.28, 0.39, 0.15), mats["void"], 0.045)
        attach(rear_paw, f"foot.{letter}")
        claw = add_cone(f"gloom_wolf_claw_{letter}", (0.39 * side, -0.46, 0.13), 0.035, 0.0, 0.18, mats["fang"], 6, (math.pi / 2, 0.0, 0.0))
        attach(claw, f"hand.{letter}")

    for index, (x, z, angle) in enumerate(((-0.38, 1.52, -0.62), (0.0, 1.57, 0.0), (0.38, 1.47, 0.62))):
        tuft = add_cone(f"gloom_wolf_ruff_tuft_{index}", (x, 0.02, z), 0.15, 0.0, 0.40, mats["fur_light"], 6, (0.0, angle, 0.0))
        attach(tuft, "chest")
    for index, (x, z) in enumerate(((-0.24, 1.44), (0.0, 1.50), (0.24, 1.41))):
        mark = add_leaf(f"gloom_wolf_moon_mark_{index}", (x, -0.49, z), (0.075, 0.025, 0.11), mats["fur_light"], (0.0, 0.0, 0.25 * index))
        attach(mark, "chest")

    tail_points = ((0.42, 0.16, 1.02), (0.74, 0.16, 1.24), (0.96, 0.10, 1.48), (1.08, -0.02, 1.30))
    for index, (start, end) in enumerate(zip(tail_points, tail_points[1:])):
        segment = add_cylinder_between(f"gloom_wolf_tail_{index}", start, end, 0.13 - index * 0.025, mats["fur" if index < 2 else "fur_light"], 7)
        attach(segment, "pelvis")
    tail_tip = add_leaf("gloom_wolf_tail_tip", tail_points[-1], (0.20, 0.14, 0.25), mats["void"], (0.2, -0.35, 0.15))
    attach(tail_tip, "pelvis")
    return BuiltModel(
        armature,
        objects,
        {
            "visualQuality": "premium-v2",
            "modelRevision": "gloom-wolf-shadow-stalker-v2",
            "rigProfile": "premium-quadruped-mapped-v2",
            "animationProfile": "gloom-wolf-pouncer-v2",
            "silhouette": "low four-legged wedge, tall alert ears, deep neck ruff, and a long swept tail",
            "materialStory": "near-black violet masses, one readable mid-violet plane, moon-cyan eyes, and tiny ivory fangs",
        },
    )


def build_fungal_brute() -> BuiltModel:
    """Premium spore bruiser: huge layered cap, gilled face, root feet, and clustered shoulder buds."""
    mats = {
        "stem": MATERIALS.get("fungal_stem", "#8C8759"),
        "stem_light": MATERIALS.get("fungal_stem_light", "#B9AE72"),
        "gill": MATERIALS.get("fungal_gill", "#E1CCA2"),
        "cap": MATERIALS.get("fungal_cap", "#A93F5B"),
        "cap_dark": MATERIALS.get("fungal_cap_dark", "#6E2945"),
        "spore": MATERIALS.get("fungal_spore", "#F0D78A"),
        "root": MATERIALS.get("fungal_root", "#303A2C"),
    }
    armature = create_standard_armature("fungal_brute", 1.10)
    objects: list[bpy.types.Object] = []

    def attach(obj: bpy.types.Object, bone: str) -> None:
        _bone_part(obj, armature, bone, objects)

    pelvis = add_ico("fungal_brute_knotted_base", (0.0, 0.04, 0.70), (0.43, 0.31, 0.27), mats["root"])
    attach(pelvis, "pelvis")
    body = add_cone("fungal_brute_stem_body", (0.0, 0.0, 1.08), 0.50, 0.38, 0.86, mats["stem"], 9)
    attach(body, "spine")
    belly = add_ico("fungal_brute_belly", (0.0, -0.29, 1.03), (0.39, 0.15, 0.35), mats["stem_light"])
    attach(belly, "spine")
    chest_collar = add_torus("fungal_brute_collar", (0.0, -0.01, 1.42), 0.37, 0.07, mats["root"])
    attach(chest_collar, "chest")

    face = add_ico("fungal_brute_stalk_face", (0.0, -0.04, 1.56), (0.34, 0.27, 0.31), mats["gill"])
    attach(face, "head")
    jaw = add_cube("fungal_brute_heavy_jaw", (0.0, -0.28, 1.45), (0.28, 0.13, 0.12), mats["root"], 0.04)
    attach(jaw, "head")
    for side in (-1, 1):
        eye = add_leaf(f"fungal_brute_eye_{side}", (0.105 * side, -0.275, 1.61), (0.06, 0.025, 0.042), mats["cap_dark"], (0.0, 0.0, 0.10 * side))
        attach(eye, "head")
        brow = add_leaf(f"fungal_brute_brow_{side}", (0.11 * side, -0.285, 1.69), (0.13, 0.025, 0.045), mats["root"], (0.0, 0.0, -0.13 * side))
        attach(brow, "head")

    undercap = add_cone("fungal_brute_gill_disc", (0.0, 0.0, 1.75), 0.61, 0.72, 0.16, mats["gill"], 12)
    attach(undercap, "head")
    brim = add_cone("fungal_brute_cap_brim", (0.0, 0.0, 1.84), 0.63, 0.78, 0.18, mats["cap_dark"], 12)
    attach(brim, "head")
    crown = add_cone("fungal_brute_cap_crown", (-0.05, 0.01, 1.99), 0.74, 0.18, 0.36, mats["cap"], 12)
    attach(crown, "head")
    crown_top = add_ico("fungal_brute_cap_peak", (-0.12, 0.01, 2.17), (0.30, 0.23, 0.14), mats["cap"])
    attach(crown_top, "head")

    for index, (x, z, size) in enumerate(((-0.43, 1.99, 0.11), (-0.18, 2.14, 0.08), (0.10, 2.10, 0.09), (0.35, 1.98, 0.10), (0.52, 1.88, 0.07))):
        spot = add_ico(f"fungal_brute_cap_spot_{index}", (x, -0.30, z), (size, 0.035, size * 0.72), mats["spore"])
        attach(spot, "head")
    for index, x in enumerate((-0.40, -0.21, 0.0, 0.21, 0.40)):
        gill = add_cube(f"fungal_brute_gill_rib_{index}", (x, -0.43, 1.75), (0.035, 0.035, 0.18 - abs(x) * 0.13), mats["stem_light"], 0.012)
        gill.rotation_euler[1] = x * 0.55
        attach(gill, "head")

    _humanoid_limbs(armature, objects, mats["stem"], mats["root"], mats["cap_dark"], 1.28)
    for side, letter in ((-1, "L"), (1, "R")):
        shoulder = add_ico(f"fungal_brute_shoulder_{letter}", (0.43 * side, 0.0, 1.38), (0.31, 0.24, 0.27), mats["stem_light"])
        attach(shoulder, f"upper_arm.{letter}")
        cuff = add_torus(f"fungal_brute_cuff_{letter}", (0.65 * side, -0.03, 1.05), 0.13, 0.045, mats["root"], (0.0, math.pi / 2, 0.0))
        attach(cuff, f"forearm.{letter}")
        fist = add_ico(f"fungal_brute_spore_fist_{letter}", (0.78 * side, -0.06, 0.92), (0.18, 0.15, 0.20), mats["cap_dark"])
        attach(fist, f"hand.{letter}")
        root_foot = add_leaf(f"fungal_brute_root_foot_{letter}", (0.22 * side, -0.28, 0.07), (0.25, 0.38, 0.10), mats["root"], (0.0, 0.0, 0.06 * side))
        attach(root_foot, f"foot.{letter}")

    for index, (x, z, radius, cap_mat) in enumerate(((-0.45, 1.52, 0.19, mats["cap"]), (0.48, 1.30, 0.15, mats["cap_dark"]), (0.39, 1.51, 0.11, mats["cap"]))):
        stalk = add_cylinder_between(f"fungal_brute_bud_stalk_{index}", (x, 0.10, z - 0.16), (x, 0.08, z), radius * 0.34, mats["gill"], 7)
        attach(stalk, "chest")
        bud = add_cone(f"fungal_brute_shoulder_bud_{index}", (x, 0.06, z + 0.06), radius * 0.65, radius, radius * 0.55, cap_mat, 8)
        attach(bud, "chest")
    for index, (x, z) in enumerate(((-0.20, 1.14), (0.0, 0.96), (0.22, 1.18))):
        wart = add_torus(f"fungal_brute_belly_spore_{index}", (x, -0.45, z), 0.065, 0.025, mats["spore"], (math.pi / 2, 0.0, 0.0))
        attach(wart, "spine")
    return BuiltModel(
        armature,
        objects,
        {
            "visualQuality": "premium-v2",
            "modelRevision": "fungal-brute-spore-bruiser-v2",
            "rigProfile": "premium-heavy-humanoid-v2",
            "animationProfile": "fungal-brute-brawler-v2",
            "silhouette": "oversized layered cap, barrel stalk body, knotted arms, root feet, and asymmetric shoulder buds",
            "materialStory": "earthy olive stem, wine-red cap, dark root masses, warm gills, and sparse golden spores",
        },
    )

def _boss_rock(
    parts: list[bpy.types.Object],
    name: str,
    location: tuple[float, float, float],
    scale: tuple[float, float, float],
    material: bpy.types.Material,
    bone: str,
    *,
    rotation: tuple[float, float, float] = (0.0, 0.0, 0.0),
    subdivision: int = 2,
) -> bpy.types.Object:
    rock = add_ico_sphere(name, location, scale, material, subdivisions=subdivision, rotation=rotation)
    parts.append(_tag(rock, bone))
    return rock


def _boss_spike(
    parts: list[bpy.types.Object],
    name: str,
    location: tuple[float, float, float],
    radius: float,
    depth: float,
    material: bpy.types.Material,
    bone: str,
    rotation: tuple[float, float, float],
) -> bpy.types.Object:
    spike = add_pointed_cone(name, location, radius, depth, material, vertices=8, rotation=rotation)
    parts.append(_tag(spike, bone))
    return spike


def build_ancient_golem() -> BuiltModel:
    """Heartstone colossus: massive strata, luminous runes, roots, and slam fists."""
    stone = make_material("golem basalt", "#39464B", roughness=0.88)
    stone_light = make_material("golem cut facets", "#647277", roughness=0.80)
    stone_dark = make_material("golem deep seams", "#202A2E", roughness=0.96)
    moss = make_material("golem moss", "#4E713D", roughness=0.92)
    moss_light = make_material("golem new growth", "#8DAE55", roughness=0.88)
    rune = make_material("golem heartstone", "#72E6D0", roughness=0.24, emission=0.46)
    rune_hot = make_material("golem rune core", "#D4FFF1", roughness=0.16, emission=0.72)
    root = make_material("golem old roots", "#5A3F2A", roughness=0.96)
    armature = create_standard_armature("ancient_golem", scale=1.18)
    parts: list[bpy.types.Object] = []

    # A three-tier geological trunk keeps the heartstone readable at runtime scale.
    _boss_rock(parts, "golem pelvis monolith", (0.0, 0.02, 0.78), (0.58, 0.36, 0.39), stone_dark, "pelvis", rotation=(0.04, 0.08, 0.02))
    _boss_rock(parts, "golem lower strata", (0.0, 0.0, 1.10), (0.68, 0.40, 0.43), stone, "spine", rotation=(-0.03, -0.07, 0.01))
    _boss_rock(parts, "golem crown strata", (0.0, 0.03, 1.48), (0.78, 0.43, 0.49), stone, "chest", rotation=(0.03, 0.04, -0.02))
    chest_back = add_cube("golem back slab", (0.0, 0.23, 1.49), (0.98, 0.20, 0.66), stone_dark, rotation=(0.04, 0.0, 0.0))
    parts.append(_tag(chest_back, "chest"))
    for index, (x, z, sx, rot) in enumerate(((-0.42, 1.52, 0.32, -0.12), (0.42, 1.52, 0.32, 0.12), (-0.24, 1.23, 0.27, 0.08), (0.24, 1.23, 0.27, -0.08))):
        _boss_rock(parts, f"golem chest plate {index}", (x, -0.30, z), (sx, 0.15, 0.24), stone_light, "chest", rotation=(0.0, rot, rot))
    heart = _boss_rock(parts, "golem exposed heartstone", (0.0, -0.44, 1.47), (0.24, 0.10, 0.28), rune, "armor_socket", rotation=(0.0, 0.0, math.radians(45)))
    heart_ring = add_torus("golem heartstone bezel", (0.0, -0.45, 1.47), 0.31, 0.045, stone_dark, major_segments=12, minor_segments=4, rotation=(math.radians(90), 0.0, 0.0))
    parts.append(_tag(heart_ring, "armor_socket"))
    heart_core = add_ico_sphere("golem heartstone core", (0.0, -0.52, 1.47), (0.09, 0.055, 0.11), rune_hot, subdivisions=2)
    parts.append(_tag(heart_core, "armor_socket"))

    # Layered shoulders and asymmetric boulder limbs communicate extreme mass.
    for side, sign in (("L", -1), ("R", 1)):
        upper = f"upper_arm.{side}"
        forearm = f"forearm.{side}"
        hand = f"hand.{side}"
        _boss_rock(parts, f"golem shoulder cap {side}", (0.68 * sign, 0.01, 1.52), (0.43, 0.43, 0.38), stone_light, upper, rotation=(0.08, 0.05 * sign, 0.12 * sign))
        _boss_rock(parts, f"golem shoulder understone {side}", (0.58 * sign, 0.05, 1.38), (0.36, 0.32, 0.31), stone_dark, upper)
        _boss_rock(parts, f"golem upper boulder {side}", (0.76 * sign, -0.02, 1.25), (0.29, 0.29, 0.37), stone, upper, rotation=(0.05, 0.15 * sign, 0.08 * sign))
        _boss_rock(parts, f"golem elbow boulder {side}", (0.88 * sign, -0.08, 1.02), (0.28, 0.28, 0.30), stone_light, forearm)
        _boss_rock(parts, f"golem forearm boulder {side}", (0.95 * sign, -0.09, 0.84), (0.34, 0.34, 0.40), stone, forearm, rotation=(0.04, 0.0, -0.09 * sign))
        _boss_rock(parts, f"golem slam fist {side}", (1.02 * sign, -0.13, 0.62), (0.43, 0.40, 0.36), stone_dark, hand)
        for knuckle in range(3):
            _boss_rock(parts, f"golem knuckle {side} {knuckle}", ((0.82 + knuckle * 0.12) * sign, -0.42, 0.66), (0.11, 0.10, 0.10), stone_light, hand, subdivision=1)
        seam = add_torus(f"golem forearm seam {side}", (0.93 * sign, -0.10, 0.96), 0.27, 0.035, rune, major_segments=10, minor_segments=4, rotation=(0.0, math.radians(12 * sign), 0.0))
        parts.append(_tag(seam, forearm))

    # Pillar legs, split feet, and toe strata anchor the slam silhouette.
    for side, sign in (("L", -1), ("R", 1)):
        thigh, shin, foot = f"thigh.{side}", f"shin.{side}", f"foot.{side}"
        _boss_rock(parts, f"golem hip {side}", (0.35 * sign, 0.03, 0.68), (0.36, 0.34, 0.34), stone, thigh)
        _boss_rock(parts, f"golem knee {side}", (0.38 * sign, -0.08, 0.43), (0.32, 0.30, 0.27), stone_light, shin)
        _boss_rock(parts, f"golem shin pillar {side}", (0.39 * sign, 0.0, 0.25), (0.30, 0.31, 0.30), stone_dark, shin)
        foot_rock = add_cube(f"golem split foot {side}", (0.40 * sign, -0.17, 0.12), (0.56, 0.65, 0.22), stone, rotation=(0.0, 0.04 * sign, 0.0))
        parts.append(_tag(foot_rock, foot))
        for toe in range(2):
            toe_rock = add_cube(f"golem toe {side} {toe}", ((0.31 + toe * 0.18) * sign, -0.47, 0.12), (0.18, 0.27, 0.14), stone_light, rotation=(0.0, 0.0, 0.03 * sign))
            parts.append(_tag(toe_rock, foot))

    # Low brow and trilithon crest keep the face ancient rather than humanoid-cute.
    _boss_rock(parts, "golem head", (0.0, -0.01, 2.03), (0.46, 0.39, 0.36), stone, "head", rotation=(0.02, 0.0, 0.0))
    jaw = add_cube("golem jaw ledge", (0.0, -0.34, 1.91), (0.62, 0.24, 0.20), stone_dark, rotation=(0.06, 0.0, 0.0))
    brow = add_cube("golem monolithic brow", (0.0, -0.38, 2.10), (0.72, 0.18, 0.15), stone_light, rotation=(0.0, 0.0, -0.02))
    parts.extend((_tag(jaw, "head"), _tag(brow, "head")))
    for side, sign in (("L", -1), ("R", 1)):
        eye = add_ico_sphere(f"golem rune eye {side}", (0.17 * sign, -0.50, 2.07), (0.07, 0.045, 0.055), rune_hot, subdivisions=2)
        parts.append(_tag(eye, "head"))
        _boss_spike(parts, f"golem crest pillar {side}", (0.28 * sign, 0.0, 2.38), 0.13, 0.48, stone_dark, "helmet_socket", (0.0, -0.08 * sign, 0.0))
    _boss_spike(parts, "golem central crest", (0.0, 0.01, 2.45), 0.14, 0.58, stone_light, "helmet_socket", (0.0, 0.0, 0.0))

    # Restrained moss and roots break up rock planes without obscuring landmarks.
    for index, (x, y, z, sx, sz, bone) in enumerate((
        (-0.40, -0.30, 1.70, 0.24, 0.13, "chest"), (0.48, -0.24, 1.31, 0.19, 0.12, "chest"),
        (-0.62, -0.18, 1.47, 0.18, 0.13, "upper_arm.L"), (0.92, -0.17, 0.94, 0.16, 0.11, "forearm.R"),
        (-0.18, -0.32, 2.27, 0.17, 0.12, "head"), (0.35, -0.27, 2.25, 0.13, 0.10, "head"),
    )):
        leaf = add_leaf(f"golem moss patch {index}", (x, y, z), (sx, 0.045, sz), moss if index % 2 == 0 else moss_light, rotation=(0.0, 0.0, 0.18 * (-1 if x < 0 else 1)))
        parts.append(_tag(leaf, bone))
    for index, (x, z, angle) in enumerate(((-0.52, 1.45, -0.18), (-0.43, 1.20, 0.10), (0.58, 1.42, 0.16), (0.50, 1.14, -0.12))):
        vine = add_cylinder(f"golem root vine {index}", (x, -0.43, z), 0.035, 0.62, root, vertices=8, rotation=(0.0, angle, 0.0))
        parts.append(_tag(vine, "chest"))
    for side, sign in (("L", -1), ("R", 1)):
        rune_stud = add_ico_sphere(f"golem shoulder rune {side}", (0.68 * sign, -0.40, 1.56), (0.085, 0.045, 0.11), rune, subdivisions=2)
        parts.append(_tag(rune_stud, f"upper_arm.{side}"))

    attach_meshes(armature, parts)
    return BuiltModel(armature, parts, {
        "unique_attack": "ground_slam",
        "modelRevision": "heartstone-colossus-v2",
        "rigProfile": "premium-heavy-humanoid-v2",
        "animationProfile": "ancient-golem-ground-slam-v2",
        "silhouetteLandmarks": ["trilithon crest", "exposed heartstone", "paired slam fists", "split pillar feet"],
        "surfaceLanguage": "faceted basalt strata, restrained moss, cyan rune seams",
        "visualQuality": "premium-v2",
    })


def build_thorn_matriarch() -> BuiltModel:
    """Regal briar sovereign with blossom crown, rooted gown, and cage-forming arms."""
    bark = make_material("matriarch heartwood", "#4D3428", roughness=0.92)
    bark_light = make_material("matriarch cut bark", "#806044", roughness=0.86)
    bark_dark = make_material("matriarch bark shadow", "#251F22", roughness=0.98)
    leaf_dark = make_material("matriarch deep leaf", "#234431", roughness=0.90)
    leaf = make_material("matriarch emerald leaf", "#467B45", roughness=0.84)
    petal = make_material("matriarch blood blossom", "#A73650", roughness=0.76)
    petal_light = make_material("matriarch petal edge", "#E68A9E", roughness=0.66)
    thorn = make_material("matriarch ivory thorns", "#D8C58D", roughness=0.75)
    pollen = make_material("matriarch pollen heart", "#FFD66B", roughness=0.30, emission=0.34)
    armature = create_standard_armature("thorn_matriarch", scale=1.17)
    parts: list[bpy.types.Object] = []

    # A tiered root-gown gives a broad royal base without reading as a generic dress.
    skirt = add_pointed_cone("matriarch root gown", (0.0, 0.05, 0.70), 0.63, 1.22, bark_dark, vertices=12)
    parts.append(_tag(skirt, "pelvis"))
    for index, (angle, radius, z, sx, sz) in enumerate((
        (-1.05, 0.45, 0.62, 0.28, 0.62), (-0.62, 0.52, 0.54, 0.31, 0.72), (-0.22, 0.54, 0.50, 0.33, 0.77),
        (0.20, 0.54, 0.50, 0.33, 0.77), (0.62, 0.52, 0.54, 0.31, 0.72), (1.05, 0.45, 0.62, 0.28, 0.62),
    )):
        x = math.sin(angle) * radius
        y = -0.18 - math.cos(angle) * 0.16
        panel = add_leaf(f"matriarch gown leaf {index}", (x, y, z), (sx, 0.07, sz), leaf_dark if index % 2 == 0 else leaf, rotation=(0.0, 0.0, -angle * 0.28))
        parts.append(_tag(panel, "pelvis"))
    waist = add_torus("matriarch living waist", (0.0, -0.01, 1.12), 0.33, 0.075, bark_light, major_segments=12, minor_segments=5)
    parts.append(_tag(waist, "spine"))
    _boss_rock(parts, "matriarch stem torso", (0.0, 0.0, 1.40), (0.34, 0.25, 0.48), bark, "chest")
    corset = add_cube("matriarch split bark corset", (0.0, -0.26, 1.38), (0.48, 0.15, 0.56), bark_light, rotation=(0.0, 0.0, math.radians(45)))
    parts.append(_tag(corset, "armor_socket"))
    sternum = add_ico_sphere("matriarch pollen jewel", (0.0, -0.40, 1.49), (0.10, 0.06, 0.14), pollen, subdivisions=2)
    parts.append(_tag(sternum, "armor_socket"))

    # Root toes radiate from the gown and visually foreshadow the thorn cage.
    for index, angle in enumerate((-1.15, -0.76, -0.38, 0.0, 0.38, 0.76, 1.15)):
        x = math.sin(angle) * 0.64
        y = -0.28 - math.cos(angle) * 0.20
        root_toe = add_pointed_cone(f"matriarch radial root {index}", (x, y, 0.15), 0.15, 0.72, bark, vertices=8, rotation=(0.0, 0.95 * angle, 0.0))
        parts.append(_tag(root_toe, "pelvis"))
        _boss_spike(parts, f"matriarch root thorn {index}", (x * 1.25, y - 0.10, 0.28), 0.055, 0.30, thorn, "pelvis", (0.30, angle * 0.30, angle * 0.25))

    # Vine arms terminate in blossom palms and carry readable cage-thorn fans.
    for side, sign in (("L", -1), ("R", 1)):
        upper_bone, fore_bone, hand_bone = f"upper_arm.{side}", f"forearm.{side}", f"hand.{side}"
        shoulder_leaf = add_leaf(f"matriarch mantle {side}", (0.40 * sign, -0.01, 1.61), (0.42, 0.10, 0.25), leaf_dark, rotation=(0.0, 0.0, 0.42 * sign))
        parts.append(_tag(shoulder_leaf, upper_bone))
        upper = add_cylinder_between(f"matriarch upper vine {side}", (0.30 * sign, 0.0, 1.48), (0.62 * sign, -0.06, 1.24), 0.10, bark, vertices=9)
        fore = add_cylinder_between(f"matriarch fore vine {side}", (0.60 * sign, -0.06, 1.25), (0.84 * sign, -0.13, 0.98), 0.075, bark_light, vertices=8)
        parts.extend((_tag(upper, upper_bone), _tag(fore, fore_bone)))
        palm = add_ico_sphere(f"matriarch blossom palm {side}", (0.88 * sign, -0.17, 0.92), (0.16, 0.10, 0.16), petal, subdivisions=2)
        parts.append(_tag(palm, hand_bone))
        for fan in range(3):
            fan_z = 0.86 + fan * 0.17
            fan_x = (0.90 + fan * 0.08) * sign
            fan_leaf = add_leaf(f"matriarch cage leaf {side} {fan}", (fan_x, -0.12, fan_z), (0.20, 0.06, 0.28), leaf, rotation=(0.0, 0.0, (0.48 - fan * 0.18) * sign))
            parts.append(_tag(fan_leaf, hand_bone if fan == 0 else fore_bone))
            _boss_spike(parts, f"matriarch cage thorn {side} {fan}", ((1.02 + fan * 0.08) * sign, -0.17, fan_z + 0.11), 0.055, 0.38, thorn, hand_bone if fan == 0 else fore_bone, (0.12, 0.42 * sign, 0.52 * sign))

    # Flower-mask head and radial crown establish a unique boss portrait silhouette.
    _boss_rock(parts, "matriarch seed mask", (0.0, -0.04, 1.97), (0.29, 0.23, 0.31), bark_dark, "head")
    for side, sign in (("L", -1), ("R", 1)):
        eye = add_ico_sphere(f"matriarch pollen eye {side}", (0.105 * sign, -0.265, 2.00), (0.045, 0.035, 0.055), pollen, subdivisions=2)
        parts.append(_tag(eye, "head"))
    for index in range(10):
        angle = (math.tau * index / 10.0) + math.pi / 2.0
        x = math.cos(angle) * 0.38
        z = 2.02 + math.sin(angle) * 0.38
        outer = add_leaf(f"matriarch crown petal {index}", (x, 0.0, z), (0.20, 0.07, 0.34), petal if index % 2 == 0 else petal_light, rotation=(0.0, 0.0, angle - math.pi / 2.0))
        parts.append(_tag(outer, "helmet_socket"))
    crown_core = add_ico_sphere("matriarch crown heart", (0.0, -0.08, 2.10), (0.24, 0.13, 0.24), pollen, subdivisions=2)
    parts.append(_tag(crown_core, "helmet_socket"))
    for index, angle in enumerate((-0.82, -0.42, 0.0, 0.42, 0.82)):
        _boss_spike(parts, f"matriarch crown thorn {index}", (math.sin(angle) * 0.42, 0.02, 2.36 + math.cos(angle) * 0.10), 0.055, 0.42, thorn, "helmet_socket", (0.0, angle, angle * 0.28))

    # Two restrained back-vines complete the circular cage motif without clutter.
    for side, sign in (("L", -1), ("R", 1)):
        points = ((0.28 * sign, 0.18, 1.50), (0.62 * sign, 0.25, 1.74), (0.80 * sign, 0.20, 1.98))
        for segment in range(2):
            vine = add_cylinder_between(f"matriarch halo vine {side} {segment}", points[segment], points[segment + 1], 0.045, bark_light, vertices=8)
            parts.append(_tag(vine, "chest"))
        _boss_spike(parts, f"matriarch halo tip {side}", points[-1], 0.06, 0.36, thorn, "chest", (0.0, -0.45 * sign, 0.50 * sign))

    attach_meshes(armature, parts)
    return BuiltModel(armature, parts, {
        "unique_attack": "thorn_cage",
        "modelRevision": "briar-sovereign-v2",
        "rigProfile": "premium-rooted-caster-v2",
        "animationProfile": "thorn-matriarch-thorn-cage-v2",
        "silhouetteLandmarks": ["ten-petal crown", "rooted royal gown", "paired cage-thorn fans", "briar halo"],
        "surfaceLanguage": "layered heartwood, emerald leaves, blood-petal crown, ivory thorns",
        "visualQuality": "premium-v2",
    })


def build_ember_wyrm() -> BuiltModel:
    """Winged furnace wyrm with plated belly, sweeping tail, and flame-sweep profile."""
    scale_dark = make_material("wyrm obsidian scale", "#241F24", roughness=0.82, metallic=0.08)
    scale = make_material("wyrm crimson scale", "#7D2F2B", roughness=0.76)
    scale_hot = make_material("wyrm ember scale", "#C9532F", roughness=0.68)
    plate = make_material("wyrm belly gold", "#D39A49", roughness=0.62, metallic=0.16)
    horn = make_material("wyrm charred horn", "#49372D", roughness=0.86)
    membrane = make_material("wyrm wing membrane", "#9A4039", roughness=0.78)
    membrane_hot = make_material("wyrm membrane ember", "#DF6B45", roughness=0.68)
    flame = make_material("wyrm flame", "#FF9B38", roughness=0.24, emission=0.55)
    flame_core = make_material("wyrm flame core", "#FFF0A0", roughness=0.16, emission=0.82)
    armature = create_standard_armature("ember_wyrm", scale=1.13)
    parts: list[bpy.types.Object] = []

    # Serpentine body volumes overlap as purposeful armour rings rather than a robe.
    _boss_rock(parts, "wyrm haunch", (0.0, 0.08, 0.73), (0.48, 0.34, 0.42), scale_dark, "pelvis", rotation=(0.04, 0.0, 0.0))
    _boss_rock(parts, "wyrm furnace belly", (0.0, 0.02, 1.08), (0.47, 0.34, 0.47), scale, "spine", rotation=(-0.04, 0.0, 0.0))
    _boss_rock(parts, "wyrm ribcage", (0.0, 0.03, 1.45), (0.55, 0.37, 0.48), scale_dark, "chest", rotation=(0.04, 0.0, 0.0))
    for index in range(6):
        z = 0.84 + index * 0.15
        belly = add_leaf(f"wyrm belly plate {index}", (0.0, -0.34 - index * 0.008, z), (0.24 + index * 0.015, 0.055, 0.13), plate if index % 2 == 0 else scale_hot, rotation=(0.0, 0.0, 0.0))
        parts.append(_tag(belly, "spine" if index < 3 else "chest"))
    for side, sign in (("L", -1), ("R", 1)):
        for index in range(3):
            body_scale = add_leaf(f"wyrm flank scale {side} {index}", ((0.34 + 0.07 * index) * sign, -0.25, 1.17 + 0.16 * index), (0.20, 0.055, 0.20), scale_hot if index == 1 else scale, rotation=(0.0, 0.0, 0.32 * sign))
            parts.append(_tag(body_scale, "chest"))

    # Digitigrade legs and triple claws keep the grounded monster silhouette clear.
    for side, sign in (("L", -1), ("R", 1)):
        thigh, shin, foot = f"thigh.{side}", f"shin.{side}", f"foot.{side}"
        _boss_rock(parts, f"wyrm thigh {side}", (0.31 * sign, 0.05, 0.59), (0.28, 0.27, 0.34), scale, thigh)
        _boss_rock(parts, f"wyrm hock {side}", (0.36 * sign, -0.02, 0.33), (0.22, 0.22, 0.26), scale_dark, shin)
        foot_plate = add_cube(f"wyrm foot {side}", (0.38 * sign, -0.20, 0.14), (0.42, 0.52, 0.18), scale, rotation=(0.0, 0.06 * sign, 0.0))
        parts.append(_tag(foot_plate, foot))
        for claw in range(3):
            claw_x = (0.25 + claw * 0.13) * sign
            _boss_spike(parts, f"wyrm foot claw {side} {claw}", (claw_x, -0.50, 0.14), 0.045, 0.28, horn, foot, (math.radians(72), 0.0, 0.0))

    # Broad bat-like wings use bone spars plus three faceted membrane panels each.
    for side, sign in (("L", -1), ("R", 1)):
        wing_bone = f"upper_arm.{side}"
        fore_bone = f"forearm.{side}"
        root_point = (0.36 * sign, 0.13, 1.56)
        elbow = (0.83 * sign, 0.16, 1.91)
        high_tip = (1.33 * sign, 0.12, 2.28)
        low_tip = (1.27 * sign, 0.10, 1.34)
        spar_a = add_cylinder_between(f"wyrm wing spar high {side}", root_point, elbow, 0.065, horn, vertices=8)
        spar_b = add_cylinder_between(f"wyrm wing spar tip {side}", elbow, high_tip, 0.055, horn, vertices=8)
        spar_c = add_cylinder_between(f"wyrm wing spar low {side}", elbow, low_tip, 0.052, horn, vertices=8)
        parts.extend((_tag(spar_a, wing_bone), _tag(spar_b, fore_bone), _tag(spar_c, fore_bone)))
        for index, (x, z, sx, sz, rot) in enumerate((
            (0.60, 1.68, 0.43, 0.45, 0.54), (0.93, 1.94, 0.47, 0.54, 0.72), (1.07, 1.55, 0.44, 0.53, -0.34),
        )):
            panel = add_leaf(f"wyrm wing membrane {side} {index}", (x * sign, 0.17, z), (sx, 0.065, sz), membrane_hot if index == 1 else membrane, rotation=(0.0, 0.0, rot * sign))
            parts.append(_tag(panel, wing_bone if index == 0 else fore_bone))
        _boss_spike(parts, f"wyrm wing hook {side}", high_tip, 0.06, 0.38, horn, fore_bone, (0.0, -0.62 * sign, 0.45 * sign))
        # Small grasping arm under each wing root.
        arm = add_cylinder_between(f"wyrm grasping arm {side}", (0.32 * sign, -0.18, 1.38), (0.62 * sign, -0.30, 1.13), 0.07, scale, vertices=8)
        parts.append(_tag(arm, wing_bone))
        for claw in range(2):
            _boss_spike(parts, f"wyrm hand claw {side} {claw}", ((0.65 + claw * 0.08) * sign, -0.33, 1.08 - claw * 0.05), 0.035, 0.22, horn, f"hand.{side}", (math.radians(64), 0.0, 0.20 * sign))

    # Long neck, horned dragon head, and visible jaws replace the old humanoid read.
    for index, (location, local_scale, bone) in enumerate((
        ((0.0, 0.02, 1.69), (0.35, 0.28, 0.30), "neck"),
        ((0.0, -0.02, 1.90), (0.31, 0.26, 0.29), "neck"),
        ((0.0, -0.08, 2.10), (0.36, 0.30, 0.30), "head"),
    )):
        _boss_rock(parts, f"wyrm neck scale {index}", location, local_scale, scale if index != 1 else scale_hot, bone)
    muzzle = add_cube("wyrm long muzzle", (0.0, -0.40, 2.05), (0.52, 0.50, 0.24), scale, rotation=(0.05, 0.0, 0.0))
    jaw = add_cube("wyrm lower jaw", (0.0, -0.43, 1.91), (0.44, 0.46, 0.12), scale_dark, rotation=(-0.10, 0.0, 0.0))
    parts.extend((_tag(muzzle, "head"), _tag(jaw, "head")))
    for side, sign in (("L", -1), ("R", 1)):
        eye = add_ico_sphere(f"wyrm furnace eye {side}", (0.16 * sign, -0.39, 2.15), (0.055, 0.04, 0.055), flame_core, subdivisions=2)
        parts.append(_tag(eye, "head"))
        _boss_spike(parts, f"wyrm crown horn {side}", (0.23 * sign, 0.0, 2.31), 0.075, 0.52, horn, "head", (0.0, -0.38 * sign, 0.20 * sign))
        cheek = add_leaf(f"wyrm cheek plate {side}", (0.24 * sign, -0.30, 2.01), (0.17, 0.05, 0.22), plate, rotation=(0.0, 0.0, 0.32 * sign))
        parts.append(_tag(cheek, "head"))
    for tooth_index, x in enumerate((-0.14, -0.05, 0.05, 0.14)):
        _boss_spike(parts, f"wyrm jaw tooth {tooth_index}", (x, -0.65, 1.95), 0.025, 0.14, horn, "head", (math.radians(180), 0.0, 0.0))
    # A reserved head-child socket scales this layered breath from hidden pilot to
    # full signature sweep without changing the fixed mesh/atlas contract.
    flame_tongue = add_pointed_cone("wyrm breath plume", (0.0, -0.88, 1.98), 0.17, 0.82, flame, vertices=10, rotation=(math.radians(78), 0.0, 0.0))
    flame_inner = add_pointed_cone("wyrm breath inner plume", (0.0, -0.78, 1.98), 0.095, 0.56, flame_core, vertices=9, rotation=(math.radians(78), 0.0, 0.0))
    flame_seed = add_ico_sphere("wyrm breath core", (0.0, -0.58, 1.99), (0.10, 0.13, 0.10), flame_core, subdivisions=2)
    parts.extend((
        _tag(flame_tongue, "helmet_socket"),
        _tag(flame_inner, "helmet_socket"),
        _tag(flame_seed, "helmet_socket"),
    ))

    # Segmented tail sweeps sideways to distinguish the Wyrm even in a still frame.
    tail_points = ((0.10, 0.16, 0.73), (0.45, 0.20, 0.61), (0.76, 0.16, 0.47), (1.02, 0.08, 0.34), (1.22, 0.0, 0.30))
    for index in range(len(tail_points) - 1):
        segment = add_cylinder_between(f"wyrm tail segment {index}", tail_points[index], tail_points[index + 1], 0.17 - index * 0.025, scale if index < 2 else scale_dark, vertices=9)
        parts.append(_tag(segment, "pelvis"))
        if index > 0:
            fin = add_leaf(f"wyrm tail fin {index}", (tail_points[index][0], tail_points[index][1], tail_points[index][2] + 0.13), (0.13, 0.04, 0.20), scale_hot, rotation=(0.0, 0.0, 0.25))
            parts.append(_tag(fin, "pelvis"))
    _boss_spike(parts, "wyrm tail barb", tail_points[-1], 0.09, 0.46, horn, "pelvis", (0.0, math.radians(72), 0.0))

    attach_meshes(armature, parts)
    return BuiltModel(armature, parts, {
        "unique_attack": "flame_sweep",
        "modelRevision": "furnace-wyrm-v2",
        "rigProfile": "premium-winged-wyrm-mapped-v2",
        "animationProfile": "ember-wyrm-flame-sweep-v2",
        "silhouetteLandmarks": ["faceted bat wings", "horned long muzzle", "gold furnace belly", "barbed sweeping tail"],
        "surfaceLanguage": "obsidian scales, crimson membranes, gold belly plates, restrained emissive flame",
        "visualQuality": "premium-v2",
    })


def build_void_knight() -> BuiltModel:
    """Horned abyss champion with layered black plate, torn cape, and void greatblade."""
    armor_dark = make_material("void knight black plate", "#20242E", roughness=0.54, metallic=0.62)
    armor = make_material("void knight gunmetal", "#41495C", roughness=0.48, metallic=0.70)
    edge = make_material("void knight silver edge", "#9EABC2", roughness=0.38, metallic=0.78)
    cloth = make_material("void knight cape", "#30233E", roughness=0.88)
    cloth_light = make_material("void knight cape lining", "#56356D", roughness=0.82)
    void = make_material("void knight abyss", "#7A3ED1", roughness=0.24, emission=0.44)
    void_hot = make_material("void knight void core", "#D8B5FF", roughness=0.16, emission=0.76)
    gold = make_material("void knight old gold", "#B88B49", roughness=0.54, metallic=0.54)
    leather = make_material("void knight grip", "#4A3028", roughness=0.90)
    armature = create_standard_armature("void_knight", scale=1.14)
    parts: list[bpy.types.Object] = []

    # Plate hierarchy: dark under-shell, overlapping chest facets, and a bright trim key.
    _boss_rock(parts, "void knight underbody", (0.0, 0.04, 1.22), (0.46, 0.30, 0.63), armor_dark, "spine")
    chest = add_cube("void knight breastplate", (0.0, -0.16, 1.47), (0.76, 0.30, 0.61), armor, rotation=(0.03, 0.0, 0.0))
    parts.append(_tag(chest, "armor_socket"))
    for index, (x, z, rot) in enumerate(((-0.21, 1.51, -0.12), (0.21, 1.51, 0.12), (-0.16, 1.25, 0.08), (0.16, 1.25, -0.08))):
        plate_piece = add_cube(f"void knight chest facet {index}", (x, -0.34, z), (0.32, 0.10, 0.27), armor_dark if index > 1 else edge, rotation=(0.0, rot, rot))
        parts.append(_tag(plate_piece, "armor_socket"))
    core = add_ico_sphere("void knight abyss core", (0.0, -0.43, 1.45), (0.13, 0.06, 0.17), void, subdivisions=2, rotation=(0.0, 0.0, math.radians(45)))
    core_seed = add_ico_sphere("void knight abyss seed", (0.0, -0.49, 1.45), (0.055, 0.035, 0.075), void_hot, subdivisions=2)
    parts.extend((_tag(core, "armor_socket"), _tag(core_seed, "armor_socket")))
    belt = add_torus("void knight war belt", (0.0, 0.0, 0.98), 0.38, 0.07, gold, major_segments=12, minor_segments=4)
    parts.append(_tag(belt, "pelvis"))
    buckle = add_cube("void knight crest buckle", (0.0, -0.38, 0.98), (0.20, 0.10, 0.24), gold, rotation=(0.0, 0.0, math.radians(45)))
    parts.append(_tag(buckle, "pelvis"))

    # Asymmetric pauldrons, vambraces, and knuckle ridges reinforce boss scale.
    for side, sign in (("L", -1), ("R", 1)):
        upper_bone, fore_bone, hand_bone = f"upper_arm.{side}", f"forearm.{side}", f"hand.{side}"
        _boss_rock(parts, f"void knight pauldron {side}", (0.53 * sign, 0.0, 1.59), (0.40 if side == "R" else 0.35, 0.31, 0.27), armor, upper_bone)
        rim = add_torus(f"void knight pauldron rim {side}", (0.55 * sign, -0.22, 1.59), 0.30 if side == "R" else 0.26, 0.035, edge, major_segments=10, minor_segments=4, rotation=(math.radians(90), 0.0, 0.0))
        parts.append(_tag(rim, upper_bone))
        if side == "R":
            _boss_spike(parts, "void knight high pauldron spike", (0.72, 0.02, 1.89), 0.09, 0.52, edge, upper_bone, (0.0, -0.36, 0.18))
        upper = add_cylinder_between(f"void knight rerebrace {side}", (0.43 * sign, 0.0, 1.42), (0.68 * sign, -0.04, 1.19), 0.18, armor_dark, vertices=10)
        fore = add_cylinder_between(f"void knight vambrace {side}", (0.68 * sign, -0.04, 1.18), (0.88 * sign, -0.10, 0.92), 0.17, armor, vertices=10)
        parts.extend((_tag(upper, upper_bone), _tag(fore, fore_bone)))
        gauntlet = add_ico_sphere(f"void knight gauntlet {side}", (0.91 * sign, -0.13, 0.86), (0.22, 0.20, 0.20), armor_dark, subdivisions=2)
        parts.append(_tag(gauntlet, hand_bone))
        for knuckle in range(3):
            ridge = add_cube(f"void knight knuckle {side} {knuckle}", ((0.79 + knuckle * 0.10) * sign, -0.31, 0.86), (0.08, 0.08, 0.08), edge)
            parts.append(_tag(ridge, hand_bone))

    # Segmented cuisses, winged knees, and sabatons create a complete armoured figure.
    for side, sign in (("L", -1), ("R", 1)):
        thigh, shin, foot = f"thigh.{side}", f"shin.{side}", f"foot.{side}"
        tasset = add_leaf(f"void knight tasset {side}", (0.26 * sign, -0.22, 0.91), (0.28, 0.07, 0.40), cloth_light, rotation=(0.0, 0.0, 0.12 * sign))
        parts.append(_tag(tasset, thigh))
        cuisse = add_cylinder(f"void knight cuisse {side}", (0.27 * sign, 0.0, 0.65), 0.20, 0.48, armor, vertices=10)
        parts.append(_tag(cuisse, thigh))
        knee = add_ico_sphere(f"void knight poleyn {side}", (0.29 * sign, -0.15, 0.43), (0.23, 0.18, 0.20), edge, subdivisions=2)
        parts.append(_tag(knee, shin))
        _boss_spike(parts, f"void knight knee wing {side}", (0.48 * sign, -0.15, 0.44), 0.06, 0.34, edge, shin, (0.0, -0.62 * sign, 0.44 * sign))
        greave = add_cylinder(f"void knight greave {side}", (0.30 * sign, 0.0, 0.24), 0.18, 0.40, armor_dark, vertices=10)
        parts.append(_tag(greave, shin))
        sabaton = add_cube(f"void knight sabaton {side}", (0.31 * sign, -0.20, 0.11), (0.38, 0.60, 0.18), armor, rotation=(0.0, 0.04 * sign, 0.0))
        toe_cap = add_cube(f"void knight toe cap {side}", (0.31 * sign, -0.48, 0.12), (0.31, 0.22, 0.14), edge)
        parts.extend((_tag(sabaton, foot), _tag(toe_cap, foot)))

    # Horned enclosed helm, narrow luminous visor, and crown ridge are iconic at 256 px.
    _boss_rock(parts, "void knight helm", (0.0, -0.02, 2.03), (0.39, 0.33, 0.38), armor_dark, "head")
    faceplate = add_cube("void knight faceplate", (0.0, -0.34, 2.01), (0.54, 0.16, 0.43), armor, rotation=(0.04, 0.0, 0.0))
    visor = add_cube("void knight void visor", (0.0, -0.44, 2.09), (0.39, 0.045, 0.075), void_hot, rotation=(0.0, 0.0, 0.0))
    chin = add_pointed_cone("void knight pointed bevor", (0.0, -0.36, 1.82), 0.22, 0.32, armor_dark, vertices=8, rotation=(0.0, 0.0, math.radians(180)))
    parts.extend((_tag(faceplate, "head"), _tag(visor, "head"), _tag(chin, "head")))
    crest = add_cube("void knight helm crest", (0.0, 0.02, 2.36), (0.12, 0.22, 0.42), gold, rotation=(0.0, 0.0, 0.0))
    parts.append(_tag(crest, "helmet_socket"))
    for side, sign in (("L", -1), ("R", 1)):
        _boss_spike(parts, f"void knight crown horn {side}", (0.26 * sign, 0.0, 2.32), 0.095, 0.62, armor_dark, "helmet_socket", (0.0, -0.52 * sign, 0.28 * sign))
        horn_tip = add_ico_sphere(f"void knight horn tip {side}", (0.48 * sign, -0.02, 2.50), (0.08, 0.07, 0.11), void, subdivisions=2)
        parts.append(_tag(horn_tip, "helmet_socket"))

    # Split torn cape sits behind the armour and streams clearly during the charge.
    for index, (x, z, sx, sz, rot) in enumerate(((-0.23, 1.30, 0.38, 0.78, -0.12), (0.23, 1.28, 0.40, 0.82, 0.14), (-0.47, 1.18, 0.27, 0.60, -0.25), (0.47, 1.14, 0.25, 0.56, 0.28))):
        cape = add_leaf(f"void knight cape panel {index}", (x, 0.26, z), (sx, 0.065, sz), cloth if index < 2 else cloth_light, rotation=(0.0, 0.0, rot))
        parts.append(_tag(cape, "chest"))

    # Oversized greatblade remains one coherent socket-driven weapon during charge.
    grip = add_cylinder("void greatblade grip", (0.91, -0.08, 1.01), 0.07, 0.46, leather, vertices=10)
    pommel = add_ico_sphere("void greatblade pommel", (0.91, -0.08, 0.77), (0.13, 0.12, 0.13), void, subdivisions=2)
    guard = add_cube("void greatblade crossguard", (0.91, -0.08, 1.25), (0.72, 0.14, 0.11), gold, rotation=(0.0, 0.0, 0.0))
    blade = add_cube("void greatblade blade", (0.91, -0.07, 1.78), (0.25, 0.12, 1.05), armor, rotation=(0.0, 0.0, 0.0))
    fuller = add_cube("void greatblade fuller", (0.91, -0.14, 1.78), (0.065, 0.035, 0.88), void, rotation=(0.0, 0.0, 0.0))
    blade_edge_l = add_cube("void greatblade edge L", (0.78, -0.08, 1.78), (0.055, 0.15, 1.06), edge, rotation=(0.0, 0.0, -0.02))
    blade_edge_r = add_cube("void greatblade edge R", (1.04, -0.08, 1.78), (0.055, 0.15, 1.06), edge, rotation=(0.0, 0.0, 0.02))
    tip = add_pointed_cone("void greatblade point", (0.91, -0.07, 2.39), 0.18, 0.44, edge, vertices=4, rotation=(0.0, 0.0, math.radians(45)))
    for piece in (grip, pommel, guard, blade, fuller, blade_edge_l, blade_edge_r, tip):
        parts.append(_tag(piece, "weapon_socket"))

    attach_meshes(armature, parts)
    return BuiltModel(armature, parts, {
        "unique_attack": "void_charge",
        "modelRevision": "abyss-champion-v2",
        "rigProfile": "premium-armored-humanoid-v2",
        "animationProfile": "void-knight-void-charge-v2",
        "silhouetteLandmarks": ["forked void horns", "asymmetric pauldrons", "split torn cape", "socket-driven greatblade"],
        "surfaceLanguage": "layered gunmetal plate, silver edges, old gold, restrained violet abyss glow",
        "visualQuality": "premium-v2",
    })


BUILDERS: dict[str, Callable[[], BuiltModel]] = {
    "hero": build_hero,
    "rootling": build_rootling,
    "stonekin": build_stonekin,
    "gloom_wolf": build_gloom_wolf,
    "fungal_brute": build_fungal_brute,
    "ancient_golem": build_ancient_golem,
    "thorn_matriarch": build_thorn_matriarch,
    "ember_wyrm": build_ember_wyrm,
    "void_knight": build_void_knight,
}


def build_character(builder: str) -> BuiltModel:
    try:
        result = BUILDERS[builder]()
    except KeyError as error:
        raise ValueError(f"Unknown character builder: {builder}") from error
    shadow_material = transparent_material(f"{builder}_shadow", "#091014", 0.32)
    result.render_objects.append(add_contact_shadow(shadow_material))
    return result


def _add_premium_legendary_piece(
    armature: bpy.types.Object,
    slot: str,
    variant_index: int,
) -> list[bpy.types.Object]:
    """Author the representative Verdant Covenant pilot set as true silhouettes."""
    gold = MATERIALS.get("covenant_gold", "#DDB85A", True)
    gold_light = MATERIALS.get("covenant_gold_light", "#FFE6A0", True)
    wood = MATERIALS.get("covenant_heartwood", "#593B2D")
    dark = MATERIALS.get("covenant_dark", "#172C29")
    leaf = MATERIALS.get("covenant_leaf", "#70C765")
    deep_leaf = MATERIALS.get("covenant_deep_leaf", "#286044")
    heart = MATERIALS.get("covenant_heart", "#8EE8B0")
    objects: list[bpy.types.Object] = []

    def attach(obj: bpy.types.Object, bone: str) -> None:
        _bone_part(obj, armature, bone, objects)

    if variant_index == 35 and slot == "weapon":  # Worldbranch
        points = (
            (0.74, -0.10, 0.34), (0.96, -0.12, 0.55), (1.11, -0.13, 0.80),
            (1.16, -0.13, 1.05), (1.09, -0.12, 1.31), (0.91, -0.10, 1.56),
            (0.69, -0.08, 1.72),
        )
        for index, (start, end) in enumerate(zip(points, points[1:])):
            attach(add_cylinder_between(
                f"worldbranch_limb_{index}", start, end,
                0.052 if index in {2, 3} else 0.044,
                wood if index % 2 == 0 else gold, 8,
            ), "weapon_socket")
        grip = add_cylinder_between("worldbranch_grip", (1.16, -0.13, 0.90),
                                    (1.16, -0.13, 1.18), 0.07, dark, 8)
        attach(grip, "weapon_socket")
        for name, start, end in (
            ("worldbranch_string_lower", points[0], (0.78, -0.145, 1.03)),
            ("worldbranch_string_upper", (0.78, -0.145, 1.03), points[-1]),
        ):
            attach(add_cylinder_between(name, start, end, 0.009, gold_light, 5),
                   "weapon_socket")
        attach(add_ico("worldbranch_heart_gem", (1.11, -0.20, 1.04),
                       (0.10, 0.035, 0.14), heart, 2), "weapon_socket")
        for index, (x, z, angle) in enumerate(((0.82, 0.48, -0.55),
                                               (0.99, 1.48, 0.45),
                                               (0.72, 1.67, 0.72))):
            attach(add_leaf(f"worldbranch_leaf_{index}", (x, -0.15, z),
                            (0.105, 0.035, 0.20), leaf,
                            (0, 0, angle)), "weapon_socket")
    elif variant_index == 36 and slot == "helmet":  # Crown of First Leaves
        attach(add_torus("first_leaves_circlet", (0, -0.02, 2.02), 0.36, 0.035,
                         gold, (math.pi / 2, 0, 0)), "helmet_socket")
        for index, (x, z, angle, material) in enumerate((
            (-0.27, 2.12, -0.42, deep_leaf), (-0.13, 2.25, -0.22, leaf),
            (0.0, 2.32, 0.0, gold_light), (0.13, 2.25, 0.22, leaf),
            (0.27, 2.12, 0.42, deep_leaf),
        )):
            attach(add_leaf(f"first_leaves_crown_{index}", (x, -0.12, z),
                            (0.105, 0.045, 0.25), material,
                            (0, angle, angle)), "helmet_socket")
        attach(add_ico("first_leaves_seed", (0, -0.30, 2.12),
                       (0.09, 0.035, 0.12), heart, 2), "helmet_socket")
    elif variant_index == 37 and slot == "armor":  # Heartwood Aegis
        attach(add_cube("heartwood_breastplate", (0, -0.30, 1.25),
                        (0.67, 0.095, 0.68), wood, 0.065), "armor_socket")
        attach(add_leaf("heartwood_center_leaf", (0, -0.405, 1.28),
                        (0.20, 0.035, 0.38), leaf), "armor_socket")
        attach(add_torus("heartwood_core_ring", (0, -0.44, 1.24), 0.13, 0.032,
                         gold, (math.pi / 2, 0, 0)), "armor_socket")
        attach(add_ico("heartwood_core", (0, -0.475, 1.24),
                       (0.065, 0.02, 0.09), heart, 1), "armor_socket")
        for side, sign in (("L", -1), ("R", 1)):
            attach(add_leaf(f"heartwood_collar_{side}", (0.27 * sign, -0.31, 1.55),
                            (0.18, 0.05, 0.27), deep_leaf,
                            (0, 0.12 * sign, 0.35 * sign)), "armor_socket")
            attach(add_cube(f"heartwood_edge_{side}", (0.31 * sign, -0.405, 1.23),
                            (0.055, 0.03, 0.55), gold, 0.015), "armor_socket")
    elif variant_index == 38 and slot == "boots":  # Boots of Three Winds
        for side, sign in (("L", -1), ("R", 1)):
            bone = f"boot_socket.{side}"
            attach(add_cube(f"three_winds_boot_{side}", (0.22 * sign, -0.11, 0.18),
                            (0.29, 0.38, 0.31), dark, 0.055), bone)
            attach(add_torus(f"three_winds_cuff_{side}", (0.22 * sign, -0.06, 0.30),
                             0.17, 0.035, gold, (math.pi / 2, 0, 0)), bone)
            for index in range(3):
                attach(add_leaf(
                    f"three_winds_wing_{side}_{index}",
                    (0.31 * sign + index * 0.055 * sign, -0.10, 0.31 + index * 0.07),
                    (0.09, 0.035, 0.18 - index * 0.02),
                    leaf if index == 0 else gold_light,
                    (0, 0.18 * sign, (0.35 + index * 0.18) * sign),
                ), bone)
    elif variant_index == 39 and slot == "ring2":  # Eternal Seed
        attach(add_torus("eternal_seed_band", (0.78, -0.07, 1.0),
                         0.115, 0.028, gold, (math.pi / 2, 0, 0)), "ring_socket.R")
        attach(add_ico("eternal_seed_gem", (0.78, -0.19, 1.08),
                       (0.07, 0.035, 0.10), heart, 2), "ring_socket.R")
        for index, sign in enumerate((-1, 1)):
            attach(add_leaf(f"eternal_seed_leaf_{index}",
                            (0.78 + 0.09 * sign, -0.16, 1.10),
                            (0.055, 0.025, 0.10), leaf,
                            (0, 0, 0.55 * sign)), "ring_socket.R")
    else:
        raise ValueError(f"Unknown premium pilot equipment variant: {variant_index}/{slot}")
    return objects


def _add_premium_equipment_piece(
    armature: bpy.types.Object,
    slot: str,
    variant_index: int,
    tier_color: str,
    visual_kind: str | None,
    item_id: str,
    tier: str,
) -> list[bpy.types.Object]:
    """Build one named equipment silhouette with restrained tier accents."""
    tier_highlights = {
        "COMMON": "#D9D0AD",
        "UNCOMMON": "#A8E286",
        "RARE": "#9DD8FF",
        "LEGENDARY": "#FFE6A0",
    }
    accent = MATERIALS.get(f"{item_id}_accent", tier_color, tier in {"RARE", "LEGENDARY"})
    highlight = MATERIALS.get(
        f"{item_id}_highlight", tier_highlights[tier], tier in {"RARE", "LEGENDARY"}
    )
    dark = MATERIALS.get(f"{item_id}_dark", "#172C2B")
    leather = MATERIALS.get(f"{item_id}_leather", "#694632")
    wood = MATERIALS.get(f"{item_id}_wood", "#765033")
    pale_wood = MATERIALS.get(f"{item_id}_pale_wood", "#AA8050")
    silver = MATERIALS.get(f"{item_id}_silver", "#9DAEAD")
    parchment = MATERIALS.get(f"{item_id}_parchment", "#D7C99B")
    leaf = MATERIALS.get(f"{item_id}_leaf", "#67A85A")
    deep_leaf = MATERIALS.get(f"{item_id}_deep_leaf", "#285842")
    cyan = MATERIALS.get(f"{item_id}_cyan", "#8DE1DC", tier == "RARE")
    crimson = MATERIALS.get(f"{item_id}_crimson", "#A9464C", tier == "RARE")
    violet = MATERIALS.get(f"{item_id}_violet", "#6C568D", tier == "RARE")
    objects: list[bpy.types.Object] = []

    def attach(obj: bpy.types.Object, bone: str) -> bpy.types.Object:
        _bone_part(obj, armature, bone, objects)
        return obj

    def front_ring(name: str, location, radius: float, thickness: float, material, bone: str):
        return attach(add_torus(
            name, location, radius, thickness, material, (math.pi / 2, 0, 0)
        ), bone)

    def segmented_bow(name: str, points, limb_material, detail_material, gem_material=None):
        for index, (start, end) in enumerate(zip(points, points[1:])):
            attach(add_cylinder_between(
                f"{name}_limb_{index}", start, end,
                0.044 if index not in {2, 3} else 0.052,
                limb_material if index % 2 == 0 else detail_material, 7,
            ), "weapon_socket")
        nocking = (points[len(points) // 2][0] - 0.32, -0.15, points[len(points) // 2][2])
        attach(add_cylinder_between(
            f"{name}_string_lower", points[0], nocking, 0.008, highlight, 5
        ), "weapon_socket")
        attach(add_cylinder_between(
            f"{name}_string_upper", nocking, points[-1], 0.008, highlight, 5
        ), "weapon_socket")
        attach(add_cylinder_between(
            f"{name}_grip", (points[len(points) // 2][0], -0.13, nocking[2] - 0.13),
            (points[len(points) // 2][0], -0.13, nocking[2] + 0.13), 0.066, dark, 8,
        ), "weapon_socket")
        if gem_material is not None:
            attach(add_ico(
                f"{name}_gem", (points[len(points) // 2][0], -0.21, nocking[2]),
                (0.085, 0.03, 0.12), gem_material, 2,
            ), "weapon_socket")

    if slot == "weapon":
        if item_id in {"ashwood_bow", "moonwood_longbow", "starfall_bow"}:
            if item_id == "ashwood_bow":
                points = ((0.73, -0.09, 0.40), (0.98, -0.12, 0.67),
                          (1.08, -0.13, 1.02), (0.96, -0.11, 1.35),
                          (0.71, -0.08, 1.58))
                segmented_bow(item_id, points, wood, pale_wood)
                attach(add_leaf("ashwood_grip_leaf", (1.08, -0.20, 1.02),
                                (0.08, 0.025, 0.15), leaf), "weapon_socket")
            elif item_id == "moonwood_longbow":
                points = ((0.68, -0.08, 0.29), (0.94, -0.11, 0.54),
                          (1.12, -0.13, 0.82), (1.17, -0.13, 1.06),
                          (1.10, -0.12, 1.32), (0.91, -0.10, 1.58),
                          (0.65, -0.07, 1.78))
                segmented_bow(item_id, points, pale_wood, accent, cyan)
                for index, (x, z, angle) in enumerate(((0.76, 0.37, -0.55), (0.73, 1.69, 0.55))):
                    attach(add_leaf(f"moonwood_tip_leaf_{index}", (x, -0.13, z),
                                    (0.09, 0.03, 0.17), leaf, (0, 0, angle)),
                           "weapon_socket")
            else:
                points = ((0.66, -0.08, 0.27), (0.92, -0.11, 0.51),
                          (1.12, -0.13, 0.78), (1.19, -0.14, 1.05),
                          (1.11, -0.12, 1.34), (0.89, -0.10, 1.61),
                          (0.62, -0.07, 1.82))
                segmented_bow(item_id, points, silver, accent, cyan)
                for index, (x, z) in enumerate(((0.72, 0.35), (1.18, 1.05), (0.69, 1.73))):
                    attach(add_ico(f"starfall_star_{index}", (x, -0.18, z),
                                   (0.065, 0.025, 0.085), highlight, 1), "weapon_socket")
        elif item_id == "militia_sabre":
            attach(add_cylinder_between("sabre_grip", (0.82, -0.09, 0.48),
                                        (0.82, -0.09, 0.92), 0.065, leather, 8),
                   "weapon_socket")
            guard = add_cube("sabre_guard", (0.82, -0.10, 0.97),
                             (0.48, 0.11, 0.09), accent, 0.025)
            guard.rotation_euler.z = -0.12
            attach(guard, "weapon_socket")
            attach(add_leaf("sabre_blade", (0.84, -0.09, 1.37),
                            (0.14, 0.055, 0.55), silver, (0, 0.08, -0.05)),
                   "weapon_socket")
            attach(add_ico("sabre_pommel", (0.82, -0.09, 0.43),
                           (0.10, 0.07, 0.10), accent, 1), "weapon_socket")
        elif item_id in {"thorn_spear", "verdant_glaive"}:
            shaft_top = 1.58 if item_id == "verdant_glaive" else 1.66
            attach(add_cylinder_between(f"{item_id}_shaft", (0.82, -0.08, 0.30),
                                        (0.82, -0.08, shaft_top), 0.045, wood, 8),
                   "weapon_socket")
            if item_id == "thorn_spear":
                attach(add_leaf("thorn_spear_head", (0.82, -0.08, 1.91),
                                (0.18, 0.055, 0.38), accent), "weapon_socket")
                for index, sign in enumerate((-1, 1)):
                    barb = add_cone(f"thorn_spear_barb_{index}",
                                    (0.82 + 0.13 * sign, -0.08, 1.66),
                                    0.06, 0.0, 0.27, dark, 5,
                                    (0, math.radians(58 * sign), 0))
                    attach(barb, "weapon_socket")
                front_ring("thorn_spear_band", (0.82, -0.08, 1.50),
                           0.09, 0.025, parchment, "weapon_socket")
            else:
                # Keep the sweeping leaf silhouette inside the 192 px safe frame
                # during the high attack recovery pose.
                attach(add_leaf("verdant_glaive_blade", (0.95, -0.09, 1.72),
                                (0.23, 0.055, 0.42), accent,
                                (0, 0.12, -0.35)), "weapon_socket")
                attach(add_leaf("verdant_glaive_hook", (0.68, -0.08, 1.61),
                                (0.115, 0.045, 0.24), leaf, (0, 0, 0.58)),
                       "weapon_socket")
                for z in (0.72, 1.40):
                    front_ring(f"verdant_glaive_band_{z}", (0.82, -0.08, z),
                               0.085, 0.023, highlight, "weapon_socket")
        elif item_id == "golem_splitter":
            attach(add_cylinder_between("splitter_haft", (0.82, -0.07, 0.28),
                                        (0.82, -0.07, 1.37), 0.060, dark, 8),
                   "weapon_socket")
            # Lower and modestly compact the complete head while retaining its
            # broad double-bit profile, impact weight, and readable center rune.
            for side, sign in (("L", -1), ("R", 1)):
                attach(add_leaf(f"splitter_blade_{side}",
                                (0.82 + 0.22 * sign, -0.08, 1.40),
                                (0.27, 0.065, 0.34), silver,
                                (0, 0.10 * sign, 0.44 * sign)), "weapon_socket")
            attach(add_cube("splitter_head_core", (0.82, -0.08, 1.39),
                            (0.27, 0.13, 0.25), accent, 0.045), "weapon_socket")
            attach(add_ico("splitter_rune", (0.82, -0.17, 1.40),
                           (0.085, 0.025, 0.11), cyan, 1), "weapon_socket")
            attach(add_ico("splitter_counterweight", (0.82, -0.07, 0.25),
                           (0.13, 0.10, 0.13), accent, 1), "weapon_socket")
        else:
            raise ValueError(f"Unknown premium weapon: {item_id}/{visual_kind}")

    elif slot == "helmet":
        bone = "helmet_socket"
        if item_id == "leather_cap":
            attach(add_cone("leather_cap_dome", (0, 0.03, 1.98),
                            0.36, 0.20, 0.38, leather, 10), bone)
            attach(add_cube("leather_cap_brim", (0, -0.25, 1.91),
                            (0.72, 0.18, 0.08), accent, 0.025), bone)
            for side, sign in (("L", -1), ("R", 1)):
                attach(add_leaf(f"leather_cap_flap_{side}", (0.29 * sign, -0.07, 1.78),
                                (0.13, 0.055, 0.26), leather,
                                (0, 0.1 * sign, 0.10 * sign)), bone)
        elif item_id == "scout_hood":
            attach(add_ico("scout_hood_shell", (0, 0.06, 1.83),
                           (0.40, 0.31, 0.43), deep_leaf, 2), bone)
            front_ring("scout_hood_face", (0, -0.25, 1.82), 0.30, 0.055, accent, bone)
            attach(add_leaf("scout_hood_peak", (0, -0.28, 2.11),
                            (0.15, 0.05, 0.24), leaf), bone)
            attach(add_cube("scout_hood_shadow", (0, -0.285, 1.93),
                            (0.48, 0.035, 0.08), dark, 0.02), bone)
        elif item_id == "fern_guard":
            front_ring("fern_guard_circlet", (0, -0.02, 2.00), 0.34, 0.035, accent, bone)
            for index, (x, z, angle) in enumerate(((-0.24, 2.08, -0.50),
                                                   (0, 2.20, 0),
                                                   (0.24, 2.08, 0.50))):
                attach(add_leaf(f"fern_guard_leaf_{index}", (x, -0.15, z),
                                (0.13, 0.045, 0.29), leaf, (0, 0, angle)), bone)
                attach(add_cube(f"fern_guard_rib_{index}", (x, -0.20, z),
                                (0.025, 0.02, 0.20), highlight, 0.005), bone)
        elif item_id == "antler_circlet":
            front_ring("antler_circlet", (0, -0.01, 1.99), 0.34, 0.035, accent, bone)
            # Compact only the upper branches so the circlet stays seated while
            # the reactive hit pose retains a clean top-edge margin.
            for side, sign in (("L", -1), ("R", 1)):
                trunk_start = (0.22 * sign, 0.0, 2.04)
                trunk_end = (0.37 * sign, 0.02, 2.38)
                attach(add_cylinder_between(f"antler_trunk_{side}", trunk_start, trunk_end,
                                            0.045, pale_wood, 7), bone)
                attach(add_cylinder_between(f"antler_tine_low_{side}",
                                            (0.29 * sign, 0.01, 2.22),
                                            (0.51 * sign, 0.0, 2.31), 0.032,
                                            parchment, 6), bone)
                attach(add_cylinder_between(f"antler_tine_high_{side}",
                                            (0.34 * sign, 0.02, 2.31),
                                            (0.49 * sign, 0.02, 2.43), 0.028,
                                            parchment, 6), bone)
        elif item_id == "owlguard_helm":
            attach(add_ico("owlguard_shell", (0, 0.01, 1.94),
                           (0.40, 0.28, 0.40), dark, 2), bone)
            for side, sign in (("L", -1), ("R", 1)):
                front_ring(f"owlguard_eye_ring_{side}",
                           (0.14 * sign, -0.27, 1.98), 0.13, 0.035, silver, bone)
                attach(add_ico(f"owlguard_eye_{side}",
                               (0.14 * sign, -0.305, 1.98),
                               (0.060, 0.022, 0.070), cyan, 1), bone)
                attach(add_leaf(f"owlguard_wing_{side}",
                                (0.35 * sign, 0.0, 1.90),
                                (0.16, 0.07, 0.33), accent,
                                (0, 0.1 * sign, 0.24 * sign)), bone)
            attach(add_cone("owlguard_beak", (0, -0.31, 1.82),
                            0.12, 0.0, 0.30, highlight, 5,
                            (math.pi / 2, 0, 0)), bone)
        else:
            raise ValueError(f"Unknown premium helmet: {item_id}")

    elif slot == "armor":
        bone = "armor_socket"
        if item_id == "padded_vest":
            attach(add_cube("padded_vest_body", (0, -0.30, 1.23),
                            (0.65, 0.10, 0.67), leather, 0.065), bone)
            attach(add_cube("padded_vest_center", (0, -0.405, 1.23),
                            (0.10, 0.025, 0.58), accent, 0.015), bone)
            for row in range(2):
                for column, sign in enumerate((-1, 1)):
                    attach(add_ico(f"padded_vest_stud_{row}_{column}",
                                   (0.20 * sign, -0.415, 1.08 + row * 0.28),
                                   (0.055, 0.018, 0.055), parchment, 1), bone)
        elif item_id == "bark_tunic":
            attach(add_leaf("bark_tunic_body", (0, -0.31, 1.25),
                            (0.37, 0.08, 0.62), wood), bone)
            for side, sign in (("L", -1), ("R", 1)):
                attach(add_leaf(f"bark_tunic_plate_{side}",
                                (0.25 * sign, -0.34, 1.25),
                                (0.18, 0.05, 0.52), pale_wood,
                                (0, 0.10 * sign, 0.10 * sign)), bone)
            front_ring("bark_tunic_knot", (0, -0.405, 1.27), 0.12, 0.032, accent, bone)
            attach(add_ico("bark_tunic_sap", (0, -0.44, 1.27),
                           (0.06, 0.02, 0.08), highlight, 1), bone)
        elif item_id == "ranger_mail":
            attach(add_cube("ranger_mail_body", (0, -0.30, 1.24),
                            (0.66, 0.09, 0.66), deep_leaf, 0.055), bone)
            for row in range(3):
                for column, sign in enumerate((-1, 1)):
                    front_ring(f"ranger_mail_link_{row}_{column}",
                               (0.16 * sign, -0.405, 1.04 + row * 0.20),
                               0.075, 0.018, silver, bone)
            for side, sign in (("L", -1), ("R", 1)):
                attach(add_cube(f"ranger_mail_strap_{side}",
                                (0.27 * sign, -0.39, 1.40),
                                (0.09, 0.025, 0.45), accent, 0.015), bone)
        elif item_id == "mossweave_coat":
            attach(add_cone("mossweave_coat", (0, -0.20, 1.18),
                            0.42, 0.30, 0.92, deep_leaf, 10), bone)
            for index, (x, z, angle) in enumerate(((-0.25, 1.40, -0.30),
                                                   (0, 1.50, 0),
                                                   (0.25, 1.40, 0.30),
                                                   (-0.18, 0.92, -0.15),
                                                   (0.18, 0.92, 0.15))):
                attach(add_leaf(f"mossweave_leaf_{index}", (x, -0.39, z),
                                (0.16, 0.04, 0.28), leaf, (0, 0, angle)), bone)
            attach(add_cube("mossweave_clasp", (0, -0.43, 1.30),
                            (0.14, 0.03, 0.14), highlight, 0.025), bone)
        elif item_id == "crystalbark_plate":
            attach(add_cube("crystalbark_body", (0, -0.30, 1.24),
                            (0.70, 0.12, 0.70), wood, 0.07), bone)
            for side, sign in (("L", -1), ("R", 1)):
                attach(add_leaf(f"crystalbark_edge_{side}",
                                (0.29 * sign, -0.41, 1.24),
                                (0.15, 0.045, 0.58), dark,
                                (0, 0.08 * sign, 0.08 * sign)), bone)
                attach(add_leaf(f"crystalbark_shard_{side}",
                                (0.16 * sign, -0.46, 1.30),
                                (0.105, 0.025, 0.30), accent,
                                (0, 0, 0.18 * sign)), bone)
            attach(add_ico("crystalbark_core", (0, -0.47, 1.21),
                           (0.11, 0.025, 0.17), cyan, 2), bone)
        elif item_id == "shadeleaf_mantle":
            attach(add_cone("shadeleaf_shadow", (0, 0.04, 1.25),
                            0.50, 0.24, 0.82, dark, 9), bone)
            for index, (x, z, angle) in enumerate(((-0.30, 1.44, -0.36),
                                                   (0, 1.51, 0),
                                                   (0.30, 1.44, 0.36),
                                                   (-0.22, 1.12, -0.20),
                                                   (0.22, 1.12, 0.20))):
                attach(add_leaf(f"shadeleaf_panel_{index}", (x, -0.33, z),
                                (0.21, 0.05, 0.35),
                                accent if index < 3 else violet,
                                (0, 0, angle)), bone)
            front_ring("shadeleaf_clasp", (0, -0.405, 1.44),
                       0.12, 0.030, cyan, bone)
        else:
            raise ValueError(f"Unknown premium armor: {item_id}")

    elif slot == "boots":
        for side, sign in (("L", -1), ("R", 1)):
            bone = f"boot_socket.{side}"
            if item_id == "trail_boots":
                attach(add_cube(f"trail_boot_{side}", (0.22 * sign, -0.10, 0.18),
                                (0.28, 0.38, 0.30), leather, 0.055), bone)
                attach(add_cube(f"trail_sole_{side}", (0.22 * sign, -0.13, 0.07),
                                (0.31, 0.42, 0.09), dark, 0.025), bone)
                front_ring(f"trail_cuff_{side}", (0.22 * sign, -0.05, 0.30),
                           0.16, 0.030, accent, bone)
            elif item_id == "hide_greaves":
                attach(add_cube(f"hide_greave_{side}", (0.22 * sign, -0.08, 0.22),
                                (0.30, 0.34, 0.38), leather, 0.05), bone)
                for index in range(3):
                    attach(add_ico(f"hide_fur_{side}_{index}",
                                   (0.22 * sign + (index - 1) * 0.08,
                                    -0.10, 0.40),
                                   (0.10, 0.08, 0.09), parchment, 1), bone)
                attach(add_leaf(f"hide_bone_guard_{side}",
                                (0.22 * sign, -0.30, 0.23),
                                (0.10, 0.035, 0.24), accent), bone)
            elif item_id == "windstep_boots":
                attach(add_cube(f"windstep_boot_{side}", (0.22 * sign, -0.10, 0.18),
                                (0.28, 0.37, 0.30), deep_leaf, 0.055), bone)
                front_ring(f"windstep_cuff_{side}", (0.22 * sign, -0.05, 0.30),
                           0.16, 0.030, accent, bone)
                for index in range(2):
                    attach(add_leaf(f"windstep_wing_{side}_{index}",
                                    (0.33 * sign + index * 0.07 * sign,
                                     -0.10, 0.31 + index * 0.08),
                                    (0.10, 0.035, 0.19),
                                    leaf if index == 0 else highlight,
                                    (0, 0.14 * sign, (0.45 + index * 0.18) * sign)), bone)
            elif item_id == "rootguard_sabatons":
                attach(add_cube(f"rootguard_boot_{side}", (0.22 * sign, -0.10, 0.18),
                                (0.32, 0.41, 0.32), dark, 0.05), bone)
                for index, x_offset in enumerate((-0.09, 0.09)):
                    attach(add_leaf(f"rootguard_bark_{side}_{index}",
                                    (0.22 * sign + x_offset, -0.31, 0.22),
                                    (0.11, 0.035, 0.25), wood,
                                    (0, 0, 0.10 * sign)), bone)
                attach(add_ico(f"rootguard_knot_{side}",
                               (0.22 * sign, -0.34, 0.32),
                               (0.07, 0.025, 0.08), accent, 1), bone)
            elif item_id == "stormrunner_boots":
                attach(add_cube(f"stormrunner_boot_{side}",
                                (0.22 * sign, -0.10, 0.18),
                                (0.29, 0.39, 0.31), dark, 0.055), bone)
                front_ring(f"stormrunner_cuff_{side}",
                           (0.22 * sign, -0.05, 0.31), 0.16, 0.032, silver, bone)
                for index in range(3):
                    attach(add_leaf(f"stormrunner_arc_{side}_{index}",
                                    (0.31 * sign + index * 0.055 * sign,
                                     -0.13, 0.27 + index * 0.08),
                                    (0.085, 0.030, 0.16),
                                    accent if index < 2 else cyan,
                                    (0, 0.15 * sign, (0.40 + index * 0.20) * sign)), bone)
            else:
                raise ValueError(f"Unknown premium boots: {item_id}")

    elif slot in {"ring1", "ring2"}:
        side = "L" if slot == "ring1" else "R"
        sign = -1 if side == "L" else 1
        bone = f"ring_socket.{side}"
        x = 0.78 * sign
        ring_material = {
            "copper_leaf_ring": leather,
            "river_pebble_ring": silver,
            "acorn_band": leather,
            "hunter_loop": dark,
            "twine_circle": parchment,
            "jade_sap_ring": accent,
            "hawk_eye_band": highlight,
            "silver_briar_ring": silver,
            "dewstone_loop": silver,
            "sapphire_luck_ring": silver,
            "bloodroot_signet": dark,
            "echo_band": accent,
        }.get(item_id, accent)
        front_ring(f"{item_id}_band", (x, -0.07, 1.0),
                   0.115 if tier != "COMMON" else 0.105, 0.027, ring_material, bone)

        if item_id == "copper_leaf_ring":
            attach(add_leaf("copper_leaf_gem", (x, -0.19, 1.09),
                            (0.075, 0.028, 0.13), leaf, (0, 0, 0.28 * sign)), bone)
        elif item_id == "river_pebble_ring":
            attach(add_ico("river_pebble", (x, -0.18, 1.08),
                           (0.09, 0.035, 0.07), accent, 2), bone)
            attach(add_cube("river_glint", (x - 0.025 * sign, -0.22, 1.10),
                            (0.025, 0.015, 0.035), highlight, 0.005), bone)
        elif item_id == "acorn_band":
            attach(add_ico("acorn_seed", (x, -0.18, 1.07),
                           (0.075, 0.035, 0.10), wood, 2), bone)
            front_ring("acorn_cap", (x, -0.20, 1.14), 0.07, 0.022, accent, bone)
        elif item_id == "hunter_loop":
            attach(add_cone("hunter_tooth", (x, -0.18, 1.08),
                            0.075, 0.0, 0.20, parchment, 6), bone)
            attach(add_leaf("hunter_fletch", (x + 0.075 * sign, -0.16, 1.09),
                            (0.045, 0.022, 0.08), leaf,
                            (0, 0, 0.45 * sign)), bone)
        elif item_id == "twine_circle":
            front_ring("twine_second_loop", (x + 0.045 * sign, -0.09, 1.04),
                       0.09, 0.023, leather, bone)
            attach(add_ico("twine_knot", (x, -0.18, 1.09),
                           (0.055, 0.025, 0.055), accent, 1), bone)
        elif item_id == "jade_sap_ring":
            attach(add_leaf("jade_sap_gem", (x, -0.19, 1.09),
                            (0.08, 0.030, 0.14), leaf), bone)
            attach(add_ico("jade_sap_drop", (x, -0.225, 1.08),
                           (0.035, 0.015, 0.05), highlight, 1), bone)
        elif item_id == "hawk_eye_band":
            front_ring("hawk_eye_socket", (x, -0.18, 1.09),
                       0.095, 0.028, accent, bone)
            attach(add_ico("hawk_eye", (x, -0.22, 1.09),
                           (0.048, 0.018, 0.060), dark, 1), bone)
            attach(add_ico("hawk_eye_glint", (x - 0.015 * sign, -0.24, 1.11),
                           (0.015, 0.008, 0.018), highlight, 1), bone)
        elif item_id == "silver_briar_ring":
            for index, angle in enumerate((-0.55, 0, 0.55)):
                attach(add_cone(f"briar_thorn_{index}",
                                (x + (index - 1) * 0.065 * sign, -0.16,
                                 1.10 + abs(index - 1) * 0.025),
                                0.035, 0.0, 0.12, accent, 5,
                                (0, angle * sign, 0)), bone)
        elif item_id == "dewstone_loop":
            attach(add_leaf("dewstone", (x, -0.20, 1.09),
                            (0.08, 0.028, 0.13), cyan), bone)
            attach(add_ico("dewstone_glint", (x - 0.025 * sign, -0.23, 1.12),
                           (0.022, 0.010, 0.030), highlight, 1), bone)
        elif item_id == "sapphire_luck_ring":
            attach(add_ico("sapphire_center", (x, -0.20, 1.09),
                           (0.085, 0.035, 0.11), accent, 2), bone)
            for index, gem_sign in enumerate((-1, 1)):
                attach(add_leaf(f"sapphire_leaf_{index}",
                                (x + 0.085 * gem_sign * sign, -0.18, 1.09),
                                (0.045, 0.020, 0.085), cyan,
                                (0, 0, 0.50 * gem_sign)), bone)
        elif item_id == "bloodroot_signet":
            attach(add_ico("bloodroot_gem", (x, -0.20, 1.09),
                           (0.09, 0.035, 0.11), crimson, 2), bone)
            for index, root_sign in enumerate((-1, 1)):
                attach(add_cylinder_between(f"bloodroot_tendril_{index}",
                                            (x, -0.17, 1.05),
                                            (x + 0.10 * root_sign * sign,
                                             -0.15, 0.99), 0.018, accent, 5), bone)
        elif item_id == "echo_band":
            front_ring("echo_inner", (x, -0.18, 1.09),
                       0.072, 0.020, cyan, bone)
            front_ring("echo_outer", (x, -0.17, 1.09),
                       0.125, 0.018, highlight, bone)
        else:
            raise ValueError(f"Unknown premium ring: {item_id}")
    else:
        raise ValueError(f"Unsupported equipment slot: {slot}")
    return objects


def add_equipment_variant(
    armature: bpy.types.Object,
    slot: str,
    variant_index: int,
    tier_color: str,
    visual_kind: str | None = None,
    item_id: str | None = None,
    tier: str | None = None,
) -> list[bpy.types.Object]:
    """Attach a deterministic premium equipment mesh to stable Hero sockets."""
    if variant_index in {35, 36, 37, 38, 39}:
        return _add_premium_legendary_piece(armature, slot, variant_index)
    if item_id is None or tier is None:
        raise ValueError("Premium equipment generation requires item id and tier")
    return _add_premium_equipment_piece(
        armature, slot, variant_index, tier_color, visual_kind, item_id, tier
    )
