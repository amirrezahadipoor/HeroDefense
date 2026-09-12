package com.amirrezahadipoor.herodefense.model;

/** Distinct regular-enemy roles. Every type closes to melee range before attacking. */
public enum EnemyType {
    ROOTLING(20f, 5f, 68f, 38f, 1.15f, 12, 3),
    STONEKIN(34f, 7f, 42f, 44f, 1.40f, 18, 5),
    GLOOM_WOLF(17f, 6f, 96f, 40f, 0.90f, 15, 4),
    FUNGAL_BRUTE(46f, 10f, 36f, 50f, 1.65f, 24, 7);

    private final float baseHealth;
    private final float baseDamage;
    private final float movementSpeed;
    private final float attackRange;
    private final float attackIntervalSeconds;
    private final int experienceReward;
    private final int coinReward;

    EnemyType(
        float baseHealth,
        float baseDamage,
        float movementSpeed,
        float attackRange,
        float attackIntervalSeconds,
        int experienceReward,
        int coinReward
    ) {
        this.baseHealth = baseHealth;
        this.baseDamage = baseDamage;
        this.movementSpeed = movementSpeed;
        this.attackRange = attackRange;
        this.attackIntervalSeconds = attackIntervalSeconds;
        this.experienceReward = experienceReward;
        this.coinReward = coinReward;
    }

    public float baseHealth() {
        return baseHealth;
    }

    public float baseDamage() {
        return baseDamage;
    }

    public float movementSpeed() {
        return movementSpeed;
    }

    public float attackRange() {
        return attackRange;
    }

    public float attackIntervalSeconds() {
        return attackIntervalSeconds;
    }

    public int experienceReward() {
        return experienceReward;
    }

    public int coinReward() {
        return coinReward;
    }

    public boolean isMelee() {
        return true;
    }

    public String assetKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
