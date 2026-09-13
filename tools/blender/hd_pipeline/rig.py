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


def author_standard_actions(
    armature: bpy.types.Object,
    name: str,
    profile: str = "standard",
) -> dict[str, bpy.types.Action]:
    """Author the locked clip contract with character-specific premium motion language.

    Equipment overlays and characters not yet upgraded continue to use ``standard``.
    Premium regular enemies opt into explicit profiles so their shared 25-bone export
    contract can still communicate distinct mass, temperament, anticipation, and impact.
    """
    profile_authors = {
        "standard": {
            "idle": _author_idle,
            "attack": _author_attack,
            "hit": _author_hit,
            "death": _author_death,
        },
        "rootling-skirmisher-v2": {
            "idle": _author_rootling_idle,
            "attack": _author_rootling_attack,
            "hit": _author_rootling_hit,
            "death": _author_rootling_death,
        },
        "stonekin-juggernaut-v2": {
            "idle": _author_stonekin_idle,
            "attack": _author_stonekin_attack,
            "hit": _author_stonekin_hit,
            "death": _author_stonekin_death,
        },
        "gloom-wolf-pouncer-v2": {
            "idle": _author_gloom_wolf_idle,
            "attack": _author_gloom_wolf_attack,
            "hit": _author_gloom_wolf_hit,
            "death": _author_gloom_wolf_death,
        },
        "fungal-brute-brawler-v2": {
            "idle": _author_fungal_brute_idle,
            "attack": _author_fungal_brute_attack,
            "hit": _author_fungal_brute_hit,
            "death": _author_fungal_brute_death,
        },
        "ancient-golem-ground-slam-v2": {
            "idle": _author_ancient_golem_idle,
            "attack": _author_ancient_golem_ground_slam,
            "hit": _author_ancient_golem_hit,
            "death": _author_ancient_golem_death,
        },
        "thorn-matriarch-thorn-cage-v2": {
            "idle": _author_thorn_matriarch_idle,
            "attack": _author_thorn_matriarch_thorn_cage,
            "hit": _author_thorn_matriarch_hit,
            "death": _author_thorn_matriarch_death,
        },
        "ember-wyrm-flame-sweep-v2": {
            "idle": _author_ember_wyrm_idle,
            "attack": _author_ember_wyrm_flame_sweep,
            "hit": _author_ember_wyrm_hit,
            "death": _author_ember_wyrm_death,
        },
        "void-knight-void-charge-v2": {
            "idle": _author_void_knight_idle,
            "attack": _author_void_knight_void_charge,
            "hit": _author_void_knight_hit,
            "death": _author_void_knight_death,
        },
    }
    if profile not in profile_authors:
        raise ValueError(f"Unknown animation profile: {profile}")

    armature.animation_data_create()
    actions: dict[str, bpy.types.Action] = {}
    for clip, frame_count in CLIPS.items():
        action = bpy.data.actions.new(f"{name}_{clip}")
        action.use_fake_user = True
        armature.animation_data.action = action
        _reset_pose(armature)
        profile_authors[profile][clip](armature, frame_count)
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


# Premium regular-enemy profiles -------------------------------------------------
# These deliberately retain the universal clip counts while giving each enemy a
# readable gameplay verb: feint-and-swipe, crushing slam, pounce, or body punch.


def _author_rootling_idle(armature: bpy.types.Object, count: int) -> None:
    crouch = {
        "pelvis": (0.0, 0.0, -0.035), "chest": (-0.025, 0.0, -0.045),
        "head": (0.025, -0.015, 0.075),
        "upper_arm.L": (0.04, -0.06, -0.12), "forearm.L": (-0.05, 0.0, -0.06),
        "upper_arm.R": (-0.025, 0.05, 0.10), "forearm.R": (0.035, 0.0, 0.04),
    }
    listen = {
        "pelvis": (0.0, 0.0, 0.025), "chest": (0.035, 0.0, 0.045),
        "head": (-0.045, 0.025, -0.085),
        "upper_arm.L": (0.015, -0.035, -0.07), "forearm.L": (-0.02, 0.0, -0.025),
        "upper_arm.R": (-0.012, 0.035, 0.065), "forearm.R": (0.015, 0.0, 0.02),
    }
    _key(armature, 1, crouch, {"chest": (0.0, 0.0, -0.008)})
    _key(armature, 1 + count // 2, listen, {"chest": (0.0, 0.0, 0.026)})
    _key(armature, count, crouch, {"chest": (0.0, 0.0, -0.008)})


def _author_rootling_attack(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0), "head": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0), "forearm.L": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.0, 0.0, 0.0), "forearm.R": (0.0, 0.0, 0.0),
        "thigh.L": (0.0, 0.0, 0.0), "thigh.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 3, {
        "pelvis": (0.05, 0.0, -0.20), "chest": (0.06, 0.0, -0.48), "head": (-0.08, 0.0, 0.20),
        "upper_arm.L": (-0.50, -0.22, -0.86), "forearm.L": (-0.42, 0.0, -0.28),
        "upper_arm.R": (-0.72, 0.26, 0.78), "forearm.R": (-0.62, 0.0, 0.28),
        "thigh.L": (-0.10, 0.0, 0.08), "thigh.R": (0.08, 0.0, -0.08),
    }, {"root": (-0.045, 0.02, -0.045)})
    _key(armature, 5, {
        "pelvis": (-0.08, 0.0, 0.30), "chest": (-0.10, 0.0, 0.63), "head": (0.05, 0.0, -0.22),
        "upper_arm.L": (0.46, 0.16, 0.67), "forearm.L": (0.31, 0.0, 0.23),
        "upper_arm.R": (0.70, -0.22, -1.02), "forearm.R": (0.38, 0.0, -0.24),
        "thigh.L": (0.08, 0.0, -0.06), "thigh.R": (-0.08, 0.0, 0.06),
    }, {"root": (0.06, -0.09, 0.015)})
    _key(armature, 6, {
        "pelvis": (-0.04, 0.0, 0.16), "chest": (-0.05, 0.0, 0.34), "head": (0.02, 0.0, -0.11),
        "upper_arm.L": (0.25, 0.08, 0.35), "forearm.L": (0.16, 0.0, 0.12),
        "upper_arm.R": (0.42, -0.12, -0.65), "forearm.R": (0.20, 0.0, -0.12),
        "thigh.L": (0.04, 0.0, -0.03), "thigh.R": (-0.04, 0.0, 0.03),
    }, {"root": (0.03, -0.04, 0.006)})
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)})


