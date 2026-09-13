package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.InventoryEquipmentSystem;
import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import org.junit.jupiter.api.Test;

final class InventoryTouchControllerTest {
    private final InventoryTouchController controller =
        new InventoryTouchController(new InventoryEquipmentSystem());

    @Test
    void tapPathSelectsEquipsUnequipsAndSellsNonPotionItems() {
        GameState state = GameState.newRun(70L);
        Item bow = EquipmentCatalog.byId("moonwood_longbow").createItem();
        Item cap = EquipmentCatalog.byId("leather_cap").createItem();
        state.inventory.add(bow);
        state.inventory.add(cap);
        controller.open();

        assertEquals(
            InventoryTouchController.Action.SELECTED,
            controller.tap(state, 200f, 600f)
        );
        assertEquals(
            InventoryTouchController.Action.EQUIPPED,
            controller.tap(state, 180f, 130f)
        );
        assertEquals(bow, state.equippedItems.get(EquipmentSlot.WEAPON.name()));

        assertEquals(
            InventoryTouchController.Action.UNEQUIPPED,
            controller.tap(state, 180f, 940f)
        );
        assertTrue(state.inventory.contains(bow));

        // The cap remains first after the bow was removed and then appended on unequip.
        controller.tap(state, 200f, 600f);
        int coinsBefore = state.coins;
        assertEquals(
            InventoryTouchController.Action.SOLD,
            controller.tap(state, 500f, 130f)
        );
        assertEquals(coinsBefore + cap.sellPrice, state.coins);
        assertFalse(state.inventory.contains(cap));
    }

    @Test
    void equipUnequipAndSellFeedbackIsSpecificAndExpiresInRealTime() {
        GameState state = GameState.newRun(72L);
        Item bow = EquipmentCatalog.byId("moonwood_longbow").createItem();
        state.inventory.add(bow);
        controller.open();
        controller.tap(state, 200f, 600f);
        controller.tap(state, 180f, 130f);
        assertEquals(InventoryTouchController.Action.EQUIPPED, controller.feedbackAction());
        assertEquals("EQUIPPED  |  " + bow.name, controller.feedbackMessage());

        controller.tap(state, 180f, 940f);
        assertEquals(InventoryTouchController.Action.UNEQUIPPED, controller.feedbackAction());
        assertEquals("RETURNED TO BAG  |  " + bow.name, controller.feedbackMessage());

        controller.tap(state, 200f, 600f);
        controller.tap(state, 500f, 130f);
        assertEquals(InventoryTouchController.Action.SOLD, controller.feedbackAction());
        assertEquals("SOLD  |  +$ " + bow.sellPrice + "  |  " + bow.name,
            controller.feedbackMessage());
        assertEquals(1f, controller.feedbackAlpha());
        controller.update(1.1f);
        assertTrue(controller.feedbackAlpha() > 0f);
        controller.update(0.2f);
        assertEquals(InventoryTouchController.Action.NONE, controller.feedbackAction());
        assertEquals(null, controller.feedbackMessage());
        assertEquals(0f, controller.feedbackAlpha());
    }

    @Test
    void dragScrollsRowsAndCloseUsesATapTarget() {
        GameState state = GameState.newRun(71L);
        for (int index = 0; index < 8; index++) {
            state.inventory.add(EquipmentCatalog.all().get(index).createItem());
        }
        controller.open();
        controller.drag(state, 120f);
        assertEquals(2, controller.firstVisibleIndex());
        controller.drag(state, -70f);
        assertEquals(1, controller.firstVisibleIndex());

        assertEquals(
            InventoryTouchController.Action.CLOSED,
            controller.tap(state, 620f, 1160f)
        );
        assertFalse(controller.isOpen());
    }
}
