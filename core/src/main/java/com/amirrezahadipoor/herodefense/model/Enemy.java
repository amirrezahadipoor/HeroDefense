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
    /** Seconds the Crown of the Hollow Eye's mark lasts; marked foes take +25%. */
    public float markRemainingSeconds;
    /**
     * Silent Rootling watcher (Codex entry 5, "The Quiet Ones"): stands at the tree line and
     * never moves, attacks, or pays out; targeting, wave-clear counts, and drops all skip it.
     */
    public boolean silentWatcher;
    /** Elite affix id (blightburst/rootward_ward/weeping_rot); null for regulars. */
    public String eliteAffix;
    /** Affix clock driving the rootward shield cycle and weeping trail cadence. */
    public float affixTimerSeconds;
    /** Remaining rootward shield time; a shielded elite takes no damage. */
    public float affixShieldRemainingSeconds;
    /** The elite's death has been resolved (blight blast, if any, already fired). */
    public boolean affixResolved;
    /** The elite kill has been claimed for counts, codex, and its lore fragment. */
    public boolean eliteKillClaimed;

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
        if (affixShieldRemainingSeconds > 0f) {
            return;
        }
        health = Math.max(0f, health - amount);
        if (health == 0f) {
            alive = false;
            active = false;
        }
    }
}
