# Inventory and Equipment Premium-v2 Review

**Decision:** ACCEPTED

**Scope:** Phase 16, item 21 — item cards, equipped slots, rarity, selection, stat comparison, equip/replace/unequip, sell states, and action feedback

**Source commit:** `4d7521353908cd6a54b8565e95b37d2fe3285561`

**Android workflow run / job:** `34758288876` / `103726423231`

**Android evidence artifact:** `10317873133` (`android-test-reports`)

**Artifact archive SHA-256:** `49cfd4ab3dda976824f929fe2eb6d3cea262455c42185a00aa8a69deaeb51ed5`

**Accepted surface audit SHA-256:** `992c255deee1076e54857ee7ff8c9877719b8ae8b97c4477c1e51252fb7b1f75`

## Evidence opened and inspected

I opened and inspected both exact 1080×2220 Pixel 3a / API 35 touch-emulator captures and the combined sheet in `docs/art_reviews/inventory_equipment_premium_v2/`:

1. `inventory_details_emulator.png` — a selected Legendary helmet with all six equipment slots, four visible backpack rows, generated item art, rarity accents, explicit empty-slot comparison, stat deltas, and enabled actions;
2. `inventory_sell_feedback_emulator.png` — the same touch journey immediately after selling, with the bag count updated, sold item removed, total sale value in the toast, action buttons disabled, and tap-ring confirmation visible;
3. `inventory_equipment_contact_sheet.png` — side-by-side selected/comparison and sold/confirmation states at the minimum portrait profile.

Every evidence file's exact dimensions, bytes, and SHA-256 is recorded in `surface_audit.json`.

## Acceptance findings

- **Hierarchy:** a quiet full-screen forest-glass field separates the title, equipped loadout, backpack, item details, transient feedback, and two primary actions without decorative clutter.
- **Equipment presentation:** all six slots are simultaneously visible. Each equipped card identifies its slot, generated icon, item name, tier, and restrained rarity accent; empty states remain legible and tappable.
- **Item cards:** four generous 320×90 rows show generated art, name, rarity, and sale price. Selection combines a structural selected frame with an accent, so meaning does not depend on color.
- **Rarity:** Common, Uncommon, Rare, and Legendary use restrained ivory, green, blue, and gold accents. Color is always accompanied by an uppercase tier label.
- **Details:** the selected item's name, rarity, slot, bag/equipped state, compared item, and every nonzero stat are visible in one panel.
- **Comparison states:** `NEW`, `UP`, `DOWN`, and `SAME` labels make comparison independent of hue. Positive, negative, and neutral values additionally use green, red, and muted treatment.
- **Actions:** Equip changes to Replace when the destination slot is occupied; unequip remains available by tapping an equipped slot; Sell includes the exact coin return. Buttons use generated normal, pressed, and disabled states with 280×110 touch targets.
- **Feedback:** equip, unequip, and sell each produce specific real-time feedback. The accepted sale frame clearly reads `SOLD | +$ 180 | Crown Of First Leaves`, updates the bag immediately, and fades independently of paused simulation time.
- **Touch proof:** the instrumentation journey continued a prepared save, opened Inventory, selected the first card, sold it, captured confirmation, and closed Inventory using touchscreen coordinates only.
- **Scope discipline:** no audio work was included.

The exact screenshots and audit above are accepted for this item. Future Inventory/Equipment presentation changes require new settled touch-emulator evidence.
