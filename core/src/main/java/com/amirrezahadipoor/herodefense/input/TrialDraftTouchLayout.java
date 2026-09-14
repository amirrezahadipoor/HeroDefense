package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.trials.TrialDraftSystem;

/** Hit targets exactly match the four visible pre-run trial cards. */
public final class TrialDraftTouchLayout {
    public static final float CARD_X = 70f;
    public static final float CARD_WIDTH = 580f;
    public static final float CARD_HEIGHT = 165f;
    public static final float FIRST_CARD_Y = 805f;
    public static final float CARD_STRIDE = 185f;

    private TrialDraftTouchLayout() {
    }

    public static int cardIndexAt(float worldX, float worldY) {
        if (worldX < CARD_X || worldX > CARD_X + CARD_WIDTH) {
            return -1;
        }
        for (int index = 0; index < TrialDraftSystem.OFFER_COUNT; index++) {
            float y = FIRST_CARD_Y - index * CARD_STRIDE;
            if (worldY >= y && worldY <= y + CARD_HEIGHT) {
                return index;
            }
        }
        return -1;
    }
}
