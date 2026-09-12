# Premium-v2 Equipment Category Review

**Date:** 2026-09-13

**Scope:** all 40 runtime equipment items and their Hero-socket animation overlays

**Pipeline:** Blender 4.2.23 LTS, headless EEVEE, 2× working renders, 8 overlay samples, alpha-safe linear downsampling

**Accepted source:** branch commit `29c8873e0d79bd9ba1a3c6b84054c678e98a9e64`, GitHub Actions run `34725936604`, artifact `10307393892`

**Result:** **Approved for promotion as the complete premium-v2 equipment category**

## Review process and iteration

The candidate was rendered outside the repository and was not promoted until the complete batch passed automated and visual review. The first full-frame audit rejected three top-edge contacts: Verdant Glaive Attack frame 5, Antler Circlet Hit frame 1, and Golem Splitter Attack frame 4. Verdant Glaive's upper blade was shortened without losing its leaf profile, Antler Circlet's upper branches were compacted while its band stayed seated, and Golem Splitter's head was lowered and modestly compacted while retaining its broad double-bit silhouette. A second render cleared the glaive and circlet but correctly rejected the splitter again. The final render gives those three assets minimum top margins of 6 px, 6 px, and 5 px respectively.

All four final icon sheets and all five final equipped-motion sheets were opened and inspected at full size. The automated audit separately examines every pixel-bearing overlay frame, so visual sampling and exhaustive contract checks complement rather than substitute for one another.

## Review evidence

| Evidence | What was inspected | SHA-256 |
|---|---|---|
| [`equipment_premium_v2/equipment_icons_common.png`](equipment_premium_v2/equipment_icons_common.png) | Before/after comparison for all 14 Common icons | `946c59b318a1c31a9cd882f136caa8440def82a6544ccc0feb7345e625e82fdb` |
| [`equipment_premium_v2/equipment_icons_uncommon.png`](equipment_premium_v2/equipment_icons_uncommon.png) | Before/after comparison for all 12 Uncommon icons | `0439bd8fe688a99186ef972a796e19825aed7f78de280d531705191890fa9ae0` |
| [`equipment_premium_v2/equipment_icons_rare.png`](equipment_premium_v2/equipment_icons_rare.png) | Before/after comparison for all 9 Rare icons | `037856ea7b10d6ee960dce5f4c8374c1f8c9af974ebceaec83504cda8c199dc1` |
| [`equipment_premium_v2/equipment_icons_legendary.png`](equipment_premium_v2/equipment_icons_legendary.png) | Reconfirmation of all 5 previously piloted Legendary icons | `01a19fdf7eb5961cbb041ea39966e115a164df47b9152cb54b258c59262d0bb0` |
| [`equipment_premium_v2/equipment_composites_1.png`](equipment_premium_v2/equipment_composites_1.png) | Items 1–8 on the finalized Hero in Idle, Anticipation, Impact, Hit, and Death | `53fd7a17684d88f5b6892c4d469770359c08f8d2f69ffd0cdceec9f1297e3cc7` |
| [`equipment_premium_v2/equipment_composites_2.png`](equipment_premium_v2/equipment_composites_2.png) | Items 9–16 across the same representative motion poses | `c434b8383a96e6aaa536a874443cf854cead77d0b795c2b05b288733e3775349` |
| [`equipment_premium_v2/equipment_composites_3.png`](equipment_premium_v2/equipment_composites_3.png) | Items 17–24 across the same representative motion poses | `e1d036aed2e04a449e5dfd10671f6952899bb27d812fbfc0bdd1e2e85b2f4651` |
| [`equipment_premium_v2/equipment_composites_4.png`](equipment_premium_v2/equipment_composites_4.png) | Items 25–32 across the same representative motion poses | `7241a113cad555622fad62bfce4b607ac9393c015963bca92711327906f84fb6` |
| [`equipment_premium_v2/equipment_composites_5.png`](equipment_premium_v2/equipment_composites_5.png) | Items 33–40 across the same representative motion poses | `cdc143996cb5fb05fc760673f26426e1efa44e8a312049b04e085bc082851f22` |
| [`equipment_premium_v2/equipment_alignment_audit.json`](equipment_premium_v2/equipment_alignment_audit.json) | Exact candidate, finalized-Hero, per-asset hash, margin, motion, attachment, and budget evidence | `024cbdb58c10294b11f7c235a5a8eaa0c086ba254bcfc588e857631bbbda251e` |

The audit also stores and verifies the SHA-256 of every accepted atlas page, icon, libGDX atlas descriptor, source metadata file, contact sheet, catalog, candidate manifest, and the exact finalized Hero sheet used for compositing.

## Visual decisions

