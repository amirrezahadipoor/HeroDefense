# Hero Defense Balance Contract

These coefficients are centralized in renderer-independent Java so the Phase 14 simulator can tune them without changing game-flow code.

## Regular enemy growth

For wave `w` clamped to 1–200 (Phase 18.4 extended the run; waves 1–100 keep the Phase 17 curve unchanged):

- The required starting candidate was `20 × 1.045^w`; Phase 14 simulation tuned it to `1.035`, and the Phase 17 lifesteal-and-skills rebalance raised it to the shipped `20 × 1.037^w` (Wave 100 enemies carry 21% more HP than before).
- Shipped HP checkpoints: Wave 1 `20.74`, Wave 25 `49.60`, Wave 50 `123.02`, Wave 75 `305.10`, and Wave 100 `756.67`.
- Baseline damage: `0.27 × 1.003^(w−1)`, reaching `0.3632` at Wave 100 before archetype scaling (Phase 17 raised growth from `1.002`).
- **Second half (waves 101–200, after the planting ceremony):** both curves continue from their Wave 100 values with their own growth, `HP × 1.023^(w−100)` and `damage × 1.008^(w−100)`. HP checkpoints: Wave 125 `1335.97`, Wave 150 `2358.78`, Wave 175 `4164.65`, Wave 200 `7353.08`; baseline damage reaches `0.8058` at Wave 200. Phase 18.4 shipped `1.021 / 1.006` against a simulator that ignored the Anvil; once the simulated player reforges equipped Rare/Legendary items (Phase 19.3) the second half fell to 1–3% pressure per wave, so the curve was tightened one notch. `1.024/1.008`, `1.024/1.010`, `1.025/1.010` and `1.0235/1.008` were rejected because their worst single wave exceeded the 35% ceiling (36–43%) or clears passed 80 s.
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

## Anvil (item reforging, Phase 18.3)

Only Rare and Legendary catalog items can be reforged, up to `+5`. Each step adds `+1` to every stat bonus on the item and raises its sell price by half the step's cost. Step costs are `base × 1.6^level`, rounded to 5 coins: Rare `150, 240, 385, 615, 985` (total `2 375`), Legendary `350, 560, 895, 1 435, 2 295` (total `5 535`). A fully reforged Legendary therefore carries `+17` whole stat points (7 authored + 10 forged), which is why the Anvil is priced like ~2–3 late stat levels per step rather than as a cheap sink.

## Economy audit (Phase 19.3)

The simulator's spending policy models a thrifty player: talent points go to the lowest base stat, coins always buy the cheapest affordable stat or skill level, spare drops are sold, and the Anvil is used on an equipped item whenever its next step is no dearer than the cheapest shop purchase (`BalanceSimulator.forgeEquippedItems`). `BalanceSimulator.lastLedger()` exposes the resulting coin flow. Baseline seed over 200 waves:

| Flow | Coins | Notes |
|---|---:|---|
| Kill income | `80 216` | regular kills scale `×(1 + 0.025·wave)`, bosses `50 + 20·n` |
| Item sales | `19 587` | ≈20% of all income; auto-sell is equivalent for the economy |
| Stat shop | `53 725` | 132 levels across five stats |
| Skill shop | `28 560` | 39 skill levels |
| Anvil | `16 845` | 27 steps; every equipped Rare/Legendary reaches +4/+5 by the end |

Across the nine gate seeds the split is stable (stats 53–56%, skills 28–30%, Anvil 15–17% of spend). Item sales matter: without them the run would lose ~two stat levels per 10 waves, which is why sell prices stay at `12 / 30 / 75 / 180` and forged items sell for more.

## Critical hits

- Every Hero projectile has a deterministic `5%` critical chance and deals `1.75×` damage on success; Critical Mastery raises both (see Skill shop).
- A confirmed critical impact freezes only combat simulation for `45 ms`; UI and rendering continue.

## Direct stat shop

