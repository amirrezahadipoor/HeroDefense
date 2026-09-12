package com.amirrezahadipoor.herodefense.potions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class AutoPotionSystemTest {
    private final HealthPotionSystem inventory = new HealthPotionSystem();
    private final AutoPotionSystem autoUse = new AutoPotionSystem(inventory);

    @Test
    void defaultsToThirtyFivePercentAndUsesWeakestAvailableTier() {
        GameState state = GameState.newRun(5L);
        state.hero.health = 34f;
        inventory.add(state, PotionTier.TIER_4, 1);
        inventory.add(state, PotionTier.TIER_2, 1);

        assertEquals(0.35f, autoUse.threshold());
        assertEquals(PotionTier.TIER_2, autoUse.update(state));
        assertEquals(59f, state.hero.health);
        assertEquals(0, state.healthPotions.get(PotionTier.TIER_2.inventoryIndex()));
        assertEquals(1, state.healthPotions.get(PotionTier.TIER_4.inventoryIndex()));
    }

    @Test
    void doesNothingAtOrAboveThresholdOrWithoutStock() {
        GameState state = GameState.newRun(6L);
        state.hero.health = 35f;
        inventory.add(state, PotionTier.TIER_1, 1);
        assertNull(autoUse.update(state));
        state.hero.health = 10f;
        state.healthPotions.set(0, 0);
        assertNull(autoUse.update(state));
    }
}
