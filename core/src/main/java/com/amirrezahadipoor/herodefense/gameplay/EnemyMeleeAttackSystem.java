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

    /** Returns true once the Hero has died and the World Tree has been destroyed. */
    public boolean update(GameState state, float deltaSeconds) {
        if (state == null || state.hero == null || deltaSeconds < 0f) {
            return false;
        }
        if (!state.hero.alive) {
            state.destroyWorldTree();
            return true;
        }
        for (Enemy enemy : state.aliveEnemies) {
            attackIfInRange(state, enemy, deltaSeconds);
        }
        for (Boss boss : state.aliveBosses) {
            attackIfInRange(state, boss, deltaSeconds);
        }
        if (!state.hero.alive) {
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
