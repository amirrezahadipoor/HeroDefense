package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PauseTouchLayoutTest {
    @Test
    void inventoryShopAndResumeHaveDistinctLargeTapTargets() {
        assertTrue(PauseTouchLayout.shopAt(360f, 1_000f));
        assertTrue(PauseTouchLayout.inventoryAt(360f, 830f));
        assertTrue(PauseTouchLayout.resumeAt(360f, 600f));
        assertFalse(PauseTouchLayout.shopAt(360f, 830f));
        assertFalse(PauseTouchLayout.inventoryAt(360f, 1_000f));
    }
}
