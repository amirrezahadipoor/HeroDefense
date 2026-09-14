package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.model.Item;
import org.junit.jupiter.api.Test;

final class ItemForgeSystemTest {
    private final ItemForgeSystem forge = new ItemForgeSystem();

    @Test
    void onlyRareAndLegendaryCatalogItemsAreForgeable() {
        assertFalse(ItemForgeSystem.isForgeable(EquipmentCatalog.byId("leather_cap").createItem()));
        assertTrue(ItemForgeSystem.isForgeable(EquipmentCatalog.byId("starfall_bow").createItem()));
        assertTrue(ItemForgeSystem.isForgeable(EquipmentCatalog.byId("worldbranch").createItem()));
        assertFalse(ItemForgeSystem.isForgeable(null));
        Item unknown = new Item("nope", "Nope", "WEAPON", "LEGENDARY");
        assertFalse(ItemForgeSystem.isForgeable(unknown));
        assertEquals(-1, ItemForgeSystem.nextCost(unknown));
    }

    @Test
    void eachStepAddsOneToEveryBonusRenamesAndRaisesSellPriceUpToFive() {
        GameState state = GameState.newRun(9L);
        Item bow = EquipmentCatalog.byId("starfall_bow").createItem();
        state.inventory.add(bow);
        state.coins = 1_000_000;
        float agility = bow.statBonuses.get(HeroStat.AGILITY.name());
        float strength = bow.statBonuses.get(HeroStat.STRENGTH.name());
        int sell = bow.sellPrice;
        int previousCost = 0;
        for (int step = 1; step <= ItemForgeSystem.MAX_UPGRADE; step++) {
            int cost = ItemForgeSystem.nextCost(bow);
            assertTrue(cost > previousCost, "step " + step);
            assertEquals(0, cost % 5);
            int coins = state.coins;
            assertEquals(ItemForgeSystem.Result.FORGED, forge.forge(state, bow));
            assertEquals(coins - cost, state.coins);
            assertEquals(step, bow.upgradeLevel);
            assertEquals("Starfall Bow +" + step, bow.name);
            assertEquals(agility + step, bow.statBonuses.get(HeroStat.AGILITY.name()), 1e-6f);
            assertEquals(strength + step, bow.statBonuses.get(HeroStat.STRENGTH.name()), 1e-6f);
            assertTrue(bow.sellPrice > sell);
            sell = bow.sellPrice;
            previousCost = cost;
        }
        assertEquals(-1, ItemForgeSystem.nextCost(bow));
        int coins = state.coins;
        assertEquals(ItemForgeSystem.Result.MAXED, forge.forge(state, bow));
        assertEquals(coins, state.coins);
        assertEquals("Starfall Bow +5", bow.name);
        assertTrue(forge.feedbackMessage().startsWith("FULLY REFORGED"));
    }

    @Test
    void legendaryCostsMoreThanRareAndInsufficientCoinsAreRefused() {
        Item rare = EquipmentCatalog.byId("starfall_bow").createItem();
        Item legendary = EquipmentCatalog.byId("worldbranch").createItem();
        assertEquals(ItemForgeSystem.RARE_BASE_COST, ItemForgeSystem.nextCost(rare));
        assertEquals(ItemForgeSystem.LEGENDARY_BASE_COST, ItemForgeSystem.nextCost(legendary));
        assertTrue(ItemForgeSystem.nextCost(legendary) > ItemForgeSystem.nextCost(rare));

        GameState state = GameState.newRun(10L);
        state.inventory.add(legendary);
        state.coins = ItemForgeSystem.LEGENDARY_BASE_COST - 1;
        assertEquals(ItemForgeSystem.Result.INSUFFICIENT_COINS, forge.forge(state, legendary));
        assertEquals("NEED $ 1 MORE  |  ANVIL", forge.feedbackMessage());
        assertEquals(0, legendary.upgradeLevel);
        assertEquals("Worldbranch", legendary.name);
    }

    @Test
    void forgingAnEquippedItemRaisesMaxHealthImmediatelyAndCommonIsRefused() {
        GameState state = GameState.newRun(12L);
        Item plate = EquipmentCatalog.byId("crystalbark_plate").createItem();
        state.inventory.add(plate);
        new InventoryEquipmentSystem().equip(state, plate);
        float maxBefore = state.hero.maxHealth;
        state.coins = 10_000;
        assertEquals(ItemForgeSystem.Result.FORGED, forge.forge(state, plate));
        assertTrue(state.hero.maxHealth > maxBefore);
        assertEquals(plate, state.equippedItems.get(EquipmentSlot.ARMOR.name()));

        Item cap = EquipmentCatalog.byId("leather_cap").createItem();
        state.inventory.add(cap);
        assertEquals(ItemForgeSystem.Result.NOT_FORGEABLE, forge.forge(state, cap));
        assertEquals("ANVIL TAKES RARE & LEGENDARY ONLY", forge.feedbackMessage());
        forge.update(2f);
        assertNull(forge.feedbackMessage());
    }

    @Test
    void baseNameNeverStacksSuffixes() {
        Item bow = EquipmentCatalog.byId("starfall_bow").createItem();
        bow.name = "Starfall Bow +3";
        assertEquals("Starfall Bow", ItemForgeSystem.baseName(bow));
        assertEquals("Starfall Bow +4", ItemForgeSystem.displayName(ItemForgeSystem.baseName(bow), 4));
        assertEquals("Starfall Bow", ItemForgeSystem.displayName("Starfall Bow", 0));
    }

    @Test
    void affixRerollChanceStartsSmallAndGrowsWithForgeLevel() {
        assertEquals(0.05f, ItemForgeSystem.affixRerollChance(0), 1e-6f);
        assertEquals(0.09f, ItemForgeSystem.affixRerollChance(2), 1e-6f);
        assertEquals(0.13f, ItemForgeSystem.affixRerollChance(4), 1e-6f);
    }

    @Test
    void luckyReforgeRerollsTheAffixInsteadOfAddingAStatStep() {
        GameState state = GameState.newRun(11L); // First affix roll lands under the 5% bar.
        Item bow = EquipmentCatalog.byId("starfall_bow").createItem();
        bow.affixId = "DAMAGE";
        state.inventory.add(bow);
        state.coins = 10_000;
        float agility = bow.statBonuses.get(HeroStat.AGILITY.name());
        int cost = ItemForgeSystem.nextCost(bow);

        assertEquals(ItemForgeSystem.Result.AFFIX_REROLLED, forge.forge(state, bow));
        assertEquals(10_000 - cost, state.coins);
        assertEquals(0, bow.upgradeLevel);
        assertEquals("Starfall Bow", bow.name);
        assertEquals(agility, bow.statBonuses.get(HeroStat.AGILITY.name()), 1e-6f);
        assertFalse(bow.affixId == null || bow.affixId.isEmpty() || bow.affixId.equals("DAMAGE"));
        assertTrue(forge.feedbackMessage().startsWith("AFFIX REROLLED"));
    }

    @Test
    void decliningRerollsKeepsEveryReforgeOnTheStatTrack() {
        ItemForgeSystem optimal = new ItemForgeSystem();
        optimal.declineAffixRerolls();
        GameState state = GameState.newRun(11L); // Would reroll with the gamble allowed.
        Item bow = EquipmentCatalog.byId("starfall_bow").createItem();
        bow.affixId = "DAMAGE";
        state.inventory.add(bow);
        state.coins = 10_000;

        assertEquals(ItemForgeSystem.Result.FORGED, optimal.forge(state, bow));
        assertEquals(1, bow.upgradeLevel);
        assertEquals("DAMAGE", bow.affixId);
    }
}