def _author_rootling_hit(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0), "head": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0), "upper_arm.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 2, {
        "pelvis": (-0.12, 0.0, 0.10), "chest": (-0.48, 0.0, 0.22), "head": (0.38, 0.0, -0.18),
        "upper_arm.L": (0.60, 0.0, -0.38), "upper_arm.R": (0.52, 0.0, 0.36),
    }, {"root": (0.11, 0.0, -0.03)})
    _key(armature, 3, {
        "pelvis": (0.05, 0.0, -0.04), "chest": (0.15, 0.0, -0.07), "head": (-0.12, 0.0, 0.06),
        "upper_arm.L": (-0.13, 0.0, 0.08), "upper_arm.R": (-0.11, 0.0, -0.08),
    }, {"root": (-0.025, 0.0, 0.0)})
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)})


def _author_rootling_death(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "root": (0.0, 0.0, 0.0), "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0),
        "head": (0.0, 0.0, 0.0), "upper_arm.L": (0.0, 0.0, 0.0), "upper_arm.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 4, {
        "root": (0.0, 0.30, -0.12), "pelvis": (0.15, 0.0, -0.20), "chest": (-0.55, 0.0, 0.22),
        "head": (0.42, 0.0, -0.18), "upper_arm.L": (0.58, 0.0, -0.62), "upper_arm.R": (0.38, 0.0, 0.70),
    }, {"root": (-0.05, 0.0, -0.14)})
    final = {
        "root": (0.0, 1.00, -0.25), "pelvis": (0.18, 0.0, -0.28), "chest": (0.82, 0.0, 0.24),
        "head": (0.58, 0.0, -0.18), "upper_arm.L": (0.80, 0.0, -0.70), "upper_arm.R": (0.60, 0.0, 0.74),
    }
    _key(armature, count - 1, final, {"root": (-0.35, 0.0, -0.40)})
    _key(armature, count, final, {"root": (-0.35, 0.0, -0.40)})


def _author_stonekin_idle(armature: bpy.types.Object, count: int) -> None:
    settle = {
        "pelvis": (0.0, 0.0, -0.018), "chest": (-0.012, 0.0, -0.018), "head": (0.008, 0.0, 0.018),
        "upper_arm.L": (0.015, -0.025, -0.035), "forearm.L": (-0.012, 0.0, -0.018),
        "upper_arm.R": (-0.012, 0.025, 0.030), "forearm.R": (0.010, 0.0, 0.015),
    }
    brace = {
        "pelvis": (0.0, 0.0, 0.014), "chest": (0.018, 0.0, 0.022), "head": (-0.012, 0.0, -0.014),
        "upper_arm.L": (0.008, -0.016, -0.022), "forearm.L": (-0.006, 0.0, -0.008),
        "upper_arm.R": (-0.006, 0.016, 0.020), "forearm.R": (0.005, 0.0, 0.008),
    }
    _key(armature, 1, settle, {"root": (-0.008, 0.0, -0.006)})
    _key(armature, 1 + count // 2, brace, {"root": (0.008, 0.0, 0.012)})
    _key(armature, count, settle, {"root": (-0.008, 0.0, -0.006)})


def _author_stonekin_attack(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0), "head": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0), "forearm.L": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.0, 0.0, 0.0), "forearm.R": (0.0, 0.0, 0.0),
        "thigh.L": (0.0, 0.0, 0.0), "thigh.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 3, {
        "pelvis": (0.10, 0.0, -0.05), "chest": (-0.22, 0.0, 0.06), "head": (0.14, 0.0, -0.04),
        "upper_arm.L": (-1.05, -0.12, -0.72), "forearm.L": (-0.84, 0.0, -0.36),
        "upper_arm.R": (-1.05, 0.12, 0.72), "forearm.R": (-0.84, 0.0, 0.36),
        "thigh.L": (-0.08, 0.0, 0.03), "thigh.R": (0.08, 0.0, -0.03),
    }, {"root": (0.0, 0.04, 0.055)})
    _key(armature, 5, {
        "pelvis": (-0.20, 0.0, 0.04), "chest": (0.68, 0.0, -0.08), "head": (-0.42, 0.0, 0.05),
        "upper_arm.L": (0.88, 0.08, 0.30), "forearm.L": (0.58, 0.0, 0.16),
        "upper_arm.R": (0.88, -0.08, -0.30), "forearm.R": (0.58, 0.0, -0.16),
        "thigh.L": (0.13, 0.0, -0.03), "thigh.R": (-0.13, 0.0, 0.03),
    }, {"root": (0.0, -0.08, -0.075)})
    _key(armature, 6, {
        "pelvis": (-0.12, 0.0, 0.02), "chest": (0.40, 0.0, -0.04), "head": (-0.24, 0.0, 0.03),
        "upper_arm.L": (0.52, 0.04, 0.16), "forearm.L": (0.34, 0.0, 0.08),
        "upper_arm.R": (0.52, -0.04, -0.16), "forearm.R": (0.34, 0.0, -0.08),
        "thigh.L": (0.07, 0.0, -0.02), "thigh.R": (-0.07, 0.0, 0.02),
    }, {"root": (0.0, -0.04, -0.035)})
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)})


