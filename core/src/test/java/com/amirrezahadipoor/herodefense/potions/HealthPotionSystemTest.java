package com.amirrezahadipoor.herodefense.potions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class HealthPotionSystemTest {
    private final HealthPotionSystem potions = new HealthPotionSystem();

    @Test
    void sixTiersHealExactPhaseFourteenMaxHealthFractions() {
        float[] expected = {0.15f, 0.25f, 0.40f, 0.60f, 0.80f, 1f};
        assertEquals(6, PotionTier.values().length);
        for (PotionTier tier : PotionTier.values()) {
            GameState state = GameState.newRun(tier.ordinal());
            state.hero.health = 0.01f;
            potions.add(state, tier, 1);
            assertTrue(potions.use(state, tier));
            assertEquals(
                Math.min(100f, 0.01f + 100f * expected[tier.ordinal()]),
                state.hero.health,
                0.001f
            );
            assertEquals(0, state.healthPotions.get(tier.inventoryIndex()));
            assertEquals(
                "generated/icons/health_potion_" + (tier.ordinal() + 1) + ".png",
                tier.iconPath()
            );
        }
    }

    @Test
    void cannotWastePotionAtFullHealthOrUseMissingPotion() {
        GameState state = GameState.newRun(2L);
        potions.add(state, PotionTier.TIER_1, 1);
        assertFalse(potions.use(state, PotionTier.TIER_1));
        state.hero.health = 50f;
        assertFalse(potions.use(state, PotionTier.TIER_2));
        assertEquals(1, state.healthPotions.get(0));
    }
}
