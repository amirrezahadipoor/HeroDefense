package com.amirrezahadipoor.herodefense.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.model.ItemTier;
import org.junit.jupiter.api.Test;

final class AffixEffectsTest {
    @Test
    void fifteenAffixesCarryDistinctDisplays() {
        assertEquals(15, AffixId.values().length);
        for (AffixId affix : AffixId.values()) {
            assertTrue(affix.display() != null && !affix.display().isBlank());
            assertEquals(affix, AffixId.forName(affix.name()));
        }
    }

    @Test
    void nullOrEmptyEquipmentAlwaysYieldsIdentity() {
        assertEquals(0f, AffixEffects.critChanceBonus(null), 0f);
        assertEquals(0f, AffixEffects.critDamageBonus(null), 0f);
        assertEquals(0f, AffixEffects.lifestealBonus(null), 0f);
        assertEquals(0f, AffixEffects.coinsOnKill(null), 0f);
        assertEquals(1f, AffixEffects.damageMultiplier(null), 0f);
        assertEquals(1f, AffixEffects.attackSpeedMultiplier(null), 0f);
        assertEquals(1f, AffixEffects.maxHealthMultiplier(null), 0f);
        assertEquals(0f, AffixEffects.dodgeBonus(null), 0f);
        assertEquals(1f, AffixEffects.experienceMultiplier(null), 0f);
        assertEquals(1f, AffixEffects.dropChanceMultiplier(null), 0f);
        assertEquals(1f, AffixEffects.potionPowerMultiplier(null), 0f);
        assertEquals(0f, AffixEffects.stunChanceBonus(null), 0f);
        assertEquals(0f, AffixEffects.chainChanceBonus(null), 0f);
        assertEquals(0f, AffixEffects.extraArrowsBonus(null), 0f);
        assertEquals(1f, AffixEffects.bossDamageMultiplier(null), 0f);

        GameState bare = GameState.newRun(1L);
        assertEquals(1f, AffixEffects.damageMultiplier(bare), 0f);
        assertEquals(0f, AffixEffects.coinsOnKill(bare), 0f);
    }

    @Test
    void everyAffixReadsFromEquippedItems() {
        assertEquals(0.03f, with(AffixId.CRIT_CHANCE, AffixEffects::critChanceBonus), 0f);
        assertEquals(0.12f, with(AffixId.CRIT_DAMAGE, AffixEffects::critDamageBonus), 0f);
        assertEquals(0.02f, with(AffixId.LIFESTEAL, AffixEffects::lifestealBonus), 0f);
        assertEquals(3f, with(AffixId.COINS_ON_KILL, AffixEffects::coinsOnKill), 0f);
        assertEquals(1.04f, with(AffixId.DAMAGE, AffixEffects::damageMultiplier), 0.0001f);
        assertEquals(1.03f, with(AffixId.ATTACK_SPEED, AffixEffects::attackSpeedMultiplier), 0.0001f);
        assertEquals(1.04f, with(AffixId.MAX_HEALTH, AffixEffects::maxHealthMultiplier), 0.0001f);
        assertEquals(0.02f, with(AffixId.DODGE, AffixEffects::dodgeBonus), 0f);
        assertEquals(1.06f, with(AffixId.EXPERIENCE, AffixEffects::experienceMultiplier), 0.0001f);
        assertEquals(1.12f, with(AffixId.ITEM_FIND, AffixEffects::dropChanceMultiplier), 0.0001f);
        assertEquals(1.20f, with(AffixId.POTION_POWER, AffixEffects::potionPowerMultiplier), 0.0001f);
        assertEquals(0.03f, with(AffixId.STUN_CHANCE, AffixEffects::stunChanceBonus), 0f);
        assertEquals(0.04f, with(AffixId.CHAIN_CHANCE, AffixEffects::chainChanceBonus), 0f);
        assertEquals(0.20f, with(AffixId.MULTISHOT, AffixEffects::extraArrowsBonus), 0f);
        assertEquals(1.08f, with(AffixId.BOSS_DAMAGE, AffixEffects::bossDamageMultiplier), 0.0001f);
    }

    @Test
    void identicalAffixesStackAndUnknownIdsAreIgnored() {
        GameState state = GameState.newRun(2L);
        state.equippedItems.put(EquipmentSlot.WEAPON.name(), affixedItem(AffixId.DAMAGE));
        state.equippedItems.put(EquipmentSlot.HELMET.name(), affixedItem(AffixId.DAMAGE));
        assertEquals(1.08f, AffixEffects.damageMultiplier(state), 0.0001f);

        GameState corrupt = GameState.newRun(3L);
        Item junk = affixedItem(AffixId.DAMAGE);
        junk.affixId = "NOT_AN_AFFIX";
        corrupt.equippedItems.put(EquipmentSlot.WEAPON.name(), junk);
        assertEquals(1f, AffixEffects.damageMultiplier(corrupt), 0f);
    }

    @Test
    void onlyRareAndLegendaryDropsRollAffixes() {
        GameState state = GameState.newRun(4L);
        assertEquals("", AffixEffects.rollForDrop(state, ItemTier.COMMON));
        assertEquals("", AffixEffects.rollForDrop(state, ItemTier.UNCOMMON));
        assertEquals("", AffixEffects.rollForDrop(state, null));
        for (int i = 0; i < 30; i++) {
            String rare = AffixEffects.rollForDrop(state, ItemTier.RARE);
            String legendary = AffixEffects.rollForDrop(state, ItemTier.LEGENDARY);
            assertTrue(AffixId.forName(rare) != null);
            assertTrue(AffixId.forName(legendary) != null);
        }
    }

    private interface Query {
        float read(GameState state);
    }

    private static float with(AffixId affix, Query query) {
        GameState state = GameState.newRun(5L);
        state.equippedItems.put(EquipmentSlot.WEAPON.name(), affixedItem(affix));
        return query.read(state);
    }

    private static Item affixedItem(AffixId affix) {
        Item item = new Item("probe", "Probe", EquipmentSlot.WEAPON.name(), "RARE");
        item.affixId = affix.name();
        return item;
    }
}
