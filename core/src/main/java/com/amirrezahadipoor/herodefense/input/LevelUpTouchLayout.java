package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.model.HeroStat;

/**
 * Fixed generous tap rows for the five-stat portrait level-up overlay.
 *
 * <p>Row 0 is the top row, so the stats read Strength, Agility, Luck, Dodge, Health from
 * top to bottom exactly as in the Shop; a player never has to relearn the order.
 */
public final class LevelUpTouchLayout {
    public static final float LEFT = 90f;
    public static final float RIGHT = 630f;
    public static final float BOTTOM = 230f;
    public static final float BUTTON_HEIGHT = 130f;
    public static final float ROW_STRIDE = 150f;

    private LevelUpTouchLayout() {
    }

    /** Bottom edge of the given row; row 0 sits highest on the screen. */
    public static float rowBottom(int row) {
        return BOTTOM + (HeroStat.values().length - 1 - row) * ROW_STRIDE;
    }

    public static HeroStat statAt(float worldX, float worldY) {
        if (worldX < LEFT || worldX > RIGHT || worldY < BOTTOM) {
            return null;
        }
        int slotFromBottom = (int) ((worldY - BOTTOM) / ROW_STRIDE);
        if (slotFromBottom < 0 || slotFromBottom >= HeroStat.values().length) {
            return null;
        }
        float slotBottom = BOTTOM + slotFromBottom * ROW_STRIDE;
        if (worldY > slotBottom + BUTTON_HEIGHT) {
            return null;
        }
        return HeroStat.values()[HeroStat.values().length - 1 - slotFromBottom];
    }
}
