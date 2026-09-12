package com.amirrezahadipoor.herodefense.input;

/** Shared pause-menu hit targets for touch-only navigation. */
public final class PauseTouchLayout {
    public static final float BUTTON_X = 180f;
    public static final float BUTTON_WIDTH = 360f;

    private PauseTouchLayout() {
    }

    public static boolean shopAt(float x, float y) {
        return inside(x, y, 930f, 140f);
    }

    public static boolean inventoryAt(float x, float y) {
        return inside(x, y, 760f, 140f);
    }

    public static boolean resumeAt(float x, float y) {
        return inside(x, y, 480f, 240f);
    }

    private static boolean inside(float x, float y, float bottom, float height) {
        return x >= BUTTON_X && x <= BUTTON_X + BUTTON_WIDTH
            && y >= bottom && y <= bottom + height;
    }
}
