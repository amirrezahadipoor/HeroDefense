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

    /** Prices never exceed this, so very deep endless levels stay representable and legible. */
    public static final int PRICE_CEILING = 9_999_995;

    /**
     * Price of buying level {@code currentLevel + 1}. Core levels follow the base curve; every
     * endless level beyond {@link SkillId#CORE_LEVELS} multiplies the price by
     * {@link SkillId#ENDLESS_PRICE_GROWTH} again. There is no cap on levels.
     */
    public static int priceForLevel(SkillId skill, int currentLevel) {
        int level = Math.max(0, currentLevel);
        int core = Math.min(SkillId.CORE_LEVELS, level);
        double price = basePrice(skill) * Math.pow(PRICE_GROWTH, core);
        if (level > SkillId.CORE_LEVELS) {
            price *= Math.pow(SkillId.ENDLESS_PRICE_GROWTH, level - SkillId.CORE_LEVELS);
        }
        if (price >= PRICE_CEILING) return PRICE_CEILING;
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
