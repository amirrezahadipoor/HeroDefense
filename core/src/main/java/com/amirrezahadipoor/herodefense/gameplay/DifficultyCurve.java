package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Central wave-number coefficients; renderer-independent for later simulations. */
public final class DifficultyCurve {
    public static final float BASE_ENEMY_HEALTH = 20f;
    public static final float ENEMY_HEALTH_GROWTH = 1.045f;
    public static final float BASE_ENEMY_DAMAGE = 5f;
    public static final float ENEMY_DAMAGE_GROWTH = 1.025f;
    public static final float MAX_REASONABLE_HEALTH_FRACTION_PER_HIT = 0.28f;

    public float baselineRegularHealth(int waveNumber) {
        int wave = clampWave(waveNumber);
        return BASE_ENEMY_HEALTH * (float) Math.pow(ENEMY_HEALTH_GROWTH, wave);
    }

    public float regularHealth(EnemyType type, int waveNumber) {
        return baselineRegularHealth(waveNumber) * type.baseHealth() / BASE_ENEMY_HEALTH;
    }

    public float uncappedRegularDamage(EnemyType type, int waveNumber) {
        int wave = clampWave(waveNumber);
        float baseline = BASE_ENEMY_DAMAGE
            * (float) Math.pow(ENEMY_DAMAGE_GROWTH, Math.max(0, wave - 1));
        return baseline * type.baseDamage() / BASE_ENEMY_DAMAGE;
    }

    public float regularDamage(EnemyType type, int waveNumber) {
        return Math.min(
            uncappedRegularDamage(type, waveNumber),
            reasonableHeroMaxHealth(waveNumber) * MAX_REASONABLE_HEALTH_FRACTION_PER_HIT
        );
    }

    public float reasonableHeroMaxHealth(int waveNumber) {
        int expectedHealthPoints = Math.max(0, clampWave(waveNumber) - 1) / 5;
        return 100f + expectedHealthPoints * 10f;
    }

    public void applyToRegularEnemy(Enemy enemy, EnemyType type, int waveNumber) {
        float health = regularHealth(type, waveNumber);
        enemy.health = health;
        enemy.maxHealth = health;
        enemy.damage = regularDamage(type, waveNumber);
    }

    private static int clampWave(int waveNumber) {
        return Math.max(1, Math.min(GameState.FINAL_WAVE, waveNumber));
    }
}