- Strength, Agility, Luck, Dodge, and Health can each be purchased up to 20 times with earned coins only.
- Base prices are `55`, `60`, `50`, `50`, and `65` coins respectively; purchase level `n` adds `20n` coins. This linear schedule tracks the 20 boss milestones without an unaffordable late-run exponential.
- The shop is entered and operated exclusively through touch targets from the paused run.
- For boss index `b` (1–20), the boss grants `50 + 20b` coins while a stat's contemporary purchase level `b−1` costs `base + 20(b−1)`. The reward alone therefore buys one upgrade in every case and is no more than 1.40× its price.
- Equipment resale is supplemental rather than the primary income source: Common `12`, Uncommon `30`, Rare `75`, and Legendary `180` coins.

## Skill shop (Phase 17)

Five coin-only skills, each with ten levels. Level `n` (0-based) costs `round5(base × 1.32^n)`; bases are Chain Lightning `260`, Multi Shot `300`, Stunning Arrows `220`, Critical Mastery `240`, Eagle Range `180`, so maxing one skill costs roughly 6–10k coins and all five ≈ 38k: a genuine late-run sink rather than an early spike.

- Chain Lightning: `10% + 5%/level` chance per primary hit to arc `55%` of the arrow's damage to the nearest `1 + (level−1)/3` foes within `210 px`. Secondary (Multi Shot) arrows never chain.
- Multi Shot: `+0.30` extra arrows per level (fractional part rolled), capped at 4, each at `70%` damage and spread across other foes in range.
- Stunning Arrows: `2%/level` chance to stun for `0.5 s + 0.05 s/level`; bosses take half duration. Stunned foes neither move, swing, nor cast specials.
- Critical Mastery: crit chance `5% + 0.5%/level` (10% at max), multiplier `1.75 + 0.075/level` (2.5× at max).
- Eagle Range: `+22 px/level` on the 420 px bow.
- Lifesteal (reward card, +2%/pick) also heals from chain arcs, which is why the Phase 17 enemy curve was raised.

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

- Boss `b` (1–40) uses multiplier `1 + 0.05 × (b−1)`, rising smoothly from `1.00` to `1.95` at boss 20 and on to `2.95` at boss 40 (`RewardPowerBudget.MAX_BOSS = 40`).
- Percentage effects multiply their base magnitude by that budget.
- Base-stat cards award the rounded budget in whole stat points: one point early and two points late.
- Every displayed description is generated from the same budget object used to apply the effect.

The card regression runs all eight card identities as the forced choice at every boss with future combat (Bosses 1–39), for 312 complete 200-wave simulations. Each remaining segment must retain at least 5% average gross damage, 25 seconds average clear time, and pressure on at least 90% of waves, while still respecting the 35% damage and 120-second spike ceilings. The calibrated scenarios retained at least `6.8017%` average damage and `33.822624 s` average clear time; their worst single wave was `30.708814%` damage and `100.86547 s`. Boss 40 is omitted because no wave remains after its reward. After the Phase 18.4 extension the 312 scenarios retained at least `6.22%` average damage and `33.58 s` average clear time; their worst single wave was `29.60%` damage and `82.90 s`.

## Renderer-independent simulation gate

`BalanceSimulator` advances the real movement, attacks, projectiles, enemy and boss behavior, progression, drops, potions, equipment, shop, reward-card, and wave-lifecycle systems at 30 Hz. Its balanced automated policy distributes talent points across all five stats, always buys the cheapest affordable stat or skill level in the shop, equips upgrades, sells spare gear, and chooses rewards by a fixed survival/power priority. The fixed baseline seed emits one CSV row per wave with HP, gross incoming damage, DPS-to-enemy-HP ratio, clear time, and timeout state.

The deterministic regression gate requires all of the following:

- Complete exactly 200 waves with the Hero alive (the simulator plants the second tree instantly at the Wave 100 ceremony).
- Average gross incoming damage from enemy attacks, divided by contemporary maximum HP, must be 5%–15% across the run. Gross damage is measured before potion and lifesteal recovery so healing cannot hide pressure.
- No single wave may exceed 35% gross damage or 120 seconds to clear.
- Every metric must be finite and no wave may hit the simulator's timeout.

