# Progression Hours (Phase 26.3)

Derives the 50-hour completion target from shipped numbers, so it is an equation
the team re-checks after every balance pass — not a one-time estimate. Every
input below is locked by `ProgressionHoursTest` (and the session gate in
`AscensionGateTest`); if any input drifts, the suite fails and this file must be
re-derived.

## Inputs (all test-locked)

- **Session:** one Wave 1–200 clear at engaged/shopping/no-idle pace takes
  **7970 s (2.214 h)** — the tier-0 3-seed median, gated within ±20% at every
  tier (26.2b). Humans slower than the sim pace push every total below upward.
- **Root Network:** 23 nodes costing **1350 Heartwood** total (15 → 200 each).
- **Heartwood income** per ascension (`peakWave/5 + (peakWave>=200 ? 50 : 0) +
  tier×10 + (flawless ? 20 : 0)`, × trial multiplier): a flawless full clear at
  tier `t` pays `110 + 10t`; non-flawless pays `90 + 10t`. Deaths pay less (no
  +50 completion bonus). `BOSS_BOUNTY` multiplies income ×1.3.
- **Codex:** 30 entries — 8 wave milestones (waves 1–100), 4 boss first-kills,
  3 Elite-affix kills, 5 ascension gates (1, 2, 3, 5, **10**), 10 secrets.
- **Mythics:** 6 total, one per equipment slot, picked uniformly at random with
  no dupe protection. Per-kill Mythic rate is `0.0015 × 0.01 × luckMult =
  1.5e-5 × luckMult`, where `luckMult = 1.02^LUCK × (1 + ITEM_FIND)`.
- **Kills per full clear:** 4400 regulars (4 + wave/2, capped at 24 from wave
  40) + 40 bosses = **4440 bodies**.

## Derivation

| Track | Runs | Hours | Notes |
|---|---|---|---|
| Root exhaustion (flawless) | 9 (tiers 0–8: 110+…+190 = 1350 exactly) | 19.9 | |
| Root exhaustion (non-flawless) | 10 (tiers 0–9: 90+…+180 = 1350 exactly) | 22.1 | |
| Root exhaustion (BOSS_BOUNTY) | ~7 | ~15.5 | ×1.3 income |
| Codex milestones/bosses/elites (1–15) | 1–2 | 2.2–4.4 | all in early full clears |
| Codex ascension gates (16–20) | 10 | 22.1 | slowest gate: 10 ascensions |
| Codex incidental secrets (22, 23, 24, 27, 28, 29, 30) | ≤5 | ≤11.1 | full set, mastery, reforged, pause, elites, fast wave, wave-200 twice |
| Codex challenge secrets (21 bare-handed, 26 no-potions) | ~1.5 run-equiv | ~1.5 | dedicated challenge runs |
| Mythic collection, baseline luck (~0.117/run) | ~21 | ~46 | collector ×2.45 over 6 uniform slots |
| Mythic collection, luck-stacked (~0.20/run) | ~12 | ~27 | 1.02^60 × find ≈ 3+ multiplier |

Mythic pace math: 4440 kills × 1.5e-5 × ~1.75 average luckMult ≈ 0.117
Mythics/run, so one Mythic every ~8.5 runs; collecting all 6 distinct slots
without dupe protection costs the coupon-collector factor H6 ≈ 2.45, hence
~21 runs. Luck-stacked builds (≈3+ multiplier) halve that to ~12 runs.

## The 50-hour equation

Mythic completion is the long pole and subsumes the rest: ~21 baseline-luck
runs (46 h) already include Root exhaustion (9–10 runs), the 10-ascension Codex
gate, and every incidental secret; adding the two challenge secrets (~1.5 h)
lands full completion at **≈48 h ≈ the 50-hour target**. Luck-stacked players
commute it to ~35 h; dying, slower-than-sim pace, and suboptimal builds push
casuals past 50 h. The target is therefore the baseline-luck full-completion
mark, with skill expression on both sides.

## Re-check protocol

After any balance pass: run `:core:test`. `ProgressionHoursTest` fails if Root
costs, Heartwood payouts, Codex structure, Mythic count/rate, or per-run kills
moved; `sessionTimesStayWithinTwentyPercentOfTierZero` fails if the 7970 s
session moved. Update this file's arithmetic from the new locked inputs before
claiming the 50-hour target still holds.
