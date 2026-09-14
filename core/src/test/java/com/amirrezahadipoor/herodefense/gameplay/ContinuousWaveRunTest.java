package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Hero;
import org.junit.jupiter.api.Test;

final class ContinuousWaveRunTest {
    private final ContinuousWaveRun run = new ContinuousWaveRun();

    @Test
    void advancesOneStateAndOneHeroSeamlesslyFromWaveOneThroughTwoHundred() {
        GameState state = GameState.newRun(90L);
        Hero originalHero = state.hero;

        for (int expectedWave = 2; expectedWave <= GameState.FINAL_WAVE; expectedWave++) {
            WaveCompletion completion = run.completeCurrentWave(state);
            if (expectedWave == GameState.PLANTING_WAVE + 1) {
                assertEquals(WaveCompletion.PLANTING_CEREMONY, completion);
                assertTrue(state.ceremonyPending);
                state.ceremonyPending = false;
                state.secondTreePlanted = true;
            } else {
                assertEquals(WaveCompletion.NEXT_WAVE, completion);
            }
            assertEquals(expectedWave, state.waveNumber);
            assertSame(originalHero, state.hero);
        }

        assertEquals(WaveCompletion.RUN_COMPLETED, run.completeCurrentWave(state));
        assertTrue(state.runComplete);
        assertEquals(GameState.FINAL_WAVE, state.waveNumber);
        assertEquals(WaveCompletion.NO_CHANGE, run.completeCurrentWave(state));
    }

    @Test
    void advancingIntoWaveTwoHundredCountsTheReachOnce() {
        GameState state = GameState.newRun(91L);
        state.waveNumber = 199;
        state.waveActive = true;
        assertEquals(WaveCompletion.NEXT_WAVE, run.completeCurrentWave(state));
        assertEquals(200, state.waveNumber);
        assertEquals(1, state.wave200ReachedCount);

        state.waveActive = true;
        assertEquals(WaveCompletion.RUN_COMPLETED, run.completeCurrentWave(state));
        assertEquals(1, state.wave200ReachedCount);

        GameState second = GameState.newRun(92L);
        second.waveNumber = 199;
        second.waveActive = true;
        second.wave200ReachedCount = 1;
        run.completeCurrentWave(second);
        assertEquals(2, second.wave200ReachedCount);
    }

    @Test
    void waveClearTimerFoldsIntoTheRecordAndRestarts() {
        GameState state = GameState.newRun(93L);
        state.waveActive = true;
        state.waveElapsedSeconds = 12.5f;
        run.completeCurrentWave(state);
        assertEquals(12.5f, state.fastestWaveClearSeconds);
        assertEquals(0f, state.waveElapsedSeconds);

        state.waveActive = true;
        state.waveElapsedSeconds = 30f;
        run.completeCurrentWave(state);
        assertEquals(12.5f, state.fastestWaveClearSeconds);
        assertEquals(0f, state.waveElapsedSeconds);
    }

    @Test
    void zeroElapsedNeverPoisonsTheRecord() {
        GameState state = GameState.newRun(94L);
        state.waveActive = true;
        state.waveElapsedSeconds = 0f;
        state.fastestWaveClearSeconds = 20f;
        run.completeCurrentWave(state);
        assertEquals(20f, state.fastestWaveClearSeconds);
    }

    @Test
    void postCardCompletionDoesNotRecordTwice() {
        GameState state = GameState.newRun(95L);
        state.waveNumber = 5;
        state.waveActive = false;
        state.waveElapsedSeconds = 0f;
        state.fastestWaveClearSeconds = 20f;
        assertEquals(WaveCompletion.NEXT_WAVE, run.completeCurrentWave(state));
        assertEquals(6, state.waveNumber);
        assertEquals(20f, state.fastestWaveClearSeconds);
    }
}
