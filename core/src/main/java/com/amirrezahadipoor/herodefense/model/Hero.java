package com.amirrezahadipoor.herodefense.model;

/** The stationary Elf defender at the arena origin. */
public final class Hero extends ArenaEntity {
    public float health = HeroStats.BASE_MAX_HEALTH;
    public float maxHealth = HeroStats.BASE_MAX_HEALTH;
    public HeroStats stats = new HeroStats();
    public float attackCooldownSeconds;
    public long currentTargetId = -1L;
    public boolean alive = true;

    public Hero() {
        super();
    }

    public Hero(long id, float centerX, float centerY) {
        super(id, centerX, centerY);
    }

    public void keepAt(float centerX, float centerY) {
        x = centerX;
        y = centerY;
    }

    public float damagePerAttack() {
        return stats.damage();
    }

    public float attackIntervalSeconds() {
        return stats.attackIntervalSeconds();
    }

    public float dropChanceMultiplier() {
        return stats.dropChanceMultiplier();
    }

    public float dodgeChance() {
        return stats.dodgeChance();
    }

    /** Repairs loaded stats and synchronizes derived HP without granting a heal. */
    public void validateAndRepair() {
        if (stats == null) {
            stats = new HeroStats();
        }
        stats.validateAndRepair();
        maxHealth = stats.maxHealth();
        health = Math.max(0f, Math.min(maxHealth, health));
        alive = health > 0f;
    }
}
