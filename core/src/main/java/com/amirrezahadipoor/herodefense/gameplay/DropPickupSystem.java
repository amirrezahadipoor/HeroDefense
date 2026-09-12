package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.items.EquipmentDefinition;
import com.amirrezahadipoor.herodefense.model.DropEntity;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Auto-collects equipment drops after a short visible pickup delay. */
public final class DropPickupSystem {
    public int update(GameState state, float deltaSeconds) {
        if (state == null || deltaSeconds < 0f) return 0;
        int collected = 0;
        for (DropEntity drop : state.drops) {
            if (drop == null || !drop.active) continue;
            drop.pickupDelaySeconds -= deltaSeconds;
            if (drop.pickupDelaySeconds > 0f || !"ITEM".equals(drop.dropType)) continue;
            EquipmentDefinition definition = EquipmentCatalog.byId(drop.itemId);
            if (definition != null) {
                state.inventory.add(definition.createItem());
                collected++;
            }
            drop.active = false;
        }
        state.drops.removeIf(drop -> drop == null || !drop.active);
        return collected;
    }
}
