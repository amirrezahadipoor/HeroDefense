package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.InventoryEquipmentSystem;
import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.model.ItemTier;
import com.amirrezahadipoor.herodefense.settings.GameSettings;
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

    @Test
    void anvilButtonReforgesSelectedRareItemsAndRefusesCommons() {
        GameState state = GameState.newRun(73L);
        Item bow = EquipmentCatalog.byId("starfall_bow").createItem();
        Item cap = EquipmentCatalog.byId("leather_cap").createItem();
        state.inventory.add(bow);
        state.inventory.add(cap);
        state.coins = 5_000;
        controller.open();
        float forgeX = InventoryTouchLayout.FORGE_X + InventoryTouchLayout.ACTION_WIDTH * 0.5f;
        float forgeY = InventoryTouchLayout.ACTION_Y + InventoryTouchLayout.ACTION_HEIGHT * 0.5f;

        // Nothing selected: the anvil does nothing.
        assertEquals(InventoryTouchController.Action.NONE, controller.tap(state, forgeX, forgeY));

        controller.tap(state, 200f, 600f); // bow
        assertEquals(InventoryTouchController.Action.FORGED, controller.tap(state, forgeX, forgeY));
        assertEquals(1, bow.upgradeLevel);
        assertEquals("Starfall Bow +1", bow.name);
        assertTrue(controller.feedbackMessage().startsWith("REFORGED  |  Starfall Bow +1"));
        assertEquals(bow, controller.selectedItem(state)); // selection is kept for repeat taps

        controller.tap(state, 200f, 500f); // cap
        assertEquals(InventoryTouchController.Action.FORGE_REFUSED, controller.tap(state, forgeX, forgeY));
        assertEquals("ANVIL TAKES RARE & LEGENDARY ONLY", controller.feedbackMessage());
        assertEquals(0, cap.upgradeLevel);
    }

    @Test
    void autoSellChipsToggleSettingsAndAreInertWithoutSettings() {
        GameState state = GameState.newRun(74L);
        GameSettings settings = new GameSettings();
        controller.open();
        float y = InventoryTouchLayout.AUTO_SELL_Y + InventoryTouchLayout.AUTO_SELL_HEIGHT * 0.5f;
        float rareX = InventoryTouchLayout.autoSellChipX(2) + InventoryTouchLayout.AUTO_SELL_WIDTH * 0.5f;
        assertEquals(ItemTier.RARE, InventoryTouchLayout.autoSellTierAt(rareX, y));
        assertEquals(InventoryTouchController.Action.NONE, controller.tap(state, null, rareX, y));
        assertFalse(settings.autoSellRare);
        assertEquals(InventoryTouchController.Action.AUTO_SELL_TOGGLED, controller.tap(state, settings, rareX, y));
        assertTrue(settings.autoSellRare);
        assertEquals("AUTO-SELL RARE  |  ON", controller.feedbackMessage());
        assertEquals(InventoryTouchController.Action.AUTO_SELL_TOGGLED, controller.tap(state, settings, rareX, y));
        assertFalse(settings.autoSellRare);
        assertEquals("AUTO-SELL RARE  |  OFF", controller.feedbackMessage());
        // Chips never overlap the close button or the loadout slots.
        assertTrue(InventoryTouchLayout.autoSellChipX(2) + InventoryTouchLayout.AUTO_SELL_WIDTH
            <= InventoryTouchLayout.CLOSE_X);
        assertTrue(InventoryTouchLayout.AUTO_SELL_Y >= InventoryTouchLayout.SLOT_TOP_Y);
        assertTrue(InventoryTouchLayout.AUTO_SELL_Y + InventoryTouchLayout.AUTO_SELL_HEIGHT
            <= InventoryTouchLayout.CLOSE_Y + InventoryTouchLayout.CLOSE_SIZE);
        // The three action buttons never overlap and stay inside the 720 design width.
        assertTrue(InventoryTouchLayout.EQUIP_X + InventoryTouchLayout.ACTION_WIDTH
            <= InventoryTouchLayout.FORGE_X);
        assertTrue(InventoryTouchLayout.FORGE_X + InventoryTouchLayout.ACTION_WIDTH
            <= InventoryTouchLayout.SELL_X);
        assertTrue(InventoryTouchLayout.SELL_X + InventoryTouchLayout.ACTION_WIDTH <= 720f);
    }
}