def _author_stonekin_hit(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0), "head": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0), "upper_arm.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 2, {
        "pelvis": (-0.05, 0.0, 0.035), "chest": (-0.20, 0.0, 0.08), "head": (0.13, 0.0, -0.05),
        "upper_arm.L": (0.21, 0.0, -0.14), "upper_arm.R": (0.19, 0.0, 0.14),
    }, {"root": (0.035, 0.0, -0.012)})
    _key(armature, 3, {
        "pelvis": (0.025, 0.0, -0.018), "chest": (0.08, 0.0, -0.03), "head": (-0.05, 0.0, 0.02),
        "upper_arm.L": (-0.06, 0.0, 0.04), "upper_arm.R": (-0.05, 0.0, -0.04),
    }, {"root": (-0.012, 0.0, 0.0)})
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)})


def _author_stonekin_death(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "root": (0.0, 0.0, 0.0), "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0),
        "head": (0.0, 0.0, 0.0), "upper_arm.L": (0.0, 0.0, 0.0), "upper_arm.R": (0.0, 0.0, 0.0),
        "thigh.L": (0.0, 0.0, 0.0), "thigh.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 5, {
        "root": (0.0, 0.22, 0.08), "pelvis": (0.12, 0.0, -0.10), "chest": (0.42, 0.0, 0.10),
        "head": (0.28, 0.0, -0.08), "upper_arm.L": (0.48, 0.0, -0.30), "upper_arm.R": (0.30, 0.0, 0.46),
        "thigh.L": (0.16, 0.0, -0.06), "thigh.R": (-0.12, 0.0, 0.05),
    }, {"root": (0.0, 0.0, -0.19)})
    final = {
        "root": (0.0, 1.06, 0.14), "pelvis": (0.20, 0.0, -0.18), "chest": (0.76, 0.0, 0.14),
        "head": (0.52, 0.0, -0.10), "upper_arm.L": (0.70, 0.0, -0.48), "upper_arm.R": (0.52, 0.0, 0.58),
        "thigh.L": (0.28, 0.0, -0.10), "thigh.R": (-0.20, 0.0, 0.08),
    }
    _key(armature, count - 1, final, {"root": (0.0, 0.0, -0.48)})
    _key(armature, count, final, {"root": (0.0, 0.0, -0.48)})


def _author_gloom_wolf_idle(armature: bpy.types.Object, count: int) -> None:
    stalk = {
        "pelvis": (0.018, 0.0, -0.035), "spine": (-0.025, 0.0, -0.018), "chest": (0.035, 0.0, 0.025),
        "head": (-0.035, -0.02, 0.045),
        "upper_arm.L": (0.018, 0.0, -0.035), "upper_arm.R": (-0.018, 0.0, 0.035),
        "thigh.L": (-0.015, 0.0, 0.025), "thigh.R": (0.015, 0.0, -0.025),
    }
    scent = {
        "pelvis": (-0.016, 0.0, 0.025), "spine": (0.030, 0.0, 0.020), "chest": (-0.045, 0.0, -0.030),
        "head": (0.055, 0.03, -0.065),
        "upper_arm.L": (-0.012, 0.0, 0.025), "upper_arm.R": (0.012, 0.0, -0.025),
        "thigh.L": (0.012, 0.0, -0.018), "thigh.R": (-0.012, 0.0, 0.018),
    }
    _key(armature, 1, stalk, {"root": (-0.015, 0.0, -0.012)})
    _key(armature, 1 + count // 2, scent, {"root": (0.015, 0.0, 0.018)})
    _key(armature, count, stalk, {"root": (-0.015, 0.0, -0.012)})


def _author_gloom_wolf_attack(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "pelvis": (0.0, 0.0, 0.0), "spine": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0),
        "head": (0.0, 0.0, 0.0), "upper_arm.L": (0.0, 0.0, 0.0), "forearm.L": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.0, 0.0, 0.0), "forearm.R": (0.0, 0.0, 0.0),
        "thigh.L": (0.0, 0.0, 0.0), "shin.L": (0.0, 0.0, 0.0),
        "thigh.R": (0.0, 0.0, 0.0), "shin.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 3, {
        "pelvis": (-0.18, 0.0, -0.05), "spine": (0.22, 0.0, 0.04), "chest": (0.20, 0.0, -0.03),
        "head": (-0.18, 0.0, 0.04), "upper_arm.L": (-0.35, 0.05, -0.10), "forearm.L": (0.44, 0.0, 0.04),
        "upper_arm.R": (-0.31, -0.05, 0.10), "forearm.R": (0.40, 0.0, -0.04),
        "thigh.L": (0.42, 0.0, -0.08), "shin.L": (-0.46, 0.0, 0.03),
        "thigh.R": (0.36, 0.0, 0.08), "shin.R": (-0.40, 0.0, -0.03),
    }, {"root": (0.0, 0.04, -0.11)})
    _key(armature, 5, {
        "pelvis": (0.24, 0.0, 0.05), "spine": (-0.26, 0.0, -0.05), "chest": (-0.38, 0.0, 0.04),
        "head": (0.38, 0.0, -0.08), "upper_arm.L": (0.74, -0.05, -0.18), "forearm.L": (-0.28, 0.0, 0.10),
        "upper_arm.R": (0.70, 0.05, 0.18), "forearm.R": (-0.25, 0.0, -0.10),
        "thigh.L": (-0.55, 0.0, 0.12), "shin.L": (0.36, 0.0, -0.05),
        "thigh.R": (-0.50, 0.0, -0.12), "shin.R": (0.33, 0.0, 0.05),
    }, {"root": (0.0, -0.02, 0.14)})
    _key(armature, 6, {
        "pelvis": (0.12, 0.0, 0.02), "spine": (-0.14, 0.0, -0.02), "chest": (-0.20, 0.0, 0.02),
        "head": (0.20, 0.0, -0.04), "upper_arm.L": (0.40, -0.02, -0.10), "forearm.L": (-0.15, 0.0, 0.05),
        "upper_arm.R": (0.38, 0.02, 0.10), "forearm.R": (-0.13, 0.0, -0.05),
        "thigh.L": (-0.28, 0.0, 0.06), "shin.L": (0.18, 0.0, -0.03),
        "thigh.R": (-0.25, 0.0, -0.06), "shin.R": (0.17, 0.0, 0.03),
    }, {"root": (0.0, -0.01, 0.06)})
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)})


