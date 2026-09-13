package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Moves every living melee enemy directly toward the fixed Hero until attack range. */
public final class EnemyMovementSystem {
    public void update(GameState state, float deltaSeconds) {
        if (state == null || state.hero == null || deltaSeconds <= 0f) {
            return;
        }
        for (Enemy enemy : state.aliveEnemies) {
            moveTowardHero(enemy, state.hero.x, state.hero.y, deltaSeconds);
        }
        for (Boss boss : state.aliveBosses) {
            moveTowardHero(boss, state.hero.x, state.hero.y, deltaSeconds);
        }
    }

    private static void moveTowardHero(Enemy enemy, float heroX, float heroY, float deltaSeconds) {
        if (enemy == null || !enemy.alive || !enemy.active) {
            return;
        }
        if (enemy.stunRemainingSeconds > 0f) {
            enemy.stunRemainingSeconds = Math.max(0f, enemy.stunRemainingSeconds - deltaSeconds);
            return;
        }
        float dx = heroX - enemy.x;
        float dy = heroY - enemy.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float remaining = distance - Math.max(0f, enemy.attackRange);
        if (distance == 0f || remaining <= 0f) {
            return;
        }
        float travel = Math.min(remaining, Math.max(0f, enemy.movementSpeed) * deltaSeconds);
        enemy.x += dx / distance * travel;
        enemy.y += dy / distance * travel;
    }
}
