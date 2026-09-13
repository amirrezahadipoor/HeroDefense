package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.WorldLayout;
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

    @Test
    void afterTheHeroFallsEnemiesMarchOnTheNearestTreeInstead() {
        GameState state = GameState.newRun(9L);
        Enemy left = factory.create(state, EnemyType.GLOOM_WOLF, 80f, 300f, 0);
        Enemy right = factory.create(state, EnemyType.GLOOM_WOLF, 700f, 900f, 0);
        right.stunRemainingSeconds = 2f;
        state.aliveEnemies.add(left);
        state.aliveEnemies.add(right);
        state.secondTreePlanted = true;
        state.hero.alive = false;

        movement.update(state, 100f);

        float leftToTree = (float) Math.sqrt(
            left.distanceSquaredTo(WorldLayout.WORLD_TREE_X, WorldLayout.WORLD_TREE_Y));
        float rightToSapling = (float) Math.sqrt(
            right.distanceSquaredTo(WorldLayout.SECOND_TREE_X, WorldLayout.SECOND_TREE_Y));
        assertEquals(EnemyMovementSystem.TREE_SIEGE_STOP_DISTANCE, leftToTree, 0.001f);
        assertEquals(EnemyMovementSystem.TREE_SIEGE_STOP_DISTANCE, rightToSapling, 0.001f);
        assertEquals(0f, right.stunRemainingSeconds);
    }
}
