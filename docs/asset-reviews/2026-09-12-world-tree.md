# World Tree Render Review

**Date:** 2026-09-12  
**Result:** **Approved**

| State | Sheet | Triangles | SHA-256 |
|---|---|---:|---|
| Healthy | `android/assets/generated/sprites/world_tree_healthy.png` | 520 | `149915c4cb38a6b3181c4656d149f2b6a0c7ffd8489c30fb6a4fe974a5daee73` |
| Damaged | `android/assets/generated/sprites/world_tree_damaged.png` | 444 | `4fe90e70a6145d561de9cc21269c40d31360c6b3036dcbf040c456d509447332` |

Both six-frame sheets were opened and reviewed. The healthy state has a full green canopy, warm trunk, roots, and a cyan heart. The damaged state has visibly reduced foliage, darker bark, exposed glowing cracks, and root damage. Crown sway is driven by the committed World Tree armature action.

Automated checks confirmed 256×256 frames, transparent four-sided margins in every frame, consistent fixed projection, asset-manifest metadata, and geometry far below the tree budget. Large-frame outlines use the style guide's deterministic three-pixel alpha-dilation method to keep headless software rendering inside memory limits while preserving the same outline color.
