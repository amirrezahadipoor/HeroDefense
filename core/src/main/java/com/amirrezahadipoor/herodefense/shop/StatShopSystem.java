package com.amirrezahadipoor.herodefense.shop;

import com.amirrezahadipoor.herodefense.gameplay.HeroStatCalculator;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;

/** Coin-only permanent stat upgrades; no platform billing or real-money path exists. */
public final class StatShopSystem {
    public enum PurchaseResult { NONE, PURCHASED, INSUFFICIENT_COINS, MAXED }

    public static final int MAX_PURCHASES_PER_STAT = 20;
    public static final int PRICE_STEP_PER_LEVEL = 20;
    private static final float FEEDBACK_DURATION_SECONDS = 1.25f;
    private final HeroStatCalculator statCalculator = new HeroStatCalculator();
    private PurchaseResult feedbackResult = PurchaseResult.NONE;
    private HeroStat feedbackStat;
    private int feedbackCoins;
    private float feedbackRemainingSeconds;

    public int purchasedLevels(GameState state, HeroStat stat) {
        if (state == null || stat == null) return 0;
        Integer value = state.shopUpgradeLevels.get(stat.name());
        return value == null ? 0 : Math.max(0, Math.min(MAX_PURCHASES_PER_STAT, value));
    }

    public int price(GameState state, HeroStat stat) {
        if (state == null || stat == null) return Integer.MAX_VALUE;
        int level = purchasedLevels(state, stat);
        if (level >= MAX_PURCHASES_PER_STAT) return Integer.MAX_VALUE;
        return basePrice(stat) + PRICE_STEP_PER_LEVEL * level;
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
        if (feedbackRemainingSeconds <= 0f || feedbackStat == null) return null;
        return switch (feedbackResult) {
            case PURCHASED -> "PURCHASED  |  " + pretty(feedbackStat) + " +1  |  -$ " + feedbackCoins;
            case INSUFFICIENT_COINS -> "NEED $ " + feedbackCoins + " MORE  |  " + pretty(feedbackStat);
            case MAXED -> "MAX LEVEL  |  " + pretty(feedbackStat);
            default -> null;
        };
    }

    public float feedbackAlpha() {
        if (feedbackRemainingSeconds <= 0f) return 0f;
        return Math.min(1f, feedbackRemainingSeconds / 0.20f);
    }

    public boolean purchase(GameState state, HeroStat stat) {
        if (state == null || state.hero == null || stat == null) return false;
        int oldLevel = purchasedLevels(state, stat);
        int price = price(state, stat);
        if (oldLevel >= MAX_PURCHASES_PER_STAT) {
            showFeedback(PurchaseResult.MAXED, stat, 0);
            return false;
        }
        if (state.coins < price) {
            showFeedback(PurchaseResult.INSUFFICIENT_COINS, stat, price - state.coins);
            return false;
        }

        float previousMaxHealth = statCalculator.maxHealth(state);
        state.coins -= price;
        state.shopUpgradeLevels.put(stat.name(), oldLevel + 1);
        increment(state, stat);
        if (stat == HeroStat.HEALTH) {
            float maxHealth = statCalculator.maxHealth(state);
            state.hero.health = Math.min(maxHealth, state.hero.health + maxHealth - previousMaxHealth);
            state.hero.maxHealth = maxHealth;
        }
        showFeedback(PurchaseResult.PURCHASED, stat, price);
        return true;
    }

    private void showFeedback(PurchaseResult result, HeroStat stat, int coins) {
        feedbackResult = result;
        feedbackStat = stat;
        feedbackCoins = Math.max(0, coins);
        feedbackRemainingSeconds = FEEDBACK_DURATION_SECONDS;
    }

    private void clearFeedback() {
        feedbackResult = PurchaseResult.NONE;
        feedbackStat = null;
        feedbackCoins = 0;
        feedbackRemainingSeconds = 0f;
    }

    private static String pretty(HeroStat stat) {
        String name = stat.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static int basePrice(HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> 55;
            case AGILITY -> 60;
            case LUCK -> 50;
            case DODGE -> 50;
            case HEALTH -> 65;
        };
    }

    private static void increment(GameState state, HeroStat stat) {
        switch (stat) {
            case STRENGTH -> state.hero.stats.strength++;
            case AGILITY -> state.hero.stats.agility++;
            case LUCK -> state.hero.stats.luck++;
            case DODGE -> state.hero.stats.dodge++;
            case HEALTH -> state.hero.stats.health++;
        }
    }
}
