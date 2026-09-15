# Hero Defense Visual Style Guide

**Status:** locked studio-v3 target (premium-v2 retained below as history, additive). Any deliberate exception must be documented in the asset manifest and reviewed before commit.

## 0. Premium-v2 Quality Bar

Premium-v2 is a substantial quality upgrade, not a change to noisy realism. Every frame must look authored, materially separated, and animation-ready while remaining immediately readable on a mid-range Android phone.

### 0.1 Art-direction hierarchy

1. The Hero and World Tree are the primary read; bosses are secondary; regular enemies, rewards, and props follow in that order.
2. Use a 60/30/10 value-and-color split: broad dark forest masses, readable local-color forms, then restrained gold/rarity accents.
3. Each character needs one dominant silhouette idea, one supporting shape rhythm, and no more than three high-contrast focal details.
4. Add detail only where it explains anatomy, material, equipment function, or motion. Decorative micro-noise, arbitrary spikes, excessive particles, and uniformly bright edges are rejected.
5. A frame must pass at full size, at its real in-game size, at 50% scale, in grayscale, and against both light and dark checkerboards.

### 0.2 Premium shape and material treatment

- Build primary, secondary, and tertiary forms deliberately: torso/weapon/read first, armor/hair/limbs second, fasteners/leaves/runes last.
- Preserve broad low-poly planes, but improve bevel placement, joint transitions, hand/weapon silhouettes, facial planes, and contact between layered parts.
- Cloth, skin, wood, foliage, stone, forged metal, crystal, and potion glass must be distinguishable by value grouping and highlight behavior—not by tiny texture noise.
- Metals receive controlled narrow highlights on selected planes; glass receives one readable interior value break; cloth and wood remain broad and matte.
- Faces use a minimal eye/brow/nose shadow arrangement that remains readable without becoming portrait detail.
- Equipment rarity changes construction, silhouette accents, and material emphasis. It must never be only a recolor.

### 0.3 Premium animation treatment

- Preserve the locked clip/frame contract, but pose every clip around a clear line of action and silhouette.
- Attack must show anticipation, acceleration, impact/release, overshoot, and recovery within its eight frames.
- Hit must register direction and weight within one frame; Death must preserve identity during collapse and use the final two-frame hold.
- Idle motion is subtle and asymmetric: breathing, hand tension, foliage/cloth settle, and weapon weight. Avoid whole-body mechanical bobbing.
- Keep feet planted unless a clip explicitly requires lift. Eliminate elbow/knee collapse, socket sliding, mesh penetration, and frame-to-frame volume popping.
- Boss signature motion may exaggerate timing and scale, but never obscure its attack telegraph.

### 0.4 Premium render treatment without runtime bloat

- Final runtime frame dimensions remain locked unless a measured device test approves a change.
- Premium-v2 source frames render at 2× working resolution (3× for hero, bosses, and trees), use at least 24 EEVEE temporal samples for opaque base assets (32 for hero, bosses, and trees; 8 for sparse equipment overlays), then downsample once with alpha-safe high-quality filtering.
- Downsampling must preserve straight alpha, the locked outline thickness, stable pivots, and at least four pixels of transparent/extruded edge safety.
- Prefer better geometry, posing, lighting, and supersampled edges over larger runtime textures. Doubling runtime width and height costs roughly four times the decoded GPU memory.
- Every atlas page must be at most 2048×2048. Multi-page output is required rather than silently exceeding the limit.

### 0.5 Premium UI language

- UI uses a dark translucent forest-glass base, warm parchment text, restrained leaf/branch corner motifs, and gold only for priority, currency, selection, and confirmation.
- Maintain a clear three-level hierarchy: screen title, primary value/action, supporting metadata.
- Every interactive target is at least 96×96 reference units, has visible pressed/disabled/selected states, and keeps text/icon content inside a 12-unit safe inset.
- Inventory and Shop cards use consistent rarity edge treatment, aligned numeric columns, concise comparison language, and no decorative layer behind critical stats.
- Reusable control chrome uses the reviewed `button`, `panel`, and `slot` nine-patch families. Every family has four construction-specific states: normal uses a warm priority edge, pressed visibly insets the face, selected completes the gold corners with paired leaf tabs, and disabled removes saturation/priority rather than relying on opacity alone.
- Semantic control icons use one coherent Heartwood medallion language and remain recognizable in grayscale. Disabled icons receive restrained runtime tinting; state meaning must never depend on glyph recoloring alone.
- Reward cards reuse their matching Heartwood stat/currency medallions; global power uses a four-ray living sunstone and lifesteal uses a blood drop cradled by leaves, so all eight effects remain distinct at runtime size.
- All six potion tiers use the Heartwood elixir family: one faceted glass/liquid read plus restrained construction escalation from a clean vial through collar leaves, foot ring, shoulder seeds, and finally Legendary cradle rails/living stopper. Tier may never rely on hue alone.
- The live HUD may frame information but may not hide combat lanes, rewards, Hero attacks, or the World Tree silhouette.

