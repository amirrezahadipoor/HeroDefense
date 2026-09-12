# Premium-v2 Visual Pilot Review

**Date:** 2026-09-13

**Pipeline:** Blender 4.2.23 LTS, headless EEVEE, 2× working renders, alpha-safe linear downsampling

**Scope:** Hero, Rootling regular enemy, Ancient Golem Boss, the five-piece Verdant Covenant equipment set, tier-6 health-potion drop, crystal arena prop, and live Inventory control

**Result:** **Approved as the premium-v2 scaling bar**

## Review evidence

The following deterministic contact sheets were opened and reviewed at full size. Checkerboards expose the real alpha edge rather than adding a presentation background.

| Review sheet | What was inspected | SHA-256 |
|---|---|---|
| [`premium_v2_pilot/pilot_characters.png`](premium_v2_pilot/pilot_characters.png) | Before/after silhouettes plus Idle, Attack anticipation/impact, Hit, and Death poses | `884176682c4073de9d23d1b44c35cdf5c08748ed5e79ca9722cbd722372e4e80` |
| [`premium_v2_pilot/pilot_equipment_set.png`](premium_v2_pilot/pilot_equipment_set.png) | Five-layer runtime composition across representative poses and every individual item icon | `b44716dc513f292d82e6861c681a1c1b8590dce6d8ece33b93148fbc43d1e216` |
| [`premium_v2_pilot/pilot_drop_prop_ui.png`](premium_v2_pilot/pilot_drop_prop_ui.png) | Tier-6 drop, environment prop, Inventory control, and transparent margins | `d175d0c829c7be60019448bacd3acafb3b52ffac09562221add585d6fb3143df` |

The first potion revision was rejected because its silhouette differed too little from the baseline. The accepted revision adds a restrained legendary cradle, leaf collar, seed cap, readable heart mark, and brighter inner facet. The Ancient Golem was likewise revised before the final batch with angular eye slits, a dark jaw, crown shards, and stronger shoulder asymmetry.

## Visual decisions

- **Hero:** accepted. Layered hair planes, facial features, pointed ears, cloak/tunic separation, bracers, belt, boots, and the luminous Covenant clasp create a materially richer but still equipment-neutral base.
- **Rootling:** accepted. Bark facets, cut-ring chest mark, leafy branch tips, layered shoulders, and asymmetric growth improve identity without harming the melee silhouette.
- **Ancient Golem:** accepted. The crown, recessed face, jaw, massive offset shoulders, moss, and heart crystal read as an ancient armored Boss rather than a scaled regular enemy.
- **Verdant Covenant set:** accepted. Worldbranch, Crown of First Leaves, Heartwood Aegis, Boots of Three Winds, and Eternal Seed share leaf, heartwood, cyan-energy, and restrained gold motifs. All five remain legible individually at 96 px and as one coherent equipped set.
- **Animation:** accepted. Idle asymmetry is subtle; Attack has readable anticipation and impact; Hit recoils away from contact; Death reaches a clear held silhouette. No reviewed action pose is clipped.
- **Equipment compositing:** accepted. All five atlases use the Hero's exact clip names, frame counts, indices, and 192×192 frame contract. Representative Idle, Attack, and Death composites keep the attachments on their intended sockets without revealing a baked base weapon.
- **Potion drop:** accepted after iteration. The gold liquid remains the dominant mass while the legendary framing and heart label survive the actual 96 px output.
- **Crystal prop:** accepted. A stone ring, moss, varied shards, and bright inset facets add depth while preserving a compact arena footprint.
- **Inventory control:** accepted. The satchel silhouette, handle, flap, strap, buckle, side bindings, and studs are immediately readable without decorative noise.
- **Alpha/outline:** accepted. Checkerboard inspection found no dark or bright fringe, no filled background, and no outline touching a frame boundary.

## Render and runtime contract

- Runtime frame dimensions are unchanged: character/equipment frames remain 192×192, the Boss remains 256×256, environment remains 192×192, and icons remain 96×96.
- Opaque assets render at 16 EEVEE temporal samples; equipment overlays render at 8 samples.
- Every source frame renders at 2× each runtime dimension, then downsamples in linear premultiplied-alpha space before the deterministic outline is applied.
- The 2048×2048 atlas-page limit and existing decoded-memory budgets remain unchanged.
- The accepted pilot contains 11 manifest assets, 16 straight-RGBA PNG pages, 227 visible frames, and **50,081,792 decoded bytes (47.76 MiB)**.
- Automated alpha measurement found no boundary-touching frame. Minimum transparent margins across the pilot are left 16 px, top 2 px, right 9 px, and bottom 4 px.

## Accepted outputs

| Asset | Baseline triangles | Premium triangles | Output SHA-256 |
|---|---:|---:|---|
| Ancient Golem | 616 | 1,524 | `1c3a3c874296e7d568121184964c1cd8675646fcc4eb740de30aba43fd9be913` |
| Crystal prop 0 | 50 | 316 | `2adb2acd5f78bb723829e76bd36038959707e4c1ada8cab995b8a58125bf3052` |
| Boots of Three Winds | 88 | 400 | `8a339a43ba8ce0d421637c5045650808830eaa0b73a9eaef4ba9d4142114bcd7` |
| Crown of First Leaves | 20 | 276 | `aae0d91ad6f1880df2166a422614ddddc587080f5fc404c87666b6f3e6db0d83` |
| Eternal Seed | 96 | 216 | `b8c7d4a3a23f7cc00d3bc6f0842bcd8dd4ace54d69877197f8c059e1c1b2b8d2` |
| Heartwood Aegis | 44 | 308 | `d2d5c345114622683998ed10316107469f459289191b4e44d4cb866b42fc82ee` |
| Worldbranch | 96 | 368 | `2be72b6b9a37ef6cd03411b3b80e71a64aba72f24ca49fa73514a6673200f55f` |
| Tier-6 health potion | 272 | 646 | `380bfb315a9ea9bd649566cbc42b163730c56deedde3996d041ad14b5c21bfff` |
| Hero | 728 | 1,508 | `d6830b89d63ebe744dadc5508445d6b931ea0476db1a6b2188f743beaf1680e9` |
| Rootling | 564 | 1,036 | `a36dff26f71e299cdddb3130d918c12b7c56f684dff8b5f422e08fec604a4754` |
| Inventory control | 132 | 504 | `bfaef1631f8767f529d952dfe9599cf518adadd5a843257193f83ed37f06367a` |

Triangle counts describe offline Blender source complexity, not runtime geometry; Android receives only the bounded sprite pages. The accepted bar is richer shape hierarchy and material separation, not maximum polygon count or emissive clutter.

## Scaling rule

Subsequent premium-v2 batches must retain this rendering contract and be accepted from category-specific contact sheets plus real-size emulator screenshots. They may vary shape language by faction or function, but must match this pilot's silhouette clarity, material separation, animation readability, restrained glow, alpha quality, and mobile-scale legibility.
