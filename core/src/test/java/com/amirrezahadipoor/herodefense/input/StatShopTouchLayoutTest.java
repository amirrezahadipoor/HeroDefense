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
}