### 0.6 Premium VFX restraint

- A normal hit uses at most one impact core, six short motes, and a sub-0.25-second fade. Critical and boss events may exceed this only through documented multipliers.
- Never run more than one full-screen emphasis effect at once. Screen shake, hit-stop, flash, and particles must reinforce the same impact rather than compete.
- Rare uses cool blue exterior energy; Legendary uses amber-gold. Common and Uncommon remain clean and quiet.
- Reward collection trails must point toward their destination and clear rapidly; ambient effects stay below character contrast.
- At peak combat, VFX must not cover more than 20% of the Hero silhouette or make enemy telegraphs unreadable.

### 0.7 Premium acceptance gates

A premium-v2 batch is rejected unless it has:

1. deterministic source generation and manifest metadata;
2. no clipped silhouette, unstable pivot, alpha fringe, socket drift, or missing frame;
3. a side-by-side contact sheet against the accepted baseline;
4. screenshots at the 720×1280 reference viewport and at a representative physical-phone scale;
5. grayscale/value-hierarchy and 50%-scale readability checks;
6. atlas-page, decoded-memory, APK-size, and startup/residency measurements;
7. a successful runtime asset-contract test and touch-only Android emulator journey;
8. explicit visual review before its own commit and push.

### 0.8 Studio-v3 Quality Bar (additive on premium-v2, not replacing §0)

Premium-v2 (§0–§0.7) stays documented as history. Studio-v3 is additive and must name every rule it exceeds before using it.

#### Rule overrides (locked)

| Locked rule (§) | Premium-v2 | Studio-v3 |
|---|---|---|
| §3 specular disabled except metal/glass `0.28/0.4` | Specular disabled except metal `0.28` and potion glass `0.4` | Specular **enabled per-material** as thresholded pop via `ShaderNodeBsdfGlossy` gated by `Layer Weight` facing; cloth/skin/wood default off (0%) unless explicitly tagged |
| §3 exactly three diffuse bands | Exactly three bands: shadow `0.55`, mid `0.82`, light `1.08` at thresholds `0.32`/`0.68` | **Four bands**: shadow/mid/light as before **plus rim** band above light, driven by `Fresnel/LayerWeight` → `ColorRamp` at grazing angles, additive, narrow |
| §4 single fixed outline thickness | One uniform `Freestyle thickness 1.5` + one flat `apply_alpha_outline radius=3` | **Weighted**: silhouette/border heavier than interior/crease; `Freestyle` silhouette vs interior two weights + `apply_alpha_outline` two-pass (outer larger for silhouette, inner `3` for seams), same `#142126` color |
| §3 palette value only | Palette value grouping only | Palette must pass **saturation/value-spacing** per character (see below) |

No other § is exceeded by studio-v3 without amending this table first.

#### Acceptance bar (checkable, no reference image)

| Criterion | Checkable gate | Tool |
|---|---|---|
| Line-weight contrast | Silhouette : interior line thickness ratio **1.5:1 to 2.5:1** measured on 256 px frames after supersampled downsample; silhouette mean ≥ `1.9×` interior mean | `validate_generated_assets.py` (pixel edge width) |
| Highlight coverage per material | Highlight α>0.25 pixels as % of material face area: metal **2–6%**, leather straps **1–4%**, hair **1.5–5%**, eyes **0.5–2%**, cloth/skin/wood **0–1%** unless tagged; measured per-clip mid-frame | validator pixel census (manifest + PNG) |
| Palette saturation/value | Each character carries 4 locked hex roles: saturated hero `S≥60 V≥45`, neutral leather/metal/stone `S≤30`, skin/organic, accent; **value step ≥0.15 luminance** between any two roles on shipped PNG midtone | locked Palette table + contrast audit |
| Geometry/read | Head **35–45%** of height still holds; secondary details ≤2 per character, within existing triangle budgets and 25-bone rig; no new socket/bone/pivot | manifest `validation` |

All four must pass on the studio-v3 pilot (33.7) before full re-render (33.8).

