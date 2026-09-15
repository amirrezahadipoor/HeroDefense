# Asset Engine (Phase 28)

How Hero Defense art is produced, reviewed, and promoted. Pipeline code lives
in `tools/blender` (procedural scenes + EEVEE/Workbench rendering) and
`tools/visual` (review sheets, promotion, validation).

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

## Runbook (expands in 28.6)

- Render: trigger CI workflow per batch, or run
  `blender --background --factory-startup --python tools/blender/generate_assets.py -- --batch <name> --output <dir>`
  on a GL-capable machine.
- Validate: `python3 tools/visual/validate_generated_assets.py <dir>`.
- Review: `tools/visual/create_*_review.py` sheets (silhouette + grade strips
  land in 28.2), then `tools/visual/promote_*_batch.py`.
- Manifest records the engine version per batch (lands in 28.6).
