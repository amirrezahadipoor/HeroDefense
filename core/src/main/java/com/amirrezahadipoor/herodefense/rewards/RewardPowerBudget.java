package com.amirrezahadipoor.herodefense.rewards;

import java.util.Locale;

/** Boss-index budget keeps late-run cards meaningful without abrupt power spikes. */
public final class RewardPowerBudget {
    public static final float GROWTH_PER_BOSS = 0.05f;
    /** Forty bosses across the 200-wave run (one every fifth wave). */
    public static final int MAX_BOSS = 40;

    public float multiplier(int bossNumber) {
        int clamped = Math.max(1, Math.min(MAX_BOSS, bossNumber));
        return 1f + (clamped - 1) * GROWTH_PER_BOSS;
    }

    public int statPoints(int bossNumber) {
        return Math.max(1, Math.round(multiplier(bossNumber)));
    }

    public float magnitude(RewardCardId card, int bossNumber) {
        return card.effectType() == RewardEffectType.BASE_STAT
            ? statPoints(bossNumber)
            : card.baseMagnitude() * multiplier(bossNumber);
    }

    public String description(RewardCardId card, int bossNumber) {
        if (card.effectType() == RewardEffectType.BASE_STAT) {
            return "+" + statPoints(bossNumber) + " " + titleCase(card.name());
        }
        return "+" + Math.round(magnitude(card, bossNumber) * 100f) + "% "
            + switch (card.effectType()) {
                case GENERAL_POWER -> "all damage";
                case COIN_INCOME -> "coin income";
                case LIFESTEAL -> "lifesteal";
                case BASE_STAT -> throw new IllegalStateException("Handled above");
            };
    }

    private static String titleCase(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
