package com.amirrezahadipoor.herodefense.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amirrezahadipoor.herodefense.model.ItemTier;
import org.junit.jupiter.api.Test;

final class EquipmentTierPowerTest {
    @Test
    void rarityTargetsMatchTheBalanceContract() {
        assertEquals(0.05f, ItemTier.COMMON.relativePower(), 0.0001f);
        assertEquals(0.12f, ItemTier.UNCOMMON.relativePower(), 0.0001f);
        assertEquals(0.25f, ItemTier.RARE.relativePower(), 0.0001f);
        assertEquals(0.45f, ItemTier.LEGENDARY.relativePower(), 0.0001f);
    }

    @Test
    void everyAuthoredItemUsesItsTiersWholeStatBudget() {
        for (EquipmentDefinition definition : EquipmentCatalog.all()) {
            int authoredPoints = definition.statBonuses().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
            assertEquals(
                definition.tier().statPointBudget(),
                authoredPoints,
                definition.id()
            );
        }
    }
}
