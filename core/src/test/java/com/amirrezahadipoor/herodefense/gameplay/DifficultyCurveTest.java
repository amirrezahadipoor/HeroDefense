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
    void regularHealthUsesTheSimulationTunedWaveFormulaAndTypeMultiplier() {
        assertEquals(
            DifficultyCurve.BASE_ENEMY_HEALTH * DifficultyCurve.ENEMY_HEALTH_GROWTH,
            curve.baselineRegularHealth(1),
            0.0001f
        );
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
    void bossesUseFifteenTimesBaselineHpAndThreeTimesBaselineDamage() {
        int wave = 50;
        assertEquals(
            curve.baselineRegularHealth(wave) * 15f,
            curve.bossHealth(wave),
            0.001f
        );
        assertEquals(
            curve.baselineRegularDamage(wave) * 3f,
            curve.bossDamage(wave),
            0.001f
        );
        assertTrue(curve.bossHealth(wave) > curve.regularHealth(EnemyType.FUNGAL_BRUTE, wave));
        assertTrue(curve.bossDamage(wave) > curve.regularDamage(EnemyType.FUNGAL_BRUTE, wave));
    }

    @Test
    void ascensionScheduleIsBitIdenticalAtTierZero() {
        assertEquals(DifficultyCurve.ENEMY_HEALTH_GROWTH, DifficultyCurve.healthGrowthForTier(0), 0.0f);
        assertEquals(DifficultyCurve.ENEMY_DAMAGE_GROWTH, DifficultyCurve.damageGrowthForTier(0), 0.0f);
        assertEquals(
            DifficultyCurve.SECOND_HALF_HEALTH_GROWTH, DifficultyCurve.secondHalfHealthGrowthForTier(0), 0.0f);
        assertEquals(
            DifficultyCurve.SECOND_HALF_DAMAGE_GROWTH, DifficultyCurve.secondHalfDamageGrowthForTier(0), 0.0f);
    }

    @Test
    void ascensionBumpsScaleMultiplicativelyAndMonotonically() {
        assertEquals(
            DifficultyCurve.ENEMY_HEALTH_GROWTH * (1f + DifficultyCurve.ASCENSION_HEALTH_BUMP_PER_TIER * 10),
            DifficultyCurve.healthGrowthForTier(10),
            0.000001f
        );
        assertEquals(
            DifficultyCurve.ENEMY_DAMAGE_GROWTH * (1f + DifficultyCurve.ASCENSION_DAMAGE_BUMP_PER_TIER * 10),
            DifficultyCurve.damageGrowthForTier(10),
            0.000001f
        );
        int[] tiers = {0, 1, 3, 6, 10};
        for (int i = 1; i < tiers.length; i++) {
            assertTrue(DifficultyCurve.healthGrowthForTier(tiers[i])
                > DifficultyCurve.healthGrowthForTier(tiers[i - 1]));
            assertTrue(DifficultyCurve.damageGrowthForTier(tiers[i])
                > DifficultyCurve.damageGrowthForTier(tiers[i - 1]));
        }
        assertEquals(
            DifficultyCurve.healthGrowthForTier(0), DifficultyCurve.healthGrowthForTier(-4), 0.0f);
        assertEquals(
            DifficultyCurve.damageGrowthForTier(0), DifficultyCurve.damageGrowthForTier(-4), 0.0f);
    }

    @Test
    void secondHalfKeepsTheSameRelativeBump() {
        for (int tier : new int[] {1, 3, 6, 10}) {
            assertEquals(
                DifficultyCurve.healthGrowthForTier(tier) / DifficultyCurve.healthGrowthForTier(0),
                DifficultyCurve.secondHalfHealthGrowthForTier(tier)
                    / DifficultyCurve.secondHalfHealthGrowthForTier(0),
                0.000001f
            );
            assertEquals(
                DifficultyCurve.damageGrowthForTier(tier) / DifficultyCurve.damageGrowthForTier(0),
                DifficultyCurve.secondHalfDamageGrowthForTier(tier)
                    / DifficultyCurve.secondHalfDamageGrowthForTier(0),
                0.000001f
            );
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
