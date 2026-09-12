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


def add_cube(name: str, location, scale, material, bevel: float = 0.0) -> bpy.types.Object:
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=location)
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


def add_torus(name: str, location, major_radius: float, minor_radius: float, material, rotation=(0.0, 0.0, 0.0)) -> bpy.types.Object:
    bpy.ops.mesh.primitive_torus_add(
        major_radius=major_radius,
        minor_radius=minor_radius,
        major_segments=12,
        minor_segments=4,
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
    armature, objects, mats = _basic_humanoid(
        "rootling", "#68432F", "#C2A95D", "ico", 0.92
    )
    dark_bark = MATERIALS.get("rootling_dark_bark", "#352923")
    moss = MATERIALS.get("rootling_moss", "#477A48")
    leaf = MATERIALS.get("rootling_leaf", "#79B85B")
    sap = MATERIALS.get("rootling_sap", "#F2C85B")

    def attach(obj: bpy.types.Object, bone: str) -> None:
        _bone_part(obj, armature, bone, objects)

    # Layered bark plates establish the imp's hunched, armored chest read.
    attach(add_leaf("rootling_chest_bark", (0, -0.31, 1.20),
                    (0.29, 0.075, 0.43), dark_bark), "chest")
    attach(add_torus("rootling_heart_knot", (0, -0.36, 1.12), 0.17, 0.045,
                     mats["accent"], (math.pi / 2, 0, 0)), "chest")
    attach(add_ico("rootling_sap_heart", (0, -0.405, 1.12),
                   (0.075, 0.025, 0.10), sap, 1), "chest")
    attach(add_leaf("rootling_moss_patch", (-0.22, -0.30, 1.36),
                    (0.16, 0.035, 0.20), moss, (0, 0, -0.35)), "chest")

    eye_material = MATERIALS.get("rootling_eye", "#FFE178")
    for side, sign in (("L", -1), ("R", 1)):
        attach(add_ico(f"rootling_eye_{side}", (0.09 * sign, -0.286, 1.78),
                       (0.044, 0.020, 0.060), eye_material, 1), "head")
        attach(add_cube(f"rootling_brow_{side}", (0.10 * sign, -0.293, 1.86),
                        (0.14, 0.025, 0.035), dark_bark, 0.008), "head")
        horn = add_cone(
            f"rootling_branch_{side}", (0.22 * sign, 0.02, 2.03), 0.085, 0.025,
            0.62, mats["accent"], 7, (0.0, math.radians(24 * sign), 0.0),
        )
        attach(horn, "head")
        attach(add_cylinder_between(
            f"rootling_twig_{side}", (0.25 * sign, 0.02, 2.13),
            (0.48 * sign, 0.01, 2.27), 0.035, dark_bark, 6,
        ), "head")
        attach(add_leaf(
            f"rootling_crown_leaf_{side}", (0.48 * sign, -0.01, 2.30),
            (0.12, 0.045, 0.20), leaf, (0, 0.22 * sign, 0.28 * sign),
        ), "head")
        attach(add_ico(f"rootling_bark_shoulder_{side}", (0.43 * sign, -0.01, 1.39),
                       (0.26, 0.18, 0.17), dark_bark, 1), f"upper_arm.{side}")
        for claw_index in range(2):
            attach(add_cone(
                f"rootling_claw_{side}_{claw_index}",
                (0.79 * sign + claw_index * 0.035 * sign, -0.10, 0.91),
                0.035, 0.0, 0.18, mats["accent"], 5,
                (math.radians(70), 0, math.radians(-8 * sign)),
            ), f"hand.{side}")
    attach(add_cube("rootling_mouth", (0, -0.302, 1.68),
                    (0.15, 0.018, 0.025), dark_bark, 0.01), "head")
    attach(add_leaf("rootling_back_leaf", (0.24, 0.18, 1.34),
                    (0.20, 0.07, 0.36), moss, (0.18, 0.15, 0.28)), "chest")
    return BuiltModel(armature, objects, {
        "silhouette": "crowned_branch_imp",
        "visualQuality": "premium-v2",
    })


def build_stonekin() -> BuiltModel:
    armature, objects, mats = _basic_humanoid("stonekin", PALETTE["stone"], "#9CB1B5", "cube", 1.08)
    for side, sign in (("L", -1), ("R", 1)):
        shoulder = add_ico(f"stone_shoulder_{side}", (0.46 * sign, 0, 1.41), (0.24, 0.22, 0.20), mats["accent"])
        _bone_part(shoulder, armature, f"upper_arm.{side}", objects)
    rune = add_cube("stonekin_rune", (0, -0.31, 1.20), (0.22, 0.035, 0.28), MATERIALS.get("rune", PALETTE["cyan"]), 0.02)
    _bone_part(rune, armature, "chest", objects)
    return BuiltModel(armature, objects, {"silhouette": "block_golem"})


def build_gloom_wolf() -> BuiltModel:
    dark = MATERIALS.get("gloom_wolf_fur", "#3F354C")
    violet = MATERIALS.get("gloom_wolf_violet", PALETTE["enemy_violet"])
    eye = MATERIALS.get("gloom_wolf_eye", PALETTE["cyan"])
    armature = create_standard_armature("gloom_wolf", 0.82)
    objects: list[bpy.types.Object] = []
    body = add_ico("wolf_body", (0, 0.10, 0.90), (0.62, 0.40, 0.40), dark, 1)
    _bone_part(body, armature, "spine", objects)
    chest = add_ico("wolf_chest", (0, -0.30, 1.05), (0.48, 0.36, 0.44), violet, 1)
    _bone_part(chest, armature, "chest", objects)
    head = add_cone("wolf_head", (0, -0.58, 1.28), 0.32, 0.14, 0.62, dark, 7, (math.radians(90), 0, 0))
    _bone_part(head, armature, "head", objects)
    for side, sign in (("L", -1), ("R", 1)):
        ear = add_cone(f"wolf_ear_{side}", (0.19 * sign, -0.43, 1.58), 0.12, 0.0, 0.38, violet, 5, (0, math.radians(8 * sign), 0))
        _bone_part(ear, armature, "head", objects)
        eye_obj = add_ico(f"wolf_eye_{side}", (0.12 * sign, -0.83, 1.36), (0.045, 0.03, 0.045), eye)
        _bone_part(eye_obj, armature, "head", objects)
    _humanoid_limbs(armature, objects, dark, dark, violet, 0.78)
    tail = add_cone("wolf_tail", (0, 0.62, 0.94), 0.13, 0.02, 0.95, dark, 7, (math.radians(-55), 0, 0))
    _bone_part(tail, armature, "pelvis", objects)
    return BuiltModel(armature, objects, {"silhouette": "low_quadruped"})


def build_fungal_brute() -> BuiltModel:
    armature, objects, mats = _basic_humanoid("fungal_brute", "#6C7452", "#C45B76", "ico", 1.12)
    cap = add_cone("fungal_cap", (0, 0, 2.05), 0.58, 0.07, 0.34, mats["accent"], 10)
    _bone_part(cap, armature, "head", objects)
    spots = MATERIALS.get("fungal_spots", "#E7D8B1")
    for index, (x, y) in enumerate(((-0.22, -0.19), (0.12, -0.28), (0.28, 0.02))):
        spot = add_ico(f"cap_spot_{index}", (x, y, 2.18), (0.075, 0.04, 0.03), spots)
        _bone_part(spot, armature, "head", objects)
    return BuiltModel(armature, objects, {"silhouette": "mushroom_brute"})


def build_ancient_golem() -> BuiltModel:
    stone = MATERIALS.get("ancient_stone", "#59686D")
    dark_stone = MATERIALS.get("ancient_dark_stone", "#303C40")
    light_stone = MATERIALS.get("ancient_light_stone", "#849497")
    moss = MATERIALS.get("ancient_moss", "#416B48")
    leaf = MATERIALS.get("ancient_leaf", "#6FA457")
    rune = MATERIALS.get("ancient_rune", "#64D8D5")
    rune_hot = MATERIALS.get("ancient_rune_hot", "#C5FFF0")
    armature = create_standard_armature("ancient_golem", 1.36)
    objects: list[bpy.types.Object] = []

    def attach(obj: bpy.types.Object, bone: str) -> None:
        _bone_part(obj, armature, bone, objects)

    # Interlocking megalith slabs create a broad silhouette with a protected heart.
    attach(add_cube("golem_monolith_torso", (0, 0.02, 1.38),
                    (1.18, 0.70, 1.23), stone, 0.14), "chest")
    attach(add_cube("golem_chest_inset", (0, -0.39, 1.43),
                    (0.66, 0.075, 0.72), dark_stone, 0.06), "chest")
    attach(add_cube("golem_upper_slab", (0, -0.18, 1.83),
                    (0.98, 0.44, 0.27), light_stone, 0.07), "chest")
    attach(add_ico("golem_core_outer", (0, -0.48, 1.43),
                   (0.27, 0.055, 0.34), rune, 2), "chest")
    attach(add_ico("golem_core_inner", (0, -0.535, 1.43),
                   (0.105, 0.025, 0.15), rune_hot, 1), "chest")
    for index, (x, z, angle) in enumerate(((-0.28, 1.43, -0.30),
                                           (0.28, 1.43, 0.30),
                                           (0, 1.14, 0.0))):
        rune_bar = add_cube(f"golem_rune_bar_{index}", (x, -0.47, z),
                            (0.055, 0.025, 0.27), rune, 0.01)
        rune_bar.rotation_euler.y = angle
        attach(rune_bar, "chest")

    attach(add_cube("golem_head", (0, -0.02, 2.25),
                    (0.72, 0.60, 0.57), stone, 0.09), "head")
    attach(add_cube("golem_brow_slab", (0, -0.37, 2.34),
                    (0.62, 0.10, 0.15), dark_stone, 0.03), "head")
    attach(add_cube("golem_jaw", (0, -0.34, 2.10),
                    (0.44, 0.10, 0.13), dark_stone, 0.03), "head")
    for index, (x, height, lean) in enumerate(((-0.25, 0.42, -0.18),
                                               (0.02, 0.55, 0.04),
                                               (0.26, 0.36, 0.20))):
        attach(add_cone(f"golem_crown_shard_{index}", (x, 0.01, 2.62),
                        0.11, 0.025, height, light_stone, 6,
                        (0, lean, 0)), "head")
    for side, sign in (("L", -1), ("R", 1)):
        eye = add_cube(f"golem_eye_{side}", (0.15 * sign, -0.435, 2.31),
                       (0.13, 0.025, 0.045), rune_hot, 0.012)
        eye.rotation_euler.y = -0.08 * sign
        attach(eye, "head")

    _humanoid_limbs(armature, objects, stone, dark_stone, moss, 1.28)
    for side, sign in (("L", -1), ("R", 1)):
        attach(add_ico(f"golem_boulder_{side}", (0.68 * sign, 0, 1.78),
                       (0.43, 0.38, 0.40), light_stone, 2), f"upper_arm.{side}")
        attach(add_cube(f"golem_forearm_plate_{side}", (0.73 * sign, -0.08, 1.10),
                        (0.30, 0.30, 0.48), dark_stone, 0.07), f"forearm.{side}")
        attach(add_cube(f"golem_shin_plate_{side}", (0.30 * sign, -0.07, 0.38),
                        (0.33, 0.36, 0.44), stone, 0.07), f"shin.{side}")
        attach(add_leaf(f"golem_moss_pauldron_{side}", (0.67 * sign, -0.20, 1.94),
                        (0.28, 0.06, 0.22), moss, (0, 0, 0.25 * sign)),
               f"upper_arm.{side}")
    for index, (x, z, angle) in enumerate(((-0.37, 1.88, -0.35),
                                           (0.34, 1.12, 0.25),
                                           (-0.18, 0.94, -0.15))):
        attach(add_leaf(f"golem_leaf_patch_{index}", (x, -0.39, z),
                        (0.14, 0.035, 0.22), leaf, (0, 0, angle)), "chest")

    return BuiltModel(armature, objects, {
        "silhouette": "heartstone_monolith",
        "unique_attack": "ground_slam",
        "visualQuality": "premium-v2",
    })


def build_thorn_matriarch() -> BuiltModel:
    bark = MATERIALS.get("matriarch_bark", "#573A34")
    leaf = MATERIALS.get("matriarch_leaf", "#4D8C50")
    bloom = MATERIALS.get("matriarch_bloom", "#C94D72")
    armature = create_standard_armature("thorn_matriarch", 1.25)
    objects: list[bpy.types.Object] = []
    skirt = add_cone("root_skirt", (0, 0, 0.82), 0.95, 0.34, 1.55, bark, 9)
    _bone_part(skirt, armature, "pelvis", objects)
    torso = add_cone("vine_torso", (0, 0, 1.62), 0.43, 0.24, 1.08, leaf, 8)
    _bone_part(torso, armature, "chest", objects)
    head = add_ico("bloom_face", (0, -0.05, 2.20), (0.34, 0.30, 0.38), bloom, 2)
    _bone_part(head, armature, "head", objects)
    for index in range(7):
        angle = index * math.tau / 7
        petal = add_cone(
            f"crown_petal_{index}",
            (math.sin(angle) * 0.34, math.cos(angle) * 0.22, 2.22 + math.cos(angle) * 0.08),
            0.18, 0.02, 0.62, leaf, 6,
            (math.radians(90), angle, 0),
        )
        _bone_part(petal, armature, "head", objects)
    _humanoid_limbs(armature, objects, bark, bark, bloom, 1.10)
    return BuiltModel(armature, objects, {"silhouette": "flower_root_queen", "unique_attack": "thorn_cage"})


def build_ember_wyrm() -> BuiltModel:
    scale = MATERIALS.get("wyrm_scale", "#8E382E")
    plate = MATERIALS.get("wyrm_plate", PALETTE["amber"])
    flame = MATERIALS.get("wyrm_flame", "#FFD15A")
    armature = create_standard_armature("ember_wyrm", 1.20)
    objects: list[bpy.types.Object] = []
    body = add_cone("wyrm_body", (0, 0.22, 1.26), 0.66, 0.32, 1.75, scale, 9)
    _bone_part(body, armature, "spine", objects)
    neck = add_cone("wyrm_neck", (0, -0.28, 2.00), 0.32, 0.22, 0.92, scale, 8, (math.radians(18), 0, 0))
    _bone_part(neck, armature, "neck", objects)
    head = add_cone("wyrm_head", (0, -0.58, 2.40), 0.32, 0.12, 0.75, plate, 8, (math.radians(90), 0, 0))
    _bone_part(head, armature, "head", objects)
    for side, sign in (("L", -1), ("R", 1)):
        wing = add_cone(
            f"wyrm_wing_{side}", (0.75 * sign, 0.14, 1.72), 0.72, 0.04, 1.75,
            scale, 5, (0, math.radians(72 * sign), math.radians(12 * sign)),
        )
        _bone_part(wing, armature, f"upper_arm.{side}", objects)
        horn = add_cone(f"wyrm_horn_{side}", (0.20 * sign, -0.32, 2.67), 0.09, 0, 0.52, plate, 6, (0, math.radians(18 * sign), 0))
        _bone_part(horn, armature, "head", objects)
    tail = add_cone("wyrm_tail", (0, 0.92, 0.82), 0.35, 0.03, 2.20, scale, 9, (math.radians(-55), 0, 0))
    _bone_part(tail, armature, "pelvis", objects)
    flame_obj = add_cone("wyrm_flame", (0, -1.03, 2.32), 0.22, 0, 0.72, flame, 7, (math.radians(90), 0, 0))
    _bone_part(flame_obj, armature, "head", objects)
    return BuiltModel(armature, objects, {"silhouette": "winged_wyrm", "unique_attack": "flame_sweep"})


def build_void_knight() -> BuiltModel:
    armor = MATERIALS.get("void_armor", "#252536", True)
    violet = MATERIALS.get("void_energy", "#8D61C7")
    silver = MATERIALS.get("void_silver", "#8794A3", True)
    armature = create_standard_armature("void_knight", 1.24)
    objects: list[bpy.types.Object] = []
    torso = add_cube("void_breastplate", (0, 0, 1.45), (0.88, 0.54, 1.10), armor, 0.10)
    _bone_part(torso, armature, "chest", objects)
    helmet = add_cone("void_helmet", (0, 0, 2.18), 0.42, 0.18, 0.72, armor, 8)
    _bone_part(helmet, armature, "head", objects)
    visor = add_cube("void_visor", (0, -0.36, 2.15), (0.48, 0.07, 0.15), violet, 0.02)
    _bone_part(visor, armature, "head", objects)
    _humanoid_limbs(armature, objects, armor, armor, silver, 1.18)
    sword = add_cylinder_between("void_greatsword", (0.88, -0.08, 0.55), (0.88, -0.08, 2.42), 0.09, silver, 6)
    _bone_part(sword, armature, "weapon_socket", objects)
    blade_tip = add_cone("void_blade_tip", (0.88, -0.08, 2.55), 0.17, 0, 0.42, violet, 4)
    _bone_part(blade_tip, armature, "weapon_socket", objects)
    cape = add_cone("void_cape", (0, 0.26, 1.35), 0.72, 0.26, 1.46, violet, 7)
    _bone_part(cape, armature, "chest", objects)
    return BuiltModel(armature, objects, {"silhouette": "armored_greatsword", "unique_attack": "void_charge"})


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
