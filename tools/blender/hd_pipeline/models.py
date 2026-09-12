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
        "gold": MATERIALS.get("hero_gold", PALETTE["hero_gold"], True),
        "skin": MATERIALS.get("hero_skin", PALETTE["skin"]),
        "wood": MATERIALS.get("hero_wood", PALETTE["wood"]),
        "hair": MATERIALS.get("hero_hair", "#D8C77C"),
        "dark": MATERIALS.get("hero_dark", "#243B35"),
    }
    armature = create_standard_armature("hero")
    objects: list[bpy.types.Object] = []
    torso = add_cone("hero_torso", (0, 0, 1.15), 0.38, 0.27, 0.75, mats["green"])
    _bone_part(torso, armature, "chest", objects)
    pelvis = add_cube("hero_belt", (0, -0.01, 0.77), (0.68, 0.38, 0.18), mats["gold"], 0.04)
    _bone_part(pelvis, armature, "pelvis", objects)
    head = add_ico("hero_head", (0, -0.015, 1.75), (0.30, 0.26, 0.34), mats["skin"], 2)
    _bone_part(head, armature, "head", objects)
    eye_material = MATERIALS.get("hero_eye", "#17262B")
    for side, sign in (("L", -1), ("R", 1)):
        eye = add_ico(f"hero_eye_{side}", (0.09 * sign, -0.258, 1.80), (0.032, 0.018, 0.042), eye_material)
        _bone_part(eye, armature, "head", objects)
    hair = add_cone("hero_hair", (0, 0.08, 1.88), 0.31, 0.10, 0.44, mats["hair"], 8)
    _bone_part(hair, armature, "head", objects)
    for side, sign in (("L", -1), ("R", 1)):
        ear = add_cone(
            f"elf_ear_{side}",
            (0.31 * sign, -0.01, 1.78),
            0.10,
            0.012,
            0.42,
            mats["skin"],
            6,
            rotation=(0.0, math.radians(76), 0.0),
        )
        ear.rotation_euler.y *= sign
        _bone_part(ear, armature, "head", objects)
    cloak = add_cone("leaf_cloak", (0, 0.20, 1.11), 0.48, 0.22, 0.82, mats["leaf"], 7)
    _bone_part(cloak, armature, "chest", objects)
    _humanoid_limbs(armature, objects, mats["green"], mats["dark"], mats["skin"])
    return BuiltModel(armature, objects, {"silhouette": "elf_archer", "attachment_variant": "equipment_neutral"})


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
    armature, objects, mats = _basic_humanoid("rootling", PALETTE["wood"], "#B5A166", "ico", 0.92)
    eye_material = MATERIALS.get("rootling_eye", "#E7C85B")
    for side, sign in (("L", -1), ("R", 1)):
        eye = add_ico(f"rootling_eye_{side}", (0.09 * sign, -0.285, 1.78), (0.035, 0.018, 0.05), eye_material)
        _bone_part(eye, armature, "head", objects)
        horn = add_cone(
            f"rootling_branch_{side}", (0.20 * sign, 0.02, 2.02), 0.08, 0.0, 0.55,
            mats["accent"], 5, (0.0, math.radians(22 * sign), 0.0),
        )
        _bone_part(horn, armature, "head", objects)
    belly = add_ico("rootling_knot", (0, -0.30, 1.10), (0.18, 0.10, 0.18), mats["accent"])
    _bone_part(belly, armature, "chest", objects)
    return BuiltModel(armature, objects, {"silhouette": "branch_imp"})


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
    stone = MATERIALS.get("ancient_stone", "#56646A")
    moss = MATERIALS.get("ancient_moss", "#4D7652")
    rune = MATERIALS.get("ancient_rune", PALETTE["cyan"])
    armature = create_standard_armature("ancient_golem", 1.36)
    objects: list[bpy.types.Object] = []
    torso = add_cube("golem_monolith_torso", (0, 0, 1.38), (1.15, 0.68, 1.22), stone, 0.13)
    _bone_part(torso, armature, "chest", objects)
    head = add_cube("golem_head", (0, -0.02, 2.25), (0.68, 0.58, 0.55), stone, 0.08)
    _bone_part(head, armature, "head", objects)
    _humanoid_limbs(armature, objects, stone, stone, moss, 1.28)
    for side, sign in (("L", -1), ("R", 1)):
        shoulder = add_ico(f"golem_boulder_{side}", (0.68 * sign, 0, 1.78), (0.40, 0.36, 0.38), moss, 1)
        _bone_part(shoulder, armature, f"upper_arm.{side}", objects)
    rune_obj = add_cube("golem_core", (0, -0.38, 1.42), (0.30, 0.06, 0.38), rune, 0.03)
    _bone_part(rune_obj, armature, "chest", objects)
    return BuiltModel(armature, objects, {"silhouette": "monolith_golem", "unique_attack": "ground_slam"})


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


