package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class EnemyMeleeAttackSystemTest {
    private final EnemyFactory factory = new EnemyFactory();
    private final EnemyMeleeAttackSystem attacks = new EnemyMeleeAttackSystem(new HeroDamageSystem());

    @Test
    void livingEnemyAttacksOnlyInsideItsMeleeRange() {
        GameState state = GameState.newRun(30L);
        Enemy enemy = factory.create(
            state, EnemyType.ROOTLING, state.hero.x - 100f, state.hero.y, 0
        );
        state.aliveEnemies.add(enemy);

        assertFalse(attacks.update(state, 0f));
        assertEquals(100f, state.hero.health);

        enemy.x = state.hero.x - enemy.attackRange;
        assertFalse(attacks.update(state, 0f));
        assertEquals(95f, state.hero.health);
        assertTrue(enemy.attackCooldownSeconds > 0f);
    }

    @Test
    void lethalHitStartsTreeSiegeBeforeTheTreeFalls() {
        GameState state = GameState.newRun(31L);
        state.hero.health = 4f;
        Enemy enemy = factory.create(
            state, EnemyType.STONEKIN, state.hero.x, state.hero.y, 1
        );
        state.aliveEnemies.add(enemy);

        assertFalse(attacks.update(state, 0f));
        assertFalse(state.hero.alive);
        assertEquals(GameState.TREE_SIEGE_SECONDS, state.treeSiegeRemainingSeconds);
        assertEquals(state.worldTreeMaxHealth, state.worldTreeHealth);

        assertFalse(attacks.update(state, GameState.TREE_SIEGE_SECONDS * 0.5f));
        assertTrue(state.worldTreeHealth > 0f);
        assertTrue(state.worldTreeHealth < state.worldTreeMaxHealth);

        assertTrue(attacks.update(state, GameState.TREE_SIEGE_SECONDS));
        assertEquals(0f, state.worldTreeHealth);
        assertTrue(attacks.update(state, 0f));
    }

    @Test
    void loadedSaveWithDeadHeroAndNoSiegeLeftEndsImmediately() {
        GameState state = GameState.newRun(32L);
        state.hero.health = 0f;
        state.hero.alive = false;
        state.treeSiegeRemainingSeconds = 0f;

        assertTrue(attacks.update(state, 0f));
        assertEquals(0f, state.worldTreeHealth);
    }

    @Test
    void silentWatcherInMeleeRangeNeverStrikesTheHero() {
        GameState state = GameState.newRun(33L);
        Enemy watcher = factory.create(
            state, EnemyType.ROOTLING, state.hero.x, state.hero.y, 0
        );
        watcher.silentWatcher = true;
        state.aliveEnemies.add(watcher);

        assertFalse(attacks.update(state, 5f));
        assertEquals(100f, state.hero.health);
        assertEquals(0f, watcher.attackCooldownSeconds);
    }
}
