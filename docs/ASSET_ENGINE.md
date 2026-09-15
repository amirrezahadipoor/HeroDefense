# Asset Engine (Phase 28 — premium-v2)

How Hero Defense art is produced, reviewed, and promoted. Pipeline code lives
in `tools/blender` (procedural scenes + EEVEE/Workbench rendering) and
`tools/visual` (review sheets, promotion, validation). Every promoted PNG
is an opaque contract: its manifest records the exact engine that built it.

## 28.0 Headless-Blender spike — verdict: SPLIT GO (2026-09-15)

Question: can the EEVEE pipeline render in a clean GPU-less sandbox?

| Probe | Result |
|---|---|
| Blender 4.2.23 starts headless (user-space syslibs, no sudo) | ✅ GO |
| `BLENDER_WORKBENCH` 1-frame render (llvmpipe) | ✅ GO (~5 s, verified PNG) |
| `BLENDER_EEVEE_NEXT` 1-frame render, bare `--background` | ❌ NO-GO — `EGL Error (0x3009): EGL_BAD_MATCH` after ~2 min of shader compile |
| `BLENDER_EEVEE_NEXT` under `xvfb-run` (+ user-space Xvfb/XKB) | ❌ NO-GO — same `EGL_BAD_MATCH`, then killed |
| CI `generate-visual-assets.yml` (ubuntu-latest + xvfb) | ✅ GO — 27 runs on record, recent all green |

Sandbox recipe (reproducible, no sudo): `scripts/install-blender-temp.sh`
with `HERO_TOOLS_ROOT` on a large disk (`/tmp` here is a 1 GB tmpfs and does
NOT fit Blender), plus `apt-get download` + `dpkg-deb -x` of `libxkbcommon0`
libx11-6 libxi6 libxxf86vm1 libxfixes3 libxrender1 libgl1 libegl1 libsm6
libice6 libxext6 (libxau6 libxdmcp6 libbsd0 libmd0) into a sysroot on
`LD_LIBRARY_PATH`, with `LIBGL_ALWAYS_SOFTWARE=1`.

Production-path decision (locked by this spike):

1. **EEVEE categories** (characters, bosses, trees, arena, equipment, vfx
   look-dev) render via CI `generate-visual-assets.yml`
   (`workflow_dispatch` → batch), or on any GPU machine — never in this
   sandbox.
2. **In-sandbox production** = pipeline code + validation + review-sheet
   upgrades, `BLENDER_WORKBENCH` categories (gear overlays today), and 2D
   VFX/icon/item art through the same review → validate → promote flow.
3. Every batch, whatever its renderer, must pass `validate_generated_assets.py`
   and a human-visible review sheet before promotion.

---

## 1) How to run the pipeline

### 1.1 Prerequisites

- **Blender 4.2.23** (checksum-pinned, see `tools/blender/hd_pipeline/config.py: BLENDER_VERSION`).
  Never commit the binary — install to a disposable cache:
  ```bash
  export HERO_TOOLS_ROOT=/tmp/herodefense-tools   # must be on a large disk, not the 1 GB /tmp tmpfs
  bash scripts/install-blender-temp.sh
  export PATH="$HERO_TOOLS_ROOT/blender-4.2.23/blender:$PATH"
  ```
  The script also vendored the X11/EGL sysroot (libxkbcommon, libGL, libEGL …) and sets `LIBGL_ALWAYS_SOFTWARE=1`
  for the Workbench path.

- **Python 3.10+** with `Pillow` for review/validation image checks:
  ```bash
  python3 -m pip install --break-system-packages pillow
  ```

- **Disk:** `/tmp` inside the CI sandbox is only ~1 GB — the Blender install alone is >400 MB.
  Always set `HERO_TOOLS_ROOT` (and `GRADLE_USER_HOME`, `HERO_PROJECT_CACHE_DIR`) to a
  cache outside the repository.

### 1.2 Batch matrix

