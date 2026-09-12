from __future__ import annotations

import math

import bpy

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
    """Rigid-skin one mesh part to a real armature bone.

    A one-bone vertex group avoids Blender's bone-parent tail offset and gives the
    procedural multipart character the same evaluated transform contract as a
    conventionally weighted mesh.
    """
    if obj.type != "MESH":
        raise TypeError(f"Bone attachment requires a mesh object, got {obj.type}")
    # Armature modifiers evaluate vertices in object space. Bake each procedural
    # primitive's object transform first so rest-pose deformation is identity.
    # Force a depsgraph update because primitives are scaled immediately before this.
    bpy.context.view_layer.update()
    obj.data.transform(obj.matrix_world.copy())
    obj.matrix_world.identity()
    vertex_group = obj.vertex_groups.new(name=bone_name)
    vertex_group.add(range(len(obj.data.vertices)), 1.0, "REPLACE")
    modifier = obj.modifiers.new("HD_ARMATURE", "ARMATURE")
    modifier.object = armature
    modifier.use_deform_preserve_volume = False


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
    _reset_pose(armature)
    armature.animation_data.action = actions["idle"]
    return actions


def stack_actions_for_single_render(
    armature: bpy.types.Object,
    actions: dict[str, bpy.types.Action],
) -> dict[str, list[int]]:
    """Arrange all clips as consecutive NLA strips so one render call emits every frame.

    Mesa software rendering can retain a large context allocation between separate
    render operator calls. A single NLA animation call is both faster and bounded.
    """
    armature.animation_data.action = None
    for track in list(armature.animation_data.nla_tracks):
        armature.animation_data.nla_tracks.remove(track)
    track = armature.animation_data.nla_tracks.new()
    track.name = "HD_EXPORT_CLIPS"
    global_frame = 1
    mapping: dict[str, list[int]] = {}
    for clip, frame_count in CLIPS.items():
        action = actions[clip]
        strip = track.strips.new(clip, global_frame, action)
        strip.action_frame_start = 1
        strip.action_frame_end = frame_count
        strip.frame_start = global_frame
        strip.frame_end = global_frame + frame_count - 1
        strip.extrapolation = "NOTHING"
        strip.blend_type = "REPLACE"
        mapping[clip] = list(range(global_frame, global_frame + frame_count))
        global_frame += frame_count
    return mapping


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
    # A planted, asymmetric breathing loop: shoulders counter-rotate while the head
    # settles a fraction later, avoiding whole-body mechanical bobbing.
    base = {
        "chest": (-0.012, 0.0, -0.018),
        "head": (0.010, 0.0, 0.025),
        "upper_arm.L": (0.025, -0.05, -0.07),
        "forearm.L": (-0.018, 0.0, -0.025),
        "upper_arm.R": (-0.018, 0.05, 0.055),
        "forearm.R": (0.012, 0.0, 0.018),
    }
    inhale = {
        "chest": (0.022, 0.0, 0.024),
        "head": (-0.014, 0.0, -0.018),
        "upper_arm.L": (0.010, -0.035, -0.045),
        "forearm.L": (-0.008, 0.0, -0.010),
        "upper_arm.R": (-0.008, 0.035, 0.040),
        "forearm.R": (0.006, 0.0, 0.010),
    }
    _key(armature, 1, base, {"chest": (0.0, 0.0, 0.0)})
    _key(armature, 1 + count // 2, inhale, {"chest": (0.0, 0.0, 0.022)})
    _key(armature, count, base, {"chest": (0.0, 0.0, 0.0)})


def _author_attack(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0),
        "head": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.0, 0.0, 0.0), "forearm.R": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0), "forearm.L": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    # Anticipation compresses and winds away from the strike direction.
    _key(armature, 3, {
        "pelvis": (0.0, 0.0, -0.12), "chest": (0.07, 0.0, -0.38),
        "head": (-0.04, 0.0, 0.13),
        "upper_arm.R": (-0.72, 0.28, 0.78), "forearm.R": (-0.62, 0.0, 0.20),
        "upper_arm.L": (-0.30, -0.28, -0.62), "forearm.L": (-0.46, 0.0, -0.24),
    }, {"root": (0.0, 0.0, -0.025)})
    # Frame five is the release/impact silhouette with the strongest line of action.
    _key(armature, 5, {
        "pelvis": (0.0, 0.0, 0.18), "chest": (-0.10, 0.0, 0.48),
        "head": (0.04, 0.0, -0.16),
        "upper_arm.R": (0.64, -0.18, -0.92), "forearm.R": (0.27, 0.0, -0.15),
        "upper_arm.L": (0.42, 0.22, 0.52), "forearm.L": (0.34, 0.0, 0.20),
    }, {"root": (0.0, -0.035, 0.015)})
    _key(armature, 6, {
        "pelvis": (0.0, 0.0, 0.10), "chest": (-0.05, 0.0, 0.28),
        "head": (0.02, 0.0, -0.08),
        "upper_arm.R": (0.42, -0.10, -0.62), "forearm.R": (0.18, 0.0, -0.08),
        "upper_arm.L": (0.28, 0.14, 0.33), "forearm.L": (0.20, 0.0, 0.12),
    }, {"root": (0.0, -0.015, 0.006)})
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)})


def _author_hit(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0),
        "head": (0.0, 0.0, 0.0), "upper_arm.L": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 2, {
        "pelvis": (-0.10, 0.0, 0.08), "chest": (-0.38, 0.0, 0.17),
        "head": (0.27, 0.0, -0.11),
        "upper_arm.L": (0.42, 0.0, -0.26), "upper_arm.R": (0.38, 0.0, 0.26),
    }, {"root": (0.08, 0.0, -0.025)})
    _key(armature, 3, {
        "pelvis": (0.04, 0.0, -0.03), "chest": (0.12, 0.0, -0.06),
        "head": (-0.08, 0.0, 0.04),
        "upper_arm.L": (-0.10, 0.0, 0.06), "upper_arm.R": (-0.08, 0.0, -0.06),
    }, {"root": (-0.02, 0.0, 0.0)})
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)})


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
