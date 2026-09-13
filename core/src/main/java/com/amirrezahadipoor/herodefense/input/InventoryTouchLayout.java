package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.ItemTier;

/** Portrait hit-target geometry shared by inventory rendering and touch handling. */
public final class InventoryTouchLayout {
    public static final float CLOSE_X = 570f;
    public static final float CLOSE_Y = 1110f;
    public static final float CLOSE_SIZE = 100f;

    public static final float SLOT_LEFT_X = 40f;
    public static final float SLOT_RIGHT_X = 380f;
    public static final float SLOT_TOP_Y = 990f;
    public static final float SLOT_WIDTH = 300f;
    public static final float SLOT_HEIGHT = 90f;
    public static final float SLOT_ROW_STRIDE = 110f;

    public static final float LIST_X = 40f;
    public static final float LIST_WIDTH = 320f;
    public static final float LIST_TOP_Y = 650f;
    public static final float LIST_ROW_HEIGHT = 90f;
    public static final float LIST_ROW_STRIDE = 100f;
    public static final int VISIBLE_ROWS = 4;

    public static final float DETAILS_X = 380f;
    public static final float DETAILS_Y = 250f;
    public static final float DETAILS_WIDTH = 300f;
    public static final float DETAILS_HEIGHT = 400f;

    public static final float EQUIP_X = 30f;
    public static final float FORGE_X = 260f;
    public static final float SELL_X = 490f;
    public static final float ACTION_Y = 80f;
    public static final float ACTION_WIDTH = 200f;
    public static final float ACTION_HEIGHT = 110f;

    /** Auto-sell chips (Common, Uncommon, Rare) in the header band left of the close button. */
    public static final float AUTO_SELL_LABEL_X = 40f;
    public static final float AUTO_SELL_FIRST_X = 200f;
    public static final float AUTO_SELL_STRIDE = 120f;
    public static final float AUTO_SELL_Y = 1046f;
    public static final float AUTO_SELL_WIDTH = 110f;
    public static final float AUTO_SELL_HEIGHT = 80f;
    public static final ItemTier[] AUTO_SELL_TIERS = {ItemTier.COMMON, ItemTier.UNCOMMON, ItemTier.RARE};

    private InventoryTouchLayout() {
    }

    public static EquipmentSlot slotAt(float x, float y) {
        for (int index = 0; index < EquipmentSlot.values().length; index++) {
            int column = index % 2;
            int row = index / 2;
            float left = column == 0 ? SLOT_LEFT_X : SLOT_RIGHT_X;
            float bottom = SLOT_TOP_Y - SLOT_HEIGHT - row * SLOT_ROW_STRIDE;
            if (inside(x, y, left, bottom, SLOT_WIDTH, SLOT_HEIGHT)) {
                return EquipmentSlot.values()[index];
            }
        }
        return null;
    }

    public static int visibleInventoryRowAt(float x, float y) {
        if (x < LIST_X || x > LIST_X + LIST_WIDTH) return -1;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            float bottom = LIST_TOP_Y - LIST_ROW_HEIGHT - row * LIST_ROW_STRIDE;
            if (inside(x, y, LIST_X, bottom, LIST_WIDTH, LIST_ROW_HEIGHT)) return row;
        }
        return -1;
    }

    public static boolean closeAt(float x, float y) {
        return inside(x, y, CLOSE_X, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE);
    }

    public static boolean equipAt(float x, float y) {
        return inside(x, y, EQUIP_X, ACTION_Y, ACTION_WIDTH, ACTION_HEIGHT);
    }

    public static boolean sellAt(float x, float y) {
        return inside(x, y, SELL_X, ACTION_Y, ACTION_WIDTH, ACTION_HEIGHT);
    }

    public static boolean forgeAt(float x, float y) {
        return inside(x, y, FORGE_X, ACTION_Y, ACTION_WIDTH, ACTION_HEIGHT);
    }

    public static float autoSellChipX(int index) {
        return AUTO_SELL_FIRST_X + index * AUTO_SELL_STRIDE;
    }

    /** Tier whose auto-sell chip contains the point, or null. */
    public static ItemTier autoSellTierAt(float x, float y) {
        for (int index = 0; index < AUTO_SELL_TIERS.length; index++) {
            if (inside(x, y, autoSellChipX(index), AUTO_SELL_Y, AUTO_SELL_WIDTH, AUTO_SELL_HEIGHT)) {
                return AUTO_SELL_TIERS[index];
            }
        }
        return null;
    }

    private static boolean inside(
        float x, float y, float left, float bottom, float width, float height
    ) {
        return x >= left && x <= left + width && y >= bottom && y <= bottom + height;
    }
}
