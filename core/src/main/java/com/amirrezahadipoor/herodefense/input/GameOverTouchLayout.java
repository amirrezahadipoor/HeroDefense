package com.amirrezahadipoor.herodefense.input;

/** Large portrait restart target for the run summary plus ascend + root network. */
public final class GameOverTouchLayout {
    public static final float RESTART_X = 120f;
    public static final float RESTART_Y = 260f;
    public static final float RESTART_WIDTH = 480f;
    public static final float RESTART_HEIGHT = 96f;

    public static final float ASCEND_X = 120f;
    public static final float ASCEND_Y = 150f;
    public static final float ASCEND_WIDTH = 480f;
    public static final float ASCEND_HEIGHT = 96f;

    public static final float ROOT_X = 120f;
    public static final float ROOT_Y = 40f;
    public static final float ROOT_WIDTH = 480f;
    public static final float ROOT_HEIGHT = 96f;

    private GameOverTouchLayout() {
    }

    public static boolean restartAt(float x, float y) {
        return x >= RESTART_X && x <= RESTART_X + RESTART_WIDTH
            && y >= RESTART_Y && y <= RESTART_Y + RESTART_HEIGHT;
    }

    public static boolean ascendAt(float x, float y) {
        return x >= ASCEND_X && x <= ASCEND_X + ASCEND_WIDTH
            && y >= ASCEND_Y && y <= ASCEND_Y + ASCEND_HEIGHT;
    }

    public static boolean rootAt(float x, float y) {
        return x >= ROOT_X && x <= ROOT_X + ROOT_WIDTH
            && y >= ROOT_Y && y <= ROOT_Y + ROOT_HEIGHT;
    }

    public enum Action { NONE, RESTART, ASCEND, ROOT_NETWORK }

    public static Action actionAt(float x, float y) {
        if (rootAt(x, y)) return Action.ROOT_NETWORK;
        if (ascendAt(x, y)) return Action.ASCEND;
        if (restartAt(x, y)) return Action.RESTART;
        return Action.NONE;
    }
}
