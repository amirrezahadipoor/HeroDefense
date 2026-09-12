package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Hero;
import com.amirrezahadipoor.herodefense.model.Projectile;

/** Deterministic nearest-target bow attacks for the stationary Hero. */
public final class HeroAutoAttackSystem {
    public static final float ATTACK_RANGE = 420f;
    public static final float PROJECTILE_SPEED = 900f;
    private static final int MAX_SHOTS_PER_UPDATE = 4;

    public void update(GameState state, float deltaSeconds) {
        if (state == null || state.hero == null || !state.hero.alive || deltaSeconds < 0f) {
            return;
        }
        updateProjectiles(state, deltaSeconds);

        Hero hero = state.hero;
        hero.attackCooldownSeconds -= deltaSeconds;
        Enemy target = findNearestTarget(state, hero.x, hero.y, ATTACK_RANGE);
        hero.currentTargetId = target == null ? -1L : target.id;
        if (target == null) {
            hero.attackCooldownSeconds = Math.max(0f, hero.attackCooldownSeconds);
            return;
        }

        int shots = 0;
        while (hero.attackCooldownSeconds <= 0f && shots < MAX_SHOTS_PER_UPDATE) {
            fire(state, hero, target);
            hero.attackCooldownSeconds += hero.attackIntervalSeconds();
            shots++;
        }
    }

    public Enemy findNearestTarget(GameState state, float x, float y, float range) {
        float maximumDistanceSquared = range * range;
        Enemy nearest = null;
        float nearestDistanceSquared = maximumDistanceSquared;
        for (Enemy enemy : state.aliveEnemies) {
            float distance = validDistanceSquared(enemy, x, y);
            if (isBetterTarget(enemy, distance, nearest, nearestDistanceSquared)) {
                nearest = enemy;
                nearestDistanceSquared = distance;
            }
        }
        for (Boss boss : state.aliveBosses) {
            float distance = validDistanceSquared(boss, x, y);
            if (isBetterTarget(boss, distance, nearest, nearestDistanceSquared)) {
                nearest = boss;
                nearestDistanceSquared = distance;
            }
        }
        return nearest;
    }

    private static float validDistanceSquared(Enemy enemy, float x, float y) {
        if (enemy == null || !enemy.alive || !enemy.active) {
            return Float.POSITIVE_INFINITY;
        }
        return enemy.distanceSquaredTo(x, y);
    }

    private static boolean isBetterTarget(
        Enemy candidate,
        float candidateDistance,
        Enemy current,
        float currentDistance
    ) {
        if (candidateDistance > currentDistance) {
            return false;
        }
        return candidateDistance < currentDistance
            || (candidateDistance != Float.POSITIVE_INFINITY && current != null && candidate.id < current.id)
            || (candidateDistance != Float.POSITIVE_INFINITY && current == null);
    }

    private static void fire(GameState state, Hero hero, Enemy target) {
        Projectile projectile = new Projectile(
            state.allocateEntityId(), hero.id, target.id, hero.x, hero.y
        );
        projectile.damage = hero.damagePerAttack();
        float distance = (float) Math.sqrt(hero.distanceSquaredTo(target.x, target.y));
        projectile.remainingLifetimeSeconds = distance / PROJECTILE_SPEED + 0.25f;
        setVelocityToward(projectile, target);
        state.projectiles.add(projectile);
    }

    private static void updateProjectiles(GameState state, float deltaSeconds) {
        for (Projectile projectile : state.projectiles) {
            if (projectile == null || !projectile.active || projectile.sourceId != state.hero.id) {
                continue;
            }
            Enemy target = findTargetById(state, projectile.targetId);
            projectile.remainingLifetimeSeconds -= deltaSeconds;
            if (target == null || projectile.remainingLifetimeSeconds < 0f) {
                projectile.active = false;
                continue;
            }

            float distanceSquared = projectile.distanceSquaredTo(target.x, target.y);
            float travel = PROJECTILE_SPEED * deltaSeconds;
            if (distanceSquared <= travel * travel) {
                projectile.x = target.x;
                projectile.y = target.y;
                target.receiveDamage(projectile.damage);
                projectile.active = false;
            } else {
                setVelocityToward(projectile, target);
                projectile.x += projectile.velocityX * deltaSeconds;
                projectile.y += projectile.velocityY * deltaSeconds;
            }
        }
        state.projectiles.removeIf(projectile -> projectile == null || !projectile.active);
    }

    private static Enemy findTargetById(GameState state, long id) {
        for (Enemy enemy : state.aliveEnemies) {
            if (enemy != null && enemy.id == id && enemy.alive) {
                return enemy;
            }
        }
        for (Boss boss : state.aliveBosses) {
            if (boss != null && boss.id == id && boss.alive) {
                return boss;
            }
        }
        return null;
    }

    private static void setVelocityToward(Projectile projectile, Enemy target) {
        float dx = target.x - projectile.x;
        float dy = target.y - projectile.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0f) {
            projectile.velocityX = 0f;
            projectile.velocityY = 0f;
            return;
        }
        projectile.velocityX = dx / length * PROJECTILE_SPEED;
        projectile.velocityY = dy / length * PROJECTILE_SPEED;
    }
}
