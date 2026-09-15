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
        boolean ceremony = isPlantingWave(state.waveNumber, state);
        state.waveNumber++;
        if (state.waveNumber == GameState.FINAL_WAVE) state.wave200ReachedCount++;
        if (ceremony) {
            state.ceremonyPending = true;
            return WaveCompletion.PLANTING_CEREMONY;
        }
        return WaveCompletion.NEXT_WAVE;
    }

    /** Plantings at 50/100/150, each once, short at 50/150, full at 100. */
    static boolean isPlantingWave(int waveNumber, GameState state) {
        if (state == null) return false;
        int planted = state.plantedTreesCount;
        if (waveNumber == 50 && planted == 0) return true;
        if (waveNumber == 100 && planted == 1) return true;
        if (waveNumber == 150 && planted == 2) return true;
        // Fallback for old saves that used boolean: if no planted count yet but wave is 100 and not yet planted
        if (waveNumber == GameState.PLANTING_WAVE && planted == 0 && !state.secondTreePlanted) return true;
        return false;
    }

    /** Which grove index is being planted when ceremony triggers at this wave. */
    public static int groveIndexForWave(int waveNumber) {
        return switch (waveNumber) {
            case 50 -> 0;
            case 100 -> 1;
            case 150 -> 2;
            default -> 1;
        };
    }

    public static boolean isShortCeremony(int waveNumber) {
        return waveNumber == 50 || waveNumber == 150;
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
        if (state.rotTrail != null) state.rotTrail.clear();
        state.drops.removeIf(drop -> drop == null || !drop.active);
        state.hero.currentTargetId = -1L;
    }
}
