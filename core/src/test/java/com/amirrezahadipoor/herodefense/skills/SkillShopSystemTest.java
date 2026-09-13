package com.amirrezahadipoor.herodefense.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.save.GameStateCodec;
import org.junit.jupiter.api.Test;

final class SkillShopSystemTest {
    private final SkillShopSystem shop = new SkillShopSystem();

    @Test
    void purchaseSpendsCoinsRaisesLevelAndRaisesTheNextPrice() {
        GameState state = GameState.newRun(1L);
        state.coins = 100_000;
        int first = shop.price(state, SkillId.CHAIN_LIGHTNING);
        assertTrue(shop.purchase(state, SkillId.CHAIN_LIGHTNING));
        assertEquals(1, shop.level(state, SkillId.CHAIN_LIGHTNING));
        assertEquals(100_000 - first, state.coins);
        assertTrue(shop.price(state, SkillId.CHAIN_LIGHTNING) > first);
        assertEquals(SkillShopSystem.PurchaseResult.PURCHASED, shop.feedbackResult());
    }

    @Test
    void skillsAreExpensiveAndEachLevelCostsMoreThanTheLast() {
        for (SkillId skill : SkillId.values()) {
            assertTrue(SkillShopSystem.priceForLevel(skill, 0) >= 180, skill.name());
            int previous = 0;
            int total = 0;
            for (int level = 0; level < SkillId.MAX_LEVEL; level++) {
                int price = SkillShopSystem.priceForLevel(skill, level);
                assertTrue(price > previous, skill.name());
                assertEquals(0, price % 5, skill.name());
                previous = price;
                total += price;
            }
            assertTrue(total >= 5_000, skill.name() + " must be a long-term coin sink");
        }
    }

    @Test
    void insufficientCoinsAndMaxLevelAreRefusedWithSpecificFeedback() {
        GameState state = GameState.newRun(2L);
        state.coins = 0;
        assertFalse(shop.purchase(state, SkillId.STUN_CHANCE));
        assertEquals(SkillShopSystem.PurchaseResult.INSUFFICIENT_COINS, shop.feedbackResult());
        assertTrue(shop.feedbackMessage().startsWith("NEED $"));

        state.skillLevels.put(SkillId.STUN_CHANCE.saveKey(), SkillId.MAX_LEVEL);
        state.coins = 1_000_000;
        assertFalse(shop.purchase(state, SkillId.STUN_CHANCE));
        assertEquals(SkillShopSystem.PurchaseResult.MAXED, shop.feedbackResult());
        assertEquals(1_000_000, state.coins);

        shop.update(10f);
        assertNull(shop.feedbackMessage());
    }

    @Test
    void purchasedLevelsSurviveSaveAndLoad() {
        GameState state = GameState.newRun(3L);
        state.coins = 1_000_000;
        assertTrue(shop.purchase(state, SkillId.LONG_RANGE));
        assertTrue(shop.purchase(state, SkillId.LONG_RANGE));
        GameStateCodec codec = new GameStateCodec();
        GameState loaded = codec.decode(codec.encode(state));
        assertEquals(2, shop.level(loaded, SkillId.LONG_RANGE));
    }
}
