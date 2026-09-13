# Planting Ceremony Premium-v2 — Category Review

- **Decision:** ACCEPTED
- **Review date:** 2026-09-14
- **Scope:** Wave 100 planting ceremony — Hero Walk / Plant / Water clips with seed pouch, seed, and watering can props, and the second World Tree's seed-to-sapling growth plus its standing idle sway
- **Accepted render source:** `7ec2292` on `phase18-living-world`
- **Accepted CI run / artifact:** `34785374167` / `10326039393` (`hero-defense-ceremony-sprites`)
- **Review evidence:** `docs/art_reviews/ceremony_premium_v2/`

## Review basis

Every one of the **46** runtime frames was decoded and audited by `tools/visual/promote_ceremony_batch.py`, and all three generated review sheets were opened and inspected: the Hero full-motion sheet (walk 8, plant 10, water 10), the sapling growth/idle sheet (grow 12, idle 6), and the 720×1280 reference-scale arena composition placing the grown sapling at `WorldLayout.SECOND_TREE` (578, 738) beside the Heartwood with the Hero at the planting stand point.

The Hero is the accepted premium-v2 elf archer on the same 25-bone humanoid rig, so the hand-off from the combat idle at the anchor into the ceremony walk and back again does not change proportions, palette, or feet placement (shared 0.5 / 0.12 pivot, 192 px frames). The three ceremony props (belt seed pouch, seed, wooden watering can) use the existing wood/parchment palette and never cover the face or the bow silhouette. The walk is a mirrored two-step gait (5 unique poses across 8 frames, by design); plant kneels, presses the seed, and rises (6 unique poses with a symmetric return); water tilts the can and holds the pour across frames 4–8 (5 unique poses) so the droplet particles in the runtime have a stable spout.

The sapling is a younger, slimmer Heartwood on the accepted 13-bone segmented tree rig with the same four silhouette landmarks (buttress roots, heart aperture, guardian boughs, tiered crown) and the same 12-material surface language. Growth starts from a 25 px sprout, rises monotonically to 186 px, then springs and settles over the last three frames (authored overshoot; final height 172 px, ≥ 85 % of the peak). The idle loop continues from the final growth frame with a 0.7 % alpha difference, so the runtime switch from `grow` to `idle` is seamless.

All geometry, materials, rigging, motion, and pixels come from the repository's offline Blender pipeline (`tools/blender/hd_pipeline/ceremony.py`). No external asset was introduced.

## Provenance and exhaustive audit

| Evidence | SHA-256 |
|---|---|
| Accepted candidate manifest | `af1d7b2f50f70ecc91711e30c5f01f9c7d44e506d8aaee5621358723c5995ce8` |
| Final exhaustive audit | `c2d8ce1e8a3ef2d5da9f1b2089e5a63c664c4ab23d9698d9733e50a3dece3b82` |

Locked audit results:

- exactly **2** assets and no unrelated payload; **46** frames total, **10,715,136** decoded bytes (one 1920×576 and one 2048×768 RGBA page);
- Hero: 1,812 triangles, 54 mesh parts, 15 materials; minimum alpha margins ≥ **13 px** on every side of every frame (no boundary frame);
- Sapling: 4,840 triangles, 143 mesh parts, 12 materials; minimum alpha margins ≥ **34 px**;
- visible-frame diversity walk 5 / plant 6 / water 5 / grow 12 / idle 5, all at or above the floors recorded in the audit;
- growth heights `25 → 186 → 172` px with strictly increasing silhouette area through frame 10.

## Decisions

| Asset | Clip | Decision | Notes |
|---|---|---|---|
| hero_ceremony | walk | Accepted | 10 fps in runtime; mirrored horizontally for the walk back. |
| hero_ceremony | plant | Accepted | Seed becomes visible at 55 % of the clip (frame 5–6 press). |
| hero_ceremony | water | Accepted | Pour hold on frames 4–8 drives the droplet emitter; last frame held during GROW. |
| world_tree_sapling | grow | Accepted | 8 fps in runtime (1.5 s); final frame held during the walk back. |
| world_tree_sapling | idle | Accepted | 10 fps permanent sway from wave 101 onward; no health, no destruction clip (leaf burst only on defeat). |

## Runtime binding

`render/CeremonyHeroRenderer` and `render/SaplingTreeRenderer` load exactly these atlases and fail fast on any frame-count drift; `gameplay/PlantingCeremony` owns the deterministic timeline.
