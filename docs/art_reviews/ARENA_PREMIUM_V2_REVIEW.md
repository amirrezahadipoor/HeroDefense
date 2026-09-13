# Arena Environment Premium-v3 Review

**Decision:** ACCEPTED
**Scope:** Display-quality pass — the portrait arena backdrop re-rendered at native 720×1280, all three ground patches and three crystal landmarks at 384×384, with the whole environment value range lifted so text and actors read on real OLED panels
**Render workflow run:** `34773320326`
**Artifact:** `hero-defense-arena-sprites`
**Render source commit:** `7e1851a` (branch `phase17-display-quality`)
**Candidate archive SHA-256:** `79f90e9bc5862b18c0f4e5346f70616fe34469550d0c37cf4a3210985d3c9264`
**Candidate manifest SHA-256:** `526736515d316dd13cde2320931272c4309e60284d2168e55ade67866f9213d8`
**Accepted audit SHA-256:** `c0d4efe8c3bb9f0db123d7780b52ba686f1089fe61af304a637f9b39f909330f`

## Why this batch exists

The accepted v2 backdrop was 360×640 and was upscaled 2× to the 720×1280 world and then 1.5× again to 1080p panels: three times its native size, visibly soft. Its mean value (≈24/255) combined with dark scrims left 93–97 % of every captured frame below 40/255. This batch fixes the source; the runtime changes (ExtendViewport, density-true fonts, lifted scrims) are reviewed separately in `DISPLAY_QUALITY_PREMIUM_V3_REVIEW.md`.

## Reviewed evidence

I opened and inspected every sheet in `docs/art_reviews/arena_premium_v2/` (directory name kept for tooling continuity):

1. `arena_integrated_composition.png` — 720×1280 baseline/candidate with Hero, enemies, and World Tree;
2. `arena_backdrop_value.png` — native portrait source, colour and grayscale, clear lane marked;
3. `arena_ground_lineup.png` — three patches before/after and overlap behaviour;
4. `arena_crystal_lineup.png` — three landmarks before/after plus silhouettes;
5. `arena_runtime_readability.png` — runtime sizes on dark/light/grayscale fields;
6. `arena_depth_hierarchy.png` — full colour and reduced value with the lane boundary.

## Acceptance findings

- Backdrop is native 720×1280: no upscale at world resolution, 1.5× at 1080p (acceptable for broad matte bands).
- Mean backdrop value rose from ≈24 to ≈37/255; the centre lane still reads lighter than the edges, the 55 % clear lane is preserved, and the Hero/World Tree keep first-priority contrast.
- Ground patches (384 px) hold a two-step value gap under the darker backdrop bands instead of merging with them.
- Crystal landmarks are unchanged in construction, only re-rendered at 2× so facets no longer alias.
- Decoded budget raised from 2 MiB to 8 MiB for this category (7225344 bytes used); combat residency budget is unchanged.

## Measured contract

- Assets/frames: `7 / 7`
- Minimum transparent margin: `20 px`
- Triangles: `504–1704`
- Decoded bytes: `7225344` of `8388608`