def add_equipment_variant(
    armature: bpy.types.Object,
    slot: str,
    variant_index: int,
    tier_color: str,
    visual_kind: str | None = None,
) -> list[bpy.types.Object]:
    """Attach a deterministic equipment mesh to the stable Hero socket bones."""
    material = MATERIALS.get(f"equipment_{slot}_{variant_index}", tier_color, slot in {"weapon", "helmet", "armor"})
    dark = MATERIALS.get("equipment_dark", "#2A3438", True)
    objects: list[bpy.types.Object] = []
    motif = variant_index % 4
    if slot == "weapon":
        kind = visual_kind or ("bow" if motif in {0, 3} else "spear")
        if kind == "bow":
            _add_bow(armature, objects, material, dark)
        else:
            shaft_top = 1.48 if kind == "sword" else 1.72
            shaft = add_cylinder_between(
                f"weapon_{variant_index}_shaft", (0.82, -0.10, 0.42),
                (0.82, -0.08, shaft_top), 0.045, dark, 6,
            )
            _bone_part(shaft, armature, "weapon_socket", objects)
            if kind == "axe":
                blade = add_cube(
                    f"weapon_{variant_index}_axe", (0.96, -0.08, 1.62),
                    (0.34, 0.10, 0.28), material, 0.035,
                )
            else:
                blade_depth = 0.55 if kind in {"spear", "glaive"} else 0.48
                blade = add_cone(
                    f"weapon_{variant_index}_{kind}",
                    (0.82, -0.08, shaft_top + blade_depth * 0.46),
                    0.24 if kind == "glaive" else 0.18,
                    0, blade_depth, material, 4,
                )
            _bone_part(blade, armature, "weapon_socket", objects)
    elif slot == "helmet":
        crown = add_cone(f"helmet_{variant_index}", (0, 0, 2.05), 0.35 + motif * 0.025, 0.05, 0.48 + motif * 0.04, material, 6 + motif)
        _bone_part(crown, armature, "helmet_socket", objects)
    elif slot == "armor":
        plate = add_cube(f"armor_{variant_index}", (0, -0.28, 1.24), (0.62 + motif * 0.04, 0.08, 0.62), material, 0.04)
        _bone_part(plate, armature, "armor_socket", objects)
    elif slot == "boots":
        for side, sign in (("L", -1), ("R", 1)):
            boot = add_cube(f"boots_{variant_index}_{side}", (0.22 * sign, -0.10, 0.18), (0.27, 0.36, 0.28 + motif * 0.02), material, 0.04)
            _bone_part(boot, armature, f"boot_socket.{side}", objects)
    elif slot in {"ring1", "ring2"}:
        side = "L" if slot == "ring1" else "R"
        sign = -1 if side == "L" else 1
        ring = add_torus(f"{slot}_{variant_index}", (0.78 * sign, -0.07, 1.0), 0.10 + motif * 0.008, 0.025, material, (math.radians(90), 0, 0))
        _bone_part(ring, armature, f"ring_socket.{side}", objects)
    else:
        raise ValueError(f"Unsupported equipment slot: {slot}")
    return objects
