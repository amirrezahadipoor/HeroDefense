package com.amirrezahadipoor.herodefense.model;

/** Four authored boss identities with matching non-recolor procedural models. */
public enum BossType {
    ANCIENT_GOLEM("GROUND_SLAM", 34f, 62f, 1.8f),
    THORN_MATRIARCH("THORN_CAGE", 46f, 150f, 1.6f),
    EMBER_WYRM("FLAME_SWEEP", 58f, 190f, 1.4f),
    VOID_KNIGHT("VOID_CHARGE", 72f, 74f, 1.2f);

    private final String uniqueAttack;
    private final float movementSpeed;
    private final float attackRange;
    private final float attackIntervalSeconds;

    BossType(
        String uniqueAttack,
        float movementSpeed,
        float attackRange,
        float attackIntervalSeconds
    ) {
        this.uniqueAttack = uniqueAttack;
        this.movementSpeed = movementSpeed;
        this.attackRange = attackRange;
        this.attackIntervalSeconds = attackIntervalSeconds;
    }

    public String uniqueAttack() {
        return uniqueAttack;
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

    public String assetKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