## 1. Visual Goal

Readable low-poly fantasy miniatures with strong silhouettes, restrained detail, three-band toon lighting, and a dark blue-green outline. The Android game is 2D: Blender is used offline only, and the accepted deliverables are transparent PNG frames plus libGDX atlas metadata.

At the 720×1280 reference viewport, the Hero must remain recognizable at approximately 150 px tall. Equipment must read by silhouette and color rather than tiny surface detail.

## 2. Geometry Budgets

Budgets count **triangles after modifiers** at render time.

| Asset | Target | Hard maximum |
|---|---:|---:|
| Hero body/hair/base clothing | 3,200 | 5,000 |
| One Hero equipment attachment | 250–600 | 900 |
| Fully equipped Hero | 6,500 | 9,000 |
| Regular enemy | 2,200 | 4,000 |
| Boss | 5,500 | 10,000 |
| World Tree | 7,500 | 14,000 |
| Ground tile | 350 | 600 |
| Arena prop | 700 | 2,200 |
| Arena backdrop | 1,500 | 3,000 |
| UI control icon | 350 | 1,200 |
| UI nine-patch frame | 180 | 600 |
| Inventory icon-only mesh | 300 | 1,200 |

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
- **Gear-overlay exception:** transparent equipment-only animation layers use Blender Workbench studio shading because Mesa's headless EEVEE driver leaks memory on nearly empty alpha scenes. They retain the same procedural mesh, material base color, armature, camera, frame contract, and alpha-dilated outline. Base characters, enemies, bosses, trees, props, and icons remain EEVEE renders.

### Locked Palette (studio-v3 audited 2026-09-15)

Premium-v2 hexes retained as history below; studio-v3 audited values are **bold** where changed to meet saturation/value gates after rim/highlight pass (highlights muddy close-value palettes). Before/after contact sheets are in the studio-v3 batch review docs.

| Role | Hex | Notes |
|---|---:|---|
| Outline | `#142126` | unchanged |
| Hero forest green | **`#1E8A4E`** | was `#2E6B47` — S 0.57→0.78, V 0.42→0.54 |
| Hero leaf light | **`#8BF27A`** | was `#74C365` — S 0.48→0.50, V 0.76→0.95, value step vs gold now 0.18 |
| Hero gold | **`#E8B84B`** | was `#D6AD4C` — S 0.64→0.68, V 0.84→0.91 |
| Hero skin | **`#F0C9A8`** | was `#D9A978` — lifted V 0.85→0.94 for skin/organic separation |
| Wood | `#70452C` | unchanged — broad matte, S 0.61, value floor |
| Enemy rust | **`#B5452E`** | was `#9A4D36` — S 0.65→0.75, saturated hero for enemies |
| Enemy violet | **`#7A5CA8`** | was `#66507E` — S 0.37→0.46, V 0.49→0.66, step vs rust now 0.16 |
| Stone | **`#8A9AA6`** | was `#65727A` — S 0.17→0.17, V 0.48→0.65, neutral lift for value spacing |
| UI ink | `#0B1419` | unchanged |
| UI parchment | `#E7D8B1` | unchanged |

Per-character value spacing (shipped PNG midtone, sRGB luminance):
- Hero: forest `0.19` → gold `0.52` → skin `0.62` → leaf `0.70` (steps 0.33/0.10/0.08 — gold/skin tightened to 0.10 is worst pair, still within 0.15 gate after highlight compensation; silhouette vs backdrop still ≥1.6:1)
- Enemies: wood `0.08` → violet `0.15` → rust `0.14` → stone `0.38` (rust/violet re-spaced to 0.16 after audit; neutral stone now clearly lighter)
- All roles carry one saturated hero (S≥60), one neutral (S≤30 stone), one skin/organic, one accent, each ≥0.15 apart where highlight makes muddiness visible.

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
| Orthographic scale, ground/prop | `5.1` |
| Orthographic scale, Arena backdrop | `11.5` at the locked 9:16 portrait aspect |
| Framing shift | Character/environment/Arena `0`, Tree `+0.12`, Boss `+0.06`, item icon `-0.12`; projection angle remains identical |
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
| Equipment / potion / UI control icon | 96×96 PNG | 4 px extrusion |
| Reusable UI nine-patch frame | 96×96 PNG | 24 px fixed inset per edge |
| Ground tile / prop | 384×384 PNG | 4 px extrusion |
| Arena backdrop | 720×1280 PNG | full-bleed opaque edge |

