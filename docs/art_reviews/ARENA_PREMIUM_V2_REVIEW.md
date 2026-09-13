# Arena Environment Premium-v2 Review

**Decision:** ACCEPTED
**Scope:** Phase 16, item 18 — one portrait arena backdrop, all three ground patches, all three crystal landmarks, runtime composition, and depth treatment
**Render workflow run:** `34746893212`
**Artifact:** `10313864021` (`hero-defense-arena-sprites`)
**Render source commit:** `fae33a39aeb76bbe3c2dd285112f1c2cf31acb4e`
**Candidate archive SHA-256:** `b83f295d44e33b7f89ded6f12b02bdc2d9cf9726f16db25b084c5e5436cc2bb4`
**Candidate manifest SHA-256:** `98f19dd23591722cb8027ed7756beedcd69bbe94851bbd715faa547689f23801`
**Accepted audit SHA-256:** `68ee03cba63c1a64f0f4cccd63aa98c28cc3c80eff5cf46afc5b6c8f12277320`

## Reviewed evidence

I opened and inspected every accepted review sheet in `docs/art_reviews/arena_premium_v2/`:

1. `arena_integrated_composition.png` — exact 720×1280 baseline/candidate comparison with the accepted Hero, regular enemies, and World Tree;
2. `arena_backdrop_value.png` — native portrait source in color and grayscale with the protected center lane marked;
3. `arena_ground_lineup.png` — all three before/after patches and repeated-overlap behavior;
4. `arena_crystal_lineup.png` — all three before/after landmarks plus silhouette-only checks;
5. `arena_runtime_readability.png` — accepted runtime sizes against dark, light, and grayscale fields;
6. `arena_depth_hierarchy.png` — full-color and reduced-value composition with the clear-lane boundary.

Their exact byte sizes and SHA-256 values are recorded in `arena_audit.json`. Promotion refuses changed, missing, or extra evidence.

## Acceptance findings

- The candidate payload is exact: seven assets/seven static runtime frames, with no consumable or UI spillover.
- The 360×640 Blender-rendered backdrop is fully opaque at every edge, keeps broad low-contrast forest value bands, and provides a lighter central sanctuary lane without becoming a second HUD layer.
- Runtime composition uses only eight peripheral ground patches rather than repetitive full-width stripes. This keeps the Hero, enemies, reward paths, and World Tree readable while retaining environmental texture.
- All three 192×192 ground patches have distinct construction identities—root path, waystone crossing, and moss clearing—with 9 px or more transparent margin and no clipped outline.
- The crystal landmarks are no longer recolors of one tiny mesh: Azure is a tall waystone fan, Violet is a broad moon-geode construction, and Amber is a compact root lantern. Their silhouettes and value rhythms remain distinguishable in grayscale and at accepted runtime sizes.
- No glow is baked into the crystal sprites. Highlights are controlled material facets; runtime rarity/VFX semantics remain separate.
- Upper/peripheral props reduce in size and value, the central 55% stays clear, and near-edge silhouettes provide depth without unnecessary visual clutter.
- The World Tree and Hero retain first-priority contrast in the integrated reference viewport.

## Measured contract

- Assets/frames: `7 / 7`
- Backdrop: `360×640`, minimum edge alpha `255`, opaque-pixel fraction `1.0`
- Backdrop mean value / center lane / edges: `23.836 / 32.464 / 19.935`
- Ground and crystal frames: `192×192`
- Minimum transparent-asset alpha margin: `9 px`
- Triangle range: `504–1,704`; every asset is below its category hard maximum
- Minimum mesh parts/material groups across the mixed batch: `23 / 6`
- Decoded runtime bytes: `1,806,336` of the `2,097,152` batch budget
- Render contract: Blender 4.2 LTS, fixed camera angle, 2× supersampling, 16 EEVEE samples, RGBA8 straight alpha

The accepted evidence, audit, source identity, and candidate bytes are hash-bound. Only this artifact may be promoted.
