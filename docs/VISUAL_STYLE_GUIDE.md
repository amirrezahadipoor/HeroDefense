# Hero Defense Visual Style Guide

**Status:** locked baseline. Any deliberate exception must be documented in the asset manifest and reviewed before commit.

## 1. Visual Goal

Readable low-poly fantasy miniatures with strong silhouettes, restrained detail, three-band toon lighting, and a dark blue-green outline. The Android game is 2D: Blender is used offline only, and the accepted deliverables are transparent PNG frames plus libGDX atlas metadata.

At the 720×1280 reference viewport, the Hero must remain recognizable at approximately 150 px tall. Equipment must read by silhouette and color rather than tiny surface detail.

## 2. Geometry Budgets

Budgets count **triangles after modifiers** at render time.

| Asset | Target | Hard maximum |
|---|---:|---:|
| Hero body/hair/base clothing | 2,400 | 3,000 |
| One Hero equipment attachment | 150–350 | 500 |
| Fully equipped Hero | 3,800 | 5,000 |
| Regular enemy | 1,400 | 2,400 |
| Boss | 3,500 | 6,500 |
| World Tree | 5,000 | 8,500 |
| Ground tile | 100 | 300 |
| Arena prop | 300 | 1,200 |
| Inventory icon-only mesh | 150 | 600 |

Use flat shading. Bevels are permitted only where they improve the silhouette, normally one segment. Hidden faces should be removed from final procedural meshes when practical.

## 3. Materials and Toon Bands

- Render engine: **EEVEE**, transparent film, ambient occlusion enabled.
- Color management: AgX, `Medium High Contrast`, exposure `0`, gamma `1`.
- Every opaque character material uses exactly three diffuse value bands:
  - shadow: base color × `0.55`;
  - midtone: base color × `0.82`;
  - light: base color × `1.08`, clamped.
- Ramp thresholds: `0.32` and `0.68`; interpolation is constant.
- Specular is disabled except metal (`0.28`) and potion glass (`0.4`). Roughness is `0.72` for cloth/skin/wood and `0.38` for metal.
- No photo textures, gradients, procedural noise smaller than four output pixels, or realistic skin shaders.
- Team readability: Hero greens/gold; regular enemies muted rust/purple/stone; boss accents may use cyan, crimson, amber, or violet.

### Locked Palette

| Role | Hex |
|---|---|
| Outline | `#142126` |
| Hero forest green | `#2E6B47` |
| Hero leaf light | `#74C365` |
| Hero gold | `#D6AD4C` |
| Hero skin | `#D9A978` |
| Wood | `#70452C` |
| Enemy rust | `#9A4D36` |
| Enemy violet | `#66507E` |
| Stone | `#65727A` |
| UI ink | `#0B1419` |
| UI parchment | `#E7D8B1` |

## 4. Outline

- One continuous dark blue-green outline using `#142126`, not pure black.
- Target apparent thickness: 2 px on 192 px character frames; 3 px on 256 px boss frames.
- Generate through Blender Freestyle or the pipeline's alpha-dilation post-process. Do not hand-paint per frame.
- Interior lines are used only for major overlaps (weapon over torso, jaw/helmet, separated limbs).
- Rare/Legendary glow is **never baked into this outline**; it is a runtime effect.

## 5. Fixed Camera — Do Not Change Per Asset

All character, equipment, item, prop, and tree renders use the named `HD_CAMERA` rig.

| Property | Locked value |
|---|---|
| Projection | Orthographic |
| Camera location | `(6.5, -9.5, 6.2)` |
| Look-at target | `(0, 0, 1.15)` |
| Orthographic scale, regular character | `3.0` |
| Orthographic scale, boss | `4.4` |
| Orthographic scale, item icon | `2.2` |
| Orthographic scale, World Tree | `6.0` |
| Framing shift | `0` except Tree `+0.12` and Boss `+0.06`; projection angle remains identical |
| Character forward direction | `(0, -1, 0)` toward camera |
| Frame center | pelvis at X center; ground plane at 12% frame height |

Never orbit the camera to make an individual asset look better. Correct the mesh/silhouette instead.

## 6. Fixed Lighting — Do Not Change Per Asset

