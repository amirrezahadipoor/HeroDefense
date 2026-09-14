package com.amirrezahadipoor.herodefense.input;

/** Shared pause-menu hit targets for touch-only navigation including Root Network. */
public final class PauseTouchLayout {
    public static final float BUTTON_X = 120f;
    public static final float BUTTON_WIDTH = 480f;
    public static final float RESUME_Y = 320f;
    public static final float RESUME_HEIGHT = 200f;
    public static final float ROOT_Y = 560f;
    public static final float INVENTORY_Y = 720f;
    public static final float SHOP_Y = 880f;
    public static final float CODEX_Y = 90f;
    public static final float SECONDARY_HEIGHT = 130f;

    private PauseTouchLayout() {
    }

    public static boolean shopAt(float x, float y) {
        return inside(x, y, SHOP_Y, SECONDARY_HEIGHT);
    }

    public static boolean inventoryAt(float x, float y) {
        return inside(x, y, INVENTORY_Y, SECONDARY_HEIGHT);
    }

    public static boolean rootAt(float x, float y) {
        return inside(x, y, ROOT_Y, SECONDARY_HEIGHT);
    }

    public static boolean codexAt(float x, float y) {
        return inside(x, y, CODEX_Y, SECONDARY_HEIGHT);
    }

    public static boolean resumeAt(float x, float y) {
        return inside(x, y, RESUME_Y, RESUME_HEIGHT);
    }

    private static boolean inside(float x, float y, float bottom, float height) {
        return x >= BUTTON_X && x <= BUTTON_X + BUTTON_WIDTH
            && y >= bottom && y <= bottom + height;
    }
}
