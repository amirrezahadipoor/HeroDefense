package com.amirrezahadipoor.herodefense.input;

/** Shared phone-HUD bounds with 120-by-100 world-unit touch targets. */
public final class HudTouchLayout {
    public static final float BUTTON_Y = 1065f;
    public static final float BUTTON_WIDTH = 120f;
    public static final float BUTTON_HEIGHT = 100f;
    public static final float SPEED_X = 430f;
    public static final float PAUSE_X = 570f;

    private HudTouchLayout() {
    }

    public static boolean speedAt(float x, float y) {
        return inside(x, y, SPEED_X);
    }

    public static boolean pauseAt(float x, float y) {
        return inside(x, y, PAUSE_X);
    }

    private static boolean inside(float x, float y, float left) {
        return x >= left && x <= left + BUTTON_WIDTH
            && y >= BUTTON_Y && y <= BUTTON_Y + BUTTON_HEIGHT;
    }
}
