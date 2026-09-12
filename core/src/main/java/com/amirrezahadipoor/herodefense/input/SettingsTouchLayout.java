package com.amirrezahadipoor.herodefense.input;

/** Tap-only settings bounds shared with the renderer. */
public final class SettingsTouchLayout {
    public enum Action { NONE, TOGGLE_SOUND, TOGGLE_MUSIC, CLOSE }

    public static final float ROW_X = 100f;
    public static final float ROW_WIDTH = 520f;
    public static final float ROW_HEIGHT = 150f;

    private SettingsTouchLayout() {
    }

    public static Action actionAt(float x, float y) {
        if (x >= 570f && x <= 670f && y >= 1120f && y <= 1220f) return Action.CLOSE;
        if (x < ROW_X || x > ROW_X + ROW_WIDTH) return Action.NONE;
        if (y >= 700f && y <= 700f + ROW_HEIGHT) return Action.TOGGLE_SOUND;
        if (y >= 500f && y <= 500f + ROW_HEIGHT) return Action.TOGGLE_MUSIC;
        return Action.NONE;
    }
}
