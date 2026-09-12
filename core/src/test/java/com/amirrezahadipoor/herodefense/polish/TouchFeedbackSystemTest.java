package com.amirrezahadipoor.herodefense.polish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class TouchFeedbackSystemTest {
    @Test
    void tapAndCardUseDistinctVisiblePulsesThatExpireInRealTime() {
        TouchFeedbackSystem feedback = new TouchFeedbackSystem();
        feedback.triggerTap(10f, 20f);
        feedback.triggerCardSelection(30f, 40f);
        assertEquals(2, feedback.pulses().size());
        assertEquals(TouchPulse.Kind.TAP, feedback.pulses().get(0).kind);
        assertEquals(TouchPulse.Kind.CARD_SELECTION, feedback.pulses().get(1).kind);
        feedback.update(0.21f);
        assertEquals(1, feedback.pulses().size());
        assertTrue(feedback.pulses().get(0).progress() > 0f);
        feedback.update(0.20f);
        assertTrue(feedback.pulses().isEmpty());
    }
}
