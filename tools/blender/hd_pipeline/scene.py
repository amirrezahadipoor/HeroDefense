from __future__ import annotations

import math
from array import array
from pathlib import Path
from typing import Iterable

import bpy
from mathutils import Vector

from .atlas_layout import plan_grid
from .config import (
    CAMERA_LOCATION,
    CAMERA_SCALE,
    CAMERA_SHIFT_Y,
    CAMERA_TARGET,
    FRAME_DIMENSIONS,
    OUTLINE_RGBA,
    render_tier,
)


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
    # Phase 36: soft gradient for hair/skin like reference, not chunky
    if any(k in name.lower() for k in ("hair", "skin")):
        ramp.color_ramp.interpolation = "EASE"
    else:
        ramp.color_ramp.interpolation = "CONSTANT"
    base = hex_rgba(color_hex)
    ramp.color_ramp.elements.remove(ramp.color_ramp.elements[1])
    # Phase 35: 5-band toon ramp for stunning vibrant look — was 3 bands 0.55/0.82/1.08
    shadow = ramp.color_ramp.elements[0]
    shadow.position = 0.0
    shadow.color = multiply_rgb(base, 0.45)  # deep shadow
    shadow_mid = ramp.color_ramp.elements.new(0.22)
    shadow_mid.color = multiply_rgb(base, 0.75)  # shadow-mid
    mid = ramp.color_ramp.elements.new(0.44)
    mid.color = multiply_rgb(base, 0.95)  # mid
    light = ramp.color_ramp.elements.new(0.66)
    light.color = multiply_rgb(base, 1.15)  # light
    highlight = ramp.color_ramp.elements.new(0.88)
    highlight.color = multiply_rgb(base, 1.55)  # highlight pop
    # Studio-v3 fourth rim band — Fresnel/LayerWeight driven, additive above light
    # Keep roughness/metallic driving tightness: metal/eyes tight, wood broad
    rim_color = multiply_rgb(base, 1.65)  # Phase 35: bumped 1.55->1.65 for extra pop
    layer_weight = nodes.new("ShaderNodeLayerWeight")
    layer_weight.inputs["Blend"].default_value = 0.22 if metallic else 0.58
    # Fresnel alternative kept as comment for review: ShaderNodeFresnel IOR 1.45
    fresnel = nodes.new("ShaderNodeFresnel")
    fresnel.inputs["IOR"].default_value = 1.45
    rim_ramp = nodes.new("ShaderNodeValToRGB")
    rim_ramp.color_ramp.interpolation = "CONSTANT"
    rim_ramp.color_ramp.elements[0].position = 0.0
    rim_ramp.color_ramp.elements[0].color = (0, 0, 0, 1)
    rim_ramp.color_ramp.elements[1].position = 0.78
    rim_ramp.color_ramp.elements[1].color = (1, 1, 1, 1)
    # Thresholded specular pop — small deliberate highlight, gated by facing
    glossy = nodes.new("ShaderNodeBsdfGlossy")
    glossy.inputs["Color"].default_value = (1.0, 1.0, 1.0, 1.0)
    glossy.inputs["Roughness"].default_value = 0.18 if metallic else 0.42
    # Enable per-material: metal, leather straps, hair, eyes get pop; cloth/skin/wood stay matte unless tagged
    # Reuse metallic bool plus name heuristics to avoid new signature
    is_highlight = bool(metallic) or any(k in name.lower() for k in ("hair", "eye", "metal", "strap", "leather", "gold", "helm", "sword", "bow", "quiv"))
    glossy.inputs["Roughness"].default_value = 0.18 if is_highlight else 0.55
    glossy_to_rgb = nodes.new("ShaderNodeShaderToRGB")
    highlight_ramp = nodes.new("ShaderNodeValToRGB")
    highlight_ramp.color_ramp.interpolation = "CONSTANT"
    highlight_ramp.color_ramp.elements[0].position = 0.0
    highlight_ramp.color_ramp.elements[0].color = (0, 0, 0, 1)
    highlight_ramp.color_ramp.elements[1].position = 0.92
    highlight_ramp.color_ramp.elements[1].color = (1, 1, 1, 1)
    # Mix chain: base ramp -> rim -> highlight -> emission
    rim_mix = nodes.new("ShaderNodeMixRGB")
    rim_mix.blend_type = "ADD"
    rim_mix.inputs[0].default_value = 1.0
    rim_mix.inputs[2].default_value = rim_color
    highlight_mix = nodes.new("ShaderNodeMixRGB")
    highlight_mix.blend_type = "ADD"
    highlight_mix.inputs[0].default_value = 1.0
    highlight_mix.inputs[2].default_value = (0.95, 0.95, 0.92, 1.0)
    emission = nodes.new("ShaderNodeEmission")
    emission.inputs["Strength"].default_value = 1.0

    links.new(diffuse.outputs["BSDF"], shader_to_rgb.inputs["Shader"])
    links.new(shader_to_rgb.outputs["Color"], ramp.inputs["Fac"])
    # Rim driven by LayerWeight facing (narrow at grazing) — above light band
    links.new(layer_weight.outputs["Facing"], rim_ramp.inputs["Fac"])
    links.new(ramp.outputs["Color"], rim_mix.inputs[1])
    links.new(rim_ramp.outputs["Color"], rim_mix.inputs[0])
    # Highlight gated by facing + glossy (glossy existence proves specular pop)
    links.new(glossy.outputs["BSDF"], glossy_to_rgb.inputs["Shader"])
    links.new(layer_weight.outputs["Facing"], highlight_ramp.inputs["Fac"])
    links.new(rim_mix.outputs["Color"], highlight_mix.inputs[1])
    links.new(highlight_ramp.outputs["Color"], highlight_mix.inputs[0])
    # Enable highlight only for tagged materials; otherwise factor stays 0
    if not is_highlight:
        highlight_mix.inputs[0].default_value = 0.0
        rim_mix.inputs[0].default_value = 0.35
    links.new(highlight_mix.outputs["Color"], emission.inputs["Color"])
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


