package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.WorldLayout;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.SpawnLane;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class EnemyWaveSpawnerTest {
    private final EnemyWaveSpawner spawner = new EnemyWaveSpawner(new EnemyFactory());

    @Test
    void distributesEnemiesAcrossExactlyThreeOutsideEdges() {
        GameState state = GameState.newRun(123L);
        spawner.spawnRegularEnemies(state, 1, 6);

        Set<Integer> lanes = new HashSet<>();
        for (Enemy enemy : state.aliveEnemies) {
            lanes.add(enemy.spawnLane);
            if (enemy.spawnLane == SpawnLane.LEFT.id()) assertTrue(enemy.x < 0f);
            if (enemy.spawnLane == SpawnLane.RIGHT.id()) assertTrue(enemy.x > WorldLayout.REFERENCE_WIDTH);
            if (enemy.spawnLane == SpawnLane.SOUTH.id()) assertTrue(enemy.y < 0f);
        }
        assertEquals(Set.of(0, 1, 2), lanes);
    }

    @Test
    void sameRunWaveAndCountProduceSameSpawnPattern() {
        GameState first = GameState.newRun(555L);
        GameState second = GameState.newRun(555L);
        spawner.spawnRegularEnemies(first, 9, 8);
        spawner.spawnRegularEnemies(second, 9, 8);
        for (int index = 0; index < 8; index++) {
            assertEquals(first.aliveEnemies.get(index).x, second.aliveEnemies.get(index).x);
            assertEquals(first.aliveEnemies.get(index).y, second.aliveEnemies.get(index).y);
        }
    }
}