| Batch name | What it renders | Frame class | Category | Renderer |
|---|---|---|---|---|
| `pilot` | Hero + one enemy | character | pilot | EEVEE (CI) |
| `premium-pilot` | 6 premium hero variants | character | pilot | EEVEE (CI) |
| `characters` | Rootling, Stonekin, Gloom Wolf, Fungal Brute | character | enemies | EEVEE (CI) |
| `enemies` | Same as characters (legacy alias) | character | enemies | EEVEE (CI) |
| `bosses` | Ancient Golem, Thorn Matriarch, Ember Wyrm, Void Knight | boss | bosses | EEVEE (CI) |
| `world-tree` | Healthy, Damaged, Sapling | tree | world-tree | EEVEE (CI) |
| `equipment` | 41 equipment items (incl. icons) | character/item | equipment | Workbench (in-sandbox) + EEVEE for hero-socket |
| `equipment-overlay` | 14 hero-worn boots+weapon subset | character | equipment_overlay | Workbench |
| `arena` | Portrait backdrop 720×1280 + depth bands | arena | arena | EEVEE (CI) |
| `environment` | Ground tiles ×3, crystal props ×3 | environment | arena/environment | EEVEE (CI) |
| `ui` | 16 icons + 12 skin states (button/panel/slot ×4 states) | item | ui | 2D Workbench/vector |
| `ui-supplement` | 6 potions + 2 missing reward semantics | item | ui-supplement | 2D |
| `skill-icons` | 5 skill glyphs | item | skill-icons | 2D |
| `ceremony` | Planting ceremony hero + sapling growth | character/tree | ceremony | EEVEE (CI) |
| `vfx` | Impact flash, shockwave ring (8-frame strips) | vfx | vfx | EEVEE (CI) |
| `projectile` | Arrow shaft/head/fletching (64×64) | projectile | projectile | EEVEE/Workbench |

### 1.3 Running the pipeline

**On CI (preferred for EEVEE):**
1. Push your `tools/blender` changes to `main`.
2. In GitHub Actions, run **Generate visual assets** (`.github/workflows/generate-visual-assets.yml`) → *Run workflow* → pick `batch` from the dropdown → *Run*. The job installs Blender in the large cache, renders headlessly via `xvfb-run`, and uploads the artifact `generated-<batch>.zip`.

**On a GPU machine (local):**
```bash
blender --background --factory-startup --python tools/blender/generate_assets.py -- \
  --batch <name> --output /tmp/herodefense-generated/<name>

# Examples:
blender --background --factory-startup --python tools/blender/generate_assets.py -- --batch bosses --output /tmp/herodefense-generated/bosses
blender --background --factory-startup --python tools/blender/generate_assets.py -- --batch equipment-overlay --output /tmp/herodefense-generated/equipment-overlay
blender --background --factory-startup --python tools/blender/generate_assets.py -- --batch all --output /tmp/herodefense-generated/all
```

**In-sandbox dry run (no EGL required, Workbench only):**
```bash
# Only batches that use BLENDER_WORKBENCH (equipment-overlay, ui, etc.) can complete here.
# The EEVEE batches will correctly fail with EGL_BAD_MATCH — that is the expected SPLIT GO behavior.
blender --background --factory-startup --python tools/blender/generate_assets.py -- --batch equipment-overlay --output /tmp/herodefense-generated/equipment-overlay --isolate-frames
```

### 1.4 Output contract

Every run writes to `<output>/`:

```
asset_manifest.json   # top-level: pipelineVersion, engineVersion, renderTierTop, batch, assets[]
sprites/*.png + .atlas
equipment/*.png + .atlas
icons/*.png
vfx/*.png + .atlas
projectile/*.png + .atlas
```

The top-level manifest records the **engine version per batch** (see §3). Per-asset entries record `visualQuality`, `renderSupersample`, `renderSamples`, `modelRevision`, `rigProfile`, `triangles`, `pivot`, `frameSize`, and `clips`.

---

## 2) Review → Validate → Promote

No PNG is committed without passing all three gates in order. The pipeline is intentionally sequential — a bad batch fails fast.

### 2.1 Review sheets (28.2)

```
python3 tools/visual/create_<category>_review.py --input /tmp/herodefense-generated/<batch> --output docs/art_reviews/<REVIEW>.png
```

