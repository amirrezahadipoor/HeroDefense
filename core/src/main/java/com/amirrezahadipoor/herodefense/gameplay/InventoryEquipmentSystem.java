package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;

/** Moves item records between inventory and the six persisted equipment slots. */
public final class InventoryEquipmentSystem {
    private final HeroStatCalculator statCalculator;

    public InventoryEquipmentSystem() {
        this(new HeroStatCalculator());
    }

    public InventoryEquipmentSystem(HeroStatCalculator statCalculator) {
        this.statCalculator = statCalculator;
    }

    public boolean equip(GameState state, Item item) {
        if (state == null || item == null || !state.inventory.contains(item)) {
            return false;
        }
        EquipmentSlot slot = EquipmentSlot.parse(item.slot);
        if (slot == null) {
            return false;
        }
        float previousMaxHealth = statCalculator.maxHealth(state);
        Item previous = state.equippedItems.put(slot.name(), item);
        state.inventory.remove(item);
        if (previous != null && previous != item) {
            state.inventory.add(previous);
        }
        synchronizeHealth(state, previousMaxHealth);
        return true;
    }

    public Item unequip(GameState state, EquipmentSlot slot) {
        if (state == null || slot == null) {
            return null;
        }
        float previousMaxHealth = statCalculator.maxHealth(state);
        Item removed = state.equippedItems.remove(slot.name());
        if (removed != null) {
            state.inventory.add(removed);
            synchronizeHealth(state, previousMaxHealth);
        }
        return removed;
    }

    public Item equipped(GameState state, EquipmentSlot slot) {
        return state == null || slot == null ? null : state.equippedItems.get(slot.name());
    }

    private void synchronizeHealth(GameState state, float previousMaxHealth) {
        float updatedMaxHealth = statCalculator.maxHealth(state);
        state.hero.health = Math.min(
            updatedMaxHealth,
            Math.max(0f, state.hero.health + updatedMaxHealth - previousMaxHealth)
        );
        state.hero.maxHealth = updatedMaxHealth;
    }
}
