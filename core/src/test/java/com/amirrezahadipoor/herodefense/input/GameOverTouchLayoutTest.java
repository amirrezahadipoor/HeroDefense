package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GameOverTouchLayoutTest {
    @Test
    void restartIsOneGenerousTouchTarget() {
        assertTrue(GameOverTouchLayout.RESTART_WIDTH >= 96f);
        assertTrue(GameOverTouchLayout.RESTART_HEIGHT >= 96f);
        assertTrue(GameOverTouchLayout.restartAt(360f, 290f));
        assertFalse(GameOverTouchLayout.restartAt(10f, 10f));
    }
}
