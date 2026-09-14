package com.amirrezahadipoor.herodefense.potions;

import com.amirrezahadipoor.herodefense.items.AffixEffects;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Stores and consumes six potion counts using percentage-of-max-health healing. */
public final class HealthPotionSystem {
    public void add(GameState state, PotionTier tier, int quantity) {
        if (state == null || tier == null || quantity <= 0) return;
        int index = tier.inventoryIndex();
        long updated = (long) state.healthPotions.get(index) + quantity;
        state.healthPotions.set(index, (int) Math.min(Integer.MAX_VALUE, updated));
    }

    public boolean use(GameState state, PotionTier tier) {
        if (state == null || state.hero == null || tier == null || !state.hero.alive
            || state.hero.health >= state.hero.maxHealth) {
            return false;
        }
        int index = tier.inventoryIndex();
        int available = state.healthPotions.get(index);
        if (available <= 0) return false;

        state.healthPotions.set(index, available - 1);
        float healing = state.hero.maxHealth * tier.maxHealthFraction()
            * AffixEffects.potionPowerMultiplier(state);
        state.hero.health = Math.min(state.hero.maxHealth, state.hero.health + healing);
        return true;
    }
}
