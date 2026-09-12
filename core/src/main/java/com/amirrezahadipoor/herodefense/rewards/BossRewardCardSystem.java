package com.amirrezahadipoor.herodefense.rewards;

import com.amirrezahadipoor.herodefense.model.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Creates exactly three unique persisted choices after every boss defeat. */
public final class BossRewardCardSystem {
    public static final int CHOICE_COUNT = 3;

    public void prepareChoices(GameState state, int bossNumber) {
        if (state == null || bossNumber < 1 || bossNumber > 20) {
            throw new IllegalArgumentException("Boss reward number must be 1..20");
        }
        if (state.awaitingBossReward
            && state.pendingRewardBossNumber == bossNumber
            && state.pendingRewardCards.size() == CHOICE_COUNT) {
            return;
        }

        List<RewardCardId> available = new ArrayList<>();
        Collections.addAll(available, RewardCardId.values());
        state.pendingRewardCards.clear();
        for (int choice = 0; choice < CHOICE_COUNT; choice++) {
            int index = Math.min(
                available.size() - 1,
                (int) (state.nextCombatRandomFloat() * available.size())
            );
            state.pendingRewardCards.add(available.remove(index).name());
        }
        state.pendingRewardBossNumber = bossNumber;
        state.awaitingBossReward = true;
    }
}
