package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HudTouchLayoutTest {
    @Test
    void pauseAndSpeedTargetsAreGenerousDistinctAndPortraitSafe() {
        assertTrue(HudTouchLayout.BUTTON_WIDTH >= 96f);
        assertTrue(HudTouchLayout.BUTTON_HEIGHT >= 96f);
        assertTrue(HudTouchLayout.speedAt(490f, 1115f));
        assertTrue(HudTouchLayout.pauseAt(630f, 1115f));
        assertFalse(HudTouchLayout.pauseAt(490f, 1115f));
        assertFalse(HudTouchLayout.speedAt(630f, 1115f));
    }
}
