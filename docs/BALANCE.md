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

This is the first playable curve, not a claim of final balance. Phase 14 simulation and manual checkpoints must validate and, if necessary, revise the coefficients.
