package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.model.Item;
import org.junit.jupiter.api.Test;

final class HeroStatCalculatorTest {
    private final HeroStatCalculator calculator = new HeroStatCalculator();

    @Test
    void equippedItemBonusesImmediatelyAffectDerivedCombatStats() {
        GameState state = GameState.newRun(40L);
        Item weapon = EquipmentCatalog.byId("worldbranch").createItem();
        state.inventory.add(weapon);
        InventoryEquipmentSystem equipment = new InventoryEquipmentSystem(calculator);

        float baseDamage = calculator.damage(state);
        assertTrue(equipment.equip(state, weapon));

        assertEquals(2, calculator.points(state, HeroStat.STRENGTH));
        assertTrue(calculator.damage(state) > baseDamage);
        assertEquals(weapon, equipment.equipped(state, EquipmentSlot.WEAPON));
    }

    @Test
    void healthEquipmentRaisesAndUnequipClampsMaximumAndCurrentHealth() {
        GameState state = GameState.newRun(41L);
        Item armor = EquipmentCatalog.byId("heartwood_aegis").createItem();
        state.inventory.add(armor);
        InventoryEquipmentSystem equipment = new InventoryEquipmentSystem(calculator);

        equipment.equip(state, armor);
        assertTrue(state.hero.maxHealth > 100f);
        state.hero.health = state.hero.maxHealth;
        equipment.unequip(state, EquipmentSlot.ARMOR);
        assertEquals(100f, state.hero.maxHealth);
        assertEquals(100f, state.hero.health);
    }
}
