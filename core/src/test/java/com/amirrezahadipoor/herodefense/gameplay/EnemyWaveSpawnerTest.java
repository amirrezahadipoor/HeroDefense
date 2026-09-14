package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.WorldLayout;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
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
    void eliteWavesFallOnEverySeventhNonBossWave() {
        assertTrue(EnemyWaveSpawner.isEliteWave(7, 0));
        assertTrue(EnemyWaveSpawner.isEliteWave(14, 0));
        assertTrue(EnemyWaveSpawner.isEliteWave(196, 0));
        assertFalse(EnemyWaveSpawner.isEliteWave(6, 0));
        assertFalse(EnemyWaveSpawner.isEliteWave(8, 0));
        assertFalse(EnemyWaveSpawner.isEliteWave(5, 0));
        assertFalse(EnemyWaveSpawner.isEliteWave(35, 0));
        assertFalse(EnemyWaveSpawner.isEliteWave(70, 0));
        assertFalse(EnemyWaveSpawner.isEliteWave(0, 0));
    }

    @Test
    void eliteWavesMarkOneOrTwoEmpoweredNonWatchers() {
        for (long seed = 1L; seed <= 10L; seed++) {
            GameState state = GameState.newRun(seed);
            spawner.spawnRegularEnemies(state, 7, EnemyWaveSpawner.MAX_REGULAR_ENEMIES);
            java.util.List<Enemy> elites = new java.util.ArrayList<>();
            for (Enemy enemy : state.aliveEnemies) {
                if (enemy.eliteAffix != null) elites.add(enemy);
            }
            assertTrue(elites.size() >= 1 && elites.size() <= 2, "seed " + seed);
            for (Enemy elite : elites) {
                assertFalse(elite.silentWatcher);
                assertTrue(com.amirrezahadipoor.herodefense.model.EliteAffix
                    .fromId(elite.eliteAffix) != null);
                Enemy regular = null;
                for (Enemy enemy : state.aliveEnemies) {
                    if (enemy.eliteAffix == null && !enemy.silentWatcher
                        && enemy.type() == elite.type()) {
                        regular = enemy;
                        break;
                    }
                }
                assertTrue(regular != null, "seed " + seed);
                assertEquals(regular.maxHealth * EnemyWaveSpawner.ELITE_HEALTH_MULT,
                    elite.maxHealth, regular.maxHealth * 1e-4f);
                assertEquals(regular.damage * EnemyWaveSpawner.ELITE_DAMAGE_MULT,
                    elite.damage, regular.damage * 1e-4f);
                assertEquals(elite.maxHealth, elite.health);
            }
        }
    }

    @Test
    void eliteMarkingIsDeterministicPerSeedAndSkipsBossWaves() {
        GameState first = GameState.newRun(4242L);
        GameState second = GameState.newRun(4242L);
        spawner.spawnRegularEnemies(first, 14, 10);
        spawner.spawnRegularEnemies(second, 14, 10);
        for (int index = 0; index < 10; index++) {
            assertEquals(first.aliveEnemies.get(index).eliteAffix,
                second.aliveEnemies.get(index).eliteAffix);
        }
        GameState bossWave = GameState.newRun(4242L);
        spawner.spawnRegularEnemies(bossWave, 35, 10);
        for (Enemy enemy : bossWave.aliveEnemies) {
            assertEquals(null, enemy.eliteAffix);
        }
    }

    @Test
    void lateWavePopulationIsCappedToAvoidUnfairMeleeSwarms() {
        assertEquals(EnemyWaveSpawner.MAX_REGULAR_ENEMIES, spawner.regularCountForWave(100));
        assertTrue(spawner.regularCountForWave(25) < EnemyWaveSpawner.MAX_REGULAR_ENEMIES);
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

    @Test
    void aboutTwoPercentOfRootlingsStandSilentAtTheTreeLine() {
        GameState state = GameState.newRun(77L);
        int rootlings = 0;
        int silent = 0;
        for (int wave = 1; wave <= GameState.FINAL_WAVE; wave++) {
            state.aliveEnemies.clear();
            spawner.spawnRegularEnemies(state, wave, spawner.regularCountForWave(wave));
            for (Enemy enemy : state.aliveEnemies) {
                if (enemy.type() != EnemyType.ROOTLING) {
                    assertFalse(enemy.silentWatcher);
                    continue;
                }
                rootlings++;
                if (!enemy.silentWatcher) continue;
                silent++;
                assertTrue(enemy.x >= 90f && enemy.x <= 630f);
                assertTrue(enemy.y >= 800f && enemy.y <= 860f);
                assertTrue(enemy.distanceSquaredTo(state.hero.x, state.hero.y) > 100f * 100f);
            }
        }
        float rate = (float) silent / rootlings;
        assertTrue(rate > 0.005f && rate < 0.04f, "silent rate " + rate + " over " + rootlings);
    }

    @Test
    void silentWatchersAreDeterministicForTheSameSeed() {
        GameState first = GameState.newRun(4242L);
        GameState second = GameState.newRun(4242L);
        spawner.spawnRegularEnemies(first, 40, 20);
        spawner.spawnRegularEnemies(second, 40, 20);
        for (int index = 0; index < 20; index++) {
            assertEquals(
                first.aliveEnemies.get(index).silentWatcher,
                second.aliveEnemies.get(index).silentWatcher
            );
        }
    }
}