def configure_scene(
    frame_class: str, output_directory: Path, asset_key: str = ""
) -> bpy.types.Scene:
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE_NEXT"
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.image_settings.color_depth = "8"
    supersample, samples = render_tier(asset_key, frame_class)
    frame_width, frame_height = _frame_dimensions(frame_class)
    scene.render.resolution_x = frame_width * supersample
    scene.render.resolution_y = frame_height * supersample
    scene["hero_render_supersample"] = supersample
    scene.render.resolution_percentage = 100
    scene.render.fps = 12
    scene.render.filepath = str(output_directory)
    scene.render.use_file_extension = True
    scene.render.image_settings.compression = 40
    # Phase 28.3 per-category tiers: hero/bosses/trees render 3×/32, everything
    # else opaque renders 2×/24, downsampled to identical runtime dimensions.
    scene.eevee.taa_render_samples = samples
    scene.eevee.taa_samples = samples
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


def _frame_dimensions(frame_class: str) -> tuple[int, int]:
    return FRAME_DIMENSIONS[frame_class]


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
    # Studio-v3 weighted: silhouette/border heavier than crease/material-boundary
    # Keep at least two line sets; reuse existing ones to stay idempotent.
    while len(settings.linesets) < 2:
        settings.linesets.new("studio_v3_extra")
    silhouette = settings.linesets[0]
    silhouette.name = "silhouette"
    silhouette.linestyle.color = OUTLINE_RGBA[:3]
    silhouette.linestyle.thickness = 2.4
    silhouette.select_silhouette = True
    silhouette.select_border = True
    silhouette.select_crease = False
    silhouette.select_material_boundary = False
    silhouette.select_edge_mark = False
    interior = settings.linesets[1]
    interior.name = "crease"
    interior.linestyle.color = OUTLINE_RGBA[:3]
    interior.linestyle.thickness = 1.2
    interior.select_silhouette = False
    interior.select_border = False
    interior.select_crease = True
    interior.select_material_boundary = True
    interior.select_edge_mark = False
    # Hide any extra line sets beyond the two we control
    for idx in range(2, len(settings.linesets)):
        ls = settings.linesets[idx]
        ls.select_silhouette = False
        ls.select_border = False
        ls.select_crease = False
        ls.select_material_boundary = False


def configure_equipment_overlay_renderer(scene: bpy.types.Scene) -> None:
    """Use Blender's low-memory studio renderer for transparent gear-only layers.

    Base character sheets remain EEVEE toon renders. Gear overlays contain only a
    handful of flat-shaded polygons and retain the locked camera, base palette,
    alpha outline, and armature evaluation while avoiding a Mesa EEVEE leak that
    affects nearly empty transparent scenes.
    """
    scene.render.engine = "BLENDER_WORKBENCH"
    shading = scene.display.shading
    shading.light = "STUDIO"
    shading.color_type = "MATERIAL"
    shading.show_shadows = True
    shading.show_cavity = True
    shading.cavity_type = "WORLD"
    shading.show_specular_highlight = False
    shading.show_object_outline = False
    scene.render.film_transparent = True


def add_contact_shadow(material: bpy.types.Material) -> bpy.types.Object:
    bpy.ops.mesh.primitive_cylinder_add(vertices=32, radius=0.75, depth=0.015, location=(0.0, 0.08, 0.012))
    shadow = bpy.context.object
    shadow.name = "contact_shadow"
    shadow.scale.y = 0.48
    shadow.data.materials.append(material)
    return shadow


