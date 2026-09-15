# Render-Tier Precision Review (Phase 28.3)

**Date:** 2026-09-15

**Pipeline:** Blender 4.2.23 LTS, headless EEVEE, per-category working resolution + temporal samples, alpha-safe linear downsampling

**Scope:** per-category render tiers — hero, `hero_*`, `boss`, and `tree` frame classes render top tier (3× working resolution / 32 EEVEE samples); every other opaque asset renders mid tier (2× / 24); sparse equipment overlays stay 8 samples (provenance-only Workbench count). Scene setup, per-asset manifests, and the batch manifest all derive from `render_tier()` in `tools/blender/hd_pipeline/config.py`.

**Result:** **Approved — tiers apply in production, hero gains edge precision, mid tier holds output stable, dimensions unchanged**

## Proof runs (pilot batch: hero + rootling)

Both runs rendered the same `pilot` batch headlessly on CI; only the tier config differs.

| Run | Role | Commit | Batch floors | hero | rootling | Wall time |
|---|---|---|---|---|---|---|
| [34945036285](https://github.com/amirrezahadipoor/Herodefense/actions/runs/34945036285) | BEFORE | `0a6eab8` (pre-tier) | 2× / 16 / 8 | 2× / 16 | 2× / 16 | ~10 min |
| [34945891580](https://github.com/amirrezahadipoor/Herodefense/actions/runs/34945891580) | AFTER | `0a52372` (tiers) | 2× / 24 / 8 + top [3, 32] | 3× / 32 | 2× / 24 | ~11 min |

## Review evidence

Full frames at 1× plus a pixel-honest 2× NEAREST center zoom per frame. Checkerboards expose the real alpha edge rather than adding a presentation background. Captions carry Laplacian-variance sharpness (B/A) and mean absolute pixel difference per frame.

| Review sheet | What was inspected | SHA-256 |
|---|---|---|
| [`render_tiers_28_3/tier_proof_hero.png`](render_tiers_28_3/tier_proof_hero.png) | Hero idle/attack/hit/death before-vs-after: edge precision gain | `4af39dc51f116bea30fcca3ddf8e8fcf06fee9a10b64a6878d1f0f1fddce0bef` |
| [`render_tiers_28_3/tier_proof_rootling.png`](render_tiers_28_3/tier_proof_rootling.png) | Rootling control: mid tier must not shift approved output | `dd4c58944848e1c1f6d160db9221d5c30526f4d729a1073233535e1bb3116e62` |

## Measurements

| Frame | Hero sharpness B → A | Hero mean\|d\| | Rootling sharpness B → A | Rootling mean\|d\| |
|---|---|---|---|---|
| idle #0 | 657 → 681 (+3.6%) | 0.14 | 527 → 527 (+0.0%) | 0.03 |
| attack #4 | 651 → 675 (+3.7%) | 0.15 | 534 → 534 (−0.1%) | 0.03 |
| hit #2 | 613 → 619 (+1.0%) | 0.13 | 533 → 534 (+0.2%) | 0.04 |
| death #5 | 465 → 475 (+2.1%) | 0.14 | 335 → 335 (+0.0%) | 0.02 |

Output PNG dimensions are identical before/after (1920×768 both sheets), so no atlas layout, pivot, or runtime contract moves.

## Visual decisions

- **Hero (treatment): accepted.** Every sampled frame gains sharpness (+1–4%); the 2× zooms show visibly cleaner silhouette and inner-detail edges (cloak rim, hat brim, clasp ring). The 3×/32 top tier earns its keep on the most-watched sprite in the game.
- **Rootling (control): accepted.** Same 2× working resolution with 16→24 samples converges to a near-identical image (mean|d| ≤ 0.04), which is the desired result: the mid-tier floor raise must not shift already-approved output, and it does not.
- **Cost: accepted.** The tiered pilot costs ~1 extra CI minute (~10%); the gain concentrates exactly where the art direction needs it (hero/bosses/trees) while everything else pays only the 16→24 sample floor.
- **Provenance: accepted.** Per-asset manifests record the tier that actually rendered (`renderSupersample`/`renderSamples`), and the batch manifest keeps validator-compatible floors plus the `renderTierTop*` rule fields.
