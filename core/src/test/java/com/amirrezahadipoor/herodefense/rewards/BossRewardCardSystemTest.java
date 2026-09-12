package com.amirrezahadipoor.herodefense.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

final class BossRewardCardSystemTest {
    private final BossRewardCardSystem rewards = new BossRewardCardSystem();

    @Test
    void preparesExactlyThreeUniquePersistedChoices() {
        GameState state = GameState.newRun(51L);
        rewards.prepareChoices(state, 1);

        assertTrue(state.awaitingBossReward);
        assertEquals(1, state.pendingRewardBossNumber);
        assertEquals(3, state.pendingRewardCards.size());
        assertEquals(3, new HashSet<>(state.pendingRewardCards).size());

        var original = new java.util.ArrayList<>(state.pendingRewardCards);
        rewards.prepareChoices(state, 1);
        assertEquals(original, state.pendingRewardCards);
    }
}
