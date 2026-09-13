# UI Assets Premium-v2 Review

**Decision:** ACCEPTED

**Scope:** Phase 16, item 19 — every equipment/drop, potion, currency, navigation, stat, speed, pause, Inventory, Shop, and reward-card icon, plus every reusable button, panel, and slot frame state

**Render workflow run:** `34748112536`

**Artifact:** `10315066154` (`hero-defense-ui-sprites`)

**Render source commit:** `1af4005742335dfa84016f12b3c449452f115347`

**Candidate archive SHA-256:** `e5c0c6fa388247c7becf9914b2b69af5a41f13d45977456a1cbb18ce878431dd`

**Candidate manifest SHA-256:** `981a2a9079bcafb5caa97cfedeca487bcb5f45bce9259f6e1bd7af3688936dce`

**Accepted audit SHA-256:** `d058a4403e947e67addcf270cbe7ef98b561d9329e36682ba51435c1b137fc32`

## Reviewed evidence

I opened and inspected all six accepted sheets in `docs/art_reviews/ui_assets_premium_v2/`:

1. `ui_icon_lineup.png` — all 16 semantic controls before and after at enlarged native-source scale;
2. `ui_icon_readability.png` — every icon at the 58 px live-HUD size against dark, light, and grayscale fields;
3. `ui_skin_state_matrix.png` — all 12 native frame assets: button, panel, and slot across normal, pressed, selected, and disabled;
4. `ui_nine_patch_stretch.png` — fixed-corner stretch checks at representative wide, tall, and row dimensions;
5. `ui_state_construction.png` — enlarged state differences proving they are not recolor-only;
6. `ui_integrated_surfaces.png` — restrained compositions for the Main Menu, live HUD, and Inventory.

Every evidence file's exact size and SHA-256 is recorded in `ui_audit.json`; promotion rejects missing, changed, or extra evidence.

## Complete icon-coverage supplement

**Supplement render workflow run:** `34748540660`

**Supplement artifact:** `10315440791` (`hero-defense-ui-supplement-sprites`)

**Supplement render source commit:** `c85ba78ad54f10dcc572083d2cbc61d9aa2e45dd`

**Supplement archive SHA-256:** `2a7a17fb4d0617a27b8529102ac4dfc5b1875f0b168509be300d49aca79fa71b`

**Supplement candidate manifest SHA-256:** `143a6f992d664f6be134a920398e7ce6d61090ecba68e2961689a39131f8fe02`

**Supplement accepted audit SHA-256:** `c8f0a8f0950a03e2d6e9ca394668fa1f0df80377720fcf8803e9041f83ca6239`

I opened and inspected all five supplemental sheets in `docs/art_reviews/ui_icon_supplement_premium_v2/`:

1. `ui_supplement_potions.png` — all six potion tiers before/after at enlarged source scale;
2. `ui_supplement_reward_icons.png` — a complete one-to-one semantic medallion map for all eight reward cards;
3. `ui_supplement_readability.png` — all potion and reward semantics at 54 px against dark, light, and grayscale fields;
4. `ui_supplement_value_progression.png` — grayscale and flat-silhouette proof that potion progression does not depend on hue;
5. `ui_supplement_integrated.png` — representative reward cards and the visible ground-to-homing Inventory destination.

The supplement contributes six premium Heartwood elixirs and the two previously absent reward semantics, Verdant Power and Crimson Root. Its exact eight-file payload, metadata, pixels, and five evidence sheets are hash-bound in `ui_supplement_audit.json`.

## Acceptance findings

- All 18 controls now use one coherent Heartwood medallion language while retaining distinct semantic glyphs for health, waves, currency, pause, speed, Inventory, Shop, settings, restart, new game, continue, close, strength, agility, luck, dodge, general power, and lifesteal.
- The complete six-tier Heartwood elixir family gains distinct vessel silhouettes and increasingly rich collars, feet, shoulder leaves, side pods, and crown construction, so value progression remains visible without hue.
- Icons retain clean alpha at 96×96 and remain readable at the actual 54–58 px gameplay sizes. They also separate from both light and dark fields without adding bloom or noisy decoration.
- The 12 frame assets cover every combination of three reusable families and four required interaction states.
- State changes are structural: pressed faces inset and gain a confirmation notch; selected frames complete their gold corners and add paired leaf tabs; disabled frames desaturate and expose a quiet center bar. Meaning never depends on hue alone.
- Nine-patch expansion preserves corner geometry, edge weight, and the dark content field across wide buttons, tall panels, and shallow list rows.
- Runtime press tracking begins on touch-down, follows touch-drag, and clears on touch-up. Disabled Main Menu controls and Inventory actions, selected Inventory rows, and normal/pressed live controls use the same reviewed state resolver.
- Runtime mappings resolve every equipment/drop icon, every potion tier, and all eight reward-card semantics from reviewed generated pixels; the two missing reward semantics now have dedicated medallions instead of placeholders.
- The integrated examples preserve the premium 60/30/10 hierarchy: dark forest glass, readable local-color glyphs, and restrained gold only for priority/selection.
- No keyboard or mouse-only path was introduced; all state feedback remains touch-driven.

## Measured contract

- Exact assets: `28` (`16` icons + `12` skin states)
- Skin matrix: `3` families × `4` states
- Runtime frame size: `96×96` RGBA8 straight-alpha
- Nine-patch fixed inset: `24 px` on every edge
- Primary minimum transparent margin: `5 px`; supplement minimum: `4 px`
- Primary triangle range: `454–904`; supplement range: `450–686`, both below locked hard maximums
- Primary minimum mesh parts/material groups: `7 / 5`; supplement minimum: `9 / 5`
- Primary decoded runtime bytes: `1,032,192` of `2,097,152`; supplement: `294,912` of `524,288`
- Supplement coverage: `6` potions + `2` new medallions, completing all `8` reward-card semantics
- Render contract: Blender 4.2 LTS, fixed camera, 2× supersampling, 16 EEVEE samples

Both audits, all eleven review sheets, both source manifests, and every candidate payload file are hash-bound. Only these exact artifacts may be promoted.
