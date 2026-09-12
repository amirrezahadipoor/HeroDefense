package com.amirrezahadipoor.herodefense.model;

/** The stationary Elf defender at the arena origin. */
public final class Hero extends ArenaEntity {
    public float health = 100f;
    public float maxHealth = 100f;
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
}
