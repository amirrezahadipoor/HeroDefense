package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.GameState;

/** Starts a wave and immediately rolls a cleared wave into the next one. */
public final class WaveLifecycleSystem {
    private final EnemyWaveSpawner spawner;
    private final ContinuousWaveRun continuousRun;

    public WaveLifecycleSystem(EnemyWaveSpawner spawner, ContinuousWaveRun continuousRun) {
        this.spawner = spawner;
        this.continuousRun = continuousRun;
    }

    public boolean startCurrentWave(GameState state) {
        if (state == null || state.runComplete || state.waveActive) {
            return false;
        }
        spawner.spawnRegularEnemies(
            state,
            state.waveNumber,
            spawner.regularCountForWave(state.waveNumber)
        );
        state.waveActive = true;
        return true;
    }

    /** Call after combat resolution. A new wave is spawned in the same update on clear. */
    public WaveCompletion updateAfterCombat(GameState state) {
        if (state == null || !state.waveActive || state.runComplete
            || state.hero == null || !state.hero.alive || state.livingEnemyCount() > 0) {
            return WaveCompletion.NO_CHANGE;
        }
        WaveCompletion result = continuousRun.completeCurrentWave(state);
        state.waveActive = false;
        if (result == WaveCompletion.NEXT_WAVE) {
            startCurrentWave(state);
        }
        return result;
    }
}
