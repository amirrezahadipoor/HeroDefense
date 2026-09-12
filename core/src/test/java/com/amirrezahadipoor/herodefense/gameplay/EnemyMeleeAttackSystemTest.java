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
    void lethalFailedDodgeDestroysTreeAndReportsGameOver() {
        GameState state = GameState.newRun(31L);
        state.hero.health = 4f;
        Enemy enemy = factory.create(
            state, EnemyType.STONEKIN, state.hero.x, state.hero.y, 1
        );
        state.aliveEnemies.add(enemy);

        assertTrue(attacks.update(state, 0f));
        assertFalse(state.hero.alive);
        assertEquals(0f, state.worldTreeHealth);
    }
}
