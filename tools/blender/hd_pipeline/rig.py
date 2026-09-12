from __future__ import annotations

import math
from typing import Iterable

import bpy
from mathutils import Matrix

from .config import CLIPS, REQUIRED_BONES


BONE_LAYOUT: dict[str, tuple[tuple[float, float, float], tuple[float, float, float], str | None]] = {
    "root": ((0.0, 0.0, 0.02), (0.0, 0.0, 0.20), None),
    "pelvis": ((0.0, 0.0, 0.20), (0.0, 0.0, 0.72), "root"),
    "spine": ((0.0, 0.0, 0.72), (0.0, 0.0, 1.12), "pelvis"),
    "chest": ((0.0, 0.0, 1.12), (0.0, 0.0, 1.48), "spine"),
    "neck": ((0.0, 0.0, 1.48), (0.0, 0.0, 1.62), "chest"),
    "head": ((0.0, 0.0, 1.62), (0.0, 0.0, 1.97), "neck"),
    "upper_arm.L": ((0.0, 0.0, 1.40), (-0.43, 0.0, 1.28), "chest"),
    "forearm.L": ((-0.43, 0.0, 1.28), (-0.70, -0.04, 1.02), "upper_arm.L"),
    "hand.L": ((-0.70, -0.04, 1.02), (-0.78, -0.06, 0.92), "forearm.L"),
    "upper_arm.R": ((0.0, 0.0, 1.40), (0.43, 0.0, 1.28), "chest"),
    "forearm.R": ((0.43, 0.0, 1.28), (0.70, -0.04, 1.02), "upper_arm.R"),
    "hand.R": ((0.70, -0.04, 1.02), (0.78, -0.06, 0.92), "forearm.R"),
    "thigh.L": ((-0.20, 0.0, 0.68), (-0.22, 0.0, 0.34), "pelvis"),
    "shin.L": ((-0.22, 0.0, 0.34), (-0.22, -0.02, 0.10), "thigh.L"),
    "foot.L": ((-0.22, -0.02, 0.10), (-0.22, -0.22, 0.05), "shin.L"),
    "thigh.R": ((0.20, 0.0, 0.68), (0.22, 0.0, 0.34), "pelvis"),
    "shin.R": ((0.22, 0.0, 0.34), (0.22, -0.02, 0.10), "thigh.R"),
    "foot.R": ((0.22, -0.02, 0.10), (0.22, -0.22, 0.05), "shin.R"),
    "weapon_socket": ((0.78, -0.06, 0.92), (0.78, -0.06, 1.10), "hand.R"),
    "helmet_socket": ((0.0, 0.0, 1.88), (0.0, 0.0, 2.10), "head"),
    "armor_socket": ((0.0, -0.04, 1.18), (0.0, -0.04, 1.42), "chest"),
    "boot_socket.L": ((-0.22, -0.03, 0.12), (-0.22, -0.03, 0.25), "shin.L"),
    "boot_socket.R": ((0.22, -0.03, 0.12), (0.22, -0.03, 0.25), "shin.R"),
    "ring_socket.L": ((-0.77, -0.06, 0.96), (-0.77, -0.06, 1.05), "hand.L"),
    "ring_socket.R": ((0.77, -0.06, 0.96), (0.77, -0.06, 1.05), "hand.R"),
}


def create_standard_armature(name: str, scale: float = 1.0) -> bpy.types.Object:
    armature_data = bpy.data.armatures.new(f"{name}_armature_data")
    armature = bpy.data.objects.new(f"{name}_armature", armature_data)
    bpy.context.collection.objects.link(armature)
    armature.show_in_front = True
    armature.data.display_type = "OCTAHEDRAL"

    bpy.context.view_layer.objects.active = armature
    armature.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")
    created = {}
    for bone_name in REQUIRED_BONES:
        head, tail, parent_name = BONE_LAYOUT[bone_name]
        bone = armature.data.edit_bones.new(bone_name)
        bone.head = tuple(value * scale for value in head)
        bone.tail = tuple(value * scale for value in tail)
        if parent_name:
            bone.parent = created[parent_name]
            bone.use_connect = False
        created[bone_name] = bone
    bpy.ops.object.mode_set(mode="POSE")
    for pose_bone in armature.pose.bones:
        pose_bone.rotation_mode = "XYZ"
    bpy.ops.object.mode_set(mode="OBJECT")
    armature.select_set(False)
    return armature


def parent_to_bone(obj: bpy.types.Object, armature: bpy.types.Object, bone_name: str) -> None:
    world_matrix: Matrix = obj.matrix_world.copy()
    obj.parent = armature
    obj.parent_type = "BONE"
    obj.parent_bone = bone_name
    obj.matrix_world = world_matrix


