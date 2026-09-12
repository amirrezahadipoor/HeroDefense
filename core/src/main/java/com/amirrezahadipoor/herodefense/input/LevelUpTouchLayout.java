package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.model.HeroStat;

/** Fixed generous tap rows for the five-stat portrait level-up overlay. */
public final class LevelUpTouchLayout {
    public static final float LEFT = 90f;
    public static final float RIGHT = 630f;
    public static final float BOTTOM = 230f;
    public static final float BUTTON_HEIGHT = 130f;
    public static final float ROW_STRIDE = 150f;

    private LevelUpTouchLayout() {
    }

    public static HeroStat statAt(float worldX, float worldY) {
        if (worldX < LEFT || worldX > RIGHT || worldY < BOTTOM) {
            return null;
        }
        int row = (int) ((worldY - BOTTOM) / ROW_STRIDE);
        if (row < 0 || row >= HeroStat.values().length) {
            return null;
        }
        float rowBottom = BOTTOM + row * ROW_STRIDE;
        if (worldY > rowBottom + BUTTON_HEIGHT) {
            return null;
        }
        return HeroStat.values()[row];
    }
}
