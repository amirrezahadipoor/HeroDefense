package com.amirrezahadipoor.herodefense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.ContinuousWaveRun;
import com.amirrezahadipoor.herodefense.gameplay.EnemyFactory;
import com.amirrezahadipoor.herodefense.gameplay.EnemyWaveSpawner;
import com.amirrezahadipoor.herodefense.gameplay.WaveLifecycleSystem;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

/** Continue on the save written right after New Game must replay the opening, never skip it. */
class OpeningReplayTest {

    @Test
    void freshRunSaveIsRecognisedAsUntouched() {
        GameState state = GameState.newRun(7L);
        assertTrue(HeroDefenseGame.untouchedFirstWave(state));
    }

    @Test
    void startedFirstWaveIsNotUntouched() {
        GameState state = GameState.newRun(7L);
        new WaveLifecycleSystem(new EnemyWaveSpawner(new EnemyFactory()), new ContinuousWaveRun())
            .startCurrentWave(state);
        assertFalse(HeroDefenseGame.untouchedFirstWave(state));
    }

    @Test
    void laterWaveBetweenSpawnsIsNotUntouched() {
        GameState state = GameState.newRun(7L);
        state.waveNumber = 2;
        assertFalse(HeroDefenseGame.untouchedFirstWave(state));
    }

    @Test
    void continueReplaysSnapshotTierNotLiveTier() {
        GameState state = GameState.newRun(7L);
        state.ascensionTier = 3;
        state.openingTier = 1;
        assertEquals(1, HeroDefenseGame.openingTierFor(state));
    }

    @Test
    void missingSnapshotFallsBackToLiveTier() {
        GameState state = GameState.newRun(7L);
        state.ascensionTier = 2;
        assertEquals(2, HeroDefenseGame.openingTierFor(state));
    }
}