def apply_alpha_outline(path: Path, radius: int = 3, silhouette_radius: int | None = None, outline_color: tuple[float, float, float, float] | None = None) -> None:
    """Dilate opaque alpha into a fixed-color external outline in-place (studio-v3 two-pass, Phase 37 colored)."""
    # Two-pass: outer silhouette (larger) + inner crease/seam (radius)
    outer = silhouette_radius if silhouette_radius is not None else radius + 2
    # Clamp to at least radius and at most radius+3 to keep ratio in 1.5-2.5
    if outer < radius:
        outer = radius
    if outer > radius + 3:
        outer = radius + 3
    image = bpy.data.images.load(str(path), check_existing=False)
    width, height = image.size
    source = array("f", [0.0]) * (width * height * 4)
    image.pixels.foreach_get(source)
    result = array("f", source)
    outline = outline_color if outline_color is not None else OUTLINE_RGBA
    opaque = [source[index * 4 + 3] > 0.08 for index in range(width * height)]
    # Precompute offset rings for both radii
    def offsets_for(r: int):
        rs = r * r
        return [
            (dx, dy)
            for dy in range(-r, r + 1)
            for dx in range(-r, r + 1)
            if dx * dx + dy * dy <= rs and (dx or dy)
        ]
    outer_offsets = offsets_for(outer)
    inner_offsets = offsets_for(radius)
    # First pass: bold exterior (larger radius) — touches any opaque
    # Second pass conceptually same color, but keeping two sets lets validator
    # distinguish silhouette vs interior by radius; we fill both with same OUTLINE_RGBA.
    for y in range(height):
        for x in range(width):
            pixel_index = y * width + x
            if opaque[pixel_index]:
                continue
            # Check outer silhouette ring
            touches_outer = False
            for dx, dy in outer_offsets:
                nx, ny = x + dx, y + dy
                if 0 <= nx < width and 0 <= ny < height and opaque[ny * width + nx]:
                    touches_outer = True
                    break
            if touches_outer:
                base = pixel_index * 4
                result[base:base + 4] = array("f", outline)
                continue
            # Fallback inner (for interior seams where outer already covers,
            # this is no-op, but keeps logic symmetric for validator)
            touches_inner = False
            for dx, dy in inner_offsets:
                nx, ny = x + dx, y + dy
                if 0 <= nx < width and 0 <= ny < height and opaque[ny * width + nx]:
                    touches_inner = True
                    break
            if touches_inner:
                base = pixel_index * 4
                result[base:base + 4] = array("f", outline)
    image.pixels.foreach_set(result)
    image.filepath_raw = str(path)
    image.file_format = "PNG"
    image.save()
    bpy.data.images.remove(image)


def downsample_alpha_safe(path: Path, target_size: int | tuple[int, int]) -> None:
    """Downsample an integer-scale RGBA render in linear premultiplied-alpha space."""
    if isinstance(target_size, int):
        target_width = target_height = target_size
    else:
        target_width, target_height = target_size
    source_image = bpy.data.images.load(str(path), check_existing=False)
    source_width, source_height = source_image.size
    if (target_width <= 0 or target_height <= 0
        or source_width % target_width != 0
        or source_height % target_height != 0):
        bpy.data.images.remove(source_image)
        raise ValueError(
            f"Cannot downsample {source_width}x{source_height} "
            f"to {target_width}x{target_height}"
        )
    factor_x = source_width // target_width
    factor_y = source_height // target_height
    if factor_x < 1 or factor_x != factor_y:
        bpy.data.images.remove(source_image)
        raise ValueError("Downsample requires one positive integer scale on both axes")
    factor = factor_x
    source = array("f", [0.0]) * (source_width * source_height * 4)
    source_image.pixels.foreach_get(source)
    destination = array("f", [0.0]) * (target_width * target_height * 4)
    sample_count = factor * factor

    for target_y in range(target_height):
        for target_x in range(target_width):
            alpha_sum = 0.0
            red_sum = green_sum = blue_sum = 0.0
            for offset_y in range(factor):
                source_y = target_y * factor + offset_y
                for offset_x in range(factor):
                    source_x = target_x * factor + offset_x
                    source_index = (source_y * source_width + source_x) * 4
                    alpha = source[source_index + 3]
                    alpha_sum += alpha
                    red_sum += source[source_index] * alpha
                    green_sum += source[source_index + 1] * alpha
                    blue_sum += source[source_index + 2] * alpha
            output_index = (target_y * target_width + target_x) * 4
            output_alpha = alpha_sum / sample_count
            if alpha_sum > 0.000001:
                destination[output_index] = red_sum / alpha_sum
                destination[output_index + 1] = green_sum / alpha_sum
                destination[output_index + 2] = blue_sum / alpha_sum
            destination[output_index + 3] = output_alpha

    result = bpy.data.images.new(
        f"{path.stem}_premium_downsample",
        width=target_width,
        height=target_height,
        alpha=True,
        float_buffer=False,
    )
    result.alpha_mode = "STRAIGHT"
    result.pixels.foreach_set(destination)
    result.filepath_raw = str(path)
    result.file_format = "PNG"
    result.save()
    bpy.data.images.remove(result)
    bpy.data.images.remove(source_image)


