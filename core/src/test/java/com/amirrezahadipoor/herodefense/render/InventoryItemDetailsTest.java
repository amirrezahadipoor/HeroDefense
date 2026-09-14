package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.amirrezahadipoor.herodefense.gameplay.InventoryEquipmentSystem;
import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.render.InventoryItemDetails.Details;
import com.amirrezahadipoor.herodefense.render.InventoryItemDetails.StatComparison;
import org.junit.jupiter.api.Test;

final class InventoryItemDetailsTest {
    @Test
    void exposesIdentityRaritySlotEveryBonusAndEquippedComparison() {
        GameState state = GameState.newRun(1601L);
        Item equippedBow = EquipmentCatalog.byId("ashwood_bow").createItem();
        Item candidate = EquipmentCatalog.byId("starfall_bow").createItem();
        state.inventory.add(equippedBow);
        new InventoryEquipmentSystem().equip(state, equippedBow);

        Details details = InventoryItemDetails.inspect(state, candidate);

        assertEquals("Starfall Bow", details.name());
        assertEquals("RARE", details.rarity());
        assertEquals(EquipmentSlot.WEAPON, details.slot());
        assertFalse(details.equipped());
        assertEquals("Ashwood Bow", details.comparedItemName());
        assertEquals(2, details.stats().size());
        assertComparison(details.stats().get(0), HeroStat.STRENGTH, 1f, 1f, 0f);
        assertComparison(details.stats().get(1), HeroStat.AGILITY, 3f, 0f, 3f);
    }

    @Test
    void handlesEmptySelectionWithoutInventingDetails() {
        assertNull(InventoryItemDetails.inspect(GameState.newRun(1602L), null));
    }

    @Test
    void mythicsExposePassiveAndStoryFlavorWhileOrdinaryItemsExposeNeither() {
        GameState state = GameState.newRun(1604L);
        Details mythic = InventoryItemDetails.inspect(
            state, EquipmentCatalog.byId("bark_first_root").createItem()
        );
        assertEquals("PASSIVE: Every 10th hit taken heals 20%", mythic.passiveLine());
        assertEquals(
            "Cut from the World Tree's own outer bark, back when it "
                + "could still spare the wood. It remembers how to close a wound.",
            mythic.flavorLine()
        );

        Details ordinary = InventoryItemDetails.inspect(
            state, EquipmentCatalog.byId("starfall_bow").createItem()
        );
        assertNull(ordinary.passiveLine());
        assertNull(ordinary.flavorLine());
    }

    @Test
    void exposesAffixLineOnlyForAffixedItems() {
        GameState state = GameState.newRun(1603L);
        Item affixed = EquipmentCatalog.byId("starfall_bow").createItem();
        affixed.affixId = "CRIT_CHANCE";
        Item plain = EquipmentCatalog.byId("ashwood_bow").createItem();

        assertEquals("AFFIX: +3% Critical Chance", InventoryItemDetails.inspect(state, affixed).affixLine());
        assertNull(InventoryItemDetails.inspect(state, plain).affixLine());
    }

    private static void assertComparison(
        StatComparison comparison,
        HeroStat stat,
        float candidate,
        float equipped,
        float difference
    ) {
        assertEquals(stat, comparison.stat());
        assertEquals(candidate, comparison.candidateValue());
        assertEquals(equipped, comparison.equippedValue());
        assertEquals(difference, comparison.difference());
    }
}
