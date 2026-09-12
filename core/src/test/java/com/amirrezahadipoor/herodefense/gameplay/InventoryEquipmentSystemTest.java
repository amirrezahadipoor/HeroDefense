package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

final class InventoryEquipmentSystemTest {
    private final InventoryEquipmentSystem equipment = new InventoryEquipmentSystem();

    @Test
    void definesExactlyTheSixRequiredSlots() {
        assertEquals(
            EnumSet.of(
                EquipmentSlot.WEAPON,
                EquipmentSlot.HELMET,
                EquipmentSlot.ARMOR,
                EquipmentSlot.BOOTS,
                EquipmentSlot.RING_1,
                EquipmentSlot.RING_2
            ),
            EnumSet.allOf(EquipmentSlot.class)
        );
    }

    @Test
    void equipSwapsSameSlotItemsAndUnequipReturnsItemToInventory() {
        GameState state = GameState.newRun(14L);
        Item first = new Item("first", "First Bow", "WEAPON", "COMMON");
        Item second = new Item("second", "Second Bow", "WEAPON", "RARE");
        state.inventory.add(first);
        state.inventory.add(second);

        assertTrue(equipment.equip(state, first));
        assertSame(first, equipment.equipped(state, EquipmentSlot.WEAPON));
        assertTrue(equipment.equip(state, second));
        assertSame(second, equipment.equipped(state, EquipmentSlot.WEAPON));
        assertTrue(state.inventory.contains(first));

        assertSame(second, equipment.unequip(state, EquipmentSlot.WEAPON));
        assertNull(equipment.equipped(state, EquipmentSlot.WEAPON));
        assertTrue(state.inventory.contains(second));
    }

    @Test
    void sellsOnlyUnequippedCatalogItemsAndCreditsTheirPositiveCoinValue() {
        GameState state = GameState.newRun(15L);
        Item sale = EquipmentCatalog.all().get(0).createItem();
        Item unknown = new Item("potion", "Potion", "POTION", "COMMON");
        state.inventory.add(sale);
        state.inventory.add(unknown);
        int before = state.coins;

        assertTrue(equipment.sell(state, sale));
        assertEquals(before + sale.sellPrice, state.coins);
        assertFalse(state.inventory.contains(sale));
        assertFalse(equipment.sell(state, unknown));
        assertTrue(state.inventory.contains(unknown));
    }
}
