package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class DifficultyCurveTest {
    private final DifficultyCurve curve = new DifficultyCurve();

    @Test
    void regularHealthUsesTheRoadmapWaveFormulaAndTypeMultiplier() {
        assertEquals(20f * 1.045f, curve.baselineRegularHealth(1), 0.0001f);
        assertEquals(
            curve.baselineRegularHealth(25) * EnemyType.STONEKIN.baseHealth() / 20f,
            curve.regularHealth(EnemyType.STONEKIN, 25),
            0.001f
        );
        assertTrue(curve.baselineRegularHealth(100) > curve.baselineRegularHealth(50));
    }

    @Test
    void damageGrowsButCannotOneShotReasonablyBuiltHero() {
        for (EnemyType type : EnemyType.values()) {
            float damage = curve.regularDamage(type, 100);
            assertTrue(damage <= curve.reasonableHeroMaxHealth(100) * 0.28f);
        }
    }

    @Test
    void waveSpawnerAppliesCurrentWaveStats() {
        GameState state = GameState.newRun(77L);
        EnemyWaveSpawner spawner = new EnemyWaveSpawner(new EnemyFactory());
        spawner.spawnRegularEnemies(state, 40, 3);
        Enemy first = state.aliveEnemies.get(0);
        EnemyType type = first.type();
        assertEquals(curve.regularHealth(type, 40), first.maxHealth, 0.001f);
        assertEquals(curve.regularDamage(type, 40), first.damage, 0.001f);
    }
}
