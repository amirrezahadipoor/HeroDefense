package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class EnemyMovementSystemTest {
    private final EnemyMovementSystem movement = new EnemyMovementSystem();
    private final EnemyFactory factory = new EnemyFactory();

    @Test
    void enemyConvergesOnHeroAndStopsAtMeleeRange() {
        GameState state = GameState.newRun(8L);
        Enemy enemy = factory.create(
            state, EnemyType.GLOOM_WOLF, state.hero.x - 300f, state.hero.y, 0
        );
        state.aliveEnemies.add(enemy);
        float before = enemy.distanceSquaredTo(state.hero.x, state.hero.y);

        movement.update(state, 1f);
        assertTrue(enemy.distanceSquaredTo(state.hero.x, state.hero.y) < before);

        movement.update(state, 100f);
        assertEquals(enemy.attackRange, (float) Math.sqrt(
            enemy.distanceSquaredTo(state.hero.x, state.hero.y)
        ), 0.001f);
    }
}
