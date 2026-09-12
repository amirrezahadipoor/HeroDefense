package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class RewardCardTouchLayoutTest {
    @Test
    void mapsOnlyTheThreeVisibleCardTapTargets() {
        float x = RewardCardTouchLayout.CARD_X + RewardCardTouchLayout.CARD_WIDTH * 0.5f;
        for (int index = 0; index < 3; index++) {
            float y = RewardCardTouchLayout.FIRST_CARD_Y
                - index * RewardCardTouchLayout.CARD_STRIDE
                + RewardCardTouchLayout.CARD_HEIGHT * 0.5f;
            assertEquals(index, RewardCardTouchLayout.cardIndexAt(x, y));
        }
        assertEquals(-1, RewardCardTouchLayout.cardIndexAt(10f, 500f));
        assertEquals(-1, RewardCardTouchLayout.cardIndexAt(x, 740f));
    }
}
