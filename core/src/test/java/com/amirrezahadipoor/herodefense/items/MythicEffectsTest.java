package com.amirrezahadipoor.herodefense.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class MythicEffectsTest {
    @Test
    void allSixMythicsHavePassiveAndFlavorWhileOrdinaryItemsHaveNeither() {
        assertEquals(6, MythicEffects.ALL_IDS.size());
        for (String id : MythicEffects.ALL_IDS) {
            assertNotNull(EquipmentCatalog.byId(id), id);
            assertTrue(MythicEffects.passiveLine(id).startsWith("PASSIVE: "), id);
            assertFalse(MythicEffects.flavorLine(id).isBlank(), id);
        }
        assertNull(MythicEffects.passiveLine("worldbranch"));
        assertNull(MythicEffects.flavorLine("worldbranch"));
        assertNull(MythicEffects.passiveLine(null));
        assertNull(MythicEffects.flavorLine("nope"));
    }

    @Test
    void equippedQueriesSeeOnlyWornMythics() {
        GameState state = GameState.newRun(3301L);
        assertFalse(MythicEffects.hasSunfall(state));
        assertEquals(0, MythicEffects.equippedMythicCount(state));

        equip(state, "WEAPON", "sunfall_last_arrow");
        equip(state, "RING_1", "verdant_oath");
        assertTrue(MythicEffects.hasSunfall(state));
        assertTrue(MythicEffects.hasVerdantOath(state));
        assertFalse(MythicEffects.hasCrown(state));
        assertEquals(2, MythicEffects.equippedMythicCount(state));
    }

    @Test
    void windrunnerRampsWithTheWaveClockAndCapsAtTwentyFivePercent() {
        GameState plain = GameState.newRun(3302L);
        plain.waveElapsedSeconds = 60f;
        assertEquals(1f, MythicEffects.windrunnerAttackSpeedMultiplier(plain), 1e-6f);

        GameState state = GameState.newRun(3302L);
        equip(state, "BOOTS", "windrunner_last_steps");
        state.waveElapsedSeconds = 0f;
        assertEquals(1f, MythicEffects.windrunnerAttackSpeedMultiplier(state), 1e-6f);
        state.waveElapsedSeconds = 10f;
        assertEquals(1.10f, MythicEffects.windrunnerAttackSpeedMultiplier(state), 1e-6f);
        state.waveElapsedSeconds = 1_000f;
        assertEquals(1.25f, MythicEffects.windrunnerAttackSpeedMultiplier(state), 1e-6f);
    }

    @Test
    void verdantLifestealFollowsItsBuffTimer() {
        GameState state = GameState.newRun(3303L);
        assertEquals(0f, MythicEffects.verdantLifestealBonus(state), 1e-6f);
        state.hero.mythicLifestealRemainingSeconds = 4f;
        assertEquals(0.05f, MythicEffects.verdantLifestealBonus(state), 1e-6f);
    }

    @Test
    void crownMarkGrantsTwentyFivePercentToFurtherHits() {
        assertEquals(1f, MythicEffects.crownMarkDamageMultiplier(null), 1e-6f);
        Enemy foe = new Enemy();
        assertEquals(1f, MythicEffects.crownMarkDamageMultiplier(foe), 1e-6f);
        foe.markRemainingSeconds = 4f;
        assertEquals(1.25f, MythicEffects.crownMarkDamageMultiplier(foe), 1e-6f);
    }

    @Test
    void barkHealsTwentyPercentOnEveryTenthLandedHitAndNeverRevives() {
        GameState state = GameState.newRun(3304L);
        equip(state, "ARMOR", "bark_first_root");
        state.hero.health = 50f;
        for (int hit = 1; hit <= 9; hit++) {
            MythicEffects.onLandedHitTaken(state);
            assertEquals(50f, state.hero.health, hit + "th hit must not heal");
        }
        MythicEffects.onLandedHitTaken(state);
        assertEquals(10, state.hero.mythicHitsTaken);
        assertEquals(50f + state.hero.maxHealth * 0.20f, state.hero.health, 1e-4f);

        GameState bare = GameState.newRun(3305L);
        bare.hero.health = 50f;
        for (int hit = 0; hit < 10; hit++) MythicEffects.onLandedHitTaken(bare);
        assertEquals(50f, bare.hero.health);

        GameState dead = GameState.newRun(3306L);
        equip(dead, "ARMOR", "bark_first_root");
        dead.hero.mythicHitsTaken = 9;
        dead.hero.alive = false;
        dead.hero.health = 0f;
        MythicEffects.onLandedHitTaken(dead);
        assertEquals(0f, dead.hero.health);
        assertFalse(dead.hero.alive);
    }

    private static void equip(GameState state, String slot, String id) {
        state.equippedItems.put(slot, EquipmentCatalog.byId(id).createItem());
    }
}
