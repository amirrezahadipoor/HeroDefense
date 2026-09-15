# Art Shortage — Phase 29.0 (frozen)

Every borrowed / placeholder / missing art enumerated via `tools/visual/audit_art_shortage.py`.
This list is frozen until Phase 29.3 closes it. Do not hand-edit — re-run the script.

## Summary

- **Total borrowed**: 10  (4 bows + 6 mythics)
- **Other families**: none — all other equipment, enemies, bosses, trees, arena, ui are on real art (see manifest 28.7 audit)

## Borrowed equipment art (must be replaced with own art per style guide)

| # | Item ID | Slot | Tier | Borrows atlas/icon of | Kind |
|---|---|---|---|---|---|
| 1 | `yew_shortbow` | WEAPON | COMMON | `ashwood_bow` | bow |
| 2 | `thornwood_bow` | WEAPON | COMMON | `ashwood_bow` | bow |
| 3 | `verdant_recurve` | WEAPON | UNCOMMON | `moonwood_longbow` | bow |
| 4 | `golemsbane_warbow` | WEAPON | RARE | `starfall_bow` | bow |
| 5 | `sunfall_last_arrow` | WEAPON | MYTHIC | `worldbranch` | mythic |
| 6 | `crown_hollow_eye` | HELMET | MYTHIC | `crown_of_first_leaves` | mythic |
| 7 | `bark_first_root` | ARMOR | MYTHIC | `heartwood_aegis` | mythic |
| 8 | `windrunner_last_steps` | BOOTS | MYTHIC | `boots_of_three_winds` | mythic |
| 9 | `verdant_oath` | RING_1 | MYTHIC | `echo_band` | mythic |
| 10 | `emberless_core` | RING_2 | MYTHIC | `eternal_seed` | mythic |

## Verification

```bash
python3 tools/visual/audit_art_shortage.py
python3 -m unittest discover -s tools/visual/tests -k ArtShortage -v
```

## Close-out criteria

- [ ] 29.1: 6 mythics have own mesh/material + atlas + icon, `EquipmentCatalog` no longer borrows, contract test green
- [ ] 29.2: 4 bows have own art, borrows unwired, contract test green
- [ ] 29.3: remaining list empty, `docs/ART_SHORTAGE.md` shows 0 borrowed, all contract tests green
- [ ] 29.4: hero LAYER_ORDER reduced to boots+weapon only

