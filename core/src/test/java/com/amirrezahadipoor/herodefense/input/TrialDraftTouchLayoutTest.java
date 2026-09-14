package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class TrialDraftTouchLayoutTest {
    @Test
    void mapsOnlyTheFourVisibleCardTapTargets() {
        float x = TrialDraftTouchLayout.CARD_X + TrialDraftTouchLayout.CARD_WIDTH * 0.5f;
        for (int index = 0; index < 4; index++) {
            float y = TrialDraftTouchLayout.FIRST_CARD_Y
                - index * TrialDraftTouchLayout.CARD_STRIDE
                + TrialDraftTouchLayout.CARD_HEIGHT * 0.5f;
            assertEquals(index, TrialDraftTouchLayout.cardIndexAt(x, y));
        }
        assertEquals(-1, TrialDraftTouchLayout.cardIndexAt(10f, 500f));
        assertEquals(-1, TrialDraftTouchLayout.cardIndexAt(x, 800f));
    }
}
