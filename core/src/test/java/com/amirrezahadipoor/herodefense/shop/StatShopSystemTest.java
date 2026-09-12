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
    void rejectsPurchaseWithoutEnoughInGameCoins() {
        GameState state = GameState.newRun(43L);
        state.coins = 0;
        assertFalse(shop.purchase(state, HeroStat.LUCK));
        assertEquals(0, state.hero.stats.luck);
    }
}
