package com.amirrezahadipoor.herodefense.input;

/** Large portrait restart target for the run summary. */
public final class GameOverTouchLayout {
    public static final float RESTART_X = 120f;
    public static final float RESTART_Y = 210f;
    public static final float RESTART_WIDTH = 480f;
    public static final float RESTART_HEIGHT = 160f;

    private GameOverTouchLayout() {
    }

    public static boolean restartAt(float x, float y) {
        return x >= RESTART_X && x <= RESTART_X + RESTART_WIDTH
            && y >= RESTART_Y && y <= RESTART_Y + RESTART_HEIGHT;
    }
}
