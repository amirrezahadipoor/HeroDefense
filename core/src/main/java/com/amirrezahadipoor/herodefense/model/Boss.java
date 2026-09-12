package com.amirrezahadipoor.herodefense.model;

/** Milestone enemy with a distinct behavior and animation identity. */
public final class Boss extends Enemy {
    public String bossType = "ANCIENT_GOLEM";
    public String uniqueAttack = "GROUND_SLAM";
    public float specialCooldownSeconds;
    public int bossNumber;

    public Boss() {
        super();
    }

    public Boss(long id, String bossType, float x, float y, int bossNumber) {
        super(id, bossType, x, y);
        this.bossType = bossType;
        this.bossNumber = bossNumber;
    }

    public BossType bossDefinition() {
        try {
            return BossType.valueOf(bossType);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return BossType.ANCIENT_GOLEM;
        }
    }
}
