package com.amirrezahadipoor.herodefense.items;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.ItemTier;

/** Stable authored identity for one equipment item. */
public final class EquipmentDefinition {
    private final String id;
    private final EquipmentSlot slot;
    private final ItemTier tier;

    public EquipmentDefinition(String id, EquipmentSlot slot, ItemTier tier) {
        this.id = id;
        this.slot = slot;
        this.tier = tier;
    }

    public String id() {
        return id;
    }

    public EquipmentSlot slot() {
        return slot;
    }

    public ItemTier tier() {
        return tier;
    }
}
