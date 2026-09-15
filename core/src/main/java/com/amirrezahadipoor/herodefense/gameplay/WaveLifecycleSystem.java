package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.trials.TrialEffects;

/** Starts a wave and immediately rolls a cleared wave into the next one. */
public final class WaveLifecycleSystem {
    private final EnemyWaveSpawner regularSpawner;
    private final BossWaveSpawner bossSpawner;
    private final BossRewardCardSystem rewardCards;
    private final ContinuousWaveRun continuousRun;

    public WaveLifecycleSystem(EnemyWaveSpawner regularSpawner, ContinuousWaveRun continuousRun) {
        this(
            regularSpawner,
            new BossWaveSpawner(new BossFactory()),
            new BossRewardCardSystem(),
            continuousRun
        );
    }

    public WaveLifecycleSystem(
        EnemyWaveSpawner regularSpawner,
        BossWaveSpawner bossSpawner,
        BossRewardCardSystem rewardCards,
        ContinuousWaveRun continuousRun
    ) {
        this.regularSpawner = regularSpawner;
        this.bossSpawner = bossSpawner;
        this.rewardCards = rewardCards;
        this.continuousRun = continuousRun;
    }

    public boolean startCurrentWave(GameState state) {
        if (state == null || state.runComplete || state.waveActive || state.awaitingBossReward
            || state.ceremonyPending) {
            return false;
        }
        if (bossSpawner.isBossWave(state.waveNumber)) {
            bossSpawner.spawn(state, state.waveNumber);
        } else {
            regularSpawner.spawnRegularEnemies(
                state,
                state.waveNumber,
                Math.min(
                    EnemyWaveSpawner.MAX_REGULAR_ENEMIES,
                    regularSpawner.regularCountForWave(state.waveNumber)
                        + TrialEffects.extraEnemiesPerWave(state.activeTrials)
                )
            );
        }
        state.waveActive = true;
        return true;
    }

    /** Call after combat resolution. A new wave is spawned in the same update on clear. */
    public WaveCompletion updateAfterCombat(GameState state) {
        if (state == null || !state.waveActive || state.runComplete
            || state.hero == null || !state.hero.alive || state.livingEnemyCount() > 0) {
            return WaveCompletion.NO_CHANGE;
        }
        if (bossSpawner.isBossWave(state.waveNumber)) {
            int bossNumber = state.waveNumber / 5;
            state.defeatedBosses = Math.max(state.defeatedBosses, bossNumber);
            ContinuousWaveRun.recordWaveClear(state);
            state.waveActive = false;
            rewardCards.prepareChoices(state, bossNumber);
            return WaveCompletion.BOSS_REWARD;
        }

        WaveCompletion result = continuousRun.completeCurrentWave(state);
        state.waveActive = false;
        if (result == WaveCompletion.NEXT_WAVE) {
            startCurrentWave(state);
        }
        return result;
    }

    /** Called when the planting ceremony ends: a new Heartwood stands and the next wave begins. */
    public boolean completePlantingCeremony(GameState state) {
        if (state == null || !state.ceremonyPending) return false;
        state.ceremonyPending = false;
        // 32.1: generalize boolean to count with per-tree HP
        if (state.plantedTreesCount < 3) {
            state.plantedTreesCount++;
            if (state.plantedTreeHealth == null) state.plantedTreeHealth = new java.util.ArrayList<>();
            if (state.plantedTreeMaxHealth == null) state.plantedTreeMaxHealth = new java.util.ArrayList<>();
            state.plantedTreeHealth.add(state.worldTreeMaxHealth);
            state.plantedTreeMaxHealth.add(state.worldTreeMaxHealth);
        }
        state.secondTreePlanted = state.plantedTreesCount > 0;
        state.anchorHeroAtArenaCenter();
        return startCurrentWave(state);
    }

    /** Continues the same run after another system has applied and cleared one card. */
    public WaveCompletion continueAfterBossReward(GameState state) {
        if (state == null || state.awaitingBossReward || !state.pendingRewardCards.isEmpty()) {
            return WaveCompletion.NO_CHANGE;
        }
        WaveCompletion result = continuousRun.completeCurrentWave(state);
        if (result == WaveCompletion.NEXT_WAVE) {
            startCurrentWave(state);
        }
        return result;
    }
}
