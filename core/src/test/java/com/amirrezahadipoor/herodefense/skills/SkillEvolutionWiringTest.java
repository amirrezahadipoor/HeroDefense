package com.amirrezahadipoor.herodefense.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.amirrezahadipoor.herodefense.gameplay.HeroAutoAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroStatCalculator;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

/** Every Evolution fork changes combat exactly as its description promises. */
final class SkillEvolutionWiringTest {
    private final HeroAutoAttackSystem system = new HeroAutoAttackSystem();

    @Test
    void stormChainAddsTwoArcTargets() {
        for (long seed = 0; seed < 200; seed++) {
            GameState plain = chainState(seed, null);
            GameState storm = chainState(seed, SkillEvolution.STORM_CHAIN);
            system.update(plain, 0f);
            system.update(storm, 0f);
            int plainArcs = system.update(plain, 0.2f).chainArcs();
            int stormArcs = system.update(storm, 0.2f).chainArcs();
            if (plainArcs == 0) continue;
            assertEquals(4, plainArcs, "level-10 baseline arcs");
            assertEquals(6, stormArcs, "storm adds exactly two");
            return;
        }
        fail("no chain proc found in 200 seeds");
    }

    @Test
    void stormArcsStunForOneSecond() {
        for (long seed = 0; seed < 200; seed++) {
            GameState storm = chainState(seed, SkillEvolution.STORM_CHAIN);
            system.update(storm, 0f);
            if (system.update(storm, 0.2f).chainArcs() == 0) continue;
            float stunned = 0f;
            for (Enemy victim : storm.aliveEnemies) {
                stunned = Math.max(stunned, victim.stunRemainingSeconds);
            }
            if (stunned <= 0f) continue;
            assertEquals(SkillEffects.STORM_STUN_SECONDS, stunned, 1e-6f);
            return;
        }
        fail("no storm stun found in 200 seeds");
    }

    @Test
    void vampiricChainHealsThirtyPercentOfArcDamage() {
        for (long seed = 0; seed < 200; seed++) {
            GameState plain = chainState(seed, null);
            GameState vamp = chainState(seed, SkillEvolution.VAMPIRIC_CHAIN);
            plain.hero.health = 50f;
            vamp.hero.health = 50f;
            system.update(plain, 0f);
            system.update(vamp, 0f);
            if (system.update(plain, 0.2f).chainArcs() == 0) continue;
            system.update(vamp, 0.2f);
            assertEquals(50f, plain.hero.health, 1e-6f);
            // Arc victims only: the struck foe (largest single hit) is excluded.
            float struck = 0f;
            float dealt = 0f;
            for (Enemy victim : vamp.aliveEnemies) {
                float damage = 1_000_000f - victim.health;
                dealt += damage;
                struck = Math.max(struck, damage);
            }
            dealt -= struck;
            assertTrue(dealt > 0f, "arcs dealt no damage");
            assertEquals(
                50f + dealt * SkillEffects.VAMPIRIC_HEAL_SHARE, vamp.hero.health, 0.01f
            );
            return;
        }
        fail("no chain proc found in 200 seeds");
    }

    @Test
    void hornetVolleyAddsTwoExtraArrows() {
        GameState plain = GameState.newRun(601L);
        GameState hornet = GameState.newRun(601L);
        for (GameState state : new GameState[] {plain, hornet}) {
            state.skillLevels.put(SkillId.MULTI_SHOT.saveKey(), 10);
            state.aliveEnemies.add(enemy(state, 90f, 0f));
        }
        hornet.skillEvolutions.put(SkillId.MULTI_SHOT.saveKey(), "hornet_volley");

        system.update(plain, 0f);
        system.update(hornet, 0f);

        assertEquals(4, plain.projectiles.size());
        assertEquals(6, hornet.projectiles.size());
        assertEquals(5, hornet.projectiles.stream().filter(p -> p.secondary).count());
    }

    @Test
    void trueFlightSecondariesDealFullDamage() {
        GameState state = GameState.newRun(602L);
        state.skillLevels.put(SkillId.MULTI_SHOT.saveKey(), 10);
        state.skillEvolutions.put(SkillId.MULTI_SHOT.saveKey(), "true_flight");
        state.aliveEnemies.add(enemy(state, 90f, 0f));
        state.aliveEnemies.add(enemy(state, 150f, 40f));

        system.update(state, 0f);

        float base = new HeroStatCalculator().damage(state);
        assertTrue(state.projectiles.stream().anyMatch(p -> p.secondary));
        for (var projectile : state.projectiles) {
            float ratio = projectile.damage / base;
            boolean plain = Math.abs(ratio - 1f) < 1e-4f;
            boolean crit = Math.abs(ratio - SkillEffects.criticalMultiplier(0)) < 1e-4f;
            assertTrue(plain || crit, "secondary reduced to " + ratio);
        }
    }