def _author_gloom_wolf_hit(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "pelvis": (0.0, 0.0, 0.0), "spine": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0),
        "head": (0.0, 0.0, 0.0), "upper_arm.L": (0.0, 0.0, 0.0), "upper_arm.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 2, {
        "pelvis": (-0.18, 0.0, 0.12), "spine": (-0.28, 0.0, 0.10), "chest": (-0.42, 0.0, 0.18),
        "head": (0.56, 0.0, -0.22), "upper_arm.L": (0.36, 0.0, -0.20), "upper_arm.R": (0.33, 0.0, 0.20),
    }, {"root": (0.12, 0.0, -0.035)})
    _key(armature, 3, {
        "pelvis": (0.07, 0.0, -0.04), "spine": (0.11, 0.0, -0.04), "chest": (0.15, 0.0, -0.07),
        "head": (-0.20, 0.0, 0.08), "upper_arm.L": (-0.10, 0.0, 0.06), "upper_arm.R": (-0.09, 0.0, -0.06),
    }, {"root": (-0.035, 0.0, 0.0)})
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)})


def _author_gloom_wolf_death(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "root": (0.0, 0.0, 0.0), "pelvis": (0.0, 0.0, 0.0), "spine": (0.0, 0.0, 0.0),
        "chest": (0.0, 0.0, 0.0), "head": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0), "upper_arm.R": (0.0, 0.0, 0.0),
        "thigh.L": (0.0, 0.0, 0.0), "thigh.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 4, {
        "root": (0.0, 0.20, 0.18), "pelvis": (0.25, 0.0, -0.10), "spine": (0.36, 0.0, 0.08),
        "chest": (0.48, 0.0, -0.08), "head": (0.46, 0.0, 0.10),
        "upper_arm.L": (0.42, 0.0, -0.28), "upper_arm.R": (0.34, 0.0, 0.35),
        "thigh.L": (-0.32, 0.0, 0.15), "thigh.R": (0.28, 0.0, -0.12),
    }, {"root": (-0.04, 0.0, -0.16)})
    final = {
        "root": (0.0, 1.30, 0.22), "pelvis": (0.46, 0.0, -0.16), "spine": (0.60, 0.0, 0.12),
        "chest": (0.72, 0.0, -0.12), "head": (0.76, 0.0, 0.16),
        "upper_arm.L": (0.72, 0.0, -0.40), "upper_arm.R": (0.62, 0.0, 0.48),
        "thigh.L": (-0.52, 0.0, 0.22), "thigh.R": (0.46, 0.0, -0.18),
    }
    _key(armature, count - 1, final, {"root": (-0.30, 0.0, -0.40)})
    _key(armature, count, final, {"root": (-0.30, 0.0, -0.40)})


def _author_fungal_brute_idle(armature: bpy.types.Object, count: int) -> None:
    sway_left = {
        "pelvis": (0.0, 0.0, -0.028), "chest": (-0.030, 0.0, -0.055), "head": (0.050, 0.0, 0.085),
        "upper_arm.L": (0.025, -0.045, -0.075), "forearm.L": (-0.022, 0.0, -0.030),
        "upper_arm.R": (-0.018, 0.035, 0.060), "forearm.R": (0.016, 0.0, 0.022),
    }
    sway_right = {
        "pelvis": (0.0, 0.0, 0.022), "chest": (0.038, 0.0, 0.065), "head": (-0.060, 0.0, -0.095),
        "upper_arm.L": (0.012, -0.026, -0.044), "forearm.L": (-0.010, 0.0, -0.014),
        "upper_arm.R": (-0.010, 0.026, 0.040), "forearm.R": (0.008, 0.0, 0.013),
    }
    _key(armature, 1, sway_left, {"chest": (-0.012, 0.0, -0.008)})
    _key(armature, 1 + count // 2, sway_right, {"chest": (0.012, 0.0, 0.024)})
    _key(armature, count, sway_left, {"chest": (-0.012, 0.0, -0.008)})


def _author_fungal_brute_attack(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0), "head": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0), "forearm.L": (0.0, 0.0, 0.0),
        "upper_arm.R": (0.0, 0.0, 0.0), "forearm.R": (0.0, 0.0, 0.0),
        "thigh.L": (0.0, 0.0, 0.0), "thigh.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 3, {
        "pelvis": (0.02, 0.0, -0.18), "chest": (0.14, 0.0, -0.46), "head": (-0.14, 0.0, 0.22),
        "upper_arm.L": (-0.40, -0.18, -0.62), "forearm.L": (-0.52, 0.0, -0.18),
        "upper_arm.R": (-0.82, 0.30, 0.88), "forearm.R": (-0.75, 0.0, 0.34),
        "thigh.L": (-0.08, 0.0, 0.04), "thigh.R": (0.08, 0.0, -0.04),
    }, {"root": (-0.035, 0.035, -0.055)})
    _key(armature, 5, {
        "pelvis": (-0.08, 0.0, 0.24), "chest": (-0.18, 0.0, 0.62), "head": (0.18, 0.0, -0.30),
        "upper_arm.L": (0.36, 0.12, 0.46), "forearm.L": (0.25, 0.0, 0.16),
        "upper_arm.R": (0.90, -0.24, -1.05), "forearm.R": (0.50, 0.0, -0.30),
        "thigh.L": (0.10, 0.0, -0.04), "thigh.R": (-0.10, 0.0, 0.04),
    }, {"root": (0.055, -0.085, 0.005)})
    _key(armature, 6, {
        "pelvis": (-0.04, 0.0, 0.13), "chest": (-0.10, 0.0, 0.34), "head": (0.10, 0.0, -0.16),
        "upper_arm.L": (0.19, 0.06, 0.24), "forearm.L": (0.13, 0.0, 0.08),
        "upper_arm.R": (0.50, -0.12, -0.60), "forearm.R": (0.27, 0.0, -0.16),
        "thigh.L": (0.05, 0.0, -0.02), "thigh.R": (-0.05, 0.0, 0.02),
    }, {"root": (0.025, -0.04, 0.0)})
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)})


