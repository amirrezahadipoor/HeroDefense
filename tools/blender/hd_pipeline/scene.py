from __future__ import annotations

import math
from array import array
from pathlib import Path
from typing import Iterable

import bpy
from mathutils import Vector

from .config import CAMERA_LOCATION, CAMERA_SCALE, CAMERA_SHIFT_Y, CAMERA_TARGET, OUTLINE_RGBA


def hex_rgba(value: str, alpha: float = 1.0) -> tuple[float, float, float, float]:
    value = value.lstrip("#")
    if len(value) != 6:
        raise ValueError(f"Expected RRGGBB color, got {value!r}")
    # Blender material inputs are linear. Convert sRGB channels explicitly.
    channels = [int(value[index:index + 2], 16) / 255.0 for index in (0, 2, 4)]

    def linear(channel: float) -> float:
        return channel / 12.92 if channel <= 0.04045 else ((channel + 0.055) / 1.055) ** 2.4

    return linear(channels[0]), linear(channels[1]), linear(channels[2]), alpha


def multiply_rgb(color: tuple[float, float, float, float], amount: float) -> tuple[float, float, float, float]:
    return tuple(min(1.0, channel * amount) for channel in color[:3]) + (color[3],)


def reset_scene() -> None:
    bpy.ops.object.mode_set(mode="OBJECT") if bpy.context.object and bpy.context.object.mode != "OBJECT" else None
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for collection in (
        bpy.data.armatures,
        bpy.data.meshes,
        bpy.data.curves,
        bpy.data.materials,
        bpy.data.cameras,
        bpy.data.lights,
        bpy.data.actions,
    ):
        for block in list(collection):
            collection.remove(block)


def toon_material(name: str, color_hex: str, metallic: float = 0.0) -> bpy.types.Material:
    material = bpy.data.materials.new(name)
    material.use_nodes = True
    material.diffuse_color = hex_rgba(color_hex)
    nodes = material.node_tree.nodes
    links = material.node_tree.links
    nodes.clear()

    output = nodes.new("ShaderNodeOutputMaterial")
    diffuse = nodes.new("ShaderNodeBsdfDiffuse")
    diffuse.inputs["Color"].default_value = hex_rgba(color_hex)
    diffuse.inputs["Roughness"].default_value = 0.38 if metallic else 0.72
    shader_to_rgb = nodes.new("ShaderNodeShaderToRGB")
    ramp = nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.interpolation = "CONSTANT"
    base = hex_rgba(color_hex)
    ramp.color_ramp.elements.remove(ramp.color_ramp.elements[1])
    shadow = ramp.color_ramp.elements[0]
    shadow.position = 0.0
    shadow.color = multiply_rgb(base, 0.55)
    mid = ramp.color_ramp.elements.new(0.32)
    mid.color = multiply_rgb(base, 0.82)
    light = ramp.color_ramp.elements.new(0.68)
    light.color = multiply_rgb(base, 1.08)
    emission = nodes.new("ShaderNodeEmission")
    emission.inputs["Strength"].default_value = 1.0

    links.new(diffuse.outputs["BSDF"], shader_to_rgb.inputs["Shader"])
    links.new(shader_to_rgb.outputs["Color"], ramp.inputs["Fac"])
    links.new(ramp.outputs["Color"], emission.inputs["Color"])
    links.new(emission.outputs["Emission"], output.inputs["Surface"])
    return material


def transparent_material(name: str, color_hex: str, alpha: float) -> bpy.types.Material:
    material = bpy.data.materials.new(name)
    material.use_nodes = True
    material.diffuse_color = hex_rgba(color_hex, alpha)
    material.surface_render_method = "DITHERED"
    principled = material.node_tree.nodes.get("Principled BSDF")
    principled.inputs["Base Color"].default_value = hex_rgba(color_hex, alpha)
    principled.inputs["Alpha"].default_value = alpha
    principled.inputs["Roughness"].default_value = 1.0
    return material


