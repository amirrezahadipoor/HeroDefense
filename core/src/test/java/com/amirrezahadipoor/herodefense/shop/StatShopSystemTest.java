package com.amirrezahadipoor.herodefense.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import org.junit.jupiter.api.Test;

final class StatShopSystemTest {
    private final StatShopSystem shop = new StatShopSystem();

    @Test
    void coinPurchaseRaisesStatAndPersistedPriceLevel() {
        GameState state = GameState.newRun(41L);
        state.coins = 10_000;
        int firstPrice = shop.price(state, HeroStat.STRENGTH);

        assertTrue(shop.purchase(state, HeroStat.STRENGTH));
        assertEquals(1, state.hero.stats.strength);
        assertEquals(1, shop.purchasedLevels(state, HeroStat.STRENGTH));
        assertEquals(10_000 - firstPrice, state.coins);
        assertTrue(shop.price(state, HeroStat.STRENGTH) > firstPrice);
    }

    @Test
    void healthPurchaseRaisesBothCurrentAndMaximumHealth() {
        GameState state = GameState.newRun(42L);
        state.coins = 1_000;
        state.hero.health = 40f;
        assertTrue(shop.purchase(state, HeroStat.HEALTH));
        assertEquals(50f, state.hero.health);
        assertEquals(110f, state.hero.maxHealth);
    }

    @Test
    void tunedPricesRiseLinearlyThroughTwentyPurchasesThenCloseTheStat() {
        GameState state = GameState.newRun(44L);
        for (HeroStat stat : HeroStat.values()) {
            state.shopUpgradeLevels.put(stat.name(), 0);
            int previous = shop.price(state, stat);
            for (int level = 1; level < StatShopSystem.MAX_PURCHASES_PER_STAT; level++) {
                state.shopUpgradeLevels.put(stat.name(), level);
                int current = shop.price(state, stat);
                assertEquals(StatShopSystem.PRICE_STEP_PER_LEVEL, current - previous);
                previous = current;
            }
            state.shopUpgradeLevels.put(stat.name(), StatShopSystem.MAX_PURCHASES_PER_STAT);
            assertEquals(Integer.MAX_VALUE, shop.price(state, stat));
        }
    }

    @Test
    void purchaseFailureAndSuccessFeedbackIsSpecificAndExpires() {
        GameState state = GameState.newRun(45L);
        state.coins = 0;
        assertFalse(shop.purchase(state, HeroStat.STRENGTH));
        assertEquals(StatShopSystem.PurchaseResult.INSUFFICIENT_COINS, shop.feedbackResult());
        assertEquals("NEED $ 55 MORE  |  Strength", shop.feedbackMessage());

        state.coins = 55;
        assertTrue(shop.purchase(state, HeroStat.STRENGTH));
        assertEquals(StatShopSystem.PurchaseResult.PURCHASED, shop.feedbackResult());
        assertEquals("PURCHASED  |  Strength +1  |  -$ 55", shop.feedbackMessage());

        state.shopUpgradeLevels.put(HeroStat.LUCK.name(), StatShopSystem.MAX_PURCHASES_PER_STAT);
        assertFalse(shop.purchase(state, HeroStat.LUCK));
        assertEquals(StatShopSystem.PurchaseResult.MAXED, shop.feedbackResult());
        assertEquals("MAX LEVEL  |  Luck", shop.feedbackMessage());
        assertEquals(1f, shop.feedbackAlpha());
        shop.update(1.1f);
        assertTrue(shop.feedbackAlpha() > 0f);
        shop.update(0.2f);
        assertEquals(StatShopSystem.PurchaseResult.NONE, shop.feedbackResult());
        assertEquals(null, shop.feedbackMessage());
    }

    @Test
    void rejectsPurchaseWithoutEnoughInGameCoins() {
        GameState state = GameState.newRun(43L);
        state.coins = 0;
        assertFalse(shop.purchase(state, HeroStat.LUCK));
        assertEquals(0, state.hero.stats.luck);
    }
}