- **Common:** accepted. Ashwood Bow, Militia Sabre, Thorn Spear, Leather Cap, Scout Hood, Padded Vest, Bark Tunic, both boot pairs, and five rings now have distinct functional silhouettes, material breaks, fasteners, leaves, stones, or knots instead of placeholder wedges, slabs, cubes, and undifferentiated bands.
- **Uncommon:** accepted. Moonwood Longbow and Verdant Glaive introduce more deliberate curved profiles; Fern Guard and Antler Circlet establish distinct head shapes; Ranger Mail and Mossweave Coat separate protective and cloth reads; boots and rings add restrained organic detail without obscuring the Hero.
- **Rare:** accepted. Starfall Bow, Golem Splitter, Owlguard Helm, Crystalbark Plate, Shadeleaf Mantle, Stormrunner Boots, and the three rings use focused silver, cyan, blue, crimson, or violet accents. Their rarity is visible through authored material hierarchy, while runtime glow remains the restrained final rarity layer rather than being baked into these sprites.
- **Legendary:** accepted and reconfirmed. Worldbranch, Crown of First Leaves, Heartwood Aegis, Boots of Three Winds, and Eternal Seed retain the already approved Verdant Covenant leaf, heartwood, cyan-energy, and restrained-gold language from the premium-v2 pilot.
- **Slot readability:** accepted. Weapons remain hand-led through every action, helmets track the head, armor follows the chest, boots remain planted to their bilateral foot sockets, and Ring 1/Ring 2 stay on opposite hand sockets.
- **Compositing:** accepted. No reviewed overlay reveals a baked base weapon, breaks the Hero's equipment-neutral contract, or produces an unintended floating part. Large armor silhouettes remain centered on the torso; compact rings remain visible without becoming VFX noise.
- **Mobile icon readability:** accepted. Every 96×96 icon keeps at least 8 transparent pixels on each outer side. Each named item remains distinguishable by silhouette and focal material at runtime size.
- **Restraint:** accepted. Complexity is concentrated at item-defining landmarks. Common items do not borrow Legendary glow, and Rare/Legendary emissive accents remain local rather than filling the silhouette.

## Exhaustive socket and motion audit

The final audit composites each equipment frame against the exact committed Hero frame identified by model revision `hero-premium-v2-final`, rig profile `premium-humanoid-v2`, and Hero sheet SHA-256 `d6830b89d63ebe744dadc5508445d6b931ea0476db1a6b2188f743beaf1680e9`.

| Contract | Accepted result |
|---|---:|
| Equipment assets | 40 |
| Visual-slot split | 8 weapons / 6 helmets / 7 armor / 6 boots / 7 Ring 1 / 6 Ring 2 |
| Tier split | 14 Common / 12 Uncommon / 9 Rare / 5 Legendary |
| Frames inspected | 1,120 (40 × 28) |
| Clip contract per asset | Idle 6 / Attack 8 / Hit 4 / Death 10 |
| Boundary-touching frames | 0 |
| Detached or non-near-socket frames | 0 |
| Minimum frame margins, L/T/R/B | 17 / 3 / 8 / 13 px |
| Minimum icon margins, L/T/R/B | 8 / 8 / 8 / 8 px |
| Minimum weighted near-Hero overlap | 193.06 px |
| Maximum offline triangle count | 708 (Ranger Mail) |
| Triangle hard cap | 900 |

Idle contains five visibly distinct raster poses plus the loop-closing endpoint for every non-boot item. Boots correctly keep one planted Idle raster because the finalized Hero's Idle motion is isolated to chest/head motion. Every item has seven distinct Attack rasters plus return, three distinct Hit rasters plus return, and nine distinct Death rasters with a terminal hold. This proves visible socket inheritance across all four clips without mistaking deliberate loop endpoints for missing animation.

## Accepted asset matrix

Margins are the minimum left/top/right/bottom transparent pixels over all 28 frames. Full 256-bit payload hashes are retained in the audit; short sheet prefixes below make the matrix readable.

