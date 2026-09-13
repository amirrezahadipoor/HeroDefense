# VFX Premium-v2 Review

**Decision:** ACCEPTED

Scope: Phase 16.5 item "Upgrade projectiles, impacts, critical hits, enemy deaths, Boss
entrances/deaths, item collection, coins, World Tree damage, and ambient arena VFX with
restrained visual layering."

## Evidence

- Workflow run / job: `34767961263` / `103752232553` (branch `phase16-vfx`)
- Artifact `android-test-reports` id `10321425053`, sha256
  `53eed4ecdc10611317ca24ac86f3c1b3878f78b272d6b76c16c4b824225d843f`
- Audit: `vfx_premium_v2/surface_audit.json`, sha256 `df66db673cc8b70989ede52d9824fa24459312427ff1065c1c7df43b9018227c`
- Captures (touch-only emulator journeys in `AndroidTouchSmokeTest`):
  `boss_entrance_emulator.png`, `impact_death_emulator.png`, `projectile_trail_emulator.png`,
  `tree_collapse_emulator.png`, plus `vfx_contact_sheet.png`.

## What changed

| Effect | Treatment | Budget (style guide 0.6) |
| --- | --- | --- |
| Projectile | 22×6 bolt + 3-step fading trail + bright tip; crit bolt cyan, 26×7 | trail alpha ≤ 0.55 |
| Impact | 1 warm core (flash-then-collapse) + ≤6 gold motes | ≤ 0.25 s |
| Critical | + expanding cyan ring, motes ×1.5 | 1.5× multiplier |
| Enemy death | ground ring + 10 dust motes (sprite death clip unchanged) | — |
| Boss entrance | 2 amber shockwaves + 12 dust motes + 0.24 s / 8 px shake | 2× multiplier |
| Boss death | death treatment + 210 px shockwave + dust (+ existing kill shake) | 2× multiplier |
| Item collection | 6 sparkles at the Inventory control when a homing drop lands | — |
| World Tree fall | 18 slow leaves + 0.60 s / 9 px low shake | — |
| Ambient | 14 time-driven spores, alpha ≤ 0.22, drawn under actors | stateless |

## Checks

- Pool still capped at 256, deterministic golden-angle bursts, pure-Java testable
  (`PremiumVfxRestraintTest`, `ProjectileTrailTest`).
- No audio, balance, or save-format changes. `Boss.entrancePresented` is a presentation flag
  decoded leniently from older saves (defaults to false).
- On-device: trail and tip legible at 1080×2220; ambient spores remain below actor contrast;
  leaves stay muted against the World Tree; shockwave rings do not obscure health bars.
- Critical ring, boss death, and collection sparkle are bound by unit tests only (5% crit
  chance and boss kill timing are not deterministic within a smoke journey).
