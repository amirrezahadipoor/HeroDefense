package com.amirrezahadipoor.herodefense.model;

/** The stationary Elf defender at the arena origin. */
public final class Hero extends ArenaEntity {
    public float health = HeroStats.BASE_MAX_HEALTH;
    public float maxHealth = HeroStats.BASE_MAX_HEALTH;
    public HeroStats stats = new HeroStats();
    public float attackCooldownSeconds;
    public long currentTargetId = -1L;
    public HeroAnimationState animationState = HeroAnimationState.IDLE;
    public float animationStateSeconds;
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

    public IncomingHitResult receiveIncomingHit(float amount, float dodgeRoll) {
        if (!alive || amount <= 0f) {
            return IncomingHitResult.IGNORED;
        }
        if (dodgeRoll < 0f || dodgeRoll >= 1f || Float.isNaN(dodgeRoll)) {
            throw new IllegalArgumentException("Dodge roll must be in [0, 1)");
        }
        if (dodgeRoll < dodgeChance()) {
            return IncomingHitResult.DODGED;
        }
        health = Math.max(0f, health - amount);
        if (health == 0f) {
            alive = false;
            active = false;
            beginDeathAnimation();
            return IncomingHitResult.KILLED;
        }
        beginHitAnimation();
        return IncomingHitResult.DAMAGED;
    }

    public void beginIdleAnimation() {
        if (alive) {
            animationState = HeroAnimationState.IDLE;
            animationStateSeconds = 0f;
        }
    }

    public void beginAttackAnimation() {
        if (alive && animationState != HeroAnimationState.HIT) {
            animationState = HeroAnimationState.ATTACK;
            animationStateSeconds = 0f;
        }
    }

    public void beginHitAnimation() {
        if (alive) {
            animationState = HeroAnimationState.HIT;
            animationStateSeconds = 0f;
        }
    }

    public void beginDeathAnimation() {
        animationState = HeroAnimationState.DEATH;
        animationStateSeconds = 0f;
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
        active = alive;
        if (animationState == null) {
            animationState = alive ? HeroAnimationState.IDLE : HeroAnimationState.DEATH;
        }
        if (!alive) {
            animationState = HeroAnimationState.DEATH;
        }
        animationStateSeconds = Math.max(0f, animationStateSeconds);
    }
}
