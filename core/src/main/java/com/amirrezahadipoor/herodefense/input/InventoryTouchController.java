package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.gameplay.InventoryEquipmentSystem;
import com.amirrezahadipoor.herodefense.gameplay.ItemForgeSystem;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.model.ItemTier;
import com.amirrezahadipoor.herodefense.settings.GameSettings;

import java.util.Locale;

/** Touch/touch-drag controller for viewing, equipping, unequipping, reforging, and selling. */
public final class InventoryTouchController {
    public enum Action {
        NONE,
        SELECTED,
        EQUIPPED,
        UNEQUIPPED,
        SOLD,
        FORGED,
        FORGE_REFUSED,
        AUTO_SELL_TOGGLED,
        CLOSED
    }

    private static final float ROW_DRAG_THRESHOLD = 55f;
    private static final float FEEDBACK_DURATION_SECONDS = 1.25f;
    private final InventoryEquipmentSystem equipmentSystem;
    private final ItemForgeSystem forgeSystem;
    private volatile boolean open;
    private int selectedIndex = -1;
    private int firstVisibleIndex;
    private float accumulatedDrag;
    private Action feedbackAction = Action.NONE;
    private String feedbackItemName;
    private int feedbackCoinDelta;
    private float feedbackRemainingSeconds;

    public InventoryTouchController(InventoryEquipmentSystem equipmentSystem) {
        this(equipmentSystem, new ItemForgeSystem());
    }

    public InventoryTouchController(InventoryEquipmentSystem equipmentSystem, ItemForgeSystem forgeSystem) {
        this.equipmentSystem = equipmentSystem;
        this.forgeSystem = forgeSystem;
    }

    public ItemForgeSystem forgeSystem() {
        return forgeSystem;
    }

    public void open() {
        open = true;
        selectedIndex = -1;
        firstVisibleIndex = 0;
        accumulatedDrag = 0f;
        clearFeedback();
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

    public void update(float realDeltaSeconds) {
        forgeSystem.update(realDeltaSeconds);
        if (realDeltaSeconds <= 0f || feedbackRemainingSeconds <= 0f) return;
        feedbackRemainingSeconds = Math.max(0f, feedbackRemainingSeconds - realDeltaSeconds);
        if (feedbackRemainingSeconds == 0f) clearFeedback();
    }

    public String feedbackMessage() {
        if (feedbackRemainingSeconds <= 0f || feedbackAction == Action.NONE) return null;
        String name = feedbackItemName == null ? "Item" : feedbackItemName;
        return switch (feedbackAction) {
            case EQUIPPED -> "EQUIPPED  |  " + name;
            case UNEQUIPPED -> "RETURNED TO BAG  |  " + name;
            case SOLD -> "SOLD  |  +$ " + feedbackCoinDelta + "  |  " + name;
            case FORGED, FORGE_REFUSED -> forgeSystem.feedbackMessage();
            case AUTO_SELL_TOGGLED -> name;
            default -> null;
        };
    }

    public Action feedbackAction() {
        return feedbackRemainingSeconds > 0f ? feedbackAction : Action.NONE;
    }

    public float feedbackAlpha() {
        if (feedbackRemainingSeconds <= 0f) return 0f;
        return Math.min(1f, feedbackRemainingSeconds / 0.20f);
    }

    public Action tap(GameState state, float x, float y) {
        return tap(state, null, x, y);
    }

    /** {@code settings} may be null when the auto-sell chips are not interactive. */
    public Action tap(GameState state, GameSettings settings, float x, float y) {
        if (!open || state == null) return Action.NONE;
        if (InventoryTouchLayout.closeAt(x, y)) {
            open = false;
            return Action.CLOSED;
        }

        ItemTier autoSellTier = InventoryTouchLayout.autoSellTierAt(x, y);
        if (autoSellTier != null) {
            if (settings == null || !settings.toggleAutoSell(autoSellTier)) return Action.NONE;
            String tierName = autoSellTier.name().charAt(0)
                + autoSellTier.name().substring(1).toLowerCase(Locale.ROOT);
            showFeedback(Action.AUTO_SELL_TOGGLED,
                "AUTO-SELL " + tierName.toUpperCase(Locale.ROOT)
                    + (settings.autoSells(autoSellTier) ? "  |  ON" : "  |  OFF"), 0);
            return Action.AUTO_SELL_TOGGLED;
        }

        if (InventoryTouchLayout.forgeAt(x, y)) {
            Item selected = selectedItem(state);
            if (selected == null) return Action.NONE;
            ItemForgeSystem.Result result = forgeSystem.forge(state, selected);
            Action action = result == ItemForgeSystem.Result.FORGED ? Action.FORGED : Action.FORGE_REFUSED;
            showFeedback(action, selected.name, 0);
            return action;
        }

        EquipmentSlot slot = InventoryTouchLayout.slotAt(x, y);
        if (slot != null) {
            Item removed = equipmentSystem.unequip(state, slot);
            clampAfterMutation(state);
            if (removed == null) return Action.NONE;
            showFeedback(Action.UNEQUIPPED, removed.name, 0);
            return Action.UNEQUIPPED;
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
                showFeedback(Action.EQUIPPED, selected.name, 0);
                selectedIndex = -1;
                clampAfterMutation(state);
                return Action.EQUIPPED;
            }
            return Action.NONE;
        }

        if (InventoryTouchLayout.sellAt(x, y)) {
            Item selected = selectedItem(state);
            if (selected != null && equipmentSystem.sell(state, selected)) {
                showFeedback(Action.SOLD, selected.name, selected.sellPrice);
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

    private void showFeedback(Action action, String itemName, int coinDelta) {
        feedbackAction = action;
        feedbackItemName = itemName;
        feedbackCoinDelta = Math.max(0, coinDelta);
        feedbackRemainingSeconds = FEEDBACK_DURATION_SECONDS;
    }

    private void clearFeedback() {
        feedbackAction = Action.NONE;
        feedbackItemName = null;
        feedbackCoinDelta = 0;
        feedbackRemainingSeconds = 0f;
    }

    private void clampAfterMutation(GameState state) {
        firstVisibleIndex = Math.min(firstVisibleIndex, maxFirstVisible(state));
        if (selectedIndex >= state.inventory.size()) selectedIndex = -1;
    }

    private static int maxFirstVisible(GameState state) {
        return Math.max(0, state.inventory.size() - InventoryTouchLayout.VISIBLE_ROWS);
    }
}
