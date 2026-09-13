package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.render.ScreenEdges;

/**
 * Shared phone-HUD bounds with 120-by-100 world-unit touch targets.
 *
 * <p>The status row hugs the physical top edge and the utility row hugs the physical bottom
 * edge: on panels taller than 16:9 both rows slide outward by up to {@link #MAX_EDGE_SHIFT}
 * so the HUD reads as part of the phone rather than floating mid-screen.
 */
public final class HudTouchLayout {
    public static final float DESIGN_BUTTON_Y = 1065f;
    public static final float BUTTON_WIDTH = 120f;
    public static final float BUTTON_HEIGHT = 100f;
    public static final float SPEED_X = 430f;
    public static final float PAUSE_X = 570f;
    public static final float DESIGN_UTILITY_BUTTON_Y = 24f;
    public static final float UTILITY_BUTTON_WIDTH = 150f;
    public static final float UTILITY_BUTTON_HEIGHT = 104f;
    public static final float INVENTORY_X = 195f;
    public static final float SHOP_X = 375f;
    /** Upper bound on how far either HUD row may leave the 1280 design grid. */
    public static final float MAX_EDGE_SHIFT = 72f;

    private HudTouchLayout() {
    }

    /** Positive world units the status row moves up on tall panels. */
    public static float topShift() {
        return Math.min(MAX_EDGE_SHIFT, Math.max(0f, ScreenEdges.top() - 1280f));
    }

    /** Positive world units the utility row moves down on tall panels. */
    public static float bottomShift() {
        return Math.min(MAX_EDGE_SHIFT, Math.max(0f, -ScreenEdges.bottom()));
    }

    public static float buttonY() {
        return DESIGN_BUTTON_Y + topShift();
    }

    public static float utilityButtonY() {
        return DESIGN_UTILITY_BUTTON_Y - bottomShift();
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
        float bottom = buttonY();
        return x >= left && x <= left + BUTTON_WIDTH
            && y >= bottom && y <= bottom + BUTTON_HEIGHT;
    }

    private static boolean insideUtility(float x, float y, float left) {
        float bottom = utilityButtonY();
        return x >= left && x <= left + UTILITY_BUTTON_WIDTH
            && y >= bottom && y <= bottom + UTILITY_BUTTON_HEIGHT;
    }
}
