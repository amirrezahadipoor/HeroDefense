# Runtime Equipment Composite Review

**Date:** 2026-09-12  
**Result:** **Approved**

The Hero base was rerendered without a baked weapon or quiver so runtime equipment replaces rather than doubles the visible loadout. The equipment-neutral Hero is 728 triangles; its 1920×768 sheet SHA-256 is `be75f98525883f344217eac043fb43734697f94f693f39e8588bed1b55054c71`.

`equipped-composites-contact-sheet.png` (SHA-256 `2fc73ceec34575e38b982a60c25093f5c891418e17d4e66c154dba981f8662b4`) was opened and reviewed at full size. It composites the base Hero with a curved bow, sword, spear, and axe across representative Idle, Attack, Hit, and Death frames. The layers share the same 192-pixel frame, fixed feet anchor, named armature, and clip index; no secondary baked weapon remains visible.

Runtime implementation details:

- Each catalog item points to its own reviewed `generated/equipment/<id>.atlas`.
- Layers render in a stable Boots → Armor → Rings → Helmet → Weapon order after the base Hero.
- Only currently equipped atlases are loaded. Unequipped atlases are immediately disposed, preventing all 40 large sheets from occupying memory.
- A new run receives exactly one Ashwood Bow overlay; the persisted one-time flag prevents duplication after unequipping, selling, or reloading.
- Automated validation confirmed that all 40 atlas files expose the four matching clip names and expected frame counts.
