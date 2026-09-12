package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.GameState;

/** Advances the existing GameState through one uninterrupted 100-wave run. */
public final class ContinuousWaveRun {
    public WaveCompletion completeCurrentWave(GameState state) {
        if (state == null || state.runComplete) {
            return WaveCompletion.NO_CHANGE;
        }
        clearTransientCombatEntities(state);
        if (state.waveNumber >= GameState.FINAL_WAVE) {
            state.waveNumber = GameState.FINAL_WAVE;
            state.runComplete = true;
            return WaveCompletion.RUN_COMPLETED;
        }
        state.waveNumber++;
        return WaveCompletion.NEXT_WAVE;
    }

    private static void clearTransientCombatEntities(GameState state) {
        state.aliveEnemies.clear();
        state.aliveBosses.clear();
        state.projectiles.clear();
        state.drops.removeIf(drop -> drop == null || !drop.active);
        state.hero.currentTargetId = -1L;
    }
}
