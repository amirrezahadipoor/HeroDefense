package com.amirrezahadipoor.herodefense.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(100, state.waveNumber);
        assertEquals(1, state.heroLevel);
        assertEquals(0, state.coins);
        assertEquals(1f, state.simulationSpeed);
    }
}
