package com.amirrezahadipoor.herodefense.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.HeroStatCalculator;
import com.amirrezahadipoor.herodefense.model.GameState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class EquipmentSetBonusTest {
    @Test
    void catalogHoldsTwoFourPieceSets() {
        Map<String, Integer> pieces = new java.util.LinkedHashMap<>();
        for (EquipmentDefinition definition : EquipmentCatalog.all()) {
            if (!definition.setId().isEmpty()) {
                pieces.merge(definition.setId(), 1, Integer::sum);
            }
        }
        assertEquals(Map.of("verdant_covenant", 4, "bastion_oath", 4), pieces);
        assertEquals(2, EquipmentSetBonus.all().size());
    }

    @Test
    void twoPiecesSwitchOnTheSmallBonusAndFourTheLargeOne() {
        GameState state = GameState.newRun(2301L);
        HeroStatCalculator stats = new HeroStatCalculator();
        float plainInterval = stats.attackIntervalSeconds(state);
        float plainHealth = stats.maxHealth(state);
        float plainDamage = stats.damage(state);

        equipById(state, "WEAPON", "verdant_recurve");
        equipById(state, "HELMET", "fern_guard");
        // Attack speed rises: the interval between shots shrinks by ~5%.
        assertEquals(
            plainInterval / stats.attackIntervalSeconds(state), 1.05f, 0.02f
        );
        assertEquals(0, EquipmentSetBonus.chainTargetsBonus(state));

        equipById(state, "ARMOR", "mossweave_coat");
        equipById(state, "BOOTS", "windstep_boots");
        assertEquals(1, EquipmentSetBonus.chainTargetsBonus(state));
        assertEquals(List.of("verdant_covenant"), EquipmentSetBonus.completedSets(state));
        // Set pieces carry their own stats too, so only the chain bonus is exact here.
        assertTrue(stats.attackIntervalSeconds(state) < plainInterval);
        assertTrue(stats.maxHealth(state) >= plainHealth);
        assertTrue(stats.damage(state) >= plainDamage);
    }

    @Test
    void bastionOathToughensAtTwoAndSharpensAtFour() {
        GameState state = GameState.newRun(2302L);
        HeroStatCalculator stats = new HeroStatCalculator();
        equipById(state, "WEAPON", "golemsbane_warbow");
        equipById(state, "HELMET", "owlguard_helm");
        assertEquals(1.05f, EquipmentSetBonus.maxHealthMultiplier(state), 1e-6f);
        assertEquals(1f, EquipmentSetBonus.damageMultiplier(state), 1e-6f);

        equipById(state, "ARMOR", "crystalbark_plate");
        equipById(state, "BOOTS", "stormrunner_boots");
        assertEquals(1.10f, EquipmentSetBonus.damageMultiplier(state), 1e-6f);
        assertEquals(List.of("bastion_oath"), EquipmentSetBonus.completedSets(state));
        assertTrue(stats.maxHealth(state) > 0f);
    }

    @Test
    void mixedSetsDoNotCompleteEachOther() {
        GameState state = GameState.newRun(2303L);
        equipById(state, "WEAPON", "verdant_recurve");
        equipById(state, "HELMET", "owlguard_helm");
        equipById(state, "ARMOR", "mossweave_coat");
        equipById(state, "BOOTS", "stormrunner_boots");
        Map<String, Integer> counts = EquipmentSetBonus.equippedCounts(state);
        assertEquals(2, counts.get("verdant_covenant"));
        assertEquals(2, counts.get("bastion_oath"));
        assertTrue(EquipmentSetBonus.completedSets(state).isEmpty());
        assertEquals(1.05f, EquipmentSetBonus.attackSpeedMultiplier(state), 1e-6f);
        assertEquals(1.05f, EquipmentSetBonus.maxHealthMultiplier(state), 1e-6f);
    }

    private static void equipById(GameState state, String slot, String id) {
        state.equippedItems.put(slot, EquipmentCatalog.byId(id).createItem());
    }
}
