package com.amirrezahadipoor.herodefense.model;

/** A melee attacker moving from a spawn edge toward the fixed Hero. */
public class Enemy extends ArenaEntity {
    public String enemyType = "ROOTLING";
    public float health;
    public float maxHealth;
    public float damage;
    public float movementSpeed;
    public float attackRange;
    public float attackIntervalSeconds;
    public float attackCooldownSeconds;
    public int spawnLane;
    public boolean itemDropRolled;
    public boolean potionDropRolled;
    public boolean killRewardsGranted;
    public boolean defeatParticlesEmitted;
    public boolean alive = true;
    /** Seconds this enemy is frozen by a stunning arrow; it neither moves nor swings. */
    public float stunRemainingSeconds;

    public Enemy() {
        super();
    }

    public Enemy(long id, String enemyType, float x, float y) {
        super(id, x, y);
        this.enemyType = enemyType;
    }

    public EnemyType type() {
        try {
            return EnemyType.valueOf(enemyType);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return EnemyType.ROOTLING;
        }
    }

    public boolean stunned() {
        return alive && stunRemainingSeconds > 0f;
    }

    public void receiveDamage(float amount) {
        if (!alive || amount <= 0f) {
            return;
        }
        health = Math.max(0f, health - amount);
        if (health == 0f) {
            alive = false;
            active = false;
        }
    }
}
