package com.amirrezahadipoor.herodefense.input;

/** Portrait main-menu bounds for the three required tap-only actions. */
public final class MainMenuTouchLayout {
    public enum Action { NONE, NEW_GAME, CONTINUE, SETTINGS }

    public static final float BUTTON_X = 120f;
    public static final float BUTTON_WIDTH = 480f;
    public static final float BUTTON_HEIGHT = 140f;

    private MainMenuTouchLayout() {
    }

    public static Action actionAt(float x, float y, boolean continueAvailable) {
        if (x < BUTTON_X || x > BUTTON_X + BUTTON_WIDTH) return Action.NONE;
        if (between(y, 690f)) return Action.NEW_GAME;
        if (between(y, 500f)) return continueAvailable ? Action.CONTINUE : Action.NONE;
        if (between(y, 310f)) return Action.SETTINGS;
        return Action.NONE;
    }

    private static boolean between(float y, float bottom) {
        return y >= bottom && y <= bottom + BUTTON_HEIGHT;
    }
}
