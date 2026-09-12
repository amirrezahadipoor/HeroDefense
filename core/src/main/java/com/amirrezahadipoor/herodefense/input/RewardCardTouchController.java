package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;

/** Converts one of exactly three large post-boss card taps into a permanent choice. */
public final class RewardCardTouchController {
    private final BossRewardCardSystem cards;

    public RewardCardTouchController(BossRewardCardSystem cards) {
        this.cards = cards;
    }

    public boolean tap(GameState state, float x, float y) {
        int choiceIndex = RewardCardTouchLayout.cardIndexAt(x, y);
        return choiceIndex >= 0 && cards.chooseCard(state, choiceIndex);
    }
}
