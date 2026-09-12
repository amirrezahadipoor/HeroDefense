package com.amirrezahadipoor.herodefense.input;

/** Shared phone-HUD bounds with 120-by-100 world-unit touch targets. */
public final class HudTouchLayout {
    public static final float BUTTON_Y = 1065f;
    public static final float BUTTON_WIDTH = 120f;
    public static final float BUTTON_HEIGHT = 100f;
    public static final float SPEED_X = 430f;
    public static final float PAUSE_X = 570f;
    public static final float UTILITY_BUTTON_Y = 24f;
    public static final float UTILITY_BUTTON_WIDTH = 150f;
    public static final float UTILITY_BUTTON_HEIGHT = 104f;
    public static final float INVENTORY_X = 195f;
    public static final float SHOP_X = 375f;

    private HudTouchLayout() {
    }

    public static boolean speedAt(float x, float y) {
        return inside(x, y, SPEED_X);
    }

    public static boolean pauseAt(float x, float y) {
        return inside(x, y, PAUSE_X);
    }

    public static boolean inventoryAt(float x, float y) {
        return insideUtility(x, y, INVENTORY_X);
    }

    public static boolean shopAt(float x, float y) {
        return insideUtility(x, y, SHOP_X);
    }

    private static boolean inside(float x, float y, float left) {
        return x >= left && x <= left + BUTTON_WIDTH
            && y >= BUTTON_Y && y <= BUTTON_Y + BUTTON_HEIGHT;
    }

    private static boolean insideUtility(float x, float y, float left) {
        return x >= left && x <= left + UTILITY_BUTTON_WIDTH
            && y >= UTILITY_BUTTON_Y && y <= UTILITY_BUTTON_Y + UTILITY_BUTTON_HEIGHT;
    }
}
