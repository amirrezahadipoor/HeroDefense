# Regular Enemy Roster Review

**Date:** 2026-09-12  
**Result:** **Approved**

Four gameplay-distinct melee archetypes now have matching procedural low-poly renders:

| Type | Combat role | Triangles | Sprite-sheet SHA-256 |
|---|---|---:|---|
| Rootling | balanced baseline | 564 | `5e50e9e842894e14df054bb7d520b868ad6d94a45f5efbb12c991373e15ec826` |
| Stonekin | slow armored tank | 596 | `3db4f39ba5cf970e460da22c28d94157ab55bccd37535c1e209e8696566aaff0` |
| Gloom Wolf | fast fragile attacker | 588 | `954ac5f2f6cae9344d5dfd40ffacb4ddc877de75b72f87d7c8754d9170b12cb7` |
| Fungal Brute | slow heavy hitter | 584 | `93665dce479f85ae243faff4378cf0015e1a1d7f7384d4ae03d3918ec5c38216` |

`regular-enemies-contact-sheet.png` was opened and reviewed at full size; SHA-256 is `f53e2daa37e3a7b417022a645671ea6a8087654e0d1d22e7c0b02113c7648fd7`. The branch imp, block golem, low quadruped, and mushroom brute remain identifiable by silhouette without relying on recoloring or scale alone.

Every sheet is 1920×768, uses a real 25-bone armature, and contains 6 Idle, 8 Attack, 4 Hit, and 10 Death frames. Automated inspection confirmed that all 112 frames contain nontransparent pixels, remain inside the frame edge, and stay below the 2,400-triangle regular-enemy limit. Their gameplay catalog exposes only short melee ranges (38–50 world units); there are no ranged regular enemies.