After the Phase 17 rebalance, baseline seed `0x4845524F444546` and eight further seeds all completed 100/100 waves; across those nine runs the average gross damage was `9.3%`, the worst single wave `28.4%`, and the longest clear `68.2 s`. Every forced-card scenario (all cards at all 19 bosses) also stays under the 35% / 120 s spikes (worst `29.6%`, `71.2 s`). Candidates `1.038–1.040` HP growth were rejected: they pushed single-wave damage past 35% under the forced Dodge/Lifesteal card scenarios. This automated gate is reproducible balance evidence; the remaining multi-seed and manual checkpoints still have to validate resource starvation and subjective play feel.

### Phase 19.3 result (waves 1–200, Anvil-aware simulator)

With the second-half curve `1.023 / 1.008` and the Anvil policy enabled, baseline seed `0x4845524F444546` plus eight (SimProbe) and fourteen (extended) further seeds all completed 200/200 waves; across the nine gate runs the average gross damage was `8.6%`, the worst single wave `29.6%` (seed 2, wave 32 — a first-half spike unchanged from Phase 17), and the longest clear `69.0 s`. The baseline's second half sits at 3–6% gross damage per wave with 22–49 s clears and a DPS-to-HP ratio falling from `0.030` at Wave 100 to `0.010` at Wave 200.

### Phase 18.4 result (waves 1–200, historical)

With the second-half curve `1.021 / 1.006`, baseline seed `0x4845524F444546` and eight further seeds all completed 200/200 waves; across those nine runs the average gross damage was `10.0%`, the worst single wave `28.8%`, and the longest clear `72.3 s`. The baseline's second half sits at 2–6% gross damage per wave with 24–43 s clears and a slowly falling DPS-to-HP ratio (`0.027` at Wave 100 → `0.011` at Wave 200), so the run keeps tightening without a cliff. The acceptance gate (`GATE_WAVE = FINAL_WAVE`) and the forced-card regression (Bosses 1–39) now cover the full run.

### Phase 25.3a result (ascension schedule search, baseline seed `0x4845524F444546`)

The ascension schedule multiplies every growth constant by `(1 + bump × tier)`: health
`0.0005`/tier, damage `0.0002`/tier, with the same relative bump on the second-half constants
(so wave-200 stats compound the bump twice). Tier 0 is bit-identical to the untiered curve.
The roadmap's starting form (`0.015` / `0.008`) died at waves 24–52 on tiers 3–10 with
57–77% average damage — roughly 50–100× too hot for the fixed-power sim hero — so the
search below re-tuned the coefficients while keeping the specified multiplicative form:

| health / damage bump | t3 avg/max/clear | t6 avg/max/clear | t10 avg/max/clear |
|---|---|---|---|
| 0.015 / 0.008 (start) | DIED w52 | DIED w36 | DIED w24 |
| 0.001 / 0.0005 | 6.5 / 27.2 / 61.4 ✓ | 11.6 / 63.3✗ / 103.0 | 20.7✗ / 162.5✗ / 205.5✗ |
| 0.0004 / 0.0002 | 5.0 / 22.0 / 56.5 | 5.2 / 23.0 / 66.5 | 4.3✗ / 25.0 / 72.7 |
| 0.0006 / 0.0003 | 8.3 / 33.6 / 79.7 | 5.3 / 28.2 / 60.0 | 9.6 / 62.1✗ / 136.3✗ |
| **0.0005 / 0.0002 (shipped)** | **6.41 / 25.77 / 70.77** | **5.61 / 27.71 / 79.70** | **5.05 / 32.87 / 105.47** |
| 0.0006 / 0.00015 | 7.85 / 31.07 / 79.67 | 4.70✗ / 24.02 / 60.03 | 7.69 / 46.66✗ / 136.27✗ |
| 0.0003 / 0.0003 | 5.38 / 19.13 / 61.43 | 3.76✗ / 17.09 / 49.70 | 4.54✗ / 32.75 / 98.63 |
| 0.0004 / 0.00025 | 5.10 / 22.61 / 56.47 | 5.46 / 24.27 / 66.47 | 4.66✗ / 27.51 / 72.73 |

