# Equipment and Potion Render Review

**Date:** 2026-09-12  
**Result:** **Approved**

## Equipment batch

- 40 separate rig-aligned equipment overlay sheets were rendered from procedural Blender meshes.
- Distribution: 14 Common, 12 Uncommon, 9 Rare, 5 Legendary.
- Slot distribution: 8 Weapons, 6 Helmets, 7 Armor, 6 Boots, 7 Ring 1, 6 Ring 2.
- Every item has a 1920×768 overlay sheet containing Idle (6), Attack (8), Hit (4), and Death (10) regions, a libGDX atlas, JSON metadata, and a fitted 96×96 inventory icon.
- Every overlay is evaluated on the same 25-bone Hero Armature and stable slot bone used by the base Hero.
- Weapon semantics were visually checked: bow, sword, spear, glaive, and axe catalog entries use corresponding silhouettes.
- Attachment meshes range from 20 to 96 triangles, below the 500-triangle hard limit.
- Rare and Legendary manifests declare `runtimeGlow: true`; no glow is baked into any PNG.
- Boots intentionally remain still during upper-body-only Attack/Hit poses; they move during the root-driven Death clip. Other socket layers visibly follow their relevant animated ancestors.

The committed review contact sheet is `equipment-contact-sheet.png` (SHA-256 `95d25860272a4fd894524a5a8ff3196cf0768e42c646f63dc385f1616a28f8b6`). It was opened and inspected at full size. The full animation sheets for a weapon, helmet, armor, and ring were also opened to verify stable transparent alignment.

Equipment-only transparent layers use the documented low-memory Blender Workbench studio pass. Geometry, material palette, fixed camera, real armature evaluation, frame grid, and deterministic outline remain shared with the EEVEE character pipeline.

## Potion batch

Six 96×96 EEVEE-rendered potion icons were opened and reviewed in `potion-contact-sheet.png` (SHA-256 `46af6ee579e692040e728c19df65a4b18e61c41871415871e783327c45700bee`).

Tier colors progress through green, cyan, blue, violet, magenta, and amber. All six have a readable low-poly bottle silhouette, cork, rim, glass highlight, and transparent margin of at least four pixels. Automated average-color checks confirm that no two tier renders are duplicates.

No `.blend` file is committed; all 46 assets are reproducible from `tools/blender/`.
