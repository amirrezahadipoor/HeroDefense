package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.items.EquipmentDefinition;
import org.junit.jupiter.api.Test;

final class VisualRarityTest {
    @Test
    void onlyRareAndLegendaryItemsGlow() {
        assertFalse(VisualRarity.COMMON.isGlowing());
        assertFalse(VisualRarity.UNCOMMON.isGlowing());
        assertTrue(VisualRarity.RARE.isGlowing());
        assertTrue(VisualRarity.LEGENDARY.isGlowing());
        assertTrue(VisualRarity.MYTHIC.isGlowing());
        assertTrue(VisualRarity.LEGENDARY.intensity() > VisualRarity.RARE.intensity());
        assertTrue(VisualRarity.MYTHIC.intensity() > VisualRarity.LEGENDARY.intensity());
    }

    @Test
    void allEquippedCatalogTiersMapToTheRuntimeGlowPolicy() {
        for (EquipmentDefinition definition : EquipmentCatalog.all()) {
            VisualRarity rarity = VisualRarity.fromTier(definition.tier().name());
            assertTrue(
                rarity.isGlowing()
                    == (definition.tier().name().equals("RARE")
                        || definition.tier().name().equals("LEGENDARY")
                        || definition.tier().name().equals("MYTHIC"))
            );
        }
    }
}
