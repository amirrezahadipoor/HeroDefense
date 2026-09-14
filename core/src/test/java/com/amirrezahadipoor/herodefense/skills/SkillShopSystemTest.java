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
            for (int level = 0; level < SkillId.CORE_LEVELS; level++) {
                int price = SkillShopSystem.priceForLevel(skill, level);
                assertTrue(price > previous, skill.name());
                assertEquals(0, price % 5, skill.name());
                previous = price;
                total += price;
            }
            assertTrue(total >= 5_000, skill.name() + " must be a long-term coin sink");
            // Endless tier keeps climbing steeply but stays representable.
            for (int level = SkillId.CORE_LEVELS; level < 120; level++) {
                int price = SkillShopSystem.priceForLevel(skill, level);
                assertTrue(price >= previous, skill.name() + " level " + level);
                assertTrue(price <= SkillShopSystem.PRICE_CEILING);
                assertEquals(0, price % 5);
                previous = price;
            }
            assertTrue(SkillShopSystem.priceForLevel(skill, 11)
                > SkillShopSystem.priceForLevel(skill, 10) * 1.4f, skill.name());
        }
    }

    @Test
    void insufficientCoinsIsRefusedAndCoreLevelsCapAtTen() {
        GameState state = GameState.newRun(2L);
        state.coins = 0;
        assertFalse(shop.purchase(state, SkillId.STUN_CHANCE));
        assertEquals(SkillShopSystem.PurchaseResult.INSUFFICIENT_COINS, shop.feedbackResult());
        assertTrue(shop.feedbackMessage().startsWith("NEED $"));

        state.skillLevels.put(SkillId.STUN_CHANCE.saveKey(), SkillId.CORE_LEVELS);
        state.coins = 1_000_000;
        assertFalse(shop.purchase(state, SkillId.STUN_CHANCE));
        assertEquals(SkillShopSystem.PurchaseResult.MAXED, shop.feedbackResult());
        assertTrue(shop.feedbackMessage().startsWith("MAXED"));
        assertEquals(SkillId.CORE_LEVELS, shop.level(state, SkillId.STUN_CHANCE));
        assertEquals(Integer.MAX_VALUE, shop.price(state, SkillId.STUN_CHANCE));

        shop.update(10f);
        assertNull(shop.feedbackMessage());
    }

    @Test
    void atEvolutionForkOpensOnlyWhileCoreCompleteAndUnevolved() {
        GameState state = GameState.newRun(9L);
        state.coins = 1_000_000;
        SkillId skill = SkillId.MULTI_SHOT;
        assertFalse(shop.atEvolutionFork(state, skill));
        for (int bought = 0; bought < SkillId.CORE_LEVELS; bought++) {
            assertTrue(shop.purchase(state, skill));
        }
        assertTrue(shop.atEvolutionFork(state, skill));
        assertTrue(shop.purchaseEvolution(state, skill, SkillEvolution.HORNET_VOLLEY));
        assertFalse(shop.atEvolutionFork(state, skill));
    }

    @Test
    void evolutionForkCostsTheRetiredLevelElevenPriceAndLocksAfterOneChoice() {
        GameState state = GameState.newRun(4L);
        state.skillLevels.put(SkillId.CHAIN_LIGHTNING.saveKey(), SkillId.CORE_LEVELS);
        int expected = SkillShopSystem.priceForLevel(SkillId.CHAIN_LIGHTNING, SkillId.CORE_LEVELS);
        assertEquals(expected, shop.evolutionPrice(state, SkillId.CHAIN_LIGHTNING));

        state.coins = expected - 1;
        assertFalse(shop.purchaseEvolution(
            state, SkillId.CHAIN_LIGHTNING, SkillEvolution.STORM_CHAIN
        ));
        assertEquals(SkillShopSystem.PurchaseResult.INSUFFICIENT_COINS, shop.feedbackResult());

        state.coins = expected;
        assertTrue(shop.purchaseEvolution(
            state, SkillId.CHAIN_LIGHTNING, SkillEvolution.STORM_CHAIN
        ));
        assertEquals(0, state.coins);
        assertEquals(SkillEvolution.STORM_CHAIN,
            SkillEffects.evolution(state, SkillId.CHAIN_LIGHTNING));
        assertEquals(SkillShopSystem.PurchaseResult.EVOLVED, shop.feedbackResult());
        assertTrue(shop.feedbackMessage().startsWith("EVOLVED"));

        state.coins = 1_000_000;
        assertFalse(shop.purchaseEvolution(
            state, SkillId.CHAIN_LIGHTNING, SkillEvolution.VAMPIRIC_CHAIN
        ));
        assertEquals(SkillEvolution.STORM_CHAIN,
            SkillEffects.evolution(state, SkillId.CHAIN_LIGHTNING));
        assertEquals(Integer.MAX_VALUE, shop.evolutionPrice(state, SkillId.CHAIN_LIGHTNING));
        assertEquals(Integer.MAX_VALUE, shop.price(state, SkillId.CHAIN_LIGHTNING));
    }

    @Test
    void evolutionRejectsWrongSkillUnderleveledAndOldEndlessSavesEvolve() {
        GameState state = GameState.newRun(5L);
        state.coins = 1_000_000;
        state.skillLevels.put(SkillId.CHAIN_LIGHTNING.saveKey(), SkillId.CORE_LEVELS);
        assertFalse(shop.purchaseEvolution(
            state, SkillId.CHAIN_LIGHTNING, SkillEvolution.HORNET_VOLLEY
        ));
        assertFalse(shop.purchaseEvolution(state, SkillId.MULTI_SHOT, null));
        assertFalse(shop.purchaseEvolution(
            state, SkillId.MULTI_SHOT, SkillEvolution.HORNET_VOLLEY
        ));
        assertEquals(
            Integer.MAX_VALUE, shop.evolutionPrice(state, SkillId.MULTI_SHOT)
        );

        // Old endless saves (level 12) open the same fork instead of stranding.
        state.skillLevels.put(SkillId.MULTI_SHOT.saveKey(), SkillId.CORE_LEVELS + 2);
        assertTrue(shop.purchaseEvolution(
            state, SkillId.MULTI_SHOT, SkillEvolution.HORNET_VOLLEY
        ));
        assertEquals(SkillEvolution.HORNET_VOLLEY,
            SkillEffects.evolution(state, SkillId.MULTI_SHOT));
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
