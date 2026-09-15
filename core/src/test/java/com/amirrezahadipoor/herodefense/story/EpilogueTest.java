package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amirrezahadipoor.herodefense.model.GameState;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EpilogueTest {
    @Test
    void flawlessVictoryNeedsNoDeathFewPotionsAndThirtyPercentHealth() {
        assertEquals(Epilogue.A, select(win(false, 2, 30f, 100f)));
        assertEquals(Epilogue.B, select(win(true, 2, 30f, 100f)));
        assertEquals(Epilogue.B, select(win(false, 3, 100f, 100f)));
        assertEquals(Epilogue.B, select(win(false, 0, 29.9f, 100f)));
        assertEquals(Epilogue.A, select(win(false, 0, 30f, 100f)));
    }

    @Test
    void lossesSplitByGameOverWave() {
        assertEquals(Epilogue.C, select(loss(1)));
        assertEquals(Epilogue.C, select(loss(49)));
        assertEquals(Epilogue.D, select(loss(50)));
        assertEquals(Epilogue.D, select(loss(149)));
        assertEquals(Epilogue.E, select(loss(150)));
        assertEquals(Epilogue.E, select(loss(200)));
    }

    @Test
    void everyEpilogueHoldsExactlyThreeBeats() {
        for (Epilogue epilogue : Epilogue.values()) {
            assertEquals(3, epilogue.lines().size(), epilogue.name());
        }
        assertEquals(2, Epilogue.TRANSITION.size());
    }

    @Test
    void textMatchesStoryContentVerbatim() {
        assertEquals(
            List.of(
                "Two hundred waves. No step lost.",
                "The Hollow needs a new plan.",
                "Till then, the Tree and I stand."
            ),
            Epilogue.A.lines()
        );
        assertEquals(
            List.of(
                "Two hundred waves. All were close.",
                "I do not recall it all. I recall not letting go.",
                "That is enough. It has to be."
            ),
            Epilogue.B.lines()
        );
        assertEquals(
            List.of(
                "Not to the middle.",
                "The Tree falls soft and quiet this early.",
                "Next time it will be loud."
            ),
            Epilogue.C.lines()
        );
        assertEquals(
            List.of(
                "Close to the second root.",
                "I went farther than last time. Far is not far enough.",
                "Again."
            ),
            Epilogue.D.lines()
        );
        assertEquals(
            List.of(
                "One tree stood when I fell. That must count.",
                "The Hollow paid past wave one hundred. It just lasted a bit more.",
                "Next time it pays for all."
            ),
            Epilogue.E.lines()
        );
        assertEquals(
            List.of(
                "The Hollow is not gone. It is quiet while it learns to fall again.",
                "Rise again. The Tree will still stand."
            ),
            Epilogue.TRANSITION
        );
    }

    @Test
    void persistedEpilogueIdWinsOverLiveSelection() {
        GameState state = loss(10);
        state.epilogueId = "E";
        assertEquals(Epilogue.E, Epilogue.endingFor(state));
        state.epilogueId = "bogus";
        assertEquals(Epilogue.C, Epilogue.endingFor(state));
        state.epilogueId = "";
        assertEquals(Epilogue.C, Epilogue.endingFor(state));
    }

    private static Epilogue select(GameState state) {
        return Epilogue.select(state);
    }

    private static GameState win(boolean heroDied, int potions, float health, float maxHealth) {
        GameState state = GameState.newRun(5L);
        state.runComplete = true;
        state.waveNumber = GameState.FINAL_WAVE;
        state.heroDiedThisRun = heroDied;
        state.potionsUsedThisRun = potions;
        state.hero.maxHealth = maxHealth;
        state.hero.health = health;
        return state;
    }

    private static GameState loss(int wave) {
        GameState state = GameState.newRun(6L);
        state.waveNumber = wave;
        state.hero.alive = false;
        return state;
    }
}
