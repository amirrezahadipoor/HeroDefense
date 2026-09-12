package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.model.HeroStat;

/** Shared portrait shop bounds; every action is exposed through generous tap targets. */
public final class StatShopTouchLayout {
    public static final float CLOSE_X = 570f;
    public static final float CLOSE_Y = 1120f;
    public static final float CLOSE_SIZE = 100f;
    public static final float ROW_X = 55f;
    public static final float ROW_WIDTH = 610f;
    public static final float ROW_TOP = 1060f;
    public static final float ROW_HEIGHT = 135f;
    public static final float ROW_STRIDE = 155f;

    private StatShopTouchLayout() {
    }

    public static HeroStat statAt(float x, float y) {
        if (x < ROW_X || x > ROW_X + ROW_WIDTH) return null;
        for (int index = 0; index < HeroStat.values().length; index++) {
            float bottom = ROW_TOP - ROW_HEIGHT - index * ROW_STRIDE;
            if (y >= bottom && y <= bottom + ROW_HEIGHT) return HeroStat.values()[index];
        }
        return null;
    }

    public static boolean closeAt(float x, float y) {
        return x >= CLOSE_X && x <= CLOSE_X + CLOSE_SIZE
            && y >= CLOSE_Y && y <= CLOSE_Y + CLOSE_SIZE;
    }
}
