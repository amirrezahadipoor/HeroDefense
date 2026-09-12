package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;

/** Hit targets exactly match the three visible post-boss cards. */
public final class RewardCardTouchLayout {
    public static final float CARD_X = 70f;
    public static final float CARD_WIDTH = 580f;
    public static final float CARD_HEIGHT = 190f;
    public static final float FIRST_CARD_Y = 760f;
    public static final float CARD_STRIDE = 230f;

    private RewardCardTouchLayout() {
    }

    public static int cardIndexAt(float worldX, float worldY) {
        if (worldX < CARD_X || worldX > CARD_X + CARD_WIDTH) {
            return -1;
        }
        for (int index = 0; index < BossRewardCardSystem.CHOICE_COUNT; index++) {
            float y = FIRST_CARD_Y - index * CARD_STRIDE;
            if (worldY >= y && worldY <= y + CARD_HEIGHT) {
                return index;
            }
        }
        return -1;
    }
}
