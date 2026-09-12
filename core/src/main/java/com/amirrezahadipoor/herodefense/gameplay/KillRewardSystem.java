package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;

/** Grants coins and XP exactly once for every regular or boss kill. */
public final class KillRewardSystem {
    private final HeroProgressionSystem progression;

    public KillRewardSystem(HeroProgressionSystem progression) {
        this.progression = progression;
    }

    public KillRewardResult processDefeatedEnemies(GameState state) {
        if (state == null) return KillRewardResult.NONE;
        int kills = 0;
        int baseCoins = 0;
        int experience = 0;
        for (Enemy enemy : state.aliveEnemies) {
            if (claim(enemy)) {
                kills++;
                baseCoins += regularCoins(enemy, state.waveNumber);
                experience += enemy.type().experienceReward();
            }
        }
        for (Boss boss : state.aliveBosses) {
            if (claim(boss)) {
                kills++;
                baseCoins += 50 + boss.bossNumber * 20;
                experience += 100 + boss.bossNumber * 30;
            }
        }
        if (kills == 0) return KillRewardResult.NONE;

        float incomeMultiplier = 1f + effectValue(state, BossRewardCardSystem.COIN_INCOME_KEY);
        int coins = Math.max(0, Math.round(baseCoins * incomeMultiplier));
        state.coins = saturatedAdd(state.coins, coins);
        state.totalKills = saturatedAdd(state.totalKills, kills);
        state.totalKillCoinsEarned = saturatedAdd(state.totalKillCoinsEarned, coins);
        int levels = progression.grantExperience(state, experience);
        return new KillRewardResult(kills, coins, experience, levels);
    }

    private static boolean claim(Enemy enemy) {
        if (enemy == null || enemy.alive || enemy.killRewardsGranted) return false;
        enemy.killRewardsGranted = true;
        return true;
    }

    private static int regularCoins(Enemy enemy, int waveNumber) {
        float waveMultiplier = 1f + Math.max(1, waveNumber) * 0.025f;
        return Math.max(1, Math.round(enemy.type().coinReward() * waveMultiplier));
    }

    private static float effectValue(GameState state, String key) {
        Float value = state.permanentEffects.get(key);
        return value == null ? 0f : Math.max(0f, value);
    }

    private static int saturatedAdd(int left, int right) {
        long result = (long) left + right;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, result));
    }
}
