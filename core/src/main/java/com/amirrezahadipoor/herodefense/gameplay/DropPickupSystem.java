package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.items.EquipmentDefinition;
import com.amirrezahadipoor.herodefense.model.DropCollectionStage;
import com.amirrezahadipoor.herodefense.model.DropEntity;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.potions.HealthPotionSystem;
import com.amirrezahadipoor.herodefense.potions.PotionTier;

/** Auto-collects equipment and potion drops after a short visible pickup delay. */
public final class DropPickupSystem {
    public static final float HOMING_DURATION_SECONDS = 0.45f;

    private final HealthPotionSystem potionSystem = new HealthPotionSystem();

    public int update(GameState state, float deltaSeconds) {
        if (state == null || deltaSeconds < 0f) return 0;
        int collected = 0;
        for (DropEntity drop : state.drops) {
            if (drop == null || !drop.active) continue;
            float remainingDelta = deltaSeconds;
            if (drop.collectionStage == null) {
                drop.collectionStage = DropCollectionStage.GROUND;
            }
            if (drop.collectionStage == DropCollectionStage.GROUND) {
                float groundTime = Math.max(0f, drop.pickupDelaySeconds);
                if (remainingDelta < groundTime) {
                    drop.pickupDelaySeconds = groundTime - remainingDelta;
                    continue;
                }
                remainingDelta -= groundTime;
                drop.pickupDelaySeconds = 0f;
                drop.collectionStage = DropCollectionStage.HOMING;
            }
            drop.homingElapsedSeconds += remainingDelta;
            if (drop.homingElapsedSeconds < HOMING_DURATION_SECONDS) continue;
            if ("ITEM".equals(drop.dropType)) {
                EquipmentDefinition definition = EquipmentCatalog.byId(drop.itemId);
                if (definition != null) {
                    state.inventory.add(definition.createItem());
                    collected++;
                }
            } else if ("POTION".equals(drop.dropType)) {
                try {
                    potionSystem.add(state, PotionTier.valueOf(drop.itemId), drop.quantity);
                    collected++;
                } catch (IllegalArgumentException | NullPointerException ignored) {
                    // Invalid loaded drop is discarded below rather than blocking the run.
                }
            }
            drop.active = false;
        }
        state.drops.removeIf(drop -> drop == null || !drop.active);
        return collected;
    }
}
