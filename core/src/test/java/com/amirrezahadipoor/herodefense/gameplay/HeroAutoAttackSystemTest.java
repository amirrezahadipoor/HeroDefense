package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class HeroAutoAttackSystemTest {
    private final HeroAutoAttackSystem system = new HeroAutoAttackSystem();

    @Test
    void selectsNearestLivingTargetInRangeAndProjectileDealsStrengthDamage() {
        GameState state = GameState.newRun(10L);
        state.hero.stats.strength = 3;
        Enemy farther = enemy(state, 300f, 100f);
        Enemy nearest = enemy(state, 90f, 100f);
        state.aliveEnemies.add(farther);
        state.aliveEnemies.add(nearest);

        system.update(state, 0f);

        assertEquals(nearest.id, state.hero.currentTargetId);
        assertEquals(1, state.projectiles.size());
        assertEquals(16f, state.projectiles.get(0).damage);

        system.update(state, 0.2f);
        assertEquals(84f, nearest.health);
        assertEquals(100f, farther.health);
    }

    @Test
    void ignoresTargetsOutsideBowRange() {
        GameState state = GameState.newRun(11L);
        Enemy outside = enemy(state, HeroAutoAttackSystem.ATTACK_RANGE + 1f, 0f);
        state.aliveEnemies.add(outside);

        system.update(state, 0f);

        assertEquals(-1L, state.hero.currentTargetId);
        assertEquals(0, state.projectiles.size());
    }

    @Test
    void persistentPowerAndLifestealEffectsApplyToTheVeryNextAttack() {
        GameState state = GameState.newRun(13L);
        state.permanentEffects.put("generalPower", 0.5f);
        state.permanentEffects.put("lifesteal", 0.1f);
        state.hero.health = 50f;
        Enemy target = enemy(state, 90f, 0f);
        state.aliveEnemies.add(target);

        system.update(state, 0f);
        assertEquals(15f, state.projectiles.get(0).damage);
        system.update(state, 0.2f);
        assertEquals(51.5f, state.hero.health);
    }

    @Test
    void agilityDerivedIntervalIsAppliedToCooldown() {
        GameState state = GameState.newRun(12L);
        state.hero.stats.agility = 10;
        state.aliveEnemies.add(enemy(state, 50f, 0f));

        system.update(state, 0f);

        assertEquals(state.hero.attackIntervalSeconds(), state.hero.attackCooldownSeconds, 0.0001f);
    }

    private static Enemy enemy(GameState state, float offsetX, float offsetY) {
        Enemy enemy = new Enemy(
            state.allocateEntityId(),
            "ROOTLING",
            state.hero.x + offsetX,
            state.hero.y + offsetY
        );
        enemy.health = 100f;
        enemy.maxHealth = 100f;
        return enemy;
    }
}
