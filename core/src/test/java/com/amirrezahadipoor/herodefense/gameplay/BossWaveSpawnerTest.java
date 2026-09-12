package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.BossType;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class BossWaveSpawnerTest {
    private final BossWaveSpawner spawner = new BossWaveSpawner(new BossFactory());

    @Test
    void rotatesFourBossesAcrossAllTwentyFifthWaveEncounters() {
        GameState state = GameState.newRun(6L);
        BossType[] rotation = BossType.values();
        for (int bossNumber = 1; bossNumber <= 20; bossNumber++) {
            int wave = bossNumber * 5;
            Boss boss = spawner.spawn(state, wave);
            assertEquals(bossNumber, boss.bossNumber);
            assertEquals(rotation[(bossNumber - 1) % 4], boss.bossDefinition());
        }
        assertEquals(20, state.aliveBosses.size());
    }

    @Test
    void spawnedBossReceivesMilestoneWaveMultipliers() {
        GameState state = GameState.newRun(7L);
        Boss boss = spawner.spawn(state, 25);
        DifficultyCurve curve = new DifficultyCurve();
        assertEquals(curve.bossHealth(25), boss.maxHealth, 0.001f);
        assertEquals(curve.bossDamage(25), boss.damage, 0.001f);
    }

    @Test
    void onlyPositiveFifthWavesThroughOneHundredAreBossWaves() {
        assertFalse(spawner.isBossWave(1));
        assertTrue(spawner.isBossWave(5));
        assertTrue(spawner.isBossWave(100));
        assertFalse(spawner.isBossWave(101));
    }
}