def _author_fungal_brute_hit(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0), "head": (0.0, 0.0, 0.0),
        "upper_arm.L": (0.0, 0.0, 0.0), "upper_arm.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 2, {
        "pelvis": (-0.10, 0.0, 0.06), "chest": (-0.36, 0.0, 0.18), "head": (0.52, 0.0, -0.28),
        "upper_arm.L": (0.38, 0.0, -0.24), "upper_arm.R": (0.34, 0.0, 0.24),
    }, {"root": (0.075, 0.0, -0.025)})
    _key(armature, 3, {
        "pelvis": (0.04, 0.0, -0.025), "chest": (0.12, 0.0, -0.06), "head": (-0.20, 0.0, 0.11),
        "upper_arm.L": (-0.10, 0.0, 0.06), "upper_arm.R": (-0.09, 0.0, -0.06),
    }, {"root": (-0.022, 0.0, 0.0)})
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)})


def _author_fungal_brute_death(armature: bpy.types.Object, count: int) -> None:
    neutral = {
        "root": (0.0, 0.0, 0.0), "pelvis": (0.0, 0.0, 0.0), "chest": (0.0, 0.0, 0.0),
        "head": (0.0, 0.0, 0.0), "upper_arm.L": (0.0, 0.0, 0.0), "upper_arm.R": (0.0, 0.0, 0.0),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 5, {
        "root": (0.0, 0.30, 0.18), "pelvis": (-0.08, 0.0, -0.12), "chest": (0.46, 0.0, -0.12),
        "head": (0.72, 0.0, 0.24), "upper_arm.L": (0.54, 0.0, -0.42), "upper_arm.R": (0.38, 0.0, 0.56),
    }, {"root": (0.0, 0.0, -0.21)})
    final = {
        "root": (0.0, 1.16, 0.12), "pelvis": (-0.12, 0.0, -0.20), "chest": (0.78, 0.0, -0.18),
        "head": (1.02, 0.0, 0.32), "upper_arm.L": (0.76, 0.0, -0.58), "upper_arm.R": (0.58, 0.0, 0.70),
    }
    _key(armature, count - 1, final, {"root": (0.0, 0.0, -0.47)})
    _key(armature, count, final, {"root": (0.0, 0.0, -0.47)})


# ---------------------------------------------------------------------------
# Premium-v2 boss motion profiles
# ---------------------------------------------------------------------------
# Bosses preserve the locked 25-bone runtime contract, but each profile is built
# around its named signature instead of inheriting the generic melee swing.


def _boss_neutral(*extra_bones: str) -> dict[str, tuple[float, float, float]]:
    bones = (
        "pelvis", "spine", "chest", "neck", "head",
        "upper_arm.L", "forearm.L", "hand.L",
        "upper_arm.R", "forearm.R", "hand.R",
    ) + extra_bones
    return {bone: (0.0, 0.0, 0.0) for bone in bones}


def _author_boss_breathe(
    armature: bpy.types.Object,
    count: int,
    *,
    mass: float,
    wing: float = 0.0,
    weapon: bool = False,
) -> None:
    low = {
        "pelvis": (0.0, 0.0, -0.025 * mass),
        "chest": (-0.018 * mass, 0.0, -0.025),
        "head": (0.014, 0.0, 0.035),
        "upper_arm.L": (0.045, -0.035, -0.07 - wing),
        "upper_arm.R": (0.045, 0.035, 0.07 + wing),
        "forearm.L": (-0.03, 0.0, -0.025),
        "forearm.R": (-0.03, 0.0, 0.025),
    }
    high = {
        "pelvis": (0.0, 0.0, 0.025 * mass),
        "chest": (0.024 * mass, 0.0, 0.025),
        "head": (-0.016, 0.0, -0.035),
        "upper_arm.L": (-0.025 - wing, -0.055, -0.11 - wing),
        "upper_arm.R": (-0.025 - wing, 0.055, 0.11 + wing),
        "forearm.L": (-0.065, 0.0, -0.055),
        "forearm.R": (-0.065, 0.0, 0.055),
    }
    if weapon:
        low["weapon_socket"] = (0.02, 0.0, -0.035)
        high["weapon_socket"] = (-0.025, 0.0, 0.045)
    _key(armature, 1, low, {"root": (0.0, 0.0, 0.0)}, {"chest": (0.99, 0.99, 0.98)})
    _key(armature, 1 + count // 2, high, {"root": (0.0, 0.0, 0.022 * mass)}, {"chest": (1.035, 1.035, 1.045)})
    _key(armature, count, low, {"root": (0.0, 0.0, 0.0)}, {"chest": (0.99, 0.99, 0.98)})


def _author_boss_hit(
    armature: bpy.types.Object,
    count: int,
    *,
    recoil: float,
    wing: float = 0.0,
    weapon: bool = False,
) -> None:
    extra = ("weapon_socket",) if weapon else ()
    neutral = _boss_neutral(*extra)
    impact = {
        "pelvis": (-0.10, 0.0, 0.07),
        "chest": (-recoil, 0.0, 0.17),
        "head": (recoil * 0.78, 0.0, -0.13),
        "upper_arm.L": (0.38 + wing, 0.0, -0.30 - wing),
        "upper_arm.R": (0.36 + wing, 0.0, 0.30 + wing),
    }
    settle = {
        "pelvis": (0.035, 0.0, -0.025),
        "chest": (recoil * 0.30, 0.0, -0.05),
        "head": (-recoil * 0.22, 0.0, 0.04),
        "upper_arm.L": (-0.09, 0.0, 0.07),
        "upper_arm.R": (-0.08, 0.0, -0.07),
    }
    if weapon:
        impact["weapon_socket"] = (0.24, -0.12, 0.08)
        settle["weapon_socket"] = (-0.06, 0.04, -0.02)
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    _key(armature, 2, impact, {"root": (0.075, 0.0, -0.03)}, {"chest": (0.94, 1.04, 0.96)})
    _key(armature, 3, settle, {"root": (-0.02, 0.0, 0.0)})
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)}, {"chest": (1.0, 1.0, 1.0)})