| Item | Slot | Tier | Triangles | Min margins L/T/R/B | Sheet SHA prefix |
|---|---|---:|---:|---:|---|
| Ashwood Bow | Weapon | Common | 176 | 48/19/14/30 | `f7d6049f1489` |
| Militia Sabre | Weapon | Common | 112 | 44/21/20/27 | `8060ce2e9d77` |
| Thorn Spear | Weapon | Common | 160 | 41/7/12/24 | `e1a41284d3b7` |
| Leather Cap | Helmet | Common | 120 | 35/25/41/89 | `c677c66b3c11` |
| Scout Hood | Helmet | Common | 240 | 38/18/41/88 | `43dba04771f9` |
| Padded Vest | Armor | Common | 168 | 48/62/53/62 | `6a89ec34d10d` |
| Bark Tunic | Armor | Common | 176 | 40/43/46/56 | `d0cf14d08ed8` |
| Trail Boots | Boots | Common | 368 | 61/118/47/15 | `5712a681f6e9` |
| Hide Greaves | Boots | Common | 248 | 65/117/51/16 | `6ca88daf790a` |
| Copper Leaf Ring | Ring 1 | Common | 116 | 18/36/92/79 | `eb2a79dfa80c` |
| River Pebble Ring | Ring 2 | Common | 220 | 64/34/29/51 | `9b8c150e2fa2` |
| Acorn Band | Ring 1 | Common | 272 | 18/36/92/79 | `8be578400494` |
| Hunter Loop | Ring 2 | Common | 126 | 64/34/29/50 | `3f9acf4a0e66` |
| Twine Circle | Ring 1 | Common | 212 | 18/36/92/79 | `6e4d69f2b8f9` |
| Moonwood Longbow | Weapon | Uncommon | 324 | 37/13/10/21 | `54336c3ed326` |
| Verdant Glaive | Weapon | Uncommon | 260 | 41/6/12/24 | `7aab09976144` |
| Fern Guard | Helmet | Uncommon | 288 | 36/8/37/93 | `7360778b8ee3` |
| Antler Circlet | Helmet | Uncommon | 224 | 29/6/21/101 | `4aa3333f25d1` |
| Ranger Mail | Armor | Uncommon | 708 | 42/58/49/63 | `7ee590cf88c5` |
| Mossweave Coat | Armor | Uncommon | 180 | 43/48/49/49 | `1be52f4094ad` |
| Windstep Boots | Boots | Uncommon | 360 | 63/110/51/18 | `480b21f6e1bf` |
| Rootguard Sabatons | Boots | Uncommon | 208 | 60/121/46/13 | `d7aed5fd96cc` |
| Jade Sap Ring | Ring 1 | Uncommon | 136 | 18/35/92/78 | `0729a5c5fd27` |
| Hawk Eye Band | Ring 2 | Uncommon | 232 | 63/34/28/50 | `39d1bdd245a0` |
| Silver Briar Ring | Ring 1 | Uncommon | 120 | 18/35/92/78 | `93fca445d0e9` |
| Dewstone Loop | Ring 2 | Uncommon | 136 | 63/34/28/51 | `89a7c848bca4` |
| Starfall Bow | Weapon | Rare | 344 | 41/12/8/24 | `abb60fbe58ba` |
| Golem Splitter | Weapon | Rare | 152 | 32/5/14/17 | `c35f1276630d` |
| Owlguard Helm | Helmet | Rare | 360 | 37/16/32/86 | `891fd44d32b3` |
| Crystalbark Plate | Armor | Rare | 204 | 35/46/42/50 | `9cf3275ee213` |
| Shadeleaf Mantle | Armor | Rare | 228 | 40/44/43/56 | `de33506e5e2e` |
| Stormrunner Boots | Boots | Rare | 400 | 62/110/50/17 | `2a9e3247ff97` |
| Sapphire Luck Ring | Ring 1 | Rare | 216 | 17/35/92/78 | `1ec637471a5e` |
| Bloodroot Signet | Ring 2 | Rare | 208 | 63/34/28/50 | `08f704af326d` |
| Echo Band | Ring 1 | Rare | 288 | 17/35/92/78 | `def5348941f0` |
| Worldbranch | Weapon | Legendary | 368 | 41/14/9/23 | `f18875c0b482` |
| Crown of First Leaves | Helmet | Legendary | 276 | 34/3/32/88 | `c1026dd6b93f` |
| Heartwood Aegis | Armor | Legendary | 308 | 40/47/44/60 | `0908affba9ec` |
| Boots of Three Winds | Boots | Legendary | 400 | 62/109/50/17 | `9b82e90ced14` |
| Eternal Seed | Ring 2 | Legendary | 216 | 64/33/28/51 | `ee37f0512c25` |

## Render and runtime contract

- Every overlay uses the Hero's exact 192×192 frame size, normalized `(0.5, 0.12)` pivot, 25-bone list, clip names, frame indices, atlas coordinates, and frame counts.
- Every item fits one 1920×768 RGBA atlas page below the 2048×2048 cap and has one separate 96×96 RGBA icon.
- The category contains 40 atlas pages plus 40 icons, totaling **237,404,160 decoded bytes (226.41 MiB)** in the complete catalog view. The accepted PNG payload is 4,444,093 compressed bytes.
- Android receives only bounded 2D textures. The 708-triangle maximum describes disposable Blender source complexity and adds no runtime geometry.
- Rare and Legendary items retain `runtimeGlow=true`; Common and Uncommon items retain `runtimeGlow=false`.
- The exact-key promotion gate requires the accepted document, all nine hash-matched sheets, the all-frame audit, candidate/catalog/Hero provenance, exact 160-file payload, and per-asset sheet/icon/atlas/metadata hashes before it will copy any file.

## Final decision

The 40-item category meets the premium-v2 bar for named silhouette identity, tier-aware material separation, restrained detail, mobile icon readability, complete Hero-socket inheritance, alpha safety, atlas bounds, and runtime provenance. The final candidate is accepted for exact promotion with model revision `equipment-premium-v2` and rig profile `hero-socket-v2`.
