# Shop Premium-v2 Review

**Decision:** ACCEPTED

**Scope:** Phase 16, item 22 — stat cards, earned-coin balance, price and affordability states, level progress, purchase feedback, and paused-game return context

**Source commit:** `8f2a03886a13edc2b8fac0cb485c19e2725dcc25`

**Android workflow run / job:** `34761841420` / `103735942735`

**Android evidence artifact:** `10319755714` (`android-test-reports`)

**Artifact archive SHA-256:** `1a89b0591564cb3917b50274f0463a6d56b97e22161797802a9015c5bae01649`

**Accepted surface audit SHA-256:** `420b81ecbc88c4b95af71f1e26d4d67c6abc1a8ba627bcc0de6c83d81f10072f`

## Evidence opened and inspected

I opened and inspected both exact 1080×2220 Pixel 3a / API 35 touch-emulator captures and their combined sheet in `docs/art_reviews/shop_premium_v2/`:

1. `shop_affordability_emulator.png` — an $80 balance showing Affordable Strength/Dodge, insufficient Agility/Health, maxed Luck, five distinct progress values, and explicit paused-return context;
2. `shop_purchase_feedback_emulator.png` — the immediate post-purchase state with $25 remaining, Strength advanced to level 1, its next price updated, every affordability state recalculated, tap rings visible, and a specific purchase toast;
3. `shop_contact_sheet.png` — side-by-side affordability and purchased states on the minimum portrait profile.

Every evidence file's exact dimensions, bytes, and SHA-256 is recorded in `surface_audit.json`.

## Acceptance findings

- **Hierarchy:** title, earned-coin balance, paused context, five upgrade cards, and transient feedback form a clear top-to-bottom reading order with restrained forest glass and gold.
- **Stat cards:** every Hero stat has a semantic generated medallion, uppercase title, plain-English permanent benefit, level count, progress bar, affordability label, and current price.
- **Affordability:** `AFFORDABLE`, `NEED $ N`, and `MAXED` labels communicate state without relying on green, red, or gold. Generated normal/pressed versus disabled frames reinforce the distinction.
- **Progress:** each row displays `LEVEL N / 20` and a proportional bar. The maxed Luck example visibly fills the bar and replaces its price with `MAX`.
- **Purchase response:** a successful touch immediately deducts earned coins, increments the level, recalculates the next price and every card's affordability, disables newly unaffordable rows, and shows `PURCHASED | Strength +1 | -$ 55`.
- **Failure response:** insufficient and maxed taps produce specific `NEED` or `MAX LEVEL` feedback rather than silently failing.
- **Paused context:** `COMBAT PAUSED` is always visible; the contextual line distinguishes `Close returns to battle` from `Close returns to Pause`, preserving nested overlay behavior and the Resume fix.
- **Touch safety:** all five purchase cards retain 610×135 hit areas and Close remains 100×100. The accepted instrumentation flow continued a prepared save, opened Shop from live gameplay, purchased Strength, verified the new balance, captured feedback, and closed using taps only.
- **Economy scope:** the surface says upgrades use earned coins, and no IAP, billing, premium currency, or audio work was added.

The exact screenshots and audit above are accepted for this item. Future Shop presentation changes require new settled touch-emulator evidence.
