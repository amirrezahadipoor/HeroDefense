package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        HeroAttackUpdateResult impact = system.update(state, 0.2f);
        assertEquals(84f, nearest.health);
        assertEquals(100f, farther.health);
        assertEquals(1, impact.events().size());
        assertEquals(CombatEvent.Kind.HIT, impact.events().get(0).kind());
        assertEquals(16f, impact.events().get(0).amount());
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
    void deterministicCriticalProjectileDealsBonusDamageAndReportsImpact() {
        GameState state = stateWhoseNextRollCrits();
        Enemy target = enemy(state, 90f, 0f);
        state.aliveEnemies.add(target);

        HeroAttackUpdateResult fired = system.update(state, 0f);
        assertEquals(1, fired.shots());
        assertTrue(state.projectiles.get(0).critical);
        assertEquals(17.5f, state.projectiles.get(0).damage);

        HeroAttackUpdateResult impact = system.update(state, 0.2f);
        assertEquals(1, impact.hits());
        assertEquals(1, impact.criticalHits());
        assertTrue(impact.hasImpact());
        assertEquals(target.x, impact.impactX());
        assertEquals(target.y, impact.impactY());
        assertEquals(82.5f, target.health);
    }

    @Test
    void agilityDerivedIntervalIsAppliedToCooldown() {
        GameState state = GameState.newRun(12L);
        state.hero.stats.agility = 10;
        state.aliveEnemies.add(enemy(state, 50f, 0f));

        system.update(state, 0f);

        assertEquals(state.hero.attackIntervalSeconds(), state.hero.attackCooldownSeconds, 0.0001f);
    }

    private static GameState stateWhoseNextRollCrits() {
        for (long seed = 0; seed < 10_000; seed++) {
            GameState state = GameState.newRun(seed);
            long before = state.combatRandomState;
            if (state.nextCombatRandomFloat() < HeroAutoAttackSystem.CRITICAL_CHANCE) {
                state.combatRandomState = before;
                return state;
            }
        }
        throw new AssertionError("Could not find deterministic critical-hit seed");
    }

    @Test
    void multiShotAtMaxLevelFiresExtraReducedArrowsAtOtherFoes() {
        GameState state = GameState.newRun(21L);
        state.skillLevels.put(com.amirrezahadipoor.herodefense.skills.SkillId.MULTI_SHOT.saveKey(), 10);
        Enemy a = enemy(state, 90f, 0f);
        Enemy b = enemy(state, 150f, 40f);
        state.aliveEnemies.add(a);
        state.aliveEnemies.add(b);

        system.update(state, 0f);

        assertEquals(4, state.projectiles.size());
        assertEquals(10f, state.projectiles.get(0).damage);
        assertEquals(7f, state.projectiles.get(1).damage);
        assertTrue(state.projectiles.stream().anyMatch(p -> p.targetId == b.id));
    }

    @Test
    void longRangeSkillExtendsBowReach() {
        GameState state = GameState.newRun(22L);
        state.skillLevels.put(com.amirrezahadipoor.herodefense.skills.SkillId.LONG_RANGE.saveKey(), 10);
        Enemy far = enemy(state, HeroAutoAttackSystem.ATTACK_RANGE + 200f, 0f);
        state.aliveEnemies.add(far);
        system.update(state, 0f);
        assertEquals(far.id, state.hero.currentTargetId);
    }

    @Test
    void chainLightningAndStunEventuallyTriggerAndStunFreezesMovement() {
        GameState state = GameState.newRun(23L);
        state.skillLevels.put(com.amirrezahadipoor.herodefense.skills.SkillId.CHAIN_LIGHTNING.saveKey(), 10);
        state.skillLevels.put(com.amirrezahadipoor.herodefense.skills.SkillId.STUN_CHANCE.saveKey(), 10);
        Enemy struck = enemy(state, 90f, 0f);
        Enemy neighbour = enemy(state, 90f, 120f);
        struck.health = struck.maxHealth = 1_000_000f;
        neighbour.health = neighbour.maxHealth = 1_000_000f;
        state.aliveEnemies.add(struck);
        state.aliveEnemies.add(neighbour);

        int arcs = 0;
        int stuns = 0;
        boolean sawArcEvent = false;
        boolean sawStunEvent = false;
        for (int i = 0; i < 400; i++) {
            HeroAttackUpdateResult result = system.update(state, 0.25f);
            arcs += result.chainArcs();
            stuns += result.stuns();
            for (CombatEvent event : result.events()) {
                if (event.kind() == CombatEvent.Kind.CHAIN_ARC) {
                    sawArcEvent = true;
                    assertEquals(struck.x, event.fromX());
                    assertEquals(neighbour.x, event.x());
                }
                if (event.kind() == CombatEvent.Kind.STUN) sawStunEvent = true;
            }
        }
        assertTrue(arcs > 0, "chain lightning never arced");
        assertTrue(stuns > 0, "stun never rolled");
        assertTrue(sawArcEvent && sawStunEvent, "presentation events must mirror the rolls");
        assertTrue(neighbour.health < neighbour.maxHealth);

        struck.stunRemainingSeconds = 1f;
        float x = struck.x;
        new EnemyMovementSystem().update(state, 0.5f);
        assertEquals(x, struck.x);
        assertEquals(0.5f, struck.stunRemainingSeconds, 1e-5f);
    }

    @Test
    void silentWatcherIsNeverTargetedEvenPointBlank() {
        GameState state = GameState.newRun(24L);
        Enemy watcher = enemy(state, 10f, 0f);
        watcher.silentWatcher = true;
        Enemy foe = enemy(state, 200f, 0f);
        state.aliveEnemies.add(watcher);
        state.aliveEnemies.add(foe);

        system.update(state, 0f);
        assertEquals(foe.id, state.hero.currentTargetId);

        state.aliveEnemies.remove(foe);
        system.update(state, 0f);
        assertEquals(-1L, state.hero.currentTargetId);
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