    @Test
    void deepRootsExtendsStunDuration() {
        for (long seed = 0; seed < 500; seed++) {
            GameState state = GameState.newRun(seed);
            state.skillLevels.put(SkillId.STUN_CHANCE.saveKey(), 10);
            state.skillEvolutions.put(SkillId.STUN_CHANCE.saveKey(), "deep_roots");
            Enemy target = enemy(state, 90f, 0f);
            state.aliveEnemies.add(target);
            system.update(state, 0f);
            system.update(state, 0.2f);
            if (target.stunRemainingSeconds <= 0f) continue;
            assertEquals(2.15f, target.stunRemainingSeconds, 1e-3f);
            return;
        }
        fail("no stun proc found in 500 seeds");
    }

    @Test
    void starfallBonusAppliesToStunnedVictims() {
        GameState state = GameState.newRun(603L);
        state.skillLevels.put(SkillId.STUN_CHANCE.saveKey(), 10);
        state.skillEvolutions.put(SkillId.STUN_CHANCE.saveKey(), "starfall");
        Enemy target = enemy(state, 90f, 0f);
        target.stunRemainingSeconds = 5f;
        state.aliveEnemies.add(target);

        system.update(state, 0f);
        float arrow = state.projectiles.get(0).damage;
        for (int i = 0; i < 60 && target.health == 1_000_000f; i++) {
            system.update(state, 0.2f);
        }
        assertEquals(arrow * 1.25f, 1_000_000f - target.health, 0.01f);
    }

    @Test
    void executionerRaisesCriticalMultiplier() {
        for (long seed = 0; seed < 500; seed++) {
            GameState state = GameState.newRun(seed);
            state.skillLevels.put(SkillId.CRITICAL_MASTERY.saveKey(), 10);
            state.skillEvolutions.put(SkillId.CRITICAL_MASTERY.saveKey(), "executioner");
            state.aliveEnemies.add(enemy(state, 90f, 0f));
            system.update(state, 0f);
            if (!state.projectiles.get(0).critical) continue;
            float base = new HeroStatCalculator().damage(state);
            assertEquals(3.0f, state.projectiles.get(0).damage / base, 1e-5f);
            return;
        }
        fail("no crit found in 500 seeds");
    }

    @Test
    void keenEyeRaisesCriticalThresholdByTenPercent() {
        boolean found = false;
        for (long seed = 0; seed < 2000; seed++) {
            GameState plain = GameState.newRun(seed);
            GameState keen = GameState.newRun(seed);
            for (GameState state : new GameState[] {plain, keen}) {
                state.skillLevels.put(SkillId.CRITICAL_MASTERY.saveKey(), 10);
                state.aliveEnemies.add(enemy(state, 90f, 0f));
            }
            keen.skillEvolutions.put(SkillId.CRITICAL_MASTERY.saveKey(), "keen_eye");
            system.update(plain, 0f);
            system.update(keen, 0f);
            boolean plainCrit = plain.projectiles.get(0).critical;
            boolean keenCrit = keen.projectiles.get(0).critical;
            if (plainCrit && !keenCrit) {
                fail("same-seed rolls misaligned at seed " + seed);
            }
            if (!plainCrit && keenCrit) {
                found = true;
                break;
            }
        }
        assertTrue(found, "no threshold-straddling roll found");
    }

    @Test
    void farstriderExtendsAttackRange() {
        GameState state = GameState.newRun(604L);
        state.skillLevels.put(SkillId.LONG_RANGE.saveKey(), 10);
        state.skillEvolutions.put(SkillId.LONG_RANGE.saveKey(), "farstrider");
        assertEquals(790f, HeroAutoAttackSystem.attackRange(state), 1e-6f);
    }

    @Test
    void deadeyeBonusAppliesBeyond350Units() {
        GameState near = GameState.newRun(605L);
        GameState far = GameState.newRun(605L);
        for (GameState state : new GameState[] {near, far}) {
            state.skillLevels.put(SkillId.LONG_RANGE.saveKey(), 10);
            state.skillEvolutions.put(SkillId.LONG_RANGE.saveKey(), "deadeye");
        }
        near.aliveEnemies.add(enemy(near, 300f, 0f));
        far.aliveEnemies.add(enemy(far, 400f, 0f));
        system.update(near, 0f);
        system.update(far, 0f);
        assertEquals(
            1.25f,
            far.projectiles.get(0).damage / near.projectiles.get(0).damage,
            1e-5f
        );
    }

    /** Level-10 chain setup: primary plus eight victims inside the arc radius. */
    private GameState chainState(long seed, SkillEvolution evolution) {
        GameState state = GameState.newRun(seed);
        state.skillLevels.put(SkillId.CHAIN_LIGHTNING.saveKey(), 10);
        if (evolution != null) {
            state.skillEvolutions.put(SkillId.CHAIN_LIGHTNING.saveKey(), evolution.id());
        }
        state.aliveEnemies.add(enemy(state, 90f, 0f));
        for (int i = 0; i < 8; i++) {
            state.aliveEnemies.add(enemy(state, 110f + i * 20f, 0f));
        }
        return state;
    }

    private static Enemy enemy(GameState state, float offsetX, float offsetY) {
        Enemy enemy = new Enemy(
            state.allocateEntityId(),
            "ROOTLING",
            state.hero.x + offsetX,
            state.hero.y + offsetY
        );
        enemy.health = enemy.maxHealth = 1_000_000f;
        return enemy;
    }
}
