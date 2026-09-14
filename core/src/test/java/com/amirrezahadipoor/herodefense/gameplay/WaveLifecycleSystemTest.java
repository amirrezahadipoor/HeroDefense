package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import org.junit.jupiter.api.Test;

final class WaveLifecycleSystemTest {
    private final EnemyWaveSpawner spawner = new EnemyWaveSpawner(new EnemyFactory());
    private final WaveLifecycleSystem lifecycle = new WaveLifecycleSystem(spawner, new ContinuousWaveRun());

    @Test
    void fullyClearedWaveAutomaticallyAdvancesAndSpawnsTheNextWave() {
        GameState state = GameState.newRun(1L);
        assertTrue(lifecycle.startCurrentWave(state));
        assertTrue(state.waveActive);
        assertTrue(state.livingEnemyCount() > 0);
        for (Enemy enemy : state.aliveEnemies) enemy.receiveDamage(Float.MAX_VALUE);

        assertEquals(WaveCompletion.NEXT_WAVE, lifecycle.updateAfterCombat(state));
        assertEquals(2, state.waveNumber);
        assertTrue(state.waveActive);
        assertTrue(state.livingEnemyCount() > 0);
    }

    @Test
    void doesNotAdvanceUntilEveryEnemyIsDead() {
        GameState state = GameState.newRun(2L);
        lifecycle.startCurrentWave(state);
        state.aliveEnemies.get(0).receiveDamage(Float.MAX_VALUE);
        assertEquals(WaveCompletion.NO_CHANGE, lifecycle.updateAfterCombat(state));
        assertEquals(1, state.waveNumber);
    }

    @Test
    void clearingWaveOneHundredPausesForThreeCardsBeforeCompletingRun() {
        GameState state = GameState.newRun(3L);
        state.waveNumber = GameState.FINAL_WAVE;
        lifecycle.startCurrentWave(state);
        state.aliveBosses.get(0).receiveDamage(Float.MAX_VALUE);

        assertEquals(WaveCompletion.BOSS_REWARD, lifecycle.updateAfterCombat(state));
        assertTrue(state.awaitingBossReward);
        assertEquals(3, state.pendingRewardCards.size());
        assertFalse(state.waveActive);

        state.awaitingBossReward = false;
        state.pendingRewardCards.clear();
        assertEquals(WaveCompletion.RUN_COMPLETED, lifecycle.continueAfterBossReward(state));
        assertTrue(state.runComplete);
        assertEquals(0, state.livingEnemyCount());
    }

    @Test
    void clearingWaveOneHundredBossFreezesCombatUntilTheCeremonyCompletes() {
        GameState state = GameState.newRun(100L);
        state.waveNumber = GameState.PLANTING_WAVE;
        assertTrue(lifecycle.startCurrentWave(state));
        assertTrue(state.aliveBosses.size() > 0);
        for (Boss boss : state.aliveBosses) boss.receiveDamage(Float.MAX_VALUE);
        assertEquals(WaveCompletion.BOSS_REWARD, lifecycle.updateAfterCombat(state));
        assertTrue(new BossRewardCardSystem().chooseCard(state, 0));

        assertEquals(WaveCompletion.PLANTING_CEREMONY, lifecycle.continueAfterBossReward(state));
        assertTrue(state.ceremonyPending);
        assertFalse(state.secondTreePlanted);
        assertEquals(GameState.PLANTING_WAVE + 1, state.waveNumber);
        assertFalse(state.waveActive);
        assertEquals(0, state.livingEnemyCount());
        assertFalse(lifecycle.startCurrentWave(state));

        assertTrue(lifecycle.completePlantingCeremony(state));
        assertFalse(state.ceremonyPending);
        assertTrue(state.secondTreePlanted);
        assertTrue(state.waveActive);
        assertTrue(state.livingEnemyCount() > 0);
        assertFalse(lifecycle.completePlantingCeremony(state));
    }

    @Test
    void reloadedSaveKeepsTheCeremonyPendingAndRepairsWaveActive() {
        GameState state = GameState.newRun(101L);
        state.waveNumber = GameState.PLANTING_WAVE + 1;
        state.ceremonyPending = true;
        state.waveActive = true;
        state.validateAndRepair();
        assertTrue(state.ceremonyPending);
        assertFalse(state.waveActive);

        GameState early = GameState.newRun(102L);
        early.waveNumber = 40;
        early.ceremonyPending = true;
        early.secondTreePlanted = true;
        early.validateAndRepair();
        assertFalse(early.ceremonyPending);
        assertFalse(early.secondTreePlanted);
    }

    @Test
    void bossWaveClearRecordsTheWaveTimerBeforeTheCardChoice() {
        GameState state = GameState.newRun(103L);
        state.waveNumber = 5;
        lifecycle.startCurrentWave(state);
        state.waveElapsedSeconds = 25f;
        state.aliveBosses.get(0).receiveDamage(Float.MAX_VALUE);

        assertEquals(WaveCompletion.BOSS_REWARD, lifecycle.updateAfterCombat(state));
        assertEquals(25f, state.fastestWaveClearSeconds);
        assertEquals(0f, state.waveElapsedSeconds);
    }

    @Test
    void waveClearsAroundALoneSilentWatcherAndDespawnsIt() {
        GameState state = GameState.newRun(104L);
        state.waveNumber = 6;
        assertTrue(lifecycle.startCurrentWave(state));
        Enemy watcher = new EnemyFactory().create(state, EnemyType.ROOTLING, 200f, 820f, 0);
        watcher.silentWatcher = true;
        state.aliveEnemies.add(watcher);
        long watcherId = watcher.id;
        for (Enemy enemy : state.aliveEnemies) {
            if (!enemy.silentWatcher) enemy.receiveDamage(Float.MAX_VALUE);
        }

        assertEquals(WaveCompletion.NEXT_WAVE, lifecycle.updateAfterCombat(state));
        assertEquals(7, state.waveNumber);
        for (Enemy enemy : state.aliveEnemies) assertFalse(enemy.id == watcherId);
    }
}
