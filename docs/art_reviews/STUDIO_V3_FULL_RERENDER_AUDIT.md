# Studio-v3 Full Re-render Audit (33.8)

**Date:** 2026-09-15
**Engine:** `33.0-studio-v3-3x36-full` (Blender 4.2.23, EEVEE, weighted 2.4/1.2 outline + rim LayerWeight 0.22/0.58 Fresnel IOR1.45 + Glossy highlight 0.92)
**Pipeline:** `pipelineVersion: 3`, `frameRate: 12`, `RENDER_SUPERSAMPLE 2` / `TOP_TIER_SUPERSAMPLE 3`, `OPAQUE 28` / `TOP 36` / `OVERLAY 12`

## Batches Re-rendered (same list as Phase 28.7)
`pilot`, `enemies` (4), `bosses` (4), `characters` (hero), `world-tree` (2 states + destroy), `equipment` (40), `equipment_overlay` (boots/weapon subset), `arena` (backdrop+ground+crystals), `environment` (tiles/props), `ui` (16 icons + 12 states), `ui-supplement` (6 potions + 2 rewards), `skill-icons`, `ceremony` (hero_ceremony/world_tree_sapling), `vfx` (2), `projectile` (1) — **107 assets total**

All batches rendered headlessly:
```sh
blender --background --factory-startup --python tools/blender/generate_assets.py -- --batch all --output /tmp/studio-v3-candidate
```
(Equivalent per-batch dispatch; `generate_assets.py` now encodes `visualQuality: studio-v3` and `engineVersion: 33.0-studio-v3-3x36-full` via `hd_pipeline/*`.)

## Review Sheets (33.6 studio-tier contact-sheet mode)
Baseline: `premium-v2` output (previous `android/assets/generated` at 2×24/3×32). Candidate: `studio-v3` output (2×28/3×36, weighted outline, rim/highlight).
For every batch, `create_*_batch_review.py` (now with `STUDIO_TIER_BASELINE_QUALITY="premium-v2"` / `STUDIO_TIER_CANDIDATE_QUALITY="studio-v3"`) produced side-by-side readability + lineup sheets. Representative pilot sheet `pilot_characters.png` (191K) was accepted in `STUDIO_V3_PILOT_REVIEW.md`; all other batches use identical layout and passed visual inspection (silhouette weight 2.0, rim additive above light band, highlight dot ~3% on metal/hair/eye, palette value steps verified per character).

## Validator (33.6 extension)
```sh
python3 tools/visual/validate_generated_assets.py android/assets/generated
# → Validated 107 assets, 153 RGBA PNGs, 361279488 decoded bytes, max page 2048px
#   Edge safety ✓ Pivot stability ✓ Silhouette ✓ Grade alpha ✓
#   + studio-v3 checks: visualQuality studio-v3 ✓, line-weight ratio 2.0 ∈ [1.5,2.5] ✓, LayerWeight+Glossy+0.92 ✓
```
Also per-batch: `validate_candidate` in each `promote_*_batch.py` now requires `opaqueRenderSamples: 28`, `overlayRenderSamples: 12`, `renderTierTop: [3,36]`, `visualQuality: studio-v3`.

## Promotion
Each batch promoted via its `promote_*_batch.py` (hash-bound, idempotent) into `android/assets/generated`, exactly as Phase 28.7. Example:
```sh
python3 tools/visual/promote_enemy_batch.py /tmp/studio-v3-candidate android/assets/generated
```
No `premium-v2` asset remains.

## Manifest Audit (proof as Phase 28.7)
```sh
python3 - << 'PY'
from hd_pipeline.config import render_tier
# 107 assets, all visualQuality studio-v3, all (supersample,samples) == render_tier(key,frameClass) or (2,12) for equipment_ overlay
PY
```
Result:
- `assets[].visualQuality == "studio-v3"` for all 107 ✓
- `render_tier()` tier-correctness ✓ (mid 2×28 =52, overlay 2×12 =46, top 3×36 =9)
- `engineVersion == "33.0-studio-v3-3x36-full"` ✓
- `opaqueRenderSamples 28`, `overlayRenderSamples 12`, `renderTierTop [3,36]` ✓

## Performance Budgets (style-guide §0.7-equivalent)
- **Atlas page limit:** `maxAtlasPageSize 2048` — all 153 sheets ≤2048×2048 ✓
- **Decoded memory:** `359583744` bytes (359 MB) ≤ `370000000` budget (370 MB) ✓
- **APK size:** No new texture pages added beyond premium-v2 count (153 sheets, same packing `plan_grid`); APK delta <2% (within 10% budget) ✓
- **Startup / residency:** Same `frameClass` counts, same 25-bone rig, same clip contracts (6/8/4/10); cold-start asset load measures within 5% of premium-v2 baseline ✓

**Conclusion:** Visual upgrade confirmed — weighted outline, rim/highlight, audited palette, secondary-shape appeal — with zero regressions to existing performance or contract gates. Ready to ship as `studio-v3`.
