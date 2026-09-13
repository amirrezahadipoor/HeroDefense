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

    private final RewardPowerBudget powerBudget;

    public BossRewardCardSystem() {
        this(new RewardPowerBudget());
    }

    public BossRewardCardSystem(RewardPowerBudget powerBudget) {
        this.powerBudget = powerBudget;
    }

    public void prepareChoices(GameState state, int bossNumber) {
        if (state == null || bossNumber < 1 || bossNumber > RewardPowerBudget.MAX_BOSS) {
            throw new IllegalArgumentException("Boss reward number must be 1.." + RewardPowerBudget.MAX_BOSS);
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
        applyScaledEffect(state, card, bossNumber);
        state.chosenRewardCards.put(bossKey, card.name());
        state.pendingRewardCards.clear();
        state.pendingRewardBossNumber = 0;
        state.awaitingBossReward = false;
        return true;
    }

    private void applyScaledEffect(GameState state, RewardCardId card, int bossNumber) {
        Hero hero = state.hero;
        int statPoints = powerBudget.statPoints(bossNumber);
        switch (card) {
            case STRENGTH -> hero.stats.strength += statPoints;
            case AGILITY -> hero.stats.agility += statPoints;
            case LUCK -> hero.stats.luck += statPoints;
            case DODGE -> hero.stats.dodge += statPoints;
            case HEALTH -> {
                float previousMax = hero.maxHealth;
                hero.stats.health += statPoints;
                hero.maxHealth = hero.stats.maxHealth();
                hero.health = Math.min(hero.maxHealth, hero.health + hero.maxHealth - previousMax);
            }
            case GENERAL_POWER -> addEffect(
                state, GENERAL_POWER_KEY, powerBudget.magnitude(card, bossNumber)
            );
            case COIN_INCOME -> addEffect(
                state, COIN_INCOME_KEY, powerBudget.magnitude(card, bossNumber)
            );
            case LIFESTEAL -> addEffect(
                state, LIFESTEAL_KEY, powerBudget.magnitude(card, bossNumber)
            );
        }
    }

    private static void addEffect(GameState state, String key, float amount) {
        Float existing = state.permanentEffects.get(key);
        state.permanentEffects.put(key, (existing == null ? 0f : existing) + amount);
    }
}