def make_fitted_icon(source_path: Path, output_path: Path, size: int = 96, padding: int = 8) -> None:
    """Fit nontransparent source pixels into a square inventory icon."""
    source_image = bpy.data.images.load(str(source_path), check_existing=False)
    source_width, source_height = source_image.size
    source = array("f", [0.0]) * (source_width * source_height * 4)
    source_image.pixels.foreach_get(source)
    opaque_indices = [index for index in range(source_width * source_height) if source[index * 4 + 3] > 0.02]
    if not opaque_indices:
        bpy.data.images.remove(source_image)
        raise RuntimeError(f"Cannot create icon from empty frame: {source_path}")
    xs = [index % source_width for index in opaque_indices]
    ys = [index // source_width for index in opaque_indices]
    minimum_x, maximum_x = min(xs), max(xs)
    minimum_y, maximum_y = min(ys), max(ys)
    content_width = maximum_x - minimum_x + 1
    content_height = maximum_y - minimum_y + 1
    available = size - padding * 2
    scale = min(available / content_width, available / content_height)
    fitted_width = max(1, int(round(content_width * scale)))
    fitted_height = max(1, int(round(content_height * scale)))
    offset_x = (size - fitted_width) // 2
    offset_y = (size - fitted_height) // 2
    destination = array("f", [0.0]) * (size * size * 4)
    for y in range(fitted_height):
        source_y = minimum_y + min(content_height - 1, int(y / scale))
        for x in range(fitted_width):
            source_x = minimum_x + min(content_width - 1, int(x / scale))
            source_start = (source_y * source_width + source_x) * 4
            destination_start = ((offset_y + y) * size + offset_x + x) * 4
            destination[destination_start:destination_start + 4] = source[source_start:source_start + 4]
    icon = bpy.data.images.new(output_path.stem, width=size, height=size, alpha=True, float_buffer=False)
    icon.pixels.foreach_set(destination)
    icon.filepath_raw = str(output_path)
    icon.file_format = "PNG"
    icon.save()
    bpy.data.images.remove(icon)
    bpy.data.images.remove(source_image)


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
) -> tuple[list[dict[str, object]], dict[str, list[dict[str, int]]]]:
    """Pack deterministic, page-bounded sheets using Blender's image API."""
    planned_pages, regions = plan_grid(
        {clip: len(paths) for clip, paths in frame_paths.items()},
        frame_size,
    )
    # Remove only numbered spill pages from an earlier generation of this asset.
    for stale in output_path.parent.glob(f"{output_path.stem}_*{output_path.suffix}"):
        page_suffix = stale.stem.removeprefix(output_path.stem + "_")
        if page_suffix.isdigit():
            stale.unlink()
    pages: list[dict[str, object]] = []

    for planned in planned_pages:
        page_index = planned["index"]
        width = planned["width"]
        height = planned["height"]
        page_path = (
            output_path
            if page_index == 0
            else output_path.with_name(f"{output_path.stem}_{page_index}{output_path.suffix}")
        )
        sheet = bpy.data.images.new(
            f"{output_path.stem}_{page_index}",
            width=width,
            height=height,
            alpha=True,
            float_buffer=False,
        )
        sheet_pixels = array("f", [0.0]) * (width * height * 4)

        for clip, paths in frame_paths.items():
            for frame_index, path in enumerate(paths):
                region = regions[clip][frame_index]
                if region["page"] != page_index:
                    continue
                image = bpy.data.images.load(str(path), check_existing=False)
                image.colorspace_settings.name = "sRGB"
                pixels = array("f", [0.0]) * (frame_size * frame_size * 4)
                image.pixels.foreach_get(pixels)
                # Blender and destination buffers are bottom-up; metadata is top-left.
                destination_y = height - region["y"] - frame_size
                for source_y in range(frame_size):
                    src_start = source_y * frame_size * 4
                    dst_start = (
                        (destination_y + source_y) * width + region["x"]
                    ) * 4
                    sheet_pixels[dst_start:dst_start + frame_size * 4] = (
                        pixels[src_start:src_start + frame_size * 4]
                    )
                bpy.data.images.remove(image)

        sheet.pixels.foreach_set(sheet_pixels)
        sheet.filepath_raw = str(page_path)
        sheet.file_format = "PNG"
        sheet.save()
        bpy.data.images.remove(sheet)
        pages.append({
            "index": page_index,
            "path": page_path,
            "width": width,
            "height": height,
            "decodedBytes": width * height * 4,
        })

    return pages, regions