Each sheet exercises **four appeal views per sprite** (§12 shape language) and **five grade variants** (§12.4):
- `FULL | 50% | GRAY | SHAPE` — the 50% view is nearest-neighbor (runtime pixels, no smoothing), grayscale uses luma, silhouette is solid `#E8F3E8` keeping original alpha.
- `BASE | DAWN 1-50 | AMBER 51-100 | TEAL 101-150 | HOLLOW 151-200` — Dawn is identity (same as BASE), Amber warms, Teal cools, Hollow darkens + lifts teal shadows; all preserve alpha exactly.

Generated sheets live in `docs/art_reviews/` and are hash-bound into the promotion audit (see promote scripts). The 28.2 upgrade guarantees every batch has a silhouette-only column and a grade strip.

### 2.2 Validation (28.5)

```
python3 tools/visual/validate_generated_assets.py <output-or-android/assets/generated>
```

The validator is the **CI fail-fast gate** — it runs in both `test-core.yml` and `build-android.yml` on every push, *before* Gradle assembles the APK.

Checks (automatable where Pillow is available, manifest-only otherwise):

| Check | What it asserts | Automatable |
|---|---|---|
| `pipelineVersion ≥ 3` | premium-v2 output | manifest |
| `renderSupersample == 2` (or `3` for top-tier) | Working-render scale | manifest |
| `opaqueRenderSamples ≥ 16`, `overlayRenderSamples ≥ 8` | Sample floors (top-tier → 32 / 24) | manifest |
| `pipelineVersion`, `blenderVersion`, `engineVersion` present | Provenance | manifest |
| `pivot.units == normalized-bottom-left` + range 0–1 | Pivot contract | manifest |
| **Pivot stability** | `x≈0.5±0.06` for all; `y≈0.12±0.04` for character/boss, `0.06±0.04` for tree, `0.5±0.04` for arena/environment/item/vfx/projectile | manifest |
| **Edge safety** | No opaque pixel (α>10) touches the 1px inner border of any frame; exempt `arena_backdrop` (full-bleed) | image (Pillow) |
| **Silhouette coverage** | Opaque ratio (α>20) within `[0.002, 0.95]` general, tighter per-family: equipment `0.002–0.20`, sapling `0.002–0.60`, character/boss/tree/item/env `0.02–0.85`; catches empty/full/bleed batches | image |
| **Grade / silhouette alpha** | Every `STAGE_GRADE` + `silhouette_view` + `grayscale_view` preserves alpha exactly | image (review_strips) |
| `alphaMode == STRAIGHT_RGBA`, `bit_depth==8`, `color_type==6` | Texture contract | image header |
| Frame & page geometry, `decodedBytes`, page size ≤2048 | Atlas integrity | manifest + header |
| Icon 96×96 RGBA | Equipment icons | manifest + header |
| `decodedCatalog ≤ 335 MB` | Memory budget | manifest |
| No undeclared/missing PNGs | Manifest completeness | filesystem |

If Pillow is not installed the validator runs in manifest-only mode and prints `Pillow not available — skipped image-level checks`.

### 2.3 Promotion

```
python3 tools/visual/promote_<batch>_batch.py --source /tmp/herodefense-generated/<batch> --destination android/assets/generated
```