- `HD_KEY`: Area light at `(-4.0, -4.5, 8.0)`, energy `900 W`, size `5.0 m`, neutral warm `#FFF3DF`.
- `HD_FILL`: Area light at `(5.0, -1.5, 4.5)`, energy `280 W`, size `4.0 m`, cool `#C7DEFF`.
- `HD_RIM`: Area light at `(0.0, 5.0, 6.5)`, energy `450 W`, size `3.0 m`, pale green `#D8FFD2`.
- World strength: `0.25`; transparent output.
- Contact shadows on. Shadow softness comes only from area-light size.
- A neutral ground catcher may inform contact shading, but exported character PNGs remain transparent.

## 7. Rig and Attachment Contract

Character armatures use these stable bone names:

`root`, `pelvis`, `spine`, `chest`, `neck`, `head`, `upper_arm.L/R`, `forearm.L/R`, `hand.L/R`, `thigh.L/R`, `shin.L/R`, `foot.L/R`, `weapon_socket`, `helmet_socket`, `armor_socket`, `boot_socket.L/R`, `ring_socket.L/R`.

- Origin: center of both feet at ground level.
- Unit: one Blender meter; Hero height `2.0 m`.
- Equipment is parented to the matching socket bone and keeps scale `(1,1,1)`.
- Ring silhouettes may be exaggerated up to 2.5× physical scale for mobile readability.
- No equipment-specific correction may alter the base animation keyframes.

## 8. Animation Contract

| Clip | Frames | Loop | Intent |
|---|---:|---|---|
| Idle | 6 | yes | breathing, bow/weapon settle |
| Attack | 8 | no | anticipation, strike/release, recovery |
| Hit | 4 | no | sharp readable recoil |
| Death | 10 | no | silhouette collapse, final hold |

- Playback baseline: 12 fps; gameplay may scale Attack timing with Agility.
- Keyframes are authored on armature bones. Mesh-object-only transforms do not count as character animation.
- Root translation is zero for the Hero. Enemies may use in-place walk cycles; game code controls travel.
- First and last Idle poses match. Death's last two frames are held.

## 9. Output and Packing

| Asset | Frame size | Padding |
|---|---:|---:|
| Hero / regular enemy | 192×192 PNG | 4 px extrusion |
| Boss | 256×256 PNG | 6 px extrusion |
| World Tree | 256×256 PNG | 6 px extrusion |
| Equipment / potion icon | 96×96 PNG | 4 px extrusion |
| Ground tile / prop | 192×192 PNG | 4 px extrusion |

- PNG: RGBA8, straight alpha, transparent background.
- Filenames: `<entity>_<variant>_<clip>_<frame:02>.png`, lowercase snake case.
- Atlas pages: maximum 2048×2048, nearest-neighbor min/mag filtering, no rotation, duplicate padding enabled.
- Pivot metadata: normalized feet/pelvis anchor stored in the manifest; do not infer pivots from opaque pixels at runtime.
- Keep all committed visual output under `android/assets/generated/` and source scripts under `tools/blender/`.

## 10. Runtime Scale and Composition

- Hero screen height: 145–175 reference pixels.
- Regular enemy: 105–155 px according to type.
- Boss: 190–260 px.
- World Tree: 290–360 px and visually behind the Hero.
- Ground props must not compete with enemies in saturation or contrast.
- UI stays vector/ShapeRenderer/Scene2D where that is clearer; not every UI panel goes through Blender.

## 11. Batch Review Checklist

Every rendered batch must pass all checks before its own commit/push:

1. Camera, lighting, color management, and frame dimensions match this guide.
2. Triangle counts are present in `asset_manifest.json` and below hard limits.
3. Armature and required clip names exist; character motion is bone-driven.
4. No frame clips the silhouette, shadow, weapon, or outline.
5. Alpha edges are clean against both light and dark checkerboards.
6. Pivot remains stable across all animation frames.
7. Equipment follows socket bones without visible sliding or intersection at key poses.
8. At 1× game scale, the silhouette and action remain readable on a phone.
9. Rare/Legendary output contains no baked glow.
10. A contact sheet has been opened and visually reviewed before acceptance.
