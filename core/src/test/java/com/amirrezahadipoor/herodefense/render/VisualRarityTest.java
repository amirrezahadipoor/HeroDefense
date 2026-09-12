package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class VisualRarityTest {
    @Test
    void onlyRareAndLegendaryItemsGlow() {
        assertFalse(VisualRarity.COMMON.isGlowing());
        assertFalse(VisualRarity.UNCOMMON.isGlowing());
        assertTrue(VisualRarity.RARE.isGlowing());
        assertTrue(VisualRarity.LEGENDARY.isGlowing());
        assertTrue(VisualRarity.LEGENDARY.intensity() > VisualRarity.RARE.intensity());
    }
}
