package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Resolves in-range melee swings through the Hero Dodge/damage pipeline. */
public final class EnemyMeleeAttackSystem {
    private static final int MAX_ATTACKS_PER_UPDATE = 4;
    private final HeroDamageSystem heroDamageSystem;

    public EnemyMeleeAttackSystem(HeroDamageSystem heroDamageSystem) {
        this.heroDamageSystem = heroDamageSystem;
    }

    /**
     * Returns true once the Hero has died and the World Tree has been destroyed. The Hero's
     * death does not end the run instantly: the survivors turn on the tree for
     * {@link GameState#TREE_SIEGE_SECONDS} (its health drains visibly) and only then does it fall.
     */
    public boolean update(GameState state, float deltaSeconds) {
        if (state == null || state.hero == null || deltaSeconds < 0f) {
            return false;
        }
        if (!state.hero.alive) {
            return advanceTreeSiege(state, deltaSeconds);
        }
        for (Enemy enemy : state.aliveEnemies) {
            attackIfInRange(state, enemy, deltaSeconds);
        }
        for (Boss boss : state.aliveBosses) {
            attackIfInRange(state, boss, deltaSeconds);
        }
        if (!state.hero.alive) {
            state.treeSiegeRemainingSeconds = GameState.TREE_SIEGE_SECONDS;
            return false;
        }
        return false;
    }

    /** True when the siege timer has run out and the tree has just been destroyed. */
    public static boolean advanceTreeSiege(GameState state, float deltaSeconds) {
        if (state.worldTreeHealth <= 0f) return true;
        if (state.treeSiegeRemainingSeconds <= 0f) {
            // Loaded a save where the Hero was already dead: no siege left to play.
            state.destroyWorldTree();
            return true;
        }
        state.treeSiegeRemainingSeconds = Math.max(
            0f, state.treeSiegeRemainingSeconds - Math.max(0f, deltaSeconds)
        );
        float ratio = state.treeSiegeRemainingSeconds / GameState.TREE_SIEGE_SECONDS;
        state.worldTreeHealth = Math.min(state.worldTreeHealth, state.worldTreeMaxHealth * ratio);
        if (state.treeSiegeRemainingSeconds <= 0f) {
            state.destroyWorldTree();
            return true;
        }
        return false;
    }

    private void attackIfInRange(GameState state, Enemy enemy, float deltaSeconds) {
        if (enemy == null || !enemy.alive || !enemy.active || !state.hero.alive) {
            return;
        }
        float range = Math.max(0f, enemy.attackRange);
        if (enemy.stunned() || enemy.distanceSquaredTo(state.hero.x, state.hero.y) > range * range) {
            enemy.attackCooldownSeconds = Math.max(0f, enemy.attackCooldownSeconds - deltaSeconds);
            return;
        }

        enemy.attackCooldownSeconds -= deltaSeconds;
        int attacks = 0;
        float interval = Math.max(0.1f, enemy.attackIntervalSeconds);
        while (enemy.attackCooldownSeconds <= 0f
            && attacks < MAX_ATTACKS_PER_UPDATE
            && state.hero.alive) {
            heroDamageSystem.applyIncomingHit(state, enemy.damage);
            enemy.attackCooldownSeconds += interval;
            attacks++;
        }
    }
}
