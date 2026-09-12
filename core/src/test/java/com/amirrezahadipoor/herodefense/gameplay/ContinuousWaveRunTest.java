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
    void advancesOneStateAndOneHeroSeamlesslyFromWaveOneThroughOneHundred() {
        GameState state = GameState.newRun(90L);
        Hero originalHero = state.hero;

        for (int expectedWave = 2; expectedWave <= GameState.FINAL_WAVE; expectedWave++) {
            assertEquals(WaveCompletion.NEXT_WAVE, run.completeCurrentWave(state));
            assertEquals(expectedWave, state.waveNumber);
            assertSame(originalHero, state.hero);
        }

        assertEquals(WaveCompletion.RUN_COMPLETED, run.completeCurrentWave(state));
        assertTrue(state.runComplete);
        assertEquals(GameState.FINAL_WAVE, state.waveNumber);
        assertEquals(WaveCompletion.NO_CHANGE, run.completeCurrentWave(state));
    }
}
