package com.amirrezahadipoor.herodefense.rewards;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Hero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Creates exactly three unique persisted choices after every boss defeat. */
public final class BossRewardCardSystem {
    public static final int CHOICE_COUNT = 3;
    public static final String GENERAL_POWER_KEY = "generalPower";
    public static final String COIN_INCOME_KEY = "coinIncome";
    public static final String LIFESTEAL_KEY = "lifesteal";

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

    /** Applies one displayed choice immediately and clears the paused offer. */
    public boolean chooseCard(GameState state, int choiceIndex) {
        if (state == null || !state.awaitingBossReward
            || state.pendingRewardCards.size() != CHOICE_COUNT
            || choiceIndex < 0 || choiceIndex >= CHOICE_COUNT) {
            return false;
        }
        int bossNumber = state.pendingRewardBossNumber;
        String bossKey = Integer.toString(bossNumber);
        if (bossNumber < 1 || state.chosenRewardCards.containsKey(bossKey)) {
            return false;
        }

        RewardCardId card;
        try {
            card = RewardCardId.valueOf(state.pendingRewardCards.get(choiceIndex));
        } catch (IllegalArgumentException | NullPointerException error) {
            return false;
        }
        applyBaseEffect(state, card);
        state.chosenRewardCards.put(bossKey, card.name());
        state.pendingRewardCards.clear();
        state.pendingRewardBossNumber = 0;
        state.awaitingBossReward = false;
        return true;
    }

    private static void applyBaseEffect(GameState state, RewardCardId card) {
        Hero hero = state.hero;
        switch (card) {
            case STRENGTH -> hero.stats.strength++;
            case AGILITY -> hero.stats.agility++;
            case LUCK -> hero.stats.luck++;
            case DODGE -> hero.stats.dodge++;
            case HEALTH -> {
                float previousMax = hero.maxHealth;
                hero.stats.health++;
                hero.maxHealth = hero.stats.maxHealth();
                hero.health = Math.min(hero.maxHealth, hero.health + hero.maxHealth - previousMax);
            }
            case GENERAL_POWER -> addEffect(state, GENERAL_POWER_KEY, card.baseMagnitude());
            case COIN_INCOME -> addEffect(state, COIN_INCOME_KEY, card.baseMagnitude());
            case LIFESTEAL -> addEffect(state, LIFESTEAL_KEY, card.baseMagnitude());
        }
    }

    private static void addEffect(GameState state, String key, float amount) {
        Float existing = state.permanentEffects.get(key);
        state.permanentEffects.put(key, (existing == null ? 0f : existing) + amount);
    }
}
