package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;

/** Moves item records between inventory and the six persisted equipment slots. */
public final class InventoryEquipmentSystem {
    public boolean equip(GameState state, Item item) {
        if (state == null || item == null || !state.inventory.contains(item)) {
            return false;
        }
        EquipmentSlot slot = EquipmentSlot.parse(item.slot);
        if (slot == null) {
            return false;
        }
        Item previous = state.equippedItems.put(slot.name(), item);
        state.inventory.remove(item);
        if (previous != null && previous != item) {
            state.inventory.add(previous);
        }
        return true;
    }

    public Item unequip(GameState state, EquipmentSlot slot) {
        if (state == null || slot == null) {
            return null;
        }
        Item removed = state.equippedItems.remove(slot.name());
        if (removed != null) {
            state.inventory.add(removed);
        }
        return removed;
    }

    public Item equipped(GameState state, EquipmentSlot slot) {
        return state == null || slot == null ? null : state.equippedItems.get(slot.name());
    }
}
