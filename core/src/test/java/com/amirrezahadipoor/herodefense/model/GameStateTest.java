package com.amirrezahadipoor.herodefense.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

final class GameStateTest {
    @Test
    void newRunContainsEveryCentralStateCategory() {
        GameState state = GameState.newRun(42L);
        assertEquals(1, state.waveNumber);
        assertEquals(0, state.coins);
        assertEquals(1, state.heroLevel);
        assertEquals(0, state.unspentTalentPoints);
        assertEquals(6, state.healthPotions.size());
        assertEquals(GameState.ARENA_CENTER_X, state.hero.x);
    }

    @Test
    void simulationAnchorAlwaysReturnsHeroToTheFixedArenaCenter() {
        GameState state = GameState.newRun(7L);
        state.hero.x = -900f;
        state.hero.y = 42f;

        state.anchorHeroAtArenaCenter();

        assertEquals(GameState.ARENA_CENTER_X, state.hero.x);
        assertEquals(GameState.ARENA_CENTER_Y, state.hero.y);
    }

    @Test
    void countsLivingRegularEnemiesAndBosses() {
        GameState state = GameState.newRun(1L);
        Enemy living = new Enemy(state.allocateEntityId(), "ROOTLING", 0f, 0f);
        Enemy dead = new Enemy(state.allocateEntityId(), "BRAMBLE", 0f, 0f);
        dead.alive = false;
        Boss boss = new Boss(state.allocateEntityId(), "ANCIENT_GOLEM", 0f, 0f, 1);
        state.aliveEnemies.add(living);
        state.aliveEnemies.add(dead);
        state.aliveBosses.add(boss);
        assertEquals(2, state.livingEnemyCount());
        assertSame(living, state.aliveEnemies.get(0));
    }

    @Test
    void repairClampsUnsafeLoadedValues() {
        GameState state = new GameState();
        state.waveNumber = 400;
        state.heroLevel = -3;
        state.coins = -100;
        state.simulationSpeed = 99f;
        state.validateAndRepair();
        assertEquals(GameState.FINAL_WAVE, state.waveNumber);
        assertEquals(1, state.heroLevel);
        assertEquals(0, state.coins);
        assertEquals(1f, state.simulationSpeed);
    }

    @Test
    void runsPastThePlantingWaveAlwaysHaveTheSecondTreeStanding() {
        GameState state = new GameState();
        state.waveNumber = 150;
        state.secondTreePlanted = false;
        state.validateAndRepair();
        assertTrue(state.secondTreePlanted);
        assertTrue(state.plantedTreesCount >= 1);

        GameState midCeremony = new GameState();
        midCeremony.waveNumber = 101;
        midCeremony.ceremonyPending = true;
        midCeremony.validateAndRepair();
        // At 101 with ceremony for 100 pending, only the 50 tree is planted so count is 1, secondTreePlanted true (grove has at least one)
        assertTrue(midCeremony.secondTreePlanted);
        assertTrue(midCeremony.ceremonyPending);
        assertEquals(1, midCeremony.plantedTreesCount);
    }

    @Test
    void usedWhispersSurviveNewRunsAndNullRepair() {
        GameState state = GameState.newRun(12L);
        state.usedWhisperIds.put("whisper_1", Boolean.TRUE);
        state.resetForNewRun(13L);
        assertTrue(Boolean.TRUE.equals(state.usedWhisperIds.get("whisper_1")));

        GameState broken = new GameState();
        broken.usedWhisperIds = null;
        broken.validateAndRepair();
        assertTrue(broken.usedWhisperIds.isEmpty());
    }

    @Test
    void openingSnapshotAndEpilogueIdResetEachRunAndRepair() {
        GameState state = GameState.newRun(14L);
        state.openingTier = 2;
        state.epilogueId = "B";
        state.resetForNewRun(15L);
        assertEquals(-1, state.openingTier);
        assertEquals("", state.epilogueId);

        GameState broken = new GameState();
        broken.openingTier = -5;
        broken.epilogueId = null;
        broken.validateAndRepair();
        assertEquals(-1, broken.openingTier);
        assertEquals("", broken.epilogueId);
    }

    @Test
    void firstBossEncountersSurviveNewRunsAndNullRepair() {
        GameState state = GameState.newRun(16L);
        state.firstBossEncounters.put("ANCIENT_GOLEM", Boolean.TRUE);
        state.resetForNewRun(17L);
        assertTrue(Boolean.TRUE.equals(state.firstBossEncounters.get("ANCIENT_GOLEM")));

        GameState broken = new GameState();
        broken.firstBossEncounters = null;
        broken.validateAndRepair();
        assertTrue(broken.firstBossEncounters.isEmpty());
    }

    @Test
    void repairClampsAnvilLevelsAndDropsNullItems() {
        GameState state = GameState.newRun(11L);
        Item forged = new Item();
        forged.upgradeLevel = 42;
        state.inventory.add(forged);
        state.inventory.add(null);
        state.validateAndRepair();
        assertEquals(1, state.inventory.size());
        assertEquals(GameState.MAX_ITEM_UPGRADE, state.inventory.get(0).upgradeLevel);
    }
}