Each promote script:
- Re-validates the candidate manifest (tier floors, model revisions, engineVersion).
- Verifies hash-bound review evidence: `docs/art_reviews/<BATCH>_REVIEW.md` contains `**Decision:** ACCEPTED` plus the SHA256 of both the candidate manifest and the review audit.
- Copies PNGs/atlases/icons into `android/assets/generated/<family>/`, merges the candidate `assets[]` into the committed `asset_manifest.json` (replacing only that batch's keys, sorted), and is idempotent — running twice leaves the tree unchanged.

**Do not hand-copy PNGs.** Promotion is the only path into `android/assets/generated`.

---

## 3) Engine versioning

Every batch is version-stamped so `28.7` can prove no old-engine asset remains.

### 3.1 Top-level manifest

```json
{
  "pipelineVersion": 3,
  "blenderVersion": "4.2.23",
  "engineVersion": "28.7-premium-v2-3x32",
  "styleGuide": "docs/VISUAL_STYLE_GUIDE.md",
  "renderTierTop": [3, 32],
  "renderTierTopClasses": ["boss", "tree"],
  "renderTierTopKeyPrefix": "hero",
  "generatedBatch": "bosses",
  "generatedAt": "2026-09-15T14:00:00Z",
  "generatedCommit": "ac47254…",
  "assets": [ … ]
}
```

- `engineVersion`: `<roadmap-phase>-premium-v2-<supersample>x<samples>` (e.g., `28.6-premium-v2-2x16`, `28.7-premium-v2-3x32`). Bumped whenever `tools/blender/hd_pipeline/config.py` (renderTier, FRAME_SIZE, samples) or `docs/VISUAL_STYLE_GUIDE.md` palette/shape changes.
- `generatedAt` / `generatedCommit`: ISO-8601 UTC timestamp and the `git rev-parse HEAD` at generation time — the audit appendix for 28.7.
- `assets[].visualQuality`: always `"premium-v2"` for the 28.x series; top-tier assets also carry the higher `renderSupersample`/`renderSamples`.

### 3.2 Per-asset record (excerpt)

```json
{
  "key": "ancient_golem",
  "family": "boss",
  "frameClass": "boss",
  "visualQuality": "premium-v2",
  "renderSupersample": 2,
  "renderSamples": 16,
  "modelRevision": "heartstone-colossus-v2",
  "rigProfile": "premium-heavy-humanoid-v2",
  "pivot": {"units": "normalized-bottom-left", "x": 0.5, "y": 0.12}
}
```

After 28.7, every `assets[]` entry has `engineVersion` inherited from the top-level (or duplicated per-asset for the audit grep). The **audit command** for 28.7 is:

```bash
python3 tools/visual/validate_generated_assets.py android/assets/generated
python3 -c "
import json; m=json.load(open('android/assets/generated/asset_manifest.json'));
assert m.get('engineVersion','').startswith('28.7'), 'not 28.7';
print(f\"engine {m['engineVersion']}  batch {m['generatedBatch']}\");
bad=[a['key'] for a in m['assets'] if a.get('visualQuality')!='premium-v2'];
assert not bad, f'old quality {bad}';
# tier check
from tools.blender.hd_pipeline.config import render_tier
for a in m['assets']:
  ss, sm = render_tier(a['key'], a['frameClass'])
  assert (a['renderSupersample'], a['renderSamples']) == (ss, sm), f\"{a['key']} tier mismatch\"
print(f\"{len(m['assets'])} assets all on engine 28.7 tier-correct\")
"
```

If any old-engine asset remains, the validator and the tier check fail.

### 3.3 Version history (locked)

| Engine | Date | Change |
|---|---|---|
| `28.0-SPLIT-GO` | 2026-09-15 | Headless-Blender spike verdict, SPLIT decision |
| `28.3-premium-v2-2x16/3x32` | 2026-09-15 | Raise working-resolution/sample floors per category (hero/bosses/trees → 3×32) |
| `28.4-premium-v2-categories` | 2026-09-15 | Add vfx/projectile/equipment-overlay categories + atlas gates |
| `28.5-premium-v2-validate` | 2026-09-15 | Edge safety + pivot stability + silhouette + grade checks, CI fail-fast |
| **`28.6-premium-v2-runbook`** | **2026-09-15** | **This runbook + engineVersion stamping** |
| `28.7-premium-v2-3x32-full` | *next* | Full re-render on the finished 28.6 engine, no old-engine asset remains |

---

## 4) Accept / Reject checklist (human gate)

Use this checklist on every review sheet before promotion. A single **REJECT** blocks the batch; the artist fixes the source and re-renders.

### 4.1 Appeal (style guide §12–13)

- [ ] **Real size:** subject reads instantly at its in-game pixel size on a mid-range phone (no squinting).
- [ ] **50% scale:** silhouette still distinct at half size (nearest-neighbor, no blur hides it).
- [ ] **Grayscale:** forms separate by value alone; no two materials collapse to the same gray.
- [ ] **Silhouette:** pure `#E8F3E8` shape is instantly recognizable — hero = archer with bow, boss = tri-lith crest, tree = layered foliage.
- [ ] **Light / dark checker:** no lost edges on either background; outline `#142126` holds.
- [ ] **Palette 60/30/10** — broad dark forest masses, local-color forms, restrained gold/rarity accents.
- [ ] **Material read** — wood / cloth / skin / stone / metal / crystal / glass distinguishable by value + highlight, not micro-noise.
- [ ] **Rarity language** — Common matte, Uncommon slight emissive, Rare glow, Legendary particle-glow tier — shader change, never baked into the albedo.

### 4.2 Color grading (guide §12.4)

- [ ] `BASE` and `DAWN 1-50` are identical (reference).
- [ ] `AMBER 51-100` warms reds, cools blues by the locked recipe.
- [ ] `TEAL 101-150` cools, `HOLLOW 151-200` darkens + lifts teal shadows without hue-shifting outlines.
- [ ] No banding, no alpha change, no clipping to 0/255 on mid-tones.

### 4.3 Technical (validator mirrors this)

- [ ] Edge safety: no opaque pixel touches the 1px frame border (arena_backdrop exempt).
- [ ] Pivot stability: `0.5, 0.12` for character/boss, `0.5, 0.06` for tree, `0.5, 0.5` otherwise, within tolerance.
- [ ] Silhouette coverage not empty (<0.2%) nor full (>95%) for non-arena.
- [ ] Page ≤2048px, `decodedCatalog` ≤335 MB, icons 96×96 RGBA straight.
- [ ] `visualQuality == premium-v2` and `engineVersion == 28.6` (or newer) on every promoted entry.
- [ ] Review sheet hash-bound: markdown contains `**Decision:** ACCEPTED` and the SHA256 of both manifest and audit, and all sheet PNGs are byte-identical to the audit record.

### 4.4 Production hygiene

- [ ] `.blend` not committed unless <5 MB and regeneratable — otherwise only `tools/blender/*.py` is the source of truth.
- [ ] No SDK/Blender/cache in `android/assets/generated` — only PNG/atlas/json/manifest.
- [ ] `validate_generated_assets.py` green on both `test-core` and `build-android` CI before merge.

**Verdict:** `ACCEPTED` only if all boxes above are green. `REJECTED` lists the failing gate(s) and the fix (e.g., "Golem fist clips at 50% — widen silhouette, re-render hero_attack 03–05").

---

## 5) CI wiring

- `test-core.yml` → `Validate generated assets (edge safety, pivot stability, silhouette + grade — fail fast)` — runs *before* `:core:test`.
- `build-android.yml` → same validation — runs *before* `:android:assembleDebug`.
- `generate-visual-assets.yml` → `workflow_dispatch` batch picker, installs Blender to `$HERO_TOOLS_ROOT`, `xvfb-run blender --background …`, uploads `generated-<batch>.zip`.
- Any validation failure aborts the workflow in <10 s — no APK is built, no wasted emulator minutes.

---

## 6) Troubleshooting

| Symptom | Fix |
|---|---|
| `EGL_BAD_MATCH` on `--background` EEVEE | Expected in the sandbox — use CI or a GPU machine (SPLIT GO). |
| `pipelineVersion < 3` | Re-run with the current `generate_assets.py` (pipeline 3 = premium-v2). |
| `pivot unstable` | Check `_pivot_for()` in `generate_assets.py` — per-class pivot must match §2.2 table. |
| `edge safety violation` | Add 1–2 px transparent padding in the Blender camera margin or re-pack with 1px gutter in `atlas_layout.py`. |
| `silhouette coverage` empty/full | The model is off-camera or fills the frame — re-center the rig at `(0,0,1.15)` and re-check `CAMERA_TARGET`. |
| `decodedCatalog exceeds budget` | The batch's page count grew — re-pack with `atlas_layout.py` `maxAtlasPageSize=2048` or split the batch. |
| `undeclared PNG` | A hand-copied PNG slipped in — delete and re-promote via the promote script only. |

---

*This runbook is the single source of truth for how art moves from `tools/blender` into `android/assets/generated`. If the code and this document disagree, the code's validator wins — then update this document to match.*

