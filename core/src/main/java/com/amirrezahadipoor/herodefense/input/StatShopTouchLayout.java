package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.skills.SkillId;

/**
 * Shared portrait shop bounds; every action is exposed through generous tap targets.
 *
 * <p>The shop has two tabs sharing the same five rows: {@link Tab#STATS} sells +1 stat
 * levels, {@link Tab#SKILLS} sells the expensive ten-level combat skills.
 */
public final class StatShopTouchLayout {
    public enum Tab { STATS, SKILLS }

    public static final float CLOSE_X = 570f;
    public static final float CLOSE_Y = 1120f;
    public static final float CLOSE_SIZE = 100f;
    public static final float TAB_Y = 1046f;
    public static final float TAB_HEIGHT = 68f;
    public static final float TAB_STATS_X = 55f;
    public static final float TAB_SKILLS_X = 365f;
    public static final float TAB_WIDTH = 300f;
    public static final float ROW_X = 55f;
    public static final float ROW_WIDTH = 610f;
    public static final float ROW_TOP = 1034f;
    public static final float ROW_HEIGHT = 136f;
    public static final float ROW_STRIDE = 150f;

    private StatShopTouchLayout() {
    }

    public static Tab tabAt(float x, float y) {
        if (y < TAB_Y || y > TAB_Y + TAB_HEIGHT) return null;
        if (x >= TAB_STATS_X && x <= TAB_STATS_X + TAB_WIDTH) return Tab.STATS;
        if (x >= TAB_SKILLS_X && x <= TAB_SKILLS_X + TAB_WIDTH) return Tab.SKILLS;
        return null;
    }

    /** Row index 0..4 under the touch, or -1. Both tabs use the same five rows. */
    public static int rowAt(float x, float y) {
        if (x < ROW_X || x > ROW_X + ROW_WIDTH) return -1;
        for (int index = 0; index < 5; index++) {
            float bottom = rowBottom(index);
            if (y >= bottom && y <= bottom + ROW_HEIGHT) return index;
        }
        return -1;
    }

    public static float rowBottom(int index) {
        return ROW_TOP - ROW_HEIGHT - index * ROW_STRIDE;
    }

    public static HeroStat statAt(float x, float y) {
        int row = rowAt(x, y);
        return row < 0 ? null : HeroStat.values()[row];
    }

    public static SkillId skillAt(float x, float y) {
        int row = rowAt(x, y);
        return row < 0 ? null : SkillId.values()[row];
    }

    public static boolean closeAt(float x, float y) {
        return x >= CLOSE_X && x <= CLOSE_X + CLOSE_SIZE
            && y >= CLOSE_Y && y <= CLOSE_Y + CLOSE_SIZE;
    }
}
