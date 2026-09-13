package com.amirrezahadipoor.herodefense.input;

/** Shared pause-menu hit targets for touch-only navigation. */
public final class PauseTouchLayout {
    public static final float BUTTON_X = 120f;
    public static final float BUTTON_WIDTH = 480f;
    public static final float RESUME_Y = 480f;
    public static final float RESUME_HEIGHT = 240f;
    public static final float INVENTORY_Y = 760f;
    public static final float SHOP_Y = 930f;
    public static final float SECONDARY_HEIGHT = 140f;

    private PauseTouchLayout() {
    }

    public static boolean shopAt(float x, float y) {
        return inside(x, y, SHOP_Y, SECONDARY_HEIGHT);
    }

    public static boolean inventoryAt(float x, float y) {
        return inside(x, y, INVENTORY_Y, SECONDARY_HEIGHT);
    }

    public static boolean resumeAt(float x, float y) {
        return inside(x, y, RESUME_Y, RESUME_HEIGHT);
    }

    private static boolean inside(float x, float y, float bottom, float height) {
        return x >= BUTTON_X && x <= BUTTON_X + BUTTON_WIDTH
            && y >= bottom && y <= bottom + height;
    }
}
