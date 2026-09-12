# Hero Defense Balance Contract

These coefficients are centralized in renderer-independent Java so the Phase 14 simulator can tune them without changing game-flow code.

## Regular enemy growth

For wave `w` clamped to 1–100:

- The required starting candidate was `20 × 1.045^w`; deterministic simulation tuned the shipped baseline to `20 × 1.035^w` to remove late-run clear-time and incoming-damage spikes.
- Shipped HP checkpoints: Wave 1 `20.70`, Wave 25 `47.26`, Wave 50 `111.70`, Wave 75 `263.97`, and Wave 100 `623.83`.
- Baseline damage: `0.27 × 1.002^(w−1)`, reaching `0.3291` at Wave 100 before archetype scaling.
- A regular hit is capped at 28% of the max HP of a reference Hero who invests one of every five earned points in Health.
- Archetype HP multipliers, relative to the 20-HP Rootling: Rootling `1.00`, Stonekin `1.70`, Gloom Wolf `0.85`, Fungal Brute `2.30`.
- Archetype damage multipliers, relative to the authored 5-damage Rootling: Rootling `1.00`, Stonekin `1.40`, Gloom Wolf `1.20`, Fungal Brute `2.00`.
- Regular populations grow from four and cap at 24 so late waves remain a readable melee defense rather than an unbounded swarm.
- Movement speed, melee reach, and attack interval remain archetype properties rather than wave-scaled values.

## Boss growth

- Boss HP on milestone wave `w`: baseline regular HP at `w` × `15`.
- Boss contact damage on milestone wave `w`: baseline regular damage at `w` × `3`.
- Boss movement, reach, interval, and special behavior remain identity-specific.

## Hero stat gains

- Strength: `+2` base damage per point from a `10`-damage baseline.
- Agility: `+0.03` attacks per second per point from `1.00`; interval is the reciprocal.
- Luck: multiplies item-drop rates by `1.02` per point.
- Dodge: `+0.5` percentage points per point, capped at `60%`.
- Health: `+10` maximum HP per point from a `100`-HP baseline.

## Equipment tier power

| Tier | Relative power target | Whole-stat budget |
|---|---:|---:|
| Common | +5% | 1 point |
| Uncommon | +12% | 2 points |
| Rare | +25% | 4 points |
| Legendary | +45% | 7 points |

The relative targets express intended contemporary-run impact. Every authored item spends its tier's entire whole-stat budget across one or two of the five Hero stats.

## Critical hits

- Every Hero projectile has a deterministic `5%` critical chance and deals `1.75×` damage on success.
- A confirmed critical impact freezes only combat simulation for `45 ms`; UI and rendering continue.

## Direct stat shop

- Strength, Agility, Luck, Dodge, and Health can each be purchased up to 20 times with earned coins only.
- Base prices are `55`, `60`, `50`, `50`, and `65` coins respectively; purchase level `n` adds `20n` coins. This linear schedule tracks the 20 boss milestones without an unaffordable late-run exponential.
- The shop is entered and operated exclusively through touch targets from the paused run.
- For boss index `b` (1–20), the boss grants `50 + 20b` coins while a stat's contemporary purchase level `b−1` costs `base + 20(b−1)`. The reward alone therefore buys one upgrade in every case and is no more than 1.40× its price.
- Equipment resale is supplemental rather than the primary income source: Common `12`, Uncommon `30`, Rare `75`, and Legendary `180` coins.

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

The card regression runs all eight card identities as the forced choice at every boss with future combat (Bosses 1–19), for 152 complete simulations. Each remaining segment must retain at least 5% average gross damage, 25 seconds average clear time, and pressure on at least 90% of waves, while still respecting the 35% damage and 120-second spike ceilings. The calibrated scenarios retained at least `6.8017%` average damage and `33.822624 s` average clear time; their worst single wave was `30.708814%` damage and `100.86547 s`. Boss 20 is omitted because no wave remains after its reward.

## Renderer-independent simulation gate

`BalanceSimulator` advances the real movement, attacks, projectiles, enemy and boss behavior, progression, drops, potions, equipment, shop, reward-card, and wave-lifecycle systems at 30 Hz. Its balanced automated policy distributes points and shop purchases across all five stats, equips upgrades, sells spare gear, and chooses rewards by a fixed survival/power priority. The fixed baseline seed emits one CSV row per wave with HP, gross incoming damage, DPS-to-enemy-HP ratio, clear time, and timeout state.

The deterministic regression gate requires all of the following:

- Complete exactly 100 waves with the Hero alive.
- Average gross incoming damage from enemy attacks, divided by contemporary maximum HP, must be 5%–15% across the run. Gross damage is measured before potion and lifesteal recovery so healing cannot hide pressure.
- No single wave may exceed 35% gross damage or 120 seconds to clear.
- Every metric must be finite and no wave may hit the simulator's timeout.

After enemy and economy tuning, baseline seed `0x4845524F444546` completed 100/100 waves with `7.365432%` average gross damage, `19.6691%` maximum single-wave damage, `34.0728 s` average clear time, and `60.499 s` maximum clear time. This automated gate is reproducible balance evidence; the remaining multi-seed and manual checkpoints still have to validate resource starvation and subjective play feel.

Run `./scripts/balance-check.sh` immediately after every coefficient change and as a mandatory precondition to any manual playtest. The script forces a fresh run rather than accepting Gradle's prior task output. `BalanceSimulatorTest` also remains part of the complete `:core:test` suite executed by the core GitHub Actions workflow on every push and pull request.