(Averages in % gross damage; max = worst single wave %; clear = worst clear seconds.
Bands: avg 5–15%, max ≤ 35%, clear ≤ 120 s.)

The shipped point is the only all-green baseline row, confirmed on three extra seeds
(`+1`, `+2`, `0x123456789`): tier averages stayed in band on 11/12 cells (5.05–7.51%),
but the single-wave max — one wave in 200, mostly boss-adjacent late waves — is seed
noise (±7pp at t10: 32.9–46.2%) and breached 35% on 4/12 cells. Two structural notes:
the elite interval `7 → 6 → 5 → 4` collides with boss waves at interval 5, so tiers 6–8
spawn no elites (every multiple of 5 is a boss wave; test-locked); and elite loot
overcompensates the stat bumps — at t10, forty elite waves' double coins plus talent
materials snowball the hero, so difficulty is non-monotonic (baseline t10 avg 5.05% sits
below t3's 6.41%). Flags for the 26.1 gate: the shipped t10 baseline average has only
0.05pp of floor margin, and the 5% average floor already fails cross-seed at tier 0
itself (seed `0x123456789`: 4.69%), so the 9-seed gate needs tier-relative, averaged, or
ceiling-only-cross-seed bands rather than the naive per-seed 5–15%.

### Phase 25.3b result (middle-third segment, baseline seed `0x4845524F444546`)

Waves 25–80 grow at their own slightly hotter first-half rate (`1.040` health /
`1.004` damage instead of `1.037` / `1.003`); waves 81–100 resume the base rate from
the hotter wave-80 value, so the second half is rebased but not reshaped. The shipped
run averages `10.14%` gross damage with a `25.17%` worst wave (178) and an `80.30 s`
longest clear — still inside the 5–15% / 35% / 120 s gate. Middle-third quarters rise
end to end: `7.75% → 8.95% → 10.77% → 11.21%` (previously flat at ~8% with zero
fitted slope). The rise is locked by a `Q4 > Q1 + 1pp` assertion on the baseline seed.

### Phase 26.1a result (ascension gate, tiers 0/3/6/10)

The tier-0 bands do not transfer to tiers: across 9 seeds the naked tier-10 average
sits at 5.03–9.26% (no room to cool) while its worst single wave reaches 37–55%
(single-wave spikes are seed noise even at tier 0, which breaches 35% on one seed),
so the naive per-cell 5–15% / 35% / 120 s window is empty above tier 0. The committed
`AscensionGateTest` therefore re-derives the bands instead of re-running them: every
cell must finish 200/200; naked and forced-card averages stay strict at 5–15% (all
68 cells green); single-wave ceilings index by tier (naked/card: 40% + 2pp per tier;
trial pairs: 35% + 3pp per tier); clear-time ceiling is 120 s + 3 s per tier; and the
trial-pair pressured floor steps down 175 → 160 → 145 → 120 waves (root bonuses plus
elite loot let empowered builds trivialize wave counts faster than they blunt
spikes). The gate covers a naked 9-seed matrix (36 runs), every forced card at boss
20 (32 runs), and power/damage/horde trial pairs on 3-seed medians (36 runs); the
full 66-pair and 312-scenario matrices stay tier-0.

### Phase 26.1b result (Elite damage accounting)

No main-code change: the Elite-affix-aware accounting path already exists — the
spawner marks Elites inside simulated runs, and Elite melee, blightburst blasts, and
weeping rot all land inside the gross-damage HP-delta window. `EliteDamageAccountingTest`
locks the inclusion: the baseline run spawns all 23 Elite waves, every one lands
pressured damage (1.74–24.14%), and Elite waves contribute +0.62pp to the reported
10.14% run average.

Run `./scripts/balance-check.sh` immediately after every coefficient change and as a mandatory precondition to any manual playtest. The script forces a fresh run rather than accepting Gradle's prior task output. `BalanceSimulatorTest` also remains part of the complete `:core:test` suite executed by the core GitHub Actions workflow on every push and pull request.