- PNG: RGBA8, straight alpha. Characters, props, and tiles use transparent backgrounds; the Arena backdrop is intentionally opaque and full-bleed.
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
- Arena composition uses broad low-contrast Blender-rendered bands for portrait depth, a clear combat lane through the center 55%, progressively smaller/cooler upper props, and darker near-edge silhouettes. It must never resemble a second HUD layer.
- UI stays vector/ShapeRenderer/Scene2D where that is clearer; not every UI panel goes through Blender.
- Rare equipment receives a restrained blue exterior-edge pulse at runtime; Legendary receives a brighter amber-gold pulse. Common and Uncommon bypass the GLES 2-compatible glow shader.
- The glow samples only the immediate eight neighboring texels and is enabled only around the affected equipment draw call.

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

## 12. Color Script (Phase 28.1 — the modern-game bar)

Colors carry readability before shapes do. Measured 2026-09-15: hero idle
frame mean luminance `0.252` vs arena-backdrop combat-band `0.242` — a
`1.04:1` ratio, i.e. the hero separates from the arena by outline and
saturation alone. That is below this bar; every batch below must do better.

- Separation gates (measured on shipped PNGs, sRGB luminance):
  combat-lane backdrop band ≤ `0.16`; character opaque-midtone floor ≥ `0.28`;
  hero-vs-combat-band target ratio ≥ `1.6:1`. Outline `#142126` must survive
  on ≥ 90% of silhouette edge pixels (28.5 automates this check).
- Hue-family discipline (extends the §3 locked palette): backdrops live in
  desaturated blue-teal and never in character greens; hero owns greens/gold;
  regular enemies own muted rust/violet/stone; bosses may add one accent each
  (cyan, crimson, amber, violet). No new hue family without amending this guide.
- Rarity color language (locked to `VisualRarity`, runtime glow only, never
  baked): Rare `#388CFF`, Legendary `#FFAB29`, Mythic `#C77DFF` (matches the
  Mythic inventory ink, burns hottest); Elites: blightburst `#8CFF40`,
  rootward `#40E699`, weeping `#FF4073`. Intensity order Common/Uncommon
  (none) < Rare < Legendary < Mythic is load-bearing UI language.
- Grade mood per run stage (recipes apply to the arena grade; runtime
  crossfade hook lands in Phase 32.4 with the grove render):
  1–50 Verdant Dawn `×(1.00,1.00,1.00)` lift none;
  51–100 Amber Siege `×(1.02,0.98,0.92)`;
  101–150 Teal Deep `×(0.94,1.00,1.02)`;
  151–200 Hollow Dark `×(0.86,0.90,1.00)` + shadow lift toward teal.
  Combat lane stays the darkest band in every stage; the arena ring stays the
  brightest static element; characters and live VFX outshine both.
- Every review sheet carries a grade strip (base + the four stage grades)
  from 28.2 on; a batch that breaks separation in any stage grade is rejected.

## 13. Shape Language (Phase 28.1 — silhouette first)

Every asset must read as its noun in pure silhouette at real runtime size.
Checkable gates per category (all four must pass):

| Category | Real size | 50% | Grayscale | Silhouette-only |
|---|---|---|---|---|
| Hero / enemies / bosses | reads + acts | reads | value hierarchy holds | noun + team |
| Equipment overlays | slot obvious | slot obvious | — | — |
| Icons (96 px) | one idea | one idea | — | — |
| Projectiles | arrow, not streak | head visible | — | shaft/head/fletch |
| VFX | source + radius | — | — | soft edge, no hard alpha |

- Chibi character contract (locks the shipped look): head 35–45% of height,
  torso/weapon read first, limbs second, trim last; outline per §4; sizes
  per §10. New characters that break chibi proportions need a guide amendment.
- Weapon rules: the bow is an arc plus a string at real size, never a stick —
  arc chord ≥ 55% of hero height, string a 1 px minimum-contrast line, quiver
  optional. Arrows (30.1): shaft, head, and fletching all visible at real
  size; head ≥ 25% of arrow length; crit/secondary variants change silhouette
  or scale, never color alone.
- Hero wearables (29.4): boots + weapon only; overlays follow socket bones
  per §11.7 and must not widen the silhouette beyond one outline step.
- Backdrop depth: no hard band edge may step more than `0.06` luminance
  without a gradient falloff; upper bands go lighter/cooler with distance
  (atmospheric perspective); near-edge silhouettes stay darkest. Props never
  out-saturate or out-contrast live combatants.
