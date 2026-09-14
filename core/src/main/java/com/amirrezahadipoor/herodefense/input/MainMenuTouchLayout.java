package com.amirrezahadipoor.herodefense.input;

/** Portrait main-menu bounds for tap-only actions including Root Network. */
public final class MainMenuTouchLayout {
    public enum Action { NONE, NEW_GAME, CONTINUE, SETTINGS, ROOT_NETWORK, CODEX }

    public static final float BUTTON_X = 120f;
    public static final float BUTTON_WIDTH = 480f;
    public static final float BUTTON_HEIGHT = 120f;

    private MainMenuTouchLayout() {
    }

    public static Action actionAt(float x, float y, boolean continueAvailable) {
        if (x < BUTTON_X || x > BUTTON_X + BUTTON_WIDTH) return Action.NONE;
        if (between(y, 780f)) return Action.NEW_GAME;
        if (between(y, 620f)) return continueAvailable ? Action.CONTINUE : Action.NONE;
        if (between(y, 460f)) return Action.ROOT_NETWORK;
        if (between(y, 300f)) return Action.CODEX;
        if (between(y, 140f)) return Action.SETTINGS;
        return Action.NONE;
    }

    private static boolean between(float y, float bottom) {
        return y >= bottom && y <= bottom + BUTTON_HEIGHT;
    }
}
