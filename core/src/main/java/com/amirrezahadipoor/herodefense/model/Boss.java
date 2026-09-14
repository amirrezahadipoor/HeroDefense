package com.amirrezahadipoor.herodefense.model;

/** Milestone enemy with a distinct behavior and animation identity. */
public final class Boss extends Enemy {
    public String bossType = "ANCIENT_GOLEM";
    public String uniqueAttack = "GROUND_SLAM";
    public float specialCooldownSeconds;
    public float specialAnimationSeconds;
    /** A telegraph is counting down; the special lands when it reaches zero. */
    public boolean specialPending;
    /** Trigger-time dodge dice for the pending special; the wyrm spends both. */
    public float specialPendingRollA;
    public float specialPendingRollB;
    /** Presentation-only flag so a loaded save never replays the arrival shockwave. */
    public boolean entrancePresented;
    public int specialUseCount;
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
