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

    @Test
    void tappedChoiceAppliesImmediatelyPersistsAndClearsPauseState() {
        GameState state = GameState.newRun(52L);
        state.awaitingBossReward = true;
        state.pendingRewardBossNumber = 3;
        state.pendingRewardCards.add("HEALTH");
        state.pendingRewardCards.add("GENERAL_POWER");
        state.pendingRewardCards.add("LIFESTEAL");
        state.hero.health = 50f;

        assertTrue(rewards.chooseCard(state, 0));

        assertEquals(1, state.hero.stats.health);
        assertEquals(110f, state.hero.maxHealth);
        assertEquals(60f, state.hero.health);
        assertEquals("HEALTH", state.chosenRewardCards.get("3"));
        assertTrue(state.pendingRewardCards.isEmpty());
        assertTrue(!state.awaitingBossReward);
    }

    @Test
    void additiveEffectsPersistInPermanentEffectMap() {
        GameState state = GameState.newRun(53L);
        state.awaitingBossReward = true;
        state.pendingRewardBossNumber = 4;
        state.pendingRewardCards.add("GENERAL_POWER");
        state.pendingRewardCards.add("COIN_INCOME");
        state.pendingRewardCards.add("LIFESTEAL");

        assertTrue(rewards.chooseCard(state, 1));
        assertEquals(
            0.15f,
            state.permanentEffects.get(BossRewardCardSystem.COIN_INCOME_KEY).floatValue(),
            0.0001f
        );
        assertEquals("COIN_INCOME", state.chosenRewardCards.get("4"));
    }
}
