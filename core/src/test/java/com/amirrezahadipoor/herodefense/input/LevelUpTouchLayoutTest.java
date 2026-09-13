package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.amirrezahadipoor.herodefense.model.HeroStat;
import org.junit.jupiter.api.Test;

final class LevelUpTouchLayoutTest {
    @Test
    void mapsFiveLargeTapRowsToFiveStats() {
        float x = (LevelUpTouchLayout.LEFT + LevelUpTouchLayout.RIGHT) * 0.5f;
        for (int row = 0; row < HeroStat.values().length; row++) {
            float y = LevelUpTouchLayout.rowBottom(row) + LevelUpTouchLayout.BUTTON_HEIGHT * 0.5f;
            assertEquals(HeroStat.values()[row], LevelUpTouchLayout.statAt(x, y));
            // Same top-to-bottom order as the Shop: Strength highest, Health lowest.
            if (row > 0) {
                assertEquals(true, LevelUpTouchLayout.rowBottom(row) < LevelUpTouchLayout.rowBottom(row - 1));
            }
        }
    }

    @Test
    void ignoresTouchesOutsideButtonsAndInRowGaps() {
        assertNull(LevelUpTouchLayout.statAt(10f, 300f));
        assertNull(LevelUpTouchLayout.statAt(360f, LevelUpTouchLayout.BOTTOM + 140f));
    }
}
