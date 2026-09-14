package com.amirrezahadipoor.herodefense.skills;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.trials.TrialEffects;

import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Coin-only skill purchases, deliberately expensive: a skill level costs several stat levels.
 * Prices grow geometrically so the last levels are genuine late-run goals.
 */
public final class SkillShopSystem {
    public enum PurchaseResult { NONE, PURCHASED, INSUFFICIENT_COINS, MAXED, EVOLVED }

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
        if (level(state, skill) >= SkillId.CORE_LEVELS
            || SkillEffects.evolution(state, skill) != null) {
            return Integer.MAX_VALUE;
        }
        int base = priceForLevel(skill, level(state, skill));
        float mult = TrialEffects.shopPriceMultiplier(state.activeTrials);
        return mult == 1f ? base : (int) Math.round(base * mult / 5.0) * 5;
    }

    /**
     * Price of the level-10 Evolution fork: exactly what level 11 would have
     * cost on the retired endless curve. Only defined at the fork.
     */
    /**
     * Whether the skill sits at its Evolution fork: core-complete but unevolved, so
     * the shop row offers the two-evolution choice instead of another level.
     */
    public boolean atEvolutionFork(GameState state, SkillId skill) {
        return state != null && skill != null
            && level(state, skill) >= SkillId.CORE_LEVELS
            && SkillEffects.evolution(state, skill) == null;
    }

    public int evolutionPrice(GameState state, SkillId skill) {
        if (!atEvolutionFork(state, skill)) {
            return Integer.MAX_VALUE;
        }
        int base = priceForLevel(skill, SkillId.CORE_LEVELS);
        float mult = TrialEffects.shopPriceMultiplier(state.activeTrials);
        return mult == 1f ? base : (int) Math.round(base * mult / 5.0) * 5;
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
            case MAXED -> "MAXED  |  " + name;
            case EVOLVED -> "EVOLVED  |  " + name + "  |  -$ " + feedbackCoins;
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
        if (current >= SkillId.CORE_LEVELS || SkillEffects.evolution(state, skill) != null) {
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

    /**
     * Buys one Evolution at the level-10 fork: one-time per skill per run, open
     * to any skill at or past level 10 (old endless saves included).
     */
    public boolean purchaseEvolution(GameState state, SkillId skill, SkillEvolution evolution) {
        if (state == null || state.hero == null || skill == null || evolution == null) return false;
        if (evolution.skill() != skill || level(state, skill) < SkillId.CORE_LEVELS) return false;
        if (SkillEffects.evolution(state, skill) != null) return false;
        int price = evolutionPrice(state, skill);
        if (state.coins < price) {
            showFeedback(PurchaseResult.INSUFFICIENT_COINS, skill, price - state.coins);
            return false;
        }
        state.coins -= price;
        if (state.skillEvolutions == null) state.skillEvolutions = new LinkedHashMap<>();
        state.skillEvolutions.put(skill.saveKey(), evolution.id());
        showFeedback(PurchaseResult.EVOLVED, skill, price);
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
