package com.amirrezahadipoor.herodefense.shop;

import com.amirrezahadipoor.herodefense.gameplay.HeroStatCalculator;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;

/** Coin-only permanent stat upgrades; no platform billing or real-money path exists. */
public final class StatShopSystem {
    public static final int MAX_PURCHASES_PER_STAT = 20;
    private static final float PRICE_GROWTH = 1.22f;
    private final HeroStatCalculator statCalculator = new HeroStatCalculator();

    public int purchasedLevels(GameState state, HeroStat stat) {
        if (state == null || stat == null) return 0;
        Integer value = state.shopUpgradeLevels.get(stat.name());
        return value == null ? 0 : Math.max(0, Math.min(MAX_PURCHASES_PER_STAT, value));
    }

    public int price(GameState state, HeroStat stat) {
        if (state == null || stat == null) return Integer.MAX_VALUE;
        int level = purchasedLevels(state, stat);
        if (level >= MAX_PURCHASES_PER_STAT) return Integer.MAX_VALUE;
        return Math.max(1, Math.round(basePrice(stat) * (float) Math.pow(PRICE_GROWTH, level)));
    }

    public boolean purchase(GameState state, HeroStat stat) {
        if (state == null || state.hero == null || stat == null) return false;
        int oldLevel = purchasedLevels(state, stat);
        int price = price(state, stat);
        if (oldLevel >= MAX_PURCHASES_PER_STAT || state.coins < price) return false;

        float previousMaxHealth = statCalculator.maxHealth(state);
        state.coins -= price;
        state.shopUpgradeLevels.put(stat.name(), oldLevel + 1);
        increment(state, stat);
        if (stat == HeroStat.HEALTH) {
            float maxHealth = statCalculator.maxHealth(state);
            state.hero.health = Math.min(maxHealth, state.hero.health + maxHealth - previousMaxHealth);
            state.hero.maxHealth = maxHealth;
        }
        return true;
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
