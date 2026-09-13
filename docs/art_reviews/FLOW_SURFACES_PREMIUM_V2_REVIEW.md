# Flow Surfaces Premium-v2 Review

**Decision:** ACCEPTED

**Scope:** Phase 16, item 23 — Pause, Settings, Level-Up, Reward Card, Game Over, and Victory surfaces upgraded to the coherent premium-v2 standard

**Source commit:** `6316c383b27211ce6c10b9d10e82e4ec179203fd`

**Android workflow run / job:** `34766141882` / `103747331693`

**Android evidence artifact:** `10320901806` (`android-test-reports`)

**Artifact archive SHA-256:** `f711ca0482f6785dee004d8d8b30ec2a013f3955487649d19e2af76a763ec313`

**Accepted surface audit SHA-256:** `02347d3aae5f1f68219f839e61f5fa261d9f9e443e033d71c319da600e6bff83`

## Evidence opened and inspected

Six exact 1080×2220 Pixel 3a / API 35 touch-emulator captures plus their combined sheet in `docs/art_reviews/flow_surfaces_premium_v2/`:

1. `pause_emulator.png` — dimmed live arena, `COMBAT PAUSED` header, Stat Shop / Inventory / gold-selected Resume, and a run-context strip (hero level, wave, coins);
2. `settings_emulator.png` — Sound Effects toggled `OFF` (normal frame, muted label) beside Music `ON` (selected frame, gold label), explicit `tap to enable` / `tap to mute` hints, and a touch-only guidance panel;
3. `level_up_emulator.png` — `LEVEL 3 REACHED`, `2 TALENT POINTS TO SPEND`, and five rows each with medallion, gain-per-point text, point count, and `NOW` → `NEXT` derived values (`12 dmg` → `14 dmg`, `1 aps` → `1.03 aps`, `x1.00` → `x1.02`, `0%` → `0.5%`, `100 HP` → `110 HP`);
4. `reward_cards_emulator.png` — `BOSS 1 OF 20 DEFEATED`, `Reward power 100%`, three framed cards with title, generated magnitude, effect kind, and `PERMANENT` tag, plus a footer stating the exactly-one rule;
5. `victory_emulator.png` — gold `WORLD TREE SAVED` treatment, five icon-led summary rows, and a gold-selected `DEFEND AGAIN` action;
6. `defeat_emulator.png` — crimson `WORLD TREE FALLEN` treatment revealed only after the destruction clip, matching summary rows, and `RESTART AT WAVE 1`.

The first review pass rejected the reward footer because its second line touched the lower frame edge; the footer was raised and deepened (`6316c38`) and the accepted captures above are from the corrected build. Every file's dimensions, bytes, and SHA-256 are recorded in `surface_audit.json`.

## Acceptance findings

- **Coherence:** all six surfaces now share the `button`/`panel` nine-patch families, `OverlayText` shadowed parchment typography, gold priority accents, and the header/body/action hierarchy already accepted for Main Menu, HUD, Inventory, and Shop. The last ShapeRenderer-only panels are gone.
- **Pause:** targets widened to the shared 480-unit column (Resume 480×240, secondary 480×140); the hit tests in `PauseTouchLayout` remain the single source for both rendering and touch.
- **Settings:** state is communicated by label, frame state, and hint text, never color alone. Close remains 100×100.
- **Level-Up:** previews are computed from `HeroStatCalculator`, so equipment bonuses are included and the 60% dodge cap is honored; rows disable when no points remain.
- **Reward cards:** descriptions still come from the same `RewardPowerBudget` object that applies the effect; the surface adds boss index, budget percentage, and permanence without changing the deterministic offer.
- **End of run:** victory and defeat are distinguished by title, colour band, copy, and action label; the `0.82 s` destruction reveal delay and interactivity gate are unchanged and remain unit-tested.
- **Touch safety:** the smoke journey drove every surface by taps only — Settings toggle and close, two talent points spent, the final Boss 20 card into Victory and a fresh run, and an organic one-HP defeat into Restart. Core suite: 179 tests green.
- **Scope:** no audio, VFX, balance, or save-format changes.

Future changes to these surfaces require new settled touch-emulator evidence.
