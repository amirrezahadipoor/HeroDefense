# Category Pipes Review (Phase 28.4)

**Date:** 2026-09-15

**Pipeline:** Blender 4.2.23 LTS, headless EEVEE, mid tier (2× / 24) for VFX and
projectiles, Workbench overlays (2× / 8) for the equipment subset

**Scope:** new `vfx`, `projectile`, and `equipment_overlay` pipeline categories
with shared atlas-layout plumbing plus review/promote scripts for each. Proof
assets are one arrow, two one-shot effects (impact flash, shockwave ring), and
the 14 boots/weapon overlays.

**Result:** **Approved as pipeline proof — categories render, pack, audit, and
review end to end. Production art direction stays with Phase 29.4 (overlay
look) and Phase 30 (arrows + skill feel); the promote scripts execute once
those phases land their ACCEPTED reviews.**

## Proof runs

| Batch | Run | Commit | Assets | Tiers | Audit |
|---|---|---|---|---|---|
| vfx | [34953384017](https://github.com/amirrezahadipoor/Herodefense/actions/runs/34953384017) | `84e6ab0` | 2 | 2× / 24 | 2 × 8 unique play frames, margins ≥ 4 px |
| projectile | [34953390436](https://github.com/amirrezahadipoor/Herodefense/actions/runs/34953390436) | `84e6ab0` | 1 | 2× / 24 | sharp +X tip, margins ≥ 4 px |
| equipment-overlay | [34951833870](https://github.com/amirrezahadipoor/Herodefense/actions/runs/34951833870) | `3e0d9d6` | 14 | 2× / 8 | 392 socketed frames, 0 detached, 0 boundary |

## Review evidence

All three review scripts ran against the real CI candidates above and passed;
every sheet carries silhouette-only views and a stage-grade strip.

| Review sheet | What was inspected | SHA-256 |
|---|---|---|
| [`category_pipes_28_4/vfx_play_strips.png`](category_pipes_28_4/vfx_play_strips.png) | Both 8-frame parametric play strips | `cc9a147d93579ce58b68e89ec3c93301e56d97d27b5dbdad05896b390d833f61` |
| [`category_pipes_28_4/vfx_shape_grade.png`](category_pipes_28_4/vfx_shape_grade.png) | Frame/silhouette rows plus stage grade | `3fee0c2280d5cd15c71033eb53bf137d1bf32ecc92a6ac5190cca4bd5e376ca1` |
| [`category_pipes_28_4/projectile_readability.png`](category_pipes_28_4/projectile_readability.png) | 1×/2×/4× zoom, silhouette, grade | `4cf6d31c785f876ccbf6441cdd9847c20a96924d6f917d6b5a6244b1d6c0ff73` |
| [`category_pipes_28_4/equipment_icons_common.png`](category_pipes_28_4/equipment_icons_common.png) | Common overlay icons, before/premium/shape | `09707fcdc74380212d913a5e8c3d8a7e754bccf25d86e1e0141c62852a54e89d` |
| [`category_pipes_28_4/equipment_icons_uncommon.png`](category_pipes_28_4/equipment_icons_uncommon.png) | Uncommon overlay icons | `97b0e7335edd98f757c5ab81e29f7f5363a341ed9af1886b545054499bb76130` |
| [`category_pipes_28_4/equipment_icons_rare.png`](category_pipes_28_4/equipment_icons_rare.png) | Rare overlay icons | `13dba4c39150070ae396a091254f15c6ed12f58c0f01416b72d6673dcdf45ac8` |
| [`category_pipes_28_4/equipment_icons_legendary.png`](category_pipes_28_4/equipment_icons_legendary.png) | Legendary overlay icons | `dd2245e4b05a739778d1856718ce7b57b7a6521d59fbf2bbbf4efc072d4c7375` |
| [`category_pipes_28_4/equipment_composites_1.png`](category_pipes_28_4/equipment_composites_1.png) | Socketed poses page 1/2 + grade | `1b9e4bbb19f3421e2457b20a0ca347b3dcd2721d79d14f0cd8b17cf1af901dd` |
| [`category_pipes_28_4/equipment_composites_2.png`](category_pipes_28_4/equipment_composites_2.png) | Socketed poses page 2/2 + grade | `8b8fd39c8be17700ad0c5d34ef5997a25bb09f21649c7ada3ab718cd9d0cecba` |

## Visual decisions

- **VFX param path: accepted.** The impact flash collapses while gold motes fly
  outward; the shockwave ring expands and thins with lifting dust. All 16 play
  frames are pairwise unique, proving per-frame geometry rebuilds.
- **Projectile: accepted as a pipe.** Shaft, faceted head, leaf fletching, and
  gold wrap read at 1× and stay crisp at 4×; the tip is a real point on +X.
  Crit/secondary variants and engine rotation belong to Phase 30.1.
- **Overlay subset: accepted as a pipe.** All 14 boots/weapon overlays stay
  socketed to the hero across 392 frames with the audit totals the promote
  gate expects (14 assets / 392 frames / 83,091,456 decoded bytes). Four icon
  cards honestly show "No baseline" — those weapons borrow art until 29.2.
- **Gates: accepted.** Batch floors (2/24/8 + tier fingerprint), per-asset
  tiers, atlas-page contracts, and triangle budgets are enforced by the new
  scripts and locked by `test_category_gates_28_4.py`; the equipment review
  now also survives missing baselines instead of crashing.
