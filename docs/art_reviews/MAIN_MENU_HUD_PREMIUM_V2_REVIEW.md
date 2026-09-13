# Main Menu and Live HUD Premium-v2 Review

**Decision:** ACCEPTED

**Scope:** Phase 16, item 20 — Main Menu and live HUD hierarchy, panels, typography treatment, buttons, touch feedback, and gameplay visibility

**Source commit:** `fcd08dfbb51d438493566666c97267d9e5a63d55`

**Android workflow run / job:** `34752508440` / `103711298622`

**Android evidence artifact:** `10316422349` (`android-test-reports`)

**Artifact archive SHA-256:** `5ed238f6a68e495f930d24c6afe8f6bdd907fffc2f481e299277f0a3028c31d1`

**Accepted surface audit SHA-256:** `8fb923814763fc593550740e3165ce677232c86c6da23da10777d3a13139597f`

## Evidence opened and inspected

I opened and inspected both exact 1080×2220 Pixel 3a / API 35 touch-emulator captures and their combined sheet in `docs/art_reviews/main_menu_hud_premium_v2/`:

1. `main_menu_emulator.png` — settled Main Menu with the reviewed arena backdrop, title hierarchy, total coins, enabled and disabled actions, and portrait safe-area behavior;
2. `live_hud_emulator.png` — active Wave 1 combat with health, wave, coins, speed, pause, Inventory, and Shop all visible at real device scale;
3. `main_menu_hud_contact_sheet.png` — side-by-side minimum-profile comparison without cropping away the emulator's actual letterbox/safe regions.

Every evidence file's exact dimensions, byte size, and SHA-256 is recorded in `surface_audit.json`.

## Acceptance findings

- **Main Menu hierarchy:** a quiet arena-derived backdrop supports a single gold title panel, one compact persistent-currency panel, and three large action rows. The 60/30/10 dark-green, local-color, and gold hierarchy is immediate without decorative noise.
- **Action clarity:** each 480×140 action retains a semantic generated icon, strong uppercase action label, concise supporting line, structural frame state, and a 4-unit pressed-content shift. Continue is visibly disabled without disappearing.
- **Typography treatment:** eyebrow, display title, action label, supporting copy, compact HUD category, and live numeric value each have a distinct scale and restrained shadow. Text remains English-only and readable on the minimum portrait profile.
- **HUD visibility:** the former 147,920-unit opaque top block is replaced by segmented health, wave, coin, speed, and pause islands totaling 116,152 units — a **21.48% occlusion reduction** while preserving the full-width health read and open sight lines into combat.
- **Live priorities:** health is first and changes from green to amber to red at explicit thresholds; wave and total coins remain persistent; speed uses the selected frame above 1×; pause remains isolated; direct Inventory and Shop stay visible at the bottom.
- **Touch safety:** top actions remain at least 120×100, bottom utilities remain 150×104, and Main Menu actions remain 480×140. The real instrumentation journey opened the game, Shop, Inventory, Pause, and resumed entirely through touchscreen events.
- **Feedback restraint:** generated pressed/selected frames and the content shift provide local contact feedback; the global tap effect is now two fading outline rings rather than an opaque disc over gameplay or labels.
- **Integrated quality:** the HUD frames and medallions match the accepted premium-v2 assets, remain legible against the upgraded arena, and do not cover the Hero, World Tree, or central combat lane.
- **Scope discipline:** no audio asset or audio-system work was included.

The exact screenshots and audit above are accepted for this item. Future changes to these primary surfaces require new settled touch-emulator evidence.
