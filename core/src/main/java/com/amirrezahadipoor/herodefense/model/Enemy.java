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
    public boolean alive = true;

    public Enemy() {
        super();
    }

    public Enemy(long id, String enemyType, float x, float y) {
        super(id, x, y);
        this.enemyType = enemyType;
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
