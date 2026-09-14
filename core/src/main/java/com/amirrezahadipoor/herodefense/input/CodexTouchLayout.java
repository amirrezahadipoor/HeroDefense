package com.amirrezahadipoor.herodefense.input;

/** Portrait hit-target geometry for the Grove Codex list. */
public final class CodexTouchLayout {
    public static final float CLOSE_X = 570f;
    public static final float CLOSE_Y = 1110f;
    public static final float CLOSE_SIZE = 100f;

    public static final float LIST_X = 40f;
    public static final float LIST_WIDTH = 640f;
    public static final float LIST_TOP_Y = 1010f;
    public static final float LIST_ROW_HEIGHT = 74f;
    public static final float LIST_ROW_STRIDE = 84f;
    public static final int VISIBLE_ROWS = 6;

    private CodexTouchLayout() {
    }

    public static float rowBottom(int visibleRow) {
        return LIST_TOP_Y - LIST_ROW_HEIGHT - visibleRow * LIST_ROW_STRIDE;
    }

    /** Visible list row containing the point, or -1. */
    public static int visibleRowAt(float x, float y) {
        if (x < LIST_X || x > LIST_X + LIST_WIDTH) return -1;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            if (inside(x, y, LIST_X, rowBottom(row), LIST_WIDTH, LIST_ROW_HEIGHT)) return row;
        }
        return -1;
    }

    public static boolean closeAt(float x, float y) {
        return inside(x, y, CLOSE_X, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE);
    }

    private static boolean inside(
        float x, float y, float left, float bottom, float width, float height
    ) {
        return x >= left && x <= left + width && y >= bottom && y <= bottom + height;
    }
}
