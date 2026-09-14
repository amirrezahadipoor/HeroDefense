package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import org.junit.jupiter.api.Test;

final class ProjectileTrailTest {
    @Test
    void trailFadesMonotonicallyAndStaysShort() {
        assertTrue(CombatEntityRenderer.PROJECTILE_TRAIL_STEPS <= 3);
        float previous = 1f;
        for (int step = 1; step <= CombatEntityRenderer.PROJECTILE_TRAIL_STEPS; step++) {
            float alpha = CombatEntityRenderer.projectileTrailAlpha(step);
            assertTrue(alpha < previous);
            assertTrue(alpha >= 0f && alpha < 0.6f);
            previous = alpha;
        }
        assertEquals(0f, CombatEntityRenderer.projectileTrailAlpha(4));
    }

    @Test
    void progressionStepAddsBowForgeAndAscensionTierCappedAtTen() {
        assertEquals(0, CombatEntityRenderer.progressionStep(null));
        GameState fresh = GameState.newRun(41L);
        assertEquals(0, CombatEntityRenderer.progressionStep(fresh));

        fresh.ascensionTier = 3;
        assertEquals(3, CombatEntityRenderer.progressionStep(fresh));

        Item bow = EquipmentCatalog.byId("ashwood_bow").createItem();
        bow.upgradeLevel = 5;
        fresh.equippedItems.put("WEAPON", bow);
        assertEquals(8, CombatEntityRenderer.progressionStep(fresh));

        fresh.ascensionTier = 9;
        assertEquals(10, CombatEntityRenderer.progressionStep(fresh));

        fresh.ascensionTier = -4;
        bow.upgradeLevel = -2;
        assertEquals(0, CombatEntityRenderer.progressionStep(fresh));
    }

    @Test
    void trailHeatAndAlphaEscalateWithPowerButStayBounded() {
        assertEquals(0f, CombatEntityRenderer.trailHeat(0), 1e-6f);
        assertEquals(0.5f, CombatEntityRenderer.trailHeat(5), 1e-6f);
        assertEquals(1f, CombatEntityRenderer.trailHeat(10), 1e-6f);
        assertEquals(1f, CombatEntityRenderer.trailHeat(40), 1e-6f);
        assertEquals(0f, CombatEntityRenderer.trailHeat(-3), 1e-6f);

        assertEquals(0.4f, CombatEntityRenderer.projectileTrailAlpha(1, 0), 1e-6f);
        assertEquals(0.7f, CombatEntityRenderer.projectileTrailAlpha(1, 10), 1e-6f);
        assertEquals(0.4f, CombatEntityRenderer.projectileTrailAlpha(3, 10), 1e-6f);
        float previous = 1f;
        for (int step = 1; step <= CombatEntityRenderer.PROJECTILE_TRAIL_STEPS; step++) {
            float alpha = CombatEntityRenderer.projectileTrailAlpha(step, 10);
            assertTrue(alpha < previous);
            previous = alpha;
        }
    }

    @Test
    void bowGlowMultiplierRunsOneToTwoPointTwo() {
        assertEquals(
            1f,
            EquipmentSpriteRenderer.progressionGlowMultiplier(GameState.newRun(42L)),
            1e-6f
        );
        GameState maxed = GameState.newRun(43L);
        maxed.ascensionTier = 10;
        assertEquals(
            2.2f, EquipmentSpriteRenderer.progressionGlowMultiplier(maxed), 1e-6f
        );
    }
}
