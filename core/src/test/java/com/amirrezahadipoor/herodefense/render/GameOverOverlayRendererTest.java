package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class GameOverOverlayRendererTest {
    @Test
    void defeatSummaryWaitsForTheCompleteTreeDestructionClip() {
        assertEquals(0f, GameOverOverlayRenderer.revealProgress(0f, false));
        assertEquals(
            0f,
            GameOverOverlayRenderer.revealProgress(
                GameOverOverlayRenderer.DESTRUCTION_REVEAL_DELAY_SECONDS,
                false
            )
        );
        assertFalse(GameOverOverlayRenderer.isInteractive(0.9f, false));
        assertEquals(1f, GameOverOverlayRenderer.revealProgress(2f, false));
        assertTrue(GameOverOverlayRenderer.isInteractive(2f, false));
    }

    @Test
    void victorySummaryDoesNotDelayBecauseTheTreeRemainsStanding() {
        assertEquals(1f, GameOverOverlayRenderer.revealProgress(0f, true));
        assertTrue(GameOverOverlayRenderer.isInteractive(0f, true));
    }

    @Test
    void invalidPresentationTimeCannotRevealDefeatControls() {
        assertEquals(0f, GameOverOverlayRenderer.revealProgress(Float.NaN, false));
        assertFalse(GameOverOverlayRenderer.isInteractive(Float.POSITIVE_INFINITY, false));
    }

    @Test
    void mythicLineShowsGrantedNameAndHidesOtherwise() {
        assertNull(GameOverOverlayRenderer.mythicEarnedName(null));
        assertNull(GameOverOverlayRenderer.mythicEarnedName(GameState.newRun(22L)));

        GameState granted = GameState.newRun(23L);
        granted.mythicGrantedItemId = "sunfall_last_arrow";
        assertEquals("Sunfall, the Last Arrow", GameOverOverlayRenderer.mythicEarnedName(granted));

        granted.mythicGrantedItemId = "retired_id";
        assertEquals("retired_id", GameOverOverlayRenderer.mythicEarnedName(granted));
    }
}
