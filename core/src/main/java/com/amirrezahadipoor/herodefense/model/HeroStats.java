package com.amirrezahadipoor.herodefense.model;

/** Five permanent, touch-allocatable Hero stats and their combat conversions. */
public final class HeroStats {
    public static final float BASE_DAMAGE = 10f;
    public static final float DAMAGE_PER_STRENGTH = 2f;
    public static final float BASE_ATTACKS_PER_SECOND = 1f;
    public static final float ATTACK_SPEED_PER_AGILITY = 0.03f;
    public static final float DROP_MULTIPLIER_PER_LUCK = 1.02f;
    public static final float DODGE_CHANCE_PER_POINT = 0.005f;
    public static final float MAX_DODGE_CHANCE = 0.60f;
    public static final float BASE_MAX_HEALTH = 100f;
    public static final float MAX_HEALTH_PER_POINT = 10f;

    public int strength;
    public int agility;
    public int luck;
    public int dodge;
    public int health;

    public float damage() {
        return BASE_DAMAGE + nonNegative(strength) * DAMAGE_PER_STRENGTH;
    }

    public float attacksPerSecond() {
        return BASE_ATTACKS_PER_SECOND + nonNegative(agility) * ATTACK_SPEED_PER_AGILITY;
    }

    public float attackIntervalSeconds() {
        return 1f / attacksPerSecond();
    }

    public float dropChanceMultiplier() {
        return (float) Math.pow(DROP_MULTIPLIER_PER_LUCK, nonNegative(luck));
    }

    public float dodgeChance() {
        return Math.min(MAX_DODGE_CHANCE, nonNegative(dodge) * DODGE_CHANCE_PER_POINT);
    }

    public float maxHealth() {
        return BASE_MAX_HEALTH + nonNegative(health) * MAX_HEALTH_PER_POINT;
    }

    public void validateAndRepair() {
        strength = nonNegative(strength);
        agility = nonNegative(agility);
        luck = nonNegative(luck);
        dodge = nonNegative(dodge);
        health = nonNegative(health);
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }
}
