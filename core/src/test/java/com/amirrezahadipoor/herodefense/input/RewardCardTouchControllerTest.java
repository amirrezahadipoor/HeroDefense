package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import org.junit.jupiter.api.Test;

final class RewardCardTouchControllerTest {
    @Test
    void aCardTapAppliesExactlyOneOfTheThreePreparedChoices() {
        GameState state = GameState.newRun(61L);
        BossRewardCardSystem cards = new BossRewardCardSystem();
        cards.prepareChoices(state, 4);
        assertEquals(3, state.pendingRewardCards.size());
        String expected = state.pendingRewardCards.get(1);

        RewardCardTouchController touch = new RewardCardTouchController(cards);
        float x = RewardCardTouchLayout.CARD_X + RewardCardTouchLayout.CARD_WIDTH / 2f;
        float y = RewardCardTouchLayout.FIRST_CARD_Y - RewardCardTouchLayout.CARD_STRIDE
            + RewardCardTouchLayout.CARD_HEIGHT / 2f;
        assertTrue(touch.tap(state, x, y));
        assertEquals(expected, state.chosenRewardCards.get("4"));
        assertFalse(state.awaitingBossReward);
        assertTrue(state.pendingRewardCards.isEmpty());
        assertFalse(touch.tap(state, x, y));
    }
}