def _author_boss_fall(
    armature: bpy.types.Object,
    count: int,
    *,
    turn: float,
    spread: float,
    weapon: bool = False,
    wilt: bool = False,
) -> None:
    extra = ("weapon_socket",) if weapon else ()
    neutral = _boss_neutral(*extra)
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)})
    mid = {
        "pelvis": (0.22, 0.0, -0.08), "chest": (0.30, 0.0, -turn),
        "head": (-0.20, 0.0, turn * 0.6),
        "upper_arm.L": (0.42, 0.0, -spread), "upper_arm.R": (0.38, 0.0, spread),
    }
    final = {
        "root": (0.0, 0.92, turn * 0.20), "pelvis": (0.62, 0.0, -0.14),
        "chest": (0.82, 0.0, -turn * 0.82), "neck": (0.56, 0.0, -turn * 0.42),
        "head": (0.54, 0.0, turn * 0.58),
        "upper_arm.L": (0.62, 0.0, -spread * 0.65),
        "upper_arm.R": (0.56, 0.0, spread * 0.65),
        "forearm.L": (0.32, 0.0, -spread * 0.22),
        "forearm.R": (0.29, 0.0, spread * 0.22),
    }
    if weapon:
        mid["weapon_socket"] = (0.42, -0.20, 0.12)
        final["weapon_socket"] = (0.92, -0.34, 0.24)
    _key(armature, 4, mid, {"root": (0.0, 0.0, -0.13)}, {"chest": (1.0, 1.0, 0.86 if wilt else 1.0)})
    _key(armature, 7, {**mid, "root": (0.0, 0.42, turn * 0.18), "chest": (0.68, 0.0, -turn * 0.8)}, {"root": (0.0, 0.03, -0.37)}, {"chest": (1.04, 1.04, 0.62 if wilt else 0.95)})
    # Compensate the screen-space center as broad wings/fists rotate into the floor.
    final_location = {"root": (-0.45, 0.05, -0.57)}
    final_scale = {"chest": (1.06, 1.06, 0.42 if wilt else 0.92)}
    _key(armature, count - 1, final, final_location, final_scale)
    _key(armature, count, final, final_location, final_scale)


def _author_ancient_golem_idle(armature: bpy.types.Object, count: int) -> None:
    _author_boss_breathe(armature, count, mass=0.65)
    _key(armature, 1, scales={"armor_socket": (0.98, 0.98, 0.98)})
    _key(armature, 1 + count // 2, scales={"armor_socket": (1.05, 1.05, 1.05)})
    _key(armature, count, scales={"armor_socket": (0.98, 0.98, 0.98)})


def _author_ancient_golem_ground_slam(armature: bpy.types.Object, count: int) -> None:
    neutral = _boss_neutral("armor_socket")
    _key(
        armature,
        1,
        neutral,
        {"root": (0.0, 0.0, 0.0)},
        {"armor_socket": (1.0, 1.0, 1.0)},
    )
    # Bilateral overhead lift keeps the heartstone exposed between separated fists.
    _key(armature, 3, {
        "pelvis": (-0.06, 0.0, 0.0), "spine": (-0.12, 0.0, 0.0),
        "chest": (-0.20, 0.0, 0.0), "head": (0.11, 0.0, 0.0),
        "upper_arm.L": (-0.82, -0.08, -0.50), "upper_arm.R": (-0.82, 0.08, 0.50),
        "forearm.L": (-0.58, 0.0, -0.12), "forearm.R": (-0.58, 0.0, 0.12),
    }, {"root": (0.0, 0.015, 0.065)}, {
        "armor_socket": (1.12, 1.12, 1.12),
    })
    # Signature impact remains crouched and frontal: both fists strike beside the feet.
    _key(armature, 5, {
        "pelvis": (0.22, 0.0, 0.0), "spine": (0.20, 0.0, 0.0),
        "chest": (0.28, 0.0, 0.0), "head": (-0.16, 0.0, 0.0),
        "upper_arm.L": (0.08, -0.03, -0.04), "upper_arm.R": (0.08, 0.03, 0.04),
        "forearm.L": (0.10, 0.0, 0.03), "forearm.R": (0.10, 0.0, -0.03),
    }, {"root": (0.0, -0.035, -0.105)}, {
        "chest": (1.035, 1.035, 0.95),
        "armor_socket": (1.25, 1.25, 1.25),
    })
    _key(armature, 6, {
        "pelvis": (0.15, 0.0, 0.0), "spine": (0.13, 0.0, 0.0),
        "chest": (0.18, 0.0, 0.0), "head": (-0.10, 0.0, 0.0),
        "upper_arm.L": (0.06, -0.02, -0.03), "upper_arm.R": (0.06, 0.02, 0.03),
        "forearm.L": (0.07, 0.0, 0.02), "forearm.R": (0.07, 0.0, -0.02),
    }, {"root": (0.0, -0.018, -0.065)}, {
        "armor_socket": (1.10, 1.10, 1.10),
    })
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)}, {
        "chest": (1.0, 1.0, 1.0),
        "armor_socket": (1.0, 1.0, 1.0),
    })


def _author_ancient_golem_hit(armature: bpy.types.Object, count: int) -> None:
    _author_boss_hit(armature, count, recoil=0.26)
    for frame, pulse in ((1, 1.0), (2, 0.88), (3, 1.06), (count, 1.0)):
        _key(armature, frame, scales={"armor_socket": (pulse, pulse, pulse)})


def _author_ancient_golem_death(armature: bpy.types.Object, count: int) -> None:
    _author_boss_fall(armature, count, turn=0.20, spread=0.48)
    for frame, pulse in ((1, 1.0), (4, 0.90), (7, 0.65), (count - 1, 0.38), (count, 0.38)):
        _key(armature, frame, scales={"armor_socket": (pulse, pulse, pulse)})


