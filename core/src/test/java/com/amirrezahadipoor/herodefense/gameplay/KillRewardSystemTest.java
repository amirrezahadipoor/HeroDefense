package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.BossType;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import org.junit.jupiter.api.Test;

final class KillRewardSystemTest {
    private final KillRewardSystem rewards = new KillRewardSystem(new HeroProgressionSystem());

    @Test
    void regularAndBossKillsGrantCoinsAndXpExactlyOnce() {
        GameState state = GameState.newRun(22L);
        state.waveNumber = 5;
        Enemy regular = new EnemyFactory().create(state, EnemyType.ROOTLING, 0f, 0f, 0);
        regular.receiveDamage(Float.MAX_VALUE);
        Boss boss = new BossFactory().create(state, BossType.ANCIENT_GOLEM, 0f, 0f, 1, 0);
        boss.receiveDamage(Float.MAX_VALUE);
        state.aliveEnemies.add(regular);
        state.aliveBosses.add(boss);

        KillRewardResult first = rewards.processDefeatedEnemies(state);
        int coinsAfterFirst = state.coins;
        int xpAfterFirst = state.heroExperience;
        assertEquals(2, first.kills());
        assertTrue(first.coins() > 0);
        assertTrue(first.experience() > 0);
        assertEquals(2, state.totalKills);
        assertEquals(first.coins(), state.totalKillCoinsEarned);

        assertEquals(0, rewards.processDefeatedEnemies(state).kills());
        assertEquals(coinsAfterFirst, state.coins);
        assertEquals(xpAfterFirst, state.heroExperience);
    }

    @Test
    void coinIncomeCardMultipliesTheNextKillReward() {
        GameState baseline = defeatedRootling(23L);
        GameState boosted = defeatedRootling(23L);
        boosted.permanentEffects.put(BossRewardCardSystem.COIN_INCOME_KEY, 0.5f);

        int baseCoins = rewards.processDefeatedEnemies(baseline).coins();
        int boostedCoins = rewards.processDefeatedEnemies(boosted).coins();
        assertEquals(Math.round(baseCoins * 1.5f), boostedCoins);
    }

    private static GameState defeatedRootling(long seed) {
        GameState state = GameState.newRun(seed);
        Enemy enemy = new EnemyFactory().create(state, EnemyType.ROOTLING, 0f, 0f, 0);
        enemy.receiveDamage(Float.MAX_VALUE);
        state.aliveEnemies.add(enemy);
        return state;
    }
}
