package com.amirrezahadipoor.herodefense.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.ContinuousWaveRun;
import com.amirrezahadipoor.herodefense.gameplay.DropPickupSystem;
import com.amirrezahadipoor.herodefense.gameplay.WaveCompletion;
import com.amirrezahadipoor.herodefense.model.DropEntity;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.settings.GameSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

/** Wave-200 Ascension guarantee, ownership tracking, and the Six Mythics secret. */
final class MythicGrantTest {
    @Test
    void firstClearGrantsTheBowAndRepeatsNothing() {
        GameState state = GameState.newRun(301L);
        Item granted = MythicEffects.grantAscensionMythic(state);

        assertNotNull(granted);
        assertEquals("sunfall_last_arrow", granted.id);
        assertEquals("sunfall_last_arrow", state.mythicGrantedItemId);
        assertEquals(List.of(0), state.mythicGrantTiers);
        assertTrue(state.inventory.contains(granted));
        assertNull(MythicEffects.grantAscensionMythic(state));
    }

    @Test
    void guaranteeOrderCyclesOneMythicPerTier() {
        List<String> expected = List.of(
            "sunfall_last_arrow",
            "crown_hollow_eye",
            "bark_first_root",
            "windrunner_last_steps",
            "verdant_oath",
            "emberless_core",
            "sunfall_last_arrow"
        );
        for (int tier = 0; tier < expected.size(); tier++) {
            GameState state = GameState.newRun(310L + tier);
            state.ascensionTier = tier;
            assertEquals(expected.get(tier), MythicEffects.grantAscensionMythic(state).id);
        }
    }

    @Test
    void waveTwoHundredClearDepositsTheGuarantee() {
        GameState state = GameState.newRun(320L);
        state.waveNumber = GameState.FINAL_WAVE;

        assertEquals(
            WaveCompletion.RUN_COMPLETED,
            new ContinuousWaveRun().completeCurrentWave(state)
        );
        assertEquals(1, state.inventory.size());
        assertEquals("sunfall_last_arrow", state.inventory.get(0).id);
        assertEquals(List.of(0), state.mythicGrantTiers);
    }

    @Test
    void grantHistorySurvivesRestartsButTheLineItemDoesNot() {
        GameState state = GameState.newRun(321L);
        MythicEffects.grantAscensionMythic(state);

        state.resetForNewRun(322L);

        assertEquals(List.of(0), state.mythicGrantTiers);
        assertNull(state.mythicGrantedItemId);
        assertNull(MythicEffects.grantAscensionMythic(state));
    }

    @Test
    void oldSavesWithoutGrantHistoryRepairToEmpty() {
        GameState state = GameState.newRun(323L);
        state.mythicGrantTiers = null;

        state.validateAndRepair();

        assertNotNull(state.mythicGrantTiers);
        assertTrue(state.mythicGrantTiers.isEmpty());
        assertEquals("sunfall_last_arrow", MythicEffects.grantAscensionMythic(state).id);
    }

    @Test
    void owningAllSixCountsBackpackAndWornDuplicatesExcluded() {
        GameState state = GameState.newRun(330L);
        assertFalse(MythicEffects.ownsAllSix(state));
        assertFalse(MythicEffects.ownsAllSix(null));

        for (int i = 0; i < 6; i++) {
            state.inventory.add(EquipmentCatalog.byId("sunfall_last_arrow").createItem());
        }
        assertFalse(MythicEffects.ownsAllSix(state));

        state.inventory.clear();
        for (String id : MythicEffects.ALL_IDS.subList(0, 5)) {
            state.inventory.add(EquipmentCatalog.byId(id).createItem());
        }
        state.equippedItems.put(
            "RING_2", EquipmentCatalog.byId("emberless_core").createItem()
        );
        assertTrue(MythicEffects.ownsAllSix(state));
    }

    @Test
    void pickedUpMythicsAreNeverAutoSold() {
        GameState state = GameState.newRun(340L);
        GameSettings settings = new GameSettings();
        settings.autoSellCommon = true;
        settings.autoSellUncommon = true;
        settings.autoSellRare = true;
        DropEntity drop = new DropEntity(state.allocateEntityId(), "ITEM", 10f, 20f, 1);
        drop.itemId = "emberless_core";
        drop.pickupDelaySeconds = 0f;
        state.drops.add(drop);

        DropPickupSystem pickup = new DropPickupSystem();
        assertEquals(1, pickup.update(state, DropPickupSystem.HOMING_DURATION_SECONDS + 1f, settings));

        assertEquals(1, state.inventory.size());
        assertEquals("emberless_core", state.inventory.get(0).id);
        assertEquals(0, pickup.lastAutoSoldItems());
        assertEquals(0, state.coins);
    }
}
