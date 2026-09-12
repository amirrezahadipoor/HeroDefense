# Hero Defense Balance Contract

These coefficients are centralized in renderer-independent Java so the Phase 14 simulator can tune them without changing game-flow code.

## Regular enemy growth

For wave `w` clamped to 1–100:

- Baseline HP: `20 × 1.045^w`.
- Baseline damage: `5 × 1.025^(w−1)`.
- A regular hit is capped at 28% of the max HP of a reference Hero who invests one of every five earned points in Health.
- Archetype HP multipliers, relative to the 20-HP Rootling: Rootling `1.00`, Stonekin `1.70`, Gloom Wolf `0.85`, Fungal Brute `2.30`.
- Archetype damage multipliers, relative to the 5-damage Rootling: Rootling `1.00`, Stonekin `1.40`, Gloom Wolf `1.20`, Fungal Brute `2.00`.
- Movement speed, melee reach, and attack interval remain archetype properties rather than wave-scaled values.

## Boss growth

- Boss HP on milestone wave `w`: baseline regular HP at `w` × `15`.
- Boss contact damage on milestone wave `w`: baseline regular damage at `w` × `3`.
- Boss movement, reach, interval, and special behavior remain identity-specific.

## Critical hits

- Every Hero projectile has a deterministic `5%` critical chance and deals `1.75×` damage on success.
- A confirmed critical impact freezes only combat simulation for `45 ms`; UI and rendering continue.

## Direct stat shop

- Strength, Agility, Luck, Dodge, and Health can each be purchased up to 20 times with earned coins only.
- Base prices are `55`, `60`, `50`, `50`, and `65` coins respectively; each repeat purchase costs `1.22^n` times its base.
- The shop is entered and operated exclusively through touch targets from the paused run.

## Kill rewards

- Regular kill coins use each enemy archetype's base reward times `1 + 0.025 × wave`.
- Boss `n` grants `50 + 20n` coins; the permanent coin-income card multiplies both reward sources.
- Regular XP uses archetype values; boss `n` grants `100 + 30n` XP. Rewards are claimed once before dead entities are removed.

## Equipment drops

- Per defeated enemy before Luck: Common `6%`, Uncommon `3%`, Rare `0.8%`, Legendary `0.15%`.
- Each effective Luck point multiplies every band by `1.02`; at most one item drops from a kill.
- The single cumulative roll checks Legendary first, then Rare, Uncommon, and Common.

## Health potions

- Tier heals are `15%`, `25%`, `40%`, `60%`, `80%`, and `100%` of current maximum HP.
- Healing is capped at max HP, full-health use is rejected, and a successful use consumes exactly one potion.
- Each defeated enemy has an independent `8%` potion chance.
- Tiers unlock progressively; within the unlocked set, tier `n` receives weight `n`, shifting drops toward contemporary potions without removing weaker stock.

## Reward-card budget

- Boss `b` (1–20) uses multiplier `1 + 0.05 × (b−1)`, rising smoothly from `1.00` to `1.95`.
- Percentage effects multiply their base magnitude by that budget.
- Base-stat cards award the rounded budget in whole stat points: one point early and two points late.
- Every displayed description is generated from the same budget object used to apply the effect.

This is the first playable curve, not a claim of final balance. Phase 14 simulation and manual checkpoints must validate and, if necessary, revise the coefficients.
