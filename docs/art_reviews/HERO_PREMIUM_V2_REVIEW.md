# Premium-v2 Hero Final Review

**Date:** 2026-09-13

**Asset:** equipment-neutral Elf Hero

**Result:** **Approved and finalized for category-wide use**

## Relationship to the pilot

The Hero's visual reauthoring was produced in the accepted premium-v2 pilot: layered hair and face planes, pointed ears, circlet, leaf mantle, clasp, separated cuirass and under-tunic, cross strap, belt, skirt panels, pauldrons, bracers, knee guards, boots, and upgraded four-clip motion. This follow-up review expands acceptance from representative pilot poses to every Hero frame, both contrast extremes, grayscale value hierarchy, silhouette-only output, rig metadata, and alpha margins.

No additional pixel churn was accepted after the full-frame review because it found no clipping, detached body part, unreadable pose, alpha fringe, or contrast defect that justified re-rendering an already accepted asset.

## Review evidence

| Sheet | Inspection | SHA-256 |
|---|---|---|
| [`hero_premium_v2/hero_full_motion.png`](hero_premium_v2/hero_full_motion.png) | All 28 runtime frames at exact 1× size, grouped as Idle 6 / Attack 8 / Hit 4 / Death 10 | `4311f319487445782cf73afae33dc291b433664e2afe0213919b01e54289f7eb` |
| [`hero_premium_v2/hero_readability.png`](hero_premium_v2/hero_readability.png) | Baseline comparison, premium idle, grayscale hierarchy, silhouette, dark/light contrast, Attack impact, and Hit recoil | `a2f376bc7e1c3e4626c4da9f44ef46d1d4fcfc8dacada4867c5cc8ab27d4e7ef` |

Both sheets were opened and inspected at full size. The motion sheet deliberately displays each 192×192 frame without scaling; the readability sheet uses nearest-neighbor zoom and performs no painted cleanup.

## Model and material review

- The premium Hero is **1,508 offline triangles**, up from the 728-triangle baseline. Android still receives sprites only, so this richer generation mesh adds no runtime geometry cost.
- The broad cloak, pointed ears, offset arm shapes, fitted torso, and planted boots retain an identifiable archer/Elf silhouette even in a single-color mask.
- Hair, skin, skin shadow, leaf green, deep green, leather, under-tunic, gold, and highlight zones remain separable in grayscale rather than relying only on hue.
- The Covenant clasp is the brightest focal accent. Emissive gold remains local and does not flatten the face or clothing hierarchy.
- The base Hero remains intentionally equipment-neutral. No bow, weapon, quiver, helmet, armor plate, ring, or upgraded boot is baked into this sheet; runtime equipment can replace each slot without doubling its silhouette.
- Dark-forest and light-parchment tests both retain the face, hair edge, hands, cloak, boots, contact shadow, and deterministic outline.

## Rig and animation review

The manifest records one real 25-bone armature, including independent pelvis/spine/chest/head chains, bilateral arm and leg chains, and seven runtime equipment sockets: weapon, helmet, armor, two boots, and two rings.

- **Idle (6):** five unique raster poses plus the intentional loop-closing endpoint. Chest lift, head settle, and counter-rotated arms avoid mechanical whole-body bobbing.
- **Attack (8):** seven unique raster poses plus the loop return. Frames 2–3 compress into anticipation, frame 4 delivers the strongest forward line of action, and frames 5–7 recover without popping.
- **Hit (4):** three unique raster poses plus the return. Frame 1 has a clearly displaced chest/head line and opened arms, followed by a small overshoot before neutral.
- **Death (10):** nine unique raster poses with a two-frame terminal hold. The sequence lowers and foreshortens the body into a forward collapse while the asymmetric arms and bowed head prevent confusion with Hit.
- Rigid one-bone weighting remains intact in all 28 inspected frames: no limb gap, detached hair lock, floating facial feature, torn mantle, or contact-shadow clipping was found.

## Sprite and alpha contract

| Property | Accepted value |
|---|---:|
| Runtime frame | 192×192 RGBA |
| Sheet | 1920×768 RGBA |
| Frames | 28 |
| Render working scale | 2× |
| EEVEE samples | 16 |
| Decoded sheet size | 5,898,240 bytes |
| Compressed PNG size | 427,857 bytes |
| PNG SHA-256 | `d6830b89d63ebe744dadc5508445d6b931ea0476db1a6b2188f743beaf1680e9` |

Every frame contains visible pixels and a transparent outer edge. The minimum measured transparent margins over all Hero poses are **17 px left, 23 px top, 28 px right, and 15 px bottom**. The normalized bottom-left pivot remains `(0.5, 0.12)`, preserving gameplay placement and equipment alignment.

## Final decision

The Hero meets the premium-v2 bar for silhouette, material separation, animation readability, rig integrity, alpha safety, and actual-size mobile output. The procedural metadata is finalized as model revision `hero-premium-v2-final` with rig profile `premium-humanoid-v2`. The next equipment batch must composite against this unchanged base and socket contract.
