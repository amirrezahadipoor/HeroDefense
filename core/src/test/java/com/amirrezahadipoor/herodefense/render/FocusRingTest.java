package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FocusRingTest {
    @Test
    void litSegmentsTrackTheChargeAndDegenerateRatiosLightNone() {
        assertTrue(CombatEntityRenderer.FOCUS_RING_SEGMENTS > 0);
        assertEquals(0, CombatEntityRenderer.focusRingLitSegments(0f));
        assertEquals(
            CombatEntityRenderer.FOCUS_RING_SEGMENTS / 2,
            CombatEntityRenderer.focusRingLitSegments(0.5f)
        );
        assertEquals(
            CombatEntityRenderer.FOCUS_RING_SEGMENTS,
            CombatEntityRenderer.focusRingLitSegments(1f)
        );
        assertEquals(
            CombatEntityRenderer.FOCUS_RING_SEGMENTS,
            CombatEntityRenderer.focusRingLitSegments(2f)
        );
        assertEquals(0, CombatEntityRenderer.focusRingLitSegments(-0.5f));
        assertEquals(0, CombatEntityRenderer.focusRingLitSegments(Float.NaN));
    }

    @Test
    void ringClearsTheHeroSprite() {
        assertTrue(
            CombatEntityRenderer.FOCUS_RING_RADIUS > HeroSpriteRenderer.FRAME_SIZE * 0.5f
        );
    }
}
