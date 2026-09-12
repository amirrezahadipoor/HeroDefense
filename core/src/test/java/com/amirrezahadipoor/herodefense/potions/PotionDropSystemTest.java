package com.amirrezahadipoor.herodefense.potions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.EnemyFactory;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class PotionDropSystemTest {
    private final PotionDropSystem drops = new PotionDropSystem();

    @Test
    void usesEightPercentDropBoundary() {
        assertEquals(0.08f, PotionDropSystem.DROP_RATE);
        assertTrue(drops.isDrop(0f));
        assertTrue(drops.isDrop(0.079999f));
        assertFalse(drops.isDrop(0.08f));
    }

    @Test
    void tierWeightUnlocksFromWeakestEarlyToAllSixLate() {
        for (int step = 0; step < 20; step++) {
            assertEquals(PotionTier.TIER_1, drops.tierForRoll(1, step / 20f));
        }
        assertEquals(PotionTier.TIER_6, drops.tierForRoll(100, 0.999f));
        assertTrue(
            drops.tierForRoll(60, 0.999f).ordinal()
                < drops.tierForRoll(100, 0.999f).ordinal()
        );

        // At Wave 100 the six tiers carry weights 1:2:3:4:5:6 (21 total).
        assertEquals(PotionTier.TIER_1, drops.tierForRoll(100, 0.02f));
        assertEquals(PotionTier.TIER_2, drops.tierForRoll(100, 0.08f));
        assertEquals(PotionTier.TIER_3, drops.tierForRoll(100, 0.20f));
        assertEquals(PotionTier.TIER_4, drops.tierForRoll(100, 0.40f));
        assertEquals(PotionTier.TIER_5, drops.tierForRoll(100, 0.60f));
        assertEquals(PotionTier.TIER_6, drops.tierForRoll(100, 0.85f));
    }

    @Test
    void oneDefeatCanCreateAndAutoCollectOnlyOnePotion() {
        GameState state = stateWhoseNextRollDrops();
        state.waveNumber = 100;
        Enemy enemy = new EnemyFactory().create(state, EnemyType.ROOTLING, 4f, 5f, 0);
        enemy.receiveDamage(Float.MAX_VALUE);
        state.aliveEnemies.add(enemy);

        assertEquals(1, drops.processDefeatedEnemies(state));
        assertEquals(0, drops.processDefeatedEnemies(state));
        com.amirrezahadipoor.herodefense.gameplay.DropPickupSystem pickup =
            new com.amirrezahadipoor.herodefense.gameplay.DropPickupSystem();
        assertEquals(1, pickup.update(state, 1f));
        assertEquals(1, state.healthPotions.stream().mapToInt(Integer::intValue).sum());
    }

    private GameState stateWhoseNextRollDrops() {
        for (long seed = 0; seed < 10_000; seed++) {
            GameState state = GameState.newRun(seed);
            long before = state.combatRandomState;
            if (state.nextCombatRandomFloat() < PotionDropSystem.DROP_RATE) {
                state.combatRandomState = before;
                return state;
            }
        }
        throw new AssertionError("Could not find deterministic drop seed");
    }
}
