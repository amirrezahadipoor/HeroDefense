package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class FocusSystemTest {
    @Test
    void everyLandedHitChargesAndCritsChargeDouble() {
        assertEquals(0.1f, FocusSystem.FOCUS_PER_HIT);
        GameState state = GameState.newRun(501L);
        assertEquals(0f, state.focus);

        FocusSystem.addHits(state, 3, 0, 0);
        assertEquals(0.3f, state.focus, 1e-6f);

        FocusSystem.addHits(state, 2, 2, 0);
        assertEquals(0.7f, state.focus, 1e-6f);

        FocusSystem.addHits(state, 0, 0, 4);
        assertEquals(1.1f, state.focus, 1e-6f);
    }

    @Test
    void chargeClampsAtMaxAndNeverRegresses() {
        GameState state = GameState.newRun(502L);
        FocusSystem.addHits(state, 1000, 0, 0);
        assertEquals(state.focusMax, state.focus, 1e-6f);
        assertTrue(FocusSystem.isFull(state));

        FocusSystem.addHits(state, 10, 0, 0);
        assertEquals(state.focusMax, state.focus, 1e-6f);

        FocusSystem.addHits(null, 10, 0, 0);
        FocusSystem.addHits(state, -5, -2, -3);
        assertEquals(state.focusMax, state.focus, 1e-6f);
    }

    @Test
    void ratioAndFullnessStaySaneOnDegenerateState() {
        assertEquals(0f, FocusSystem.ratio(null), 1e-6f);
        assertFalse(FocusSystem.isFull(null));

        GameState state = GameState.newRun(503L);
        state.focus = 50f;
        assertEquals(0.5f, FocusSystem.ratio(state), 1e-6f);
        assertFalse(FocusSystem.isFull(state));

        state.focusMax = 0f;
        assertEquals(0f, FocusSystem.ratio(state), 1e-6f);
        assertFalse(FocusSystem.isFull(state));

        state.focusMax = Float.NaN;
        state.focus = Float.NaN;
        assertEquals(0f, FocusSystem.ratio(state), 1e-6f);
        assertFalse(FocusSystem.isFull(state));
    }

    @Test
    void fillRateScalesWithHeroLevelAndEquippedMythics() {
        assertEquals(1f, FocusSystem.fillRateMultiplier(null), 1e-6f);
        GameState fresh = GameState.newRun(505L);
        assertEquals(1f, FocusSystem.fillRateMultiplier(fresh), 1e-6f);

        fresh.heroLevel = 51;
        assertEquals(2f, FocusSystem.fillRateMultiplier(fresh), 1e-6f);

        fresh.equippedItems.put(
            "WEAPON", EquipmentCatalog.byId("sunfall_last_arrow").createItem()
        );
        fresh.equippedItems.put(
            "HELMET", EquipmentCatalog.byId("crown_hollow_eye").createItem()
        );
        assertEquals(2.4f, FocusSystem.fillRateMultiplier(fresh), 1e-5f);

        FocusSystem.addHits(fresh, 10, 0, 0);
        assertEquals(2.4f, fresh.focus, 1e-4f);
    }

    @Test
    void landedArrowsChargeFocusThroughTheAttackSystem() {
        GameState state = GameState.newRun(504L);
        Enemy target = new Enemy(
            state.allocateEntityId(), "ROOTLING", state.hero.x + 90f, state.hero.y
        );
        target.health = target.maxHealth = 1_000_000f;
        state.aliveEnemies.add(target);

        HeroAutoAttackSystem system = new HeroAutoAttackSystem();
        system.update(state, 0f);
        for (int i = 0; i < 60 && target.health == target.maxHealth; i++) {
            system.update(state, 0.2f);
        }
        assertTrue(target.health < target.maxHealth, "arrow never landed");
        assertTrue(state.focus > 0f, "landed arrow charged no focus");
        assertTrue(state.focus <= state.focusMax);
    }
}