def _author_thorn_matriarch_idle(armature: bpy.types.Object, count: int) -> None:
    _author_boss_breathe(armature, count, mass=1.0, wing=0.06)
    resting = {
        "ring_socket.L": (0.72, 0.72, 0.72),
        "ring_socket.R": (0.72, 0.72, 0.72),
    }
    for frame in (1, 1 + count // 2, count):
        _key(armature, frame, scales=resting)


def _author_thorn_matriarch_thorn_cage(armature: bpy.types.Object, count: int) -> None:
    neutral = _boss_neutral("ring_socket.L", "ring_socket.R")
    resting = {
        "ring_socket.L": (0.72, 0.72, 0.72),
        "ring_socket.R": (0.72, 0.72, 0.72),
    }
    _key(armature, 1, neutral, {"root": (0.0, 0.0, 0.0)}, resting)
    # Fold the thorn fans inward before the root-cage erupts.
    _key(armature, 3, {
        "pelvis": (0.16, 0.0, 0.0), "spine": (0.18, 0.0, 0.0), "chest": (0.22, 0.0, 0.0), "head": (-0.20, 0.0, 0.0),
        "upper_arm.L": (0.30, -0.28, 0.58), "upper_arm.R": (0.30, 0.28, -0.58),
        "forearm.L": (-0.56, 0.0, 0.34), "forearm.R": (-0.56, 0.0, -0.34),
    }, {"root": (0.0, 0.0, -0.08)}, {
        "chest": (0.95, 0.95, 0.94),
        "ring_socket.L": (0.52, 0.52, 0.52),
        "ring_socket.R": (0.52, 0.52, 0.52),
    })
    # Signature cage opens both vine arms and thorn fans into a near-circle.
    _key(armature, 5, {
        "pelvis": (-0.20, 0.0, 0.0), "spine": (-0.18, 0.0, 0.0), "chest": (-0.25, 0.0, 0.0), "head": (0.16, 0.0, 0.0),
        "upper_arm.L": (-0.82, -0.24, -1.00), "upper_arm.R": (-0.82, 0.24, 1.00),
        "forearm.L": (-0.40, 0.0, -0.52), "forearm.R": (-0.40, 0.0, 0.52),
        "hand.L": (0.0, 0.0, -0.36), "hand.R": (0.0, 0.0, 0.36),
    }, {"root": (0.0, -0.04, 0.10)}, {
        "chest": (1.09, 1.09, 1.12),
        "ring_socket.L": (1.28, 1.28, 1.28),
        "ring_socket.R": (1.28, 1.28, 1.28),
    })
    _key(armature, 6, {
        "pelvis": (-0.10, 0.0, 0.0), "chest": (-0.14, 0.0, 0.0), "head": (0.08, 0.0, 0.0),
        "upper_arm.L": (-0.56, -0.15, -0.72), "upper_arm.R": (-0.56, 0.15, 0.72),
        "forearm.L": (-0.28, 0.0, -0.34), "forearm.R": (-0.28, 0.0, 0.34),
    }, {"root": (0.0, -0.02, 0.055)}, {
        "chest": (1.04, 1.04, 1.06),
        "ring_socket.L": (0.95, 0.95, 0.95),
        "ring_socket.R": (0.95, 0.95, 0.95),
    })
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)}, {
        "chest": (1.0, 1.0, 1.0),
        **resting,
    })


def _author_thorn_matriarch_hit(armature: bpy.types.Object, count: int) -> None:
    _author_boss_hit(armature, count, recoil=0.42, wing=0.08)
    resting = {
        "ring_socket.L": (0.72, 0.72, 0.72),
        "ring_socket.R": (0.72, 0.72, 0.72),
    }
    for frame in (1, 2, 3, count):
        _key(armature, frame, scales=resting)


def _author_thorn_matriarch_death(armature: bpy.types.Object, count: int) -> None:
    _author_boss_fall(armature, count, turn=0.38, spread=0.88, wilt=True)
    resting = {
        "ring_socket.L": (0.72, 0.72, 0.72),
        "ring_socket.R": (0.72, 0.72, 0.72),
    }
    for frame in (1, 4, 7, count - 1, count):
        _key(armature, frame, scales=resting)


def _author_ember_wyrm_idle(armature: bpy.types.Object, count: int) -> None:
    _author_boss_breathe(armature, count, mass=1.15, wing=0.12)
    hidden = {"helmet_socket": (0.01, 0.01, 0.01)}
    for frame in (1, 1 + count // 2, count):
        _key(armature, frame, scales=hidden)


def _author_ember_wyrm_flame_sweep(armature: bpy.types.Object, count: int) -> None:
    neutral = _boss_neutral("helmet_socket")
    _key(
        armature,
        1,
        neutral,
        {"root": (0.0, 0.0, 0.0)},
        {"helmet_socket": (0.01, 0.01, 0.01)},
    )
    # Coil the neck and draw both wings tight to pressurise the breath.
    _key(armature, 3, {
        "pelvis": (0.12, 0.0, -0.18), "spine": (0.10, 0.0, -0.30), "chest": (0.06, 0.0, -0.46), "neck": (-0.10, 0.0, -0.48), "head": (-0.12, 0.0, -0.58),
        "upper_arm.L": (0.18, -0.18, 0.42), "upper_arm.R": (0.18, 0.18, -0.42),
        "forearm.L": (0.20, 0.0, 0.24), "forearm.R": (0.20, 0.0, -0.24),
    }, {"root": (-0.04, 0.02, -0.055)}, {
        "chest": (0.96, 0.96, 1.08),
        "helmet_socket": (0.03, 0.03, 0.03),
    })
    # Signature flame sweep crosses the silhouette as both wings snap open.
    _key(armature, 5, {
        "pelvis": (-0.10, 0.0, 0.28), "spine": (-0.12, 0.0, 0.42), "chest": (-0.10, 0.0, 0.62), "neck": (0.08, 0.0, 0.66), "head": (0.10, 0.0, 0.82),
        "upper_arm.L": (-0.42, -0.18, -0.72), "upper_arm.R": (-0.42, 0.18, 0.72),
        "forearm.L": (-0.30, 0.0, -0.34), "forearm.R": (-0.30, 0.0, 0.34),
    }, {"root": (0.08, -0.05, 0.025)}, {
        "chest": (1.06, 1.06, 0.98),
        "helmet_socket": (1.35, 1.35, 1.35),
    })
    _key(armature, 6, {
        "pelvis": (-0.05, 0.0, 0.16), "chest": (-0.06, 0.0, 0.36), "neck": (0.05, 0.0, 0.38), "head": (0.06, 0.0, 0.46),
        "upper_arm.L": (-0.24, -0.10, -0.42), "upper_arm.R": (-0.24, 0.10, 0.42),
    }, {"root": (0.04, -0.02, 0.012)}, {
        "helmet_socket": (0.82, 0.82, 0.82),
    })
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)}, {
        "chest": (1.0, 1.0, 1.0),
        "helmet_socket": (0.01, 0.01, 0.01),
    })


