package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.HeroStat;
import org.junit.jupiter.api.Test;

final class StatShopTouchLayoutTest {
    @Test
    void eachUpgradeAndCloseAreReachableByPortraitTap() {
        for (int index = 0; index < HeroStat.values().length; index++) {
            float y = StatShopTouchLayout.ROW_TOP - StatShopTouchLayout.ROW_HEIGHT / 2f
                - index * StatShopTouchLayout.ROW_STRIDE;
            assertEquals(HeroStat.values()[index], StatShopTouchLayout.statAt(360f, y));
        }
        assertTrue(StatShopTouchLayout.closeAt(620f, 1170f));
        assertNull(StatShopTouchLayout.statAt(10f, 10f));
    }

    @Test
    void evolutionForkSplitsEachRowIntoLeftAndRightOptions() {
        for (int index = 0; index < 5; index++) {
            float y = StatShopTouchLayout.rowBottom(index) + StatShopTouchLayout.ROW_HEIGHT / 2f;
            assertEquals(0, StatShopTouchLayout.evolutionOptionAt(
                StatShopTouchLayout.ROW_X + 10f, y), "row " + index);
            assertEquals(1, StatShopTouchLayout.evolutionOptionAt(
                StatShopTouchLayout.ROW_X + StatShopTouchLayout.ROW_WIDTH - 10f, y),
                "row " + index);
        }
        assertEquals(-1, StatShopTouchLayout.evolutionOptionAt(10f, 10f));
        assertEquals(-1, StatShopTouchLayout.evolutionOptionAt(360f, 60f));
    }
}