def configure_scene(frame_class: str, output_directory: Path) -> bpy.types.Scene:
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE_NEXT"
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.image_settings.color_depth = "8"
    scene.render.resolution_x = scene.render.resolution_y = _frame_size(frame_class)
    scene.render.resolution_percentage = 100
    scene.render.fps = 12
    scene.render.filepath = str(output_directory)
    scene.render.use_file_extension = True
    scene.render.image_settings.compression = 40
    # Four temporal samples are sufficient for small, flat-shaded sprite frames and
    # keep the offline batch practical on CPU-only CI renderers.
    scene.eevee.taa_render_samples = 4
    scene.eevee.taa_samples = 4
    # Use deterministic alpha-dilation outlines for every asset class. Unlike
    # Freestyle, this keeps repeated software-GL renders memory-bounded in CI.
    scene.render.use_freestyle = False
    scene.render.line_thickness = 1.0

    try:
        scene.view_settings.look = "AgX - Medium High Contrast"
    except TypeError:
        # Exact look labels differ slightly among Blender patch releases.
        pass
    scene.view_settings.exposure = 0.0
    scene.view_settings.gamma = 1.0

    world = bpy.data.worlds.get("World") or bpy.data.worlds.new("World")
    scene.world = world
    world.use_nodes = True
    background = world.node_tree.nodes.get("Background")
    background.inputs["Color"].default_value = (0.02, 0.03, 0.025, 1.0)
    background.inputs["Strength"].default_value = 0.25

    _add_camera(frame_class)
    _add_area_light("HD_KEY", (-4.0, -4.5, 8.0), 900.0, 5.0, "#FFF3DF")
    _add_area_light("HD_FILL", (5.0, -1.5, 4.5), 280.0, 4.0, "#C7DEFF")
    _add_area_light("HD_RIM", (0.0, 5.0, 6.5), 450.0, 3.0, "#D8FFD2")
    _configure_freestyle(scene)
    return scene


def _frame_size(frame_class: str) -> int:
    from .config import FRAME_SIZE
    return FRAME_SIZE[frame_class]


def _add_camera(frame_class: str) -> bpy.types.Object:
    data = bpy.data.cameras.new("HD_CAMERA")
    data.type = "ORTHO"
    data.ortho_scale = CAMERA_SCALE[frame_class]
    data.shift_y = CAMERA_SHIFT_Y[frame_class]
    camera = bpy.data.objects.new("HD_CAMERA", data)
    bpy.context.collection.objects.link(camera)
    camera.location = CAMERA_LOCATION
    direction = Vector(CAMERA_TARGET) - camera.location
    camera.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
    bpy.context.scene.camera = camera
    return camera


def _add_area_light(
    name: str,
    location: tuple[float, float, float],
    energy: float,
    size: float,
    color_hex: str,
) -> bpy.types.Object:
    data = bpy.data.lights.new(name, type="AREA")
    data.energy = energy
    data.shape = "DISK"
    data.size = size
    data.color = hex_rgba(color_hex)[:3]
    light = bpy.data.objects.new(name, data)
    bpy.context.collection.objects.link(light)
    light.location = location
    light.rotation_euler = (Vector((0.0, 0.0, 1.4)) - light.location).to_track_quat("-Z", "Y").to_euler()
    return light


def _configure_freestyle(scene: bpy.types.Scene) -> None:
    settings = scene.view_layers[0].freestyle_settings
    line_set = settings.linesets[0]
    line_set.linestyle.color = OUTLINE_RGBA[:3]
    line_set.linestyle.thickness = 1.5
    line_set.select_silhouette = True
    line_set.select_border = True
    line_set.select_crease = True
    line_set.select_material_boundary = False


