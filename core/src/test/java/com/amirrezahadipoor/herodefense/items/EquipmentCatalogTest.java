package com.amirrezahadipoor.herodefense.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.ItemTier;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class EquipmentCatalogTest {
    @Test
    void definesExactlyFortyUniqueItemsAcrossFourRequiredTiers() {
        assertEquals(40, EquipmentCatalog.all().size());
        Set<String> ids = new HashSet<>();
        Map<ItemTier, Integer> counts = new EnumMap<>(ItemTier.class);
        for (EquipmentDefinition item : EquipmentCatalog.all()) {
            ids.add(item.id());
            counts.put(item.tier(), counts.getOrDefault(item.tier(), 0) + 1);
        }
        assertEquals(40, ids.size());
        assertEquals(14, counts.get(ItemTier.COMMON));
        assertEquals(12, counts.get(ItemTier.UNCOMMON));
        assertEquals(9, counts.get(ItemTier.RARE));
        assertEquals(5, counts.get(ItemTier.LEGENDARY));
    }

    @Test
    void everyItemHasNameSlotTierBonusesAndReviewedIconPath() {
        EnumSet<EquipmentSlot> slots = EnumSet.noneOf(EquipmentSlot.class);
        Set<String> names = new HashSet<>();
        for (EquipmentDefinition item : EquipmentCatalog.all()) {
            slots.add(item.slot());
            names.add(item.name());
            assertFalse(item.name().isBlank());
            assertFalse(item.statBonuses().isEmpty());
            assertTrue(item.statBonuses().values().stream().allMatch(value -> value > 0));
            assertEquals("generated/icons/equipment_" + item.id() + ".png", item.iconPath());
            assertEquals(item.iconPath(), item.createItem().iconKey);
            assertTrue(item.createItem().sellPrice > 0);
        }
        assertEquals(40, names.size());
        assertEquals(EnumSet.allOf(EquipmentSlot.class), slots);
        assertNotNull(EquipmentCatalog.byId("worldbranch"));
    }
}
