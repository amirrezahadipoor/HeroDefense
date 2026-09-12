package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class WaveLifecycleSystemTest {
    private final EnemyWaveSpawner spawner = new EnemyWaveSpawner(new EnemyFactory());
    private final WaveLifecycleSystem lifecycle = new WaveLifecycleSystem(spawner, new ContinuousWaveRun());

    @Test
    void fullyClearedWaveAutomaticallyAdvancesAndSpawnsTheNextWave() {
        GameState state = GameState.newRun(1L);
        assertTrue(lifecycle.startCurrentWave(state));
        assertTrue(state.waveActive);
        assertTrue(state.livingEnemyCount() > 0);
        for (Enemy enemy : state.aliveEnemies) enemy.receiveDamage(Float.MAX_VALUE);

        assertEquals(WaveCompletion.NEXT_WAVE, lifecycle.updateAfterCombat(state));
        assertEquals(2, state.waveNumber);
        assertTrue(state.waveActive);
        assertTrue(state.livingEnemyCount() > 0);
    }

    @Test
    void doesNotAdvanceUntilEveryEnemyIsDead() {
        GameState state = GameState.newRun(2L);
        lifecycle.startCurrentWave(state);
        state.aliveEnemies.get(0).receiveDamage(Float.MAX_VALUE);
        assertEquals(WaveCompletion.NO_CHANGE, lifecycle.updateAfterCombat(state));
        assertEquals(1, state.waveNumber);
    }

    @Test
    void clearingWaveOneHundredCompletesWithoutSpawningWaveOneHundredOne() {
        GameState state = GameState.newRun(3L);
        state.waveNumber = GameState.FINAL_WAVE;
        lifecycle.startCurrentWave(state);
        for (Enemy enemy : state.aliveEnemies) enemy.receiveDamage(Float.MAX_VALUE);

        assertEquals(WaveCompletion.RUN_COMPLETED, lifecycle.updateAfterCombat(state));
        assertTrue(state.runComplete);
        assertFalse(state.waveActive);
        assertEquals(0, state.livingEnemyCount());
    }
}
