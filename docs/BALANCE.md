# Hero Defense Balance Specification

This document records the shipped coefficients used by the renderer-independent gameplay systems. Values are clamped to Waves 1–100 unless stated otherwise. Simulation evidence and later tuning revisions belong in `docs/balance/`.

## Regular enemy health

The baseline health curve is:

`EnemyHP(w) = 20 × 1.045^w`

Regular enemy archetypes multiply that baseline by their authored base-health ratio:

| Archetype | Multiplier |
|---|---:|
| Rootling | 1.00× |
| Stonekin | 1.70× |
| Gloom Wolf | 0.85× |
| Fungal Brute | 2.30× |

Baseline checkpoints are 20.90 HP at Wave 1, 60.11 at Wave 25, 180.65 at Wave 50, 542.94 at Wave 75, and 1,631.77 at Wave 100. `DifficultyCurve` is the executable source of truth and `DifficultyCurveTest` locks the formula and its monotonic growth.
