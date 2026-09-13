package com.amirrezahadipoor.herodefense.skills;

import com.amirrezahadipoor.herodefense.model.GameState;

import java.util.Locale;

/**
 * Coin-only skill purchases, deliberately expensive: a skill level costs several stat levels.
 * Prices grow geometrically so the last levels are genuine late-run goals.
 */
public final class SkillShopSystem {
    public enum PurchaseResult { NONE, PURCHASED, INSUFFICIENT_COINS, MAXED }

    public static final float PRICE_GROWTH = 1.32f;
    private static final float FEEDBACK_DURATION_SECONDS = 1.25f;

    private PurchaseResult feedbackResult = PurchaseResult.NONE;
    private SkillId feedbackSkill;
    private int feedbackCoins;
    private float feedbackRemainingSeconds;

    public int level(GameState state, SkillId skill) {
        return SkillEffects.level(state, skill);
    }

    public int price(GameState state, SkillId skill) {
        if (state == null || skill == null) return Integer.MAX_VALUE;
        return priceForLevel(skill, level(state, skill));
    }

    /** Price of buying level {@code currentLevel + 1}; MAX_VALUE once maxed. */
    public static int priceForLevel(SkillId skill, int currentLevel) {
        if (currentLevel >= SkillId.MAX_LEVEL) return Integer.MAX_VALUE;
        double price = basePrice(skill) * Math.pow(PRICE_GROWTH, currentLevel);
        return (int) Math.round(price / 5.0) * 5;
    }

    public static int basePrice(SkillId skill) {
        return switch (skill) {
            case CHAIN_LIGHTNING -> 260;
            case MULTI_SHOT -> 300;
            case STUN_CHANCE -> 220;
            case CRITICAL_MASTERY -> 240;
            case LONG_RANGE -> 180;
        };
    }

    public void update(float realDeltaSeconds) {
        if (realDeltaSeconds <= 0f || feedbackRemainingSeconds <= 0f) return;
        feedbackRemainingSeconds = Math.max(0f, feedbackRemainingSeconds - realDeltaSeconds);
        if (feedbackRemainingSeconds == 0f) clearFeedback();
    }

    public PurchaseResult feedbackResult() {
        return feedbackRemainingSeconds > 0f ? feedbackResult : PurchaseResult.NONE;
    }

    public String feedbackMessage() {
        if (feedbackRemainingSeconds <= 0f || feedbackSkill == null) return null;
        String name = feedbackSkill.displayName().toUpperCase(Locale.ROOT);
        return switch (feedbackResult) {
            case PURCHASED -> "LEARNED  |  " + name + " +1  |  -$ " + feedbackCoins;
            case INSUFFICIENT_COINS -> "NEED $ " + feedbackCoins + " MORE  |  " + name;
            case MAXED -> "MASTERED  |  " + name;
            default -> null;
        };
    }

    public float feedbackAlpha() {
        if (feedbackRemainingSeconds <= 0f) return 0f;
        return Math.min(1f, feedbackRemainingSeconds / 0.20f);
    }

    public boolean purchase(GameState state, SkillId skill) {
        if (state == null || state.hero == null || skill == null) return false;
        int current = level(state, skill);
        if (current >= SkillId.MAX_LEVEL) {
            showFeedback(PurchaseResult.MAXED, skill, 0);
            return false;
        }
        int price = price(state, skill);
        if (state.coins < price) {
            showFeedback(PurchaseResult.INSUFFICIENT_COINS, skill, price - state.coins);
            return false;
        }
        state.coins -= price;
        state.skillLevels.put(skill.saveKey(), current + 1);
        showFeedback(PurchaseResult.PURCHASED, skill, price);
        return true;
    }

    private void showFeedback(PurchaseResult result, SkillId skill, int coins) {
        feedbackResult = result;
        feedbackSkill = skill;
        feedbackCoins = Math.max(0, coins);
        feedbackRemainingSeconds = FEEDBACK_DURATION_SECONDS;
    }

    private void clearFeedback() {
        feedbackResult = PurchaseResult.NONE;
        feedbackSkill = null;
        feedbackCoins = 0;
        feedbackRemainingSeconds = 0f;
    }
}