def author_standard_actions(armature: bpy.types.Object, name: str) -> dict[str, bpy.types.Action]:
    armature.animation_data_create()
    actions: dict[str, bpy.types.Action] = {}
    for clip, frame_count in CLIPS.items():
        action = bpy.data.actions.new(f"{name}_{clip}")
        action.use_fake_user = True
        armature.animation_data.action = action
        _reset_pose(armature)
        if clip == "idle":
            _author_idle(armature, frame_count)
        elif clip == "attack":
            _author_attack(armature, frame_count)
        elif clip == "hit":
            _author_hit(armature, frame_count)
        elif clip == "death":
            _author_death(armature, frame_count)
        for curve in action.fcurves:
            for keyframe in curve.keyframe_points:
                keyframe.interpolation = "BEZIER" if clip == "idle" else "LINEAR"
        actions[clip] = action
    armature.animation_data.action = actions["idle"]
    return actions


def _reset_pose(armature: bpy.types.Object) -> None:
    for bone in armature.pose.bones:
        bone.rotation_euler = (0.0, 0.0, 0.0)
        bone.location = (0.0, 0.0, 0.0)
        bone.scale = (1.0, 1.0, 1.0)


def _key(
    armature: bpy.types.Object,
    frame: int,
    rotations: dict[str, tuple[float, float, float]] | None = None,
    locations: dict[str, tuple[float, float, float]] | None = None,
    scales: dict[str, tuple[float, float, float]] | None = None,
) -> None:
    rotations = rotations or {}
    locations = locations or {}
    scales = scales or {}
    touched = set(rotations) | set(locations) | set(scales)
    for bone_name in touched:
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


def _author_idle(armature: bpy.types.Object, count: int) -> None:
    base = {
        "upper_arm.L": (0.02, -0.05, -0.05),
        "upper_arm.R": (-0.02, 0.05, 0.05),
    }
    _key(armature, 1, base, {"chest": (0.0, 0.0, 0.0)})
    lifted = dict(base)
    lifted["chest"] = (0.015, 0.0, 0.0)
    _key(armature, 1 + count // 2, lifted, {"chest": (0.0, 0.0, 0.025)})
    _key(armature, count, base, {"chest": (0.0, 0.0, 0.0)})


def _author_attack(armature: bpy.types.Object, count: int) -> None:
    _key(armature, 1, {
        "chest": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.0, 0.0, 0.0),
        "forearm.R": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0),
    })
    _key(armature, 3, {
        "chest": (0.05, 0.0, -0.30),
        "upper_arm.R": (-0.65, 0.25, 0.70),
        "forearm.R": (-0.55, 0.0, 0.15),
        "upper_arm.L": (-0.25, -0.25, -0.55),
        "forearm.L": (-0.40, 0.0, -0.20),
    })
    _key(armature, 5, {
        "chest": (-0.08, 0.0, 0.38),
        "upper_arm.R": (0.55, -0.15, -0.80),
        "forearm.R": (0.20, 0.0, -0.10),
        "upper_arm.L": (0.35, 0.20, 0.45),
        "forearm.L": (0.30, 0.0, 0.15),
    })
    _key(armature, count, {
        "chest": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.0, 0.0, 0.0),
        "forearm.R": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0),
        "forearm.L": (0.0, 0.0, 0.0),
    })


def _author_hit(armature: bpy.types.Object, count: int) -> None:
    _key(armature, 1, {"chest": (0.0, 0.0, 0.0), "head": (0.0, 0.0, 0.0)})
    _key(armature, 2, {
        "chest": (-0.32, 0.0, 0.12),
        "head": (0.22, 0.0, -0.08),
        "upper_arm.L": (0.35, 0.0, -0.20),
        "upper_arm.R": (0.35, 0.0, 0.20),
    })
    _key(armature, count, {
        "chest": (0.0, 0.0, 0.0),
        "head": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.0, 0.0, 0.0),
    })


def _author_death(armature: bpy.types.Object, count: int) -> None:
    _key(armature, 1, {"root": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0)}, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 5, {
        "root": (0.0, 0.45, 0.28),
        "chest": (0.75, 0.0, -0.18),
        "head": (0.55, 0.0, 0.12),
        "upper_arm.L": (0.65, 0.0, -0.5),
        "upper_arm.R": (0.35, 0.0, 0.7),
    }, {"root": (0.0, 0.0, -0.22)})
    final_rotation = {
        "root": (0.0, 1.18, 0.18),
        "chest": (0.95, 0.0, -0.25),
        "head": (0.68, 0.0, 0.15),
        "upper_arm.L": (0.80, 0.0, -0.65),
        "upper_arm.R": (0.55, 0.0, 0.75),
    }
    _key(armature, count - 1, final_rotation, {"root": (0.0, 0.0, -0.46)})
    _key(armature, count, final_rotation, {"root": (0.0, 0.0, -0.46)})
