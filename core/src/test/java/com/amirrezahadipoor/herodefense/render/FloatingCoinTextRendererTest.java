package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class FloatingCoinTextRendererTest {
    @Test
    void labelPlacesDollarSignBesideTheVisibleAward() {
        assertEquals("$ +27", FloatingCoinTextRenderer.labelFor(27));
        assertEquals("$ +0", FloatingCoinTextRenderer.labelFor(-1));
    }
}
