from __future__ import annotations

import math

import bpy

from .config import PALETTE
from .models import BuiltModel, MATERIALS, add_cone, add_cube, add_cylinder_between, add_ico, add_torus
from .rig import parent_to_bone


def build_world_tree(damaged: bool = False) -> BuiltModel:
    bark = MATERIALS.get("tree_bark", PALETTE["wood"])
    dark_bark = MATERIALS.get("tree_dark_bark", "#422D28")
    leaf = MATERIALS.get("tree_leaf", PALETTE["hero_green"])
    light_leaf = MATERIALS.get("tree_light_leaf", PALETTE["hero_leaf"])
    heart = MATERIALS.get("tree_heart", "#7DE2A7")

    armature_data = bpy.data.armatures.new("world_tree_armature_data")
    armature = bpy.data.objects.new("world_tree_armature", armature_data)
    bpy.context.collection.objects.link(armature)
    bpy.context.view_layer.objects.active = armature
    armature.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")
    bones = {}
    layout = {
        "root": ((0, 0, 0), (0, 0, 0.25), None),
        "trunk": ((0, 0, 0.25), (0, 0, 2.8), "root"),
        "crown": ((0, 0, 2.4), (0, 0, 3.3), "trunk"),
        "branch.L": ((0, 0, 2.25), (-1.5, 0.0, 3.2), "trunk"),
        "branch.R": ((0, 0, 2.25), (1.5, 0.0, 3.2), "trunk"),
    }
    for name, (head, tail, parent) in layout.items():
        bone = armature.data.edit_bones.new(name)
        bone.head, bone.tail = head, tail
        if parent:
            bone.parent = bones[parent]
        bones[name] = bone
    bpy.ops.object.mode_set(mode="OBJECT")
    armature.select_set(False)

    objects = []
    trunk = add_cone("world_tree_trunk", (0, 0, 1.55), 0.72, 0.38, 2.9, dark_bark if damaged else bark, 9)
    parent_to_bone(trunk, armature, "trunk")
    objects.append(trunk)
    for side, sign in (("L", -1), ("R", 1)):
        branch = add_cylinder_between(
            f"world_tree_branch_{side}", (0, 0, 2.25), (1.55 * sign, 0.10, 3.18), 0.22, bark, 7
        )
        parent_to_bone(branch, armature, f"branch.{side}")
        objects.append(branch)
    roots = ((-1.15, -0.2), (-0.58, -0.65), (0.62, -0.62), (1.18, -0.15), (0.0, 0.52))
    for index, (x, y) in enumerate(roots):
        root = add_cone(
            f"world_tree_root_{index}", (x * 0.48, y * 0.48, 0.18), 0.25, 0.04, 1.55,
            dark_bark if damaged and index % 2 else bark, 7, (0, math.radians(68), math.atan2(y, x)),
        )
        parent_to_bone(root, armature, "root")
        objects.append(root)
    foliage_count = 7 if damaged else 12
    for index in range(foliage_count):
        angle = index * math.tau / max(1, foliage_count)
        radius = 0.55 + (index % 3) * 0.34
        x, y = math.cos(angle) * radius, math.sin(angle) * radius * 0.55
        z = 3.03 + (index % 4) * 0.22
        foliage = add_ico(
            f"world_tree_foliage_{index}", (x, y, z),
            (0.66 + (index % 2) * 0.12, 0.54, 0.58),
            light_leaf if index % 3 == 0 else leaf,
            1,
        )
        parent_to_bone(foliage, armature, "crown")
        objects.append(foliage)
    heart_obj = add_ico("world_tree_heart", (0, -0.43, 1.65), (0.22, 0.12, 0.34), heart, 2)
    parent_to_bone(heart_obj, armature, "trunk")
    objects.append(heart_obj)
    if damaged:
        for index, x in enumerate((-0.18, 0.14)):
            crack = add_cube(f"tree_crack_{index}", (x, -0.58, 1.28 + index * 0.5), (0.05, 0.025, 0.42), heart)
            crack.rotation_euler.y = (-0.24 if index == 0 else 0.32)
            parent_to_bone(crack, armature, "trunk")
            objects.append(crack)

    # A real armature action drives a subtle crown sway in both states.
    armature.animation_data_create()
    action = bpy.data.actions.new(f"world_tree_{'damaged' if damaged else 'healthy'}_idle")
    action.use_fake_user = True
    armature.animation_data.action = action
    for frame, angle in ((1, -0.025), (4, 0.025), (6, -0.025)):
        pose_bone = armature.pose.bones["crown"]
        pose_bone.rotation_mode = "XYZ"
        pose_bone.rotation_euler.y = angle
        pose_bone.keyframe_insert("rotation_euler", frame=frame, group="crown")
    return BuiltModel(armature, objects, {"state": "damaged" if damaged else "healthy", "rigged": True})


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
    base = MATERIALS.get("prop_stone", PALETTE["stone"])
    crystal = MATERIALS.get(f"prop_crystal_{variant}", ("#5FCAD2", "#A06DD0", "#E19C3B")[variant % 3])
    objects = [add_ico("crystal_base", (0, 0, 0.20), (0.58, 0.46, 0.26), base)]
    for index in range(3):
        shard = add_cone(
            f"crystal_shard_{index}",
            ((index - 1) * 0.26, 0.02 * index, 0.62 + index * 0.10),
            0.17, 0.0, 1.05 + index * 0.16, crystal, 6,
            (math.radians(index * 4), math.radians((index - 1) * 10), 0),
        )
        objects.append(shard)
    return BuiltModel(None, objects, {"variant": variant, "prop": "crystal_cluster"})


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
        cube("pack_body", (0, 0, 0.90), (1.15, 0.48, 1.05), wood, bevel=0.10)
        cube("pack_flap", (0, -0.28, 1.30), (1.0, 0.12, 0.36), green, bevel=0.08)
        cube("pack_buckle", (0, -0.42, 0.98), (0.24, 0.10, 0.26), gold)
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

    return BuiltModel(None, objects, {"uiIcon": key.removeprefix("ui_"), "touchOnlyUI": True})


def build_potion_icon(tier: int) -> BuiltModel:
    glass = MATERIALS.get(f"potion_glass_{tier}", "#B9D6CF")
    liquid_colors = ("#6CCB78", "#63C8B7", "#5EA7D8", "#986BD2", "#D35F9A", "#F0B84B")
    liquid = MATERIALS.get(f"potion_liquid_{tier}", liquid_colors[tier - 1])
    cork = MATERIALS.get("potion_cork", "#8B6138")
    gold = MATERIALS.get("potion_gold", PALETTE["hero_gold"], True)
    objects = [
        # The liquid defines the bottle's large readable color mass. A separate
        # opaque outer glass shell would hide tier color at a 96 px icon size.
        add_ico("potion_bottle_liquid", (0, 0, 0.62), (0.52, 0.34, 0.58), liquid, 2),
        add_cube("potion_glass_highlight", (-0.19, -0.30, 0.72), (0.075, 0.025, 0.30), glass, 0.02),
        add_cone("potion_neck", (0, 0, 1.06), 0.20, 0.18, 0.40, glass, 8),
        add_cone("potion_cork", (0, 0, 1.29), 0.18, 0.15, 0.22, cork, 7),
        add_torus("potion_band", (0, 0, 1.16), 0.21, 0.035, gold),
    ]
    return BuiltModel(None, objects, {"tier": tier, "heal_icon": True})
