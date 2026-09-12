package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.gameplay.InventoryEquipmentSystem;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;

/** Touch/touch-drag controller for viewing, equipping, unequipping, and selling. */
public final class InventoryTouchController {
    public enum Action {
        NONE,
        SELECTED,
        EQUIPPED,
        UNEQUIPPED,
        SOLD,
        CLOSED
    }

    private static final float ROW_DRAG_THRESHOLD = 55f;
    private final InventoryEquipmentSystem equipmentSystem;
    private volatile boolean open;
    private int selectedIndex = -1;
    private int firstVisibleIndex;
    private float accumulatedDrag;

    public InventoryTouchController(InventoryEquipmentSystem equipmentSystem) {
        this.equipmentSystem = equipmentSystem;
    }

    public void open() {
        open = true;
        selectedIndex = -1;
        firstVisibleIndex = 0;
        accumulatedDrag = 0f;
    }

    public boolean isOpen() {
        return open;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public int firstVisibleIndex() {
        return firstVisibleIndex;
    }

    public Action tap(GameState state, float x, float y) {
        if (!open || state == null) return Action.NONE;
        if (InventoryTouchLayout.closeAt(x, y)) {
            open = false;
            return Action.CLOSED;
        }

        EquipmentSlot slot = InventoryTouchLayout.slotAt(x, y);
        if (slot != null) {
            Item removed = equipmentSystem.unequip(state, slot);
            clampAfterMutation(state);
            return removed == null ? Action.NONE : Action.UNEQUIPPED;
        }

        int visibleRow = InventoryTouchLayout.visibleInventoryRowAt(x, y);
        if (visibleRow >= 0) {
            int index = firstVisibleIndex + visibleRow;
            if (index < state.inventory.size()) {
                selectedIndex = index;
                return Action.SELECTED;
            }
            return Action.NONE;
        }

        if (InventoryTouchLayout.equipAt(x, y)) {
            Item selected = selectedItem(state);
            if (selected != null && equipmentSystem.equip(state, selected)) {
                selectedIndex = -1;
                clampAfterMutation(state);
                return Action.EQUIPPED;
            }
            return Action.NONE;
        }

        if (InventoryTouchLayout.sellAt(x, y)) {
            Item selected = selectedItem(state);
            if (equipmentSystem.sell(state, selected)) {
                selectedIndex = -1;
                clampAfterMutation(state);
                return Action.SOLD;
            }
        }
        return Action.NONE;
    }

    public void drag(GameState state, float deltaY) {
        if (!open || state == null) return;
        accumulatedDrag += deltaY;
        while (accumulatedDrag >= ROW_DRAG_THRESHOLD) {
            firstVisibleIndex = Math.min(maxFirstVisible(state), firstVisibleIndex + 1);
            accumulatedDrag -= ROW_DRAG_THRESHOLD;
        }
        while (accumulatedDrag <= -ROW_DRAG_THRESHOLD) {
            firstVisibleIndex = Math.max(0, firstVisibleIndex - 1);
            accumulatedDrag += ROW_DRAG_THRESHOLD;
        }
    }

    public Item selectedItem(GameState state) {
        if (state == null || selectedIndex < 0 || selectedIndex >= state.inventory.size()) {
            return null;
        }
        return state.inventory.get(selectedIndex);
    }

    private void clampAfterMutation(GameState state) {
        firstVisibleIndex = Math.min(firstVisibleIndex, maxFirstVisible(state));
        if (selectedIndex >= state.inventory.size()) selectedIndex = -1;
    }

    private static int maxFirstVisible(GameState state) {
        return Math.max(0, state.inventory.size() - InventoryTouchLayout.VISIBLE_ROWS);
    }
}
