package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.items.MythicEffects;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Advances the existing GameState through one uninterrupted 200-wave run. */
public final class ContinuousWaveRun {
    public WaveCompletion completeCurrentWave(GameState state) {
        if (state == null || state.runComplete) {
            return WaveCompletion.NO_CHANGE;
        }
        if (state.waveActive) recordWaveClear(state);
        clearTransientCombatEntities(state);
        if (state.waveNumber >= GameState.FINAL_WAVE) {
            state.waveNumber = GameState.FINAL_WAVE;
            state.runComplete = true;
            MythicEffects.grantAscensionMythic(state);
            return WaveCompletion.RUN_COMPLETED;
        }
        boolean ceremony = state.waveNumber == GameState.PLANTING_WAVE && !state.secondTreePlanted;
        state.waveNumber++;
        if (state.waveNumber == GameState.FINAL_WAVE) state.wave200ReachedCount++;
        if (ceremony) {
            state.ceremonyPending = true;
            return WaveCompletion.PLANTING_CEREMONY;
        }
        return WaveCompletion.NEXT_WAVE;
    }

    /** Folds the cleared wave's combat time into the run record and restarts the timer. */
    public static void recordWaveClear(GameState state) {
        if (state == null) return;
        if (state.waveElapsedSeconds > 0f && state.waveElapsedSeconds < state.fastestWaveClearSeconds) {
            state.fastestWaveClearSeconds = state.waveElapsedSeconds;
        }
        state.waveElapsedSeconds = 0f;
    }

    private static void clearTransientCombatEntities(GameState state) {
        state.aliveEnemies.clear();
        state.aliveBosses.clear();
        state.projectiles.clear();
        state.drops.removeIf(drop -> drop == null || !drop.active);
        state.hero.currentTargetId = -1L;
    }
}
