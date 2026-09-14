package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Central wave-number coefficients; renderer-independent for later simulations. */
public final class DifficultyCurve {
    public static final float BASE_ENEMY_HEALTH = 20f;
    public static final float ENEMY_HEALTH_GROWTH = 1.037f;
    public static final float BASE_ENEMY_DAMAGE = 0.27f;
    public static final float ENEMY_TYPE_REFERENCE_DAMAGE = 5f;
    public static final float ENEMY_DAMAGE_GROWTH = 1.003f;
    public static final float SECOND_HALF_HEALTH_GROWTH = 1.023f;
    public static final float SECOND_HALF_DAMAGE_GROWTH = 1.008f;
    public static final float BOSS_HEALTH_MULTIPLIER = 15f;
    public static final float BOSS_DAMAGE_MULTIPLIER = 3f;
    public static final float MAX_REASONABLE_HEALTH_FRACTION_PER_HIT = 0.28f;

    // Ascension schedule (Phase 25.3): relative per-tier bumps on every growth constant,
    // tuned against the simulator at tiers 0/3/6/10 (search in docs/BALANCE.md).
    public static final float ASCENSION_HEALTH_BUMP_PER_TIER = 0.0005f;
    public static final float ASCENSION_DAMAGE_BUMP_PER_TIER = 0.0002f;

    public static float healthGrowthForTier(int tier) {
        return ENEMY_HEALTH_GROWTH * (1f + ASCENSION_HEALTH_BUMP_PER_TIER * Math.max(0, tier));
    }

    public static float damageGrowthForTier(int tier) {
        return ENEMY_DAMAGE_GROWTH * (1f + ASCENSION_DAMAGE_BUMP_PER_TIER * Math.max(0, tier));
    }

    public static float secondHalfHealthGrowthForTier(int tier) {
        return SECOND_HALF_HEALTH_GROWTH * (1f + ASCENSION_HEALTH_BUMP_PER_TIER * Math.max(0, tier));
    }

    public static float secondHalfDamageGrowthForTier(int tier) {
        return SECOND_HALF_DAMAGE_GROWTH * (1f + ASCENSION_DAMAGE_BUMP_PER_TIER * Math.max(0, tier));
    }

    public float baselineRegularHealth(int waveNumber) {
        return baselineRegularHealth(waveNumber, 0);
    }

    public float baselineRegularHealth(int waveNumber, int ascensionTier) {
        int wave = clampWave(waveNumber);
        int firstHalf = Math.min(wave, GameState.PLANTING_WAVE);
        int secondHalf = Math.max(0, wave - GameState.PLANTING_WAVE);
        return BASE_ENEMY_HEALTH
            * (float) Math.pow(healthGrowthForTier(ascensionTier), firstHalf)
            * (float) Math.pow(secondHalfHealthGrowthForTier(ascensionTier), secondHalf);
    }

    public float regularHealth(EnemyType type, int waveNumber) {
        return regularHealth(type, waveNumber, 0);
    }

    public float regularHealth(EnemyType type, int waveNumber, int ascensionTier) {
        return baselineRegularHealth(waveNumber, ascensionTier) * type.baseHealth() / BASE_ENEMY_HEALTH;
    }

    public float baselineRegularDamage(int waveNumber) {
        return baselineRegularDamage(waveNumber, 0);
    }

    public float baselineRegularDamage(int waveNumber, int ascensionTier) {
        int wave = clampWave(waveNumber);
        int firstHalf = Math.max(0, Math.min(wave, GameState.PLANTING_WAVE) - 1);
        int secondHalf = Math.max(0, wave - GameState.PLANTING_WAVE);
        return BASE_ENEMY_DAMAGE
            * (float) Math.pow(damageGrowthForTier(ascensionTier), firstHalf)
            * (float) Math.pow(secondHalfDamageGrowthForTier(ascensionTier), secondHalf);
    }

    public float uncappedRegularDamage(EnemyType type, int waveNumber) {
        return uncappedRegularDamage(type, waveNumber, 0);
    }

    public float uncappedRegularDamage(EnemyType type, int waveNumber, int ascensionTier) {
        return baselineRegularDamage(waveNumber, ascensionTier)
            * type.baseDamage() / ENEMY_TYPE_REFERENCE_DAMAGE;
    }

    public float regularDamage(EnemyType type, int waveNumber) {
        return regularDamage(type, waveNumber, 0);
    }

    public float regularDamage(EnemyType type, int waveNumber, int ascensionTier) {
        return Math.min(
            uncappedRegularDamage(type, waveNumber, ascensionTier),
            reasonableHeroMaxHealth(waveNumber) * MAX_REASONABLE_HEALTH_FRACTION_PER_HIT
        );
    }

    public float reasonableHeroMaxHealth(int waveNumber) {
        int expectedHealthPoints = Math.max(0, clampWave(waveNumber) - 1) / 5;
        return 100f + expectedHealthPoints * 10f;
    }

    public float bossHealth(int waveNumber) {
        return bossHealth(waveNumber, 0);
    }

    public float bossHealth(int waveNumber, int ascensionTier) {
        return baselineRegularHealth(waveNumber, ascensionTier) * BOSS_HEALTH_MULTIPLIER;
    }

    public float bossDamage(int waveNumber) {
        return bossDamage(waveNumber, 0);
    }

    public float bossDamage(int waveNumber, int ascensionTier) {
        return baselineRegularDamage(waveNumber, ascensionTier) * BOSS_DAMAGE_MULTIPLIER;
    }

    public void applyToRegularEnemy(Enemy enemy, EnemyType type, int waveNumber) {
        applyToRegularEnemy(enemy, type, waveNumber, 0);
    }

    public void applyToRegularEnemy(Enemy enemy, EnemyType type, int waveNumber, int ascensionTier) {
        float health = regularHealth(type, waveNumber, ascensionTier);
        enemy.health = health;
        enemy.maxHealth = health;
        enemy.damage = regularDamage(type, waveNumber, ascensionTier);
    }

    public void applyToBoss(Enemy boss, int waveNumber) {
        applyToBoss(boss, waveNumber, 0);
    }

    public void applyToBoss(Enemy boss, int waveNumber, int ascensionTier) {
        boss.health = bossHealth(waveNumber, ascensionTier);
        boss.maxHealth = boss.health;
        boss.damage = bossDamage(waveNumber, ascensionTier);
    }

    private static int clampWave(int waveNumber) {
        return Math.max(1, Math.min(GameState.FINAL_WAVE, waveNumber));
    }
}