def _author_ember_wyrm_hit(armature: bpy.types.Object, count: int) -> None:
    _author_boss_hit(armature, count, recoil=0.40, wing=0.16)
    hidden = {"helmet_socket": (0.01, 0.01, 0.01)}
    for frame in (1, 2, 3, count):
        _key(armature, frame, scales=hidden)


def _author_ember_wyrm_death(armature: bpy.types.Object, count: int) -> None:
    _author_boss_fall(armature, count, turn=0.30, spread=1.08)
    hidden = {"helmet_socket": (0.01, 0.01, 0.01)}
    for frame in (1, 4, 7, count - 1, count):
        _key(armature, frame, scales=hidden)


def _author_void_knight_idle(armature: bpy.types.Object, count: int) -> None:
    _author_boss_breathe(armature, count, mass=0.85, weapon=True)
    _key(armature, 1, scales={"armor_socket": (0.92, 0.92, 0.92)})
    _key(armature, 1 + count // 2, scales={"armor_socket": (1.08, 1.08, 1.08)})
    _key(armature, count, scales={"armor_socket": (0.92, 0.92, 0.92)})


def _author_void_knight_void_charge(armature: bpy.types.Object, count: int) -> None:
    neutral = _boss_neutral("weapon_socket", "armor_socket", "thigh.L", "thigh.R")
    _key(
        armature,
        1,
        neutral,
        {"root": (0.0, 0.0, 0.0)},
        {"armor_socket": (1.0, 1.0, 1.0)},
    )
    # Pull the greatblade behind the horned silhouette and compress before launch.
    _key(armature, 3, {
        "pelvis": (0.22, 0.0, -0.20), "spine": (0.12, 0.0, -0.30), "chest": (0.10, 0.0, -0.42), "head": (-0.12, 0.0, 0.20),
        "upper_arm.R": (-0.74, 0.34, 0.58), "forearm.R": (-0.82, 0.0, 0.20), "weapon_socket": (-0.58, -0.38, 0.12),
        "upper_arm.L": (0.28, -0.20, -0.42), "forearm.L": (-0.34, 0.0, -0.12),
        "thigh.L": (-0.12, 0.0, 0.0), "thigh.R": (0.18, 0.0, 0.0),
    }, {"root": (-0.08, 0.05, -0.10)}, {
        "chest": (0.96, 0.96, 0.95),
        "armor_socket": (0.72, 0.72, 0.72),
    })
    # Signature charge uses root travel and a spear-like blade presentation.
    _key(armature, 5, {
        "pelvis": (-0.34, 0.0, 0.36), "spine": (-0.20, 0.0, 0.44), "chest": (-0.18, 0.0, 0.58), "head": (0.12, 0.0, -0.24),
        "upper_arm.R": (0.64, -0.26, -0.74), "forearm.R": (0.34, 0.0, -0.24), "weapon_socket": (0.72, 0.32, -0.16),
        "upper_arm.L": (-0.44, 0.18, 0.46), "forearm.L": (0.30, 0.0, 0.12),
        "thigh.L": (0.30, 0.0, 0.0), "thigh.R": (-0.32, 0.0, 0.0),
    }, {"root": (0.24, -0.15, 0.03)}, {
        "chest": (1.06, 1.03, 1.0),
        "armor_socket": (1.35, 1.35, 1.35),
    })
    _key(armature, 6, {
        "pelvis": (-0.20, 0.0, 0.22), "chest": (-0.10, 0.0, 0.34), "head": (0.07, 0.0, -0.14),
        "upper_arm.R": (0.40, -0.15, -0.46), "forearm.R": (0.22, 0.0, -0.14), "weapon_socket": (0.42, 0.18, -0.10),
        "upper_arm.L": (-0.26, 0.10, 0.28), "forearm.L": (0.18, 0.0, 0.08),
    }, {"root": (0.14, -0.08, 0.015)}, {
        "armor_socket": (1.12, 1.12, 1.12),
    })
    _key(armature, count, neutral, {"root": (0.0, 0.0, 0.0)}, {
        "chest": (1.0, 1.0, 1.0),
        "armor_socket": (1.0, 1.0, 1.0),
    })


def _author_void_knight_hit(armature: bpy.types.Object, count: int) -> None:
    _author_boss_hit(armature, count, recoil=0.35, weapon=True)
    for frame, pulse in ((1, 1.0), (2, 0.82), (3, 1.08), (count, 1.0)):
        _key(armature, frame, scales={"armor_socket": (pulse, pulse, pulse)})


def _author_void_knight_death(armature: bpy.types.Object, count: int) -> None:
    _author_boss_fall(armature, count, turn=0.30, spread=0.68, weapon=True)
    for frame, pulse in ((1, 1.0), (4, 0.88), (7, 0.62), (count - 1, 0.34), (count, 0.34)):
        _key(armature, frame, scales={"armor_socket": (pulse, pulse, pulse)})
