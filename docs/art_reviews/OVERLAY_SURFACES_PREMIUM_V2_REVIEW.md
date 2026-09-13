# Pause, Progression, and Run-Result Surfaces Premium-v2 Review

**Decision:** ACCEPTED

**Scope:** Phase 16, item 23 — Pause, Settings, Level-Up, Reward Card, Game Over/Defeat, and Victory surfaces

**Source commit:** `37880ad6e1f85566e3eba67d9fe3b293c585ac76`

**Android workflow run / job:** `34768821991` / `103754537381`

**Android evidence artifact:** `10320544285` (`android-test-reports`)

**Artifact archive SHA-256:** `96ff58227ab2b72a0aaf3f1cf674cebf68418ddffbc452bf047b6bd0e32bdeab`

**Accepted surface audit SHA-256:** `ac49b0b0ad2bffb9a1f5a15c9590531ac6460593a117f4b2a88fda721f696dc5`

## Evidence opened and inspected

I opened and inspected all six exact 1080×2220 Pixel 3a / API 35 touch-emulator captures and both combined sheets in `docs/art_reviews/overlay_surfaces_premium_v2/`:

1. `pause_emulator.png` — paused battlefield context, Shop, Inventory, and dominant Resume action;
2. `settings_emulator.png` — immediate-save context and explicit selected `ON` states for both large toggles;
3. `level_up_emulator.png` — Level 7, two pending points, five generated stat cards, exact gains, current values, and mandatory-choice context;
4. `reward_cards_emulator.png` — Boss 1 milestone and three generated semantic reward cards with exact permanent run bonuses;
5. `defeat_emulator.png` — red outcome hierarchy, Wave 47 run ledger, recovery message, and restart action;
6. `victory_emulator.png` — green/gold outcome hierarchy, Wave 100 run ledger, sanctuary confirmation, and restart action;
7. `pause_settings_level_contact_sheet.png` and `reward_results_contact_sheet.png` — grouped real-size comparisons proving one coherent system across all states.

Every evidence file's exact dimensions, bytes, and SHA-256 is recorded in `surface_audit.json`. The test harness suppresses Android's own immersive-mode education and error dialogs so evidence contains only the game surface.

## Acceptance findings

- **Shared language:** all six states use the accepted Heartwood panel/button frames, semantic medallions, dark forest glass, restrained gold, local green/red outcome accents, and one scale hierarchy.
- **Pause:** the underlying battle remains faintly contextual while simulation is stopped. Stat Shop and Inventory are equal secondary actions; the 360×240 Resume card is intentionally dominant and retains pressed movement.
- **Settings:** title/context and two 520×150 toggle rows are visually separate. `ON`/`OFF` text accompanies selected/normal frames, so state does not depend on color. Changes still save immediately.
- **Level-Up:** every 540×130 card states the stat, exact gain, current value, and `TAP TO ADD`. The header shows level, pending points, and why combat remains paused.
- **Reward Cards:** three 580×190 choices identify the defeated Boss, semantic reward name, exact magnitude, permanence, and claim action. Combat-resume behavior is explicit.
- **Defeat:** `DEFEAT` and `WORLD TREE FALLEN` use restrained red against the common gold hierarchy. Wave, level, kills, earned kill coins, and defeated Bosses remain readable, followed by recovery guidance.
- **Victory:** `VICTORY`, `WORLD TREE SAVED`, and `100 WAVES ENDURED` clearly differ from Defeat while retaining identical ledger geometry for honest comparison.
- **Restart:** both terminal outcomes keep the same 480×160 touch action and clearly state that a new defense restarts at Wave 1.
- **Persisted results:** a terminal save now exposes `VIEW RESULT` from the Main Menu, allowing both Defeat and Victory ledgers to be reopened through touch instead of becoming unreachable.
- **Touch proof:** seven Android tests passed. Every navigation, toggle, talent allocation, reward choice, terminal-result opening, and restart target is exercised through touchscreen coordinates only.
- **Scope discipline:** Settings received visual treatment only; no audio asset or audio-system work was added.

The exact screenshots and audit above are accepted for this item. Future changes to these overlays require new settled touch-emulator evidence.
