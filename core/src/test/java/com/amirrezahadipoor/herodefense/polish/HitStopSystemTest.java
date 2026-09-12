package com.amirrezahadipoor.herodefense.polish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HitStopSystemTest {
    @Test
    void criticalFreezeIsBriefAndConsumesOnlySimulationTime() {
        HitStopSystem stop = new HitStopSystem();
        assertTrue(HitStopSystem.CRITICAL_HIT_STOP_SECONDS <= 0.05f);
        stop.triggerCriticalHit();
        assertTrue(stop.active());
        assertEquals(0f, stop.consume(0.016f), 0.0001f);
        assertEquals(0f, stop.consume(0.016f), 0.0001f);
        assertEquals(0.003f, stop.consume(0.016f), 0.0001f);
        assertFalse(stop.active());
        assertEquals(0.016f, stop.consume(0.016f), 0.0001f);
    }
}
