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
                "Two hundred waves. Not one step lost.",
                "The Hollow will need a better plan than waves.",
                "Until it finds one — the Tree stands, and so do I."
            ),
            Epilogue.A.lines()
        );
        assertEquals(
            List.of(
                "Two hundred waves. Every one of them close.",
                "I don't remember all of it clearly. I remember not letting go.",
                "That's enough. It has to be."
            ),
            Epilogue.B.lines()
        );
        assertEquals(
            List.of(
                "Not even to the middle.",
                "The Tree falls quietly, when it falls this early. Almost gently.",
                "It will not be this quiet next time."
            ),
            Epilogue.C.lines()
        );
        assertEquals(
            List.of(
                "So close to the second root.",
                "I got further than the fall before. That is not the same as far enough.",
                "Again, then."
            ),
            Epilogue.D.lines()
        );
        assertEquals(
            List.of(
                "One tree still stood when I fell. That has to count for something.",
                "The Hollow paid for every wave past a hundred. It just outlasted me by a few.",
                "Next time, it pays for all two hundred."
            ),
            Epilogue.E.lines()
        );
        assertEquals(
            List.of(
                "The Hollow isn't gone. It's only quiet — for as long as it takes to remember how to fall again.",
                "Rise again? The Tree will still be standing when you do."
            ),
            Epilogue.TRANSITION
        );
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
