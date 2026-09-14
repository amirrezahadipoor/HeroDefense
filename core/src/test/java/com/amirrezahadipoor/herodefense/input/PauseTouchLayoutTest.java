package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PauseTouchLayoutTest {
    @Test
    void inventoryShopAndResumeHaveDistinctLargeTapTargets() {
        assertTrue(PauseTouchLayout.shopAt(360f, 940f));
        assertTrue(PauseTouchLayout.inventoryAt(360f, 780f));
        assertTrue(PauseTouchLayout.rootAt(360f, 620f));
        assertTrue(PauseTouchLayout.resumeAt(360f, 400f));
        assertFalse(PauseTouchLayout.shopAt(360f, 780f));
        assertFalse(PauseTouchLayout.inventoryAt(360f, 940f));
    }
}