def add_contact_shadow(material: bpy.types.Material) -> bpy.types.Object:
    bpy.ops.mesh.primitive_cylinder_add(vertices=32, radius=0.75, depth=0.015, location=(0.0, 0.08, 0.012))
    shadow = bpy.context.object
    shadow.name = "contact_shadow"
    shadow.scale.y = 0.48
    shadow.data.materials.append(material)
    return shadow


def apply_alpha_outline(path: Path, radius: int = 3) -> None:
    """Dilate opaque alpha into a fixed-color external outline in-place."""
    image = bpy.data.images.load(str(path), check_existing=False)
    width, height = image.size
    source = array("f", [0.0]) * (width * height * 4)
    image.pixels.foreach_get(source)
    result = array("f", source)
    outline = OUTLINE_RGBA
    opaque = [source[index * 4 + 3] > 0.08 for index in range(width * height)]
    radius_squared = radius * radius
    offsets = [
        (dx, dy)
        for dy in range(-radius, radius + 1)
        for dx in range(-radius, radius + 1)
        if dx * dx + dy * dy <= radius_squared and (dx or dy)
    ]
    for y in range(height):
        for x in range(width):
            pixel_index = y * width + x
            if opaque[pixel_index]:
                continue
            touches = False
            for dx, dy in offsets:
                nx, ny = x + dx, y + dy
                if 0 <= nx < width and 0 <= ny < height and opaque[ny * width + nx]:
                    touches = True
                    break
            if touches:
                base = pixel_index * 4
                result[base:base + 4] = array("f", outline)
    image.pixels.foreach_set(result)
    image.filepath_raw = str(path)
    image.file_format = "PNG"
    image.save()
    bpy.data.images.remove(image)


def triangle_count(objects: Iterable[bpy.types.Object]) -> int:
    depsgraph = bpy.context.evaluated_depsgraph_get()
    total = 0
    for obj in objects:
        if obj.type != "MESH":
            continue
        evaluated = obj.evaluated_get(depsgraph)
        mesh = evaluated.to_mesh()
        mesh.calc_loop_triangles()
        total += len(mesh.loop_triangles)
        evaluated.to_mesh_clear()
    return total


def pack_grid(
    frame_paths: dict[str, list[Path]],
    output_path: Path,
    frame_size: int,
) -> tuple[int, int, dict[str, list[dict[str, int]]]]:
    """Pack clip rows using Blender's image API; returns top-left atlas coordinates."""
    clips = list(frame_paths)
    columns = max(len(frame_paths[clip]) for clip in clips)
    width = columns * frame_size
    height = len(clips) * frame_size
    sheet = bpy.data.images.new(output_path.stem, width=width, height=height, alpha=True, float_buffer=False)
    sheet_pixels = array("f", [0.0]) * (width * height * 4)
    regions: dict[str, list[dict[str, int]]] = {}

    for row, clip in enumerate(clips):
        regions[clip] = []
        for column, path in enumerate(frame_paths[clip]):
            image = bpy.data.images.load(str(path), check_existing=False)
            image.colorspace_settings.name = "sRGB"
            pixels = array("f", [0.0]) * (frame_size * frame_size * 4)
            image.pixels.foreach_get(pixels)
            # Blender pixel buffers and our destination are both bottom-up. Place the
            # first clip at the atlas top while retaining conventional top-left metadata.
            destination_row = len(clips) - row - 1
            for source_y in range(frame_size):
                src_start = source_y * frame_size * 4
                dst_start = ((destination_row * frame_size + source_y) * width + column * frame_size) * 4
                sheet_pixels[dst_start:dst_start + frame_size * 4] = pixels[src_start:src_start + frame_size * 4]
            regions[clip].append({
                "x": column * frame_size,
                "y": row * frame_size,
                "width": frame_size,
                "height": frame_size,
                "index": column,
            })
            bpy.data.images.remove(image)

    sheet.pixels.foreach_set(sheet_pixels)
    sheet.filepath_raw = str(output_path)
    sheet.file_format = "PNG"
    sheet.save()
    bpy.data.images.remove(sheet)
    return width, height, regions
