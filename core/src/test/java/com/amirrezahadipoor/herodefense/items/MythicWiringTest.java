package com.amirrezahadipoor.herodefense.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.EnemyMovementSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroAttackUpdateResult;
import com.amirrezahadipoor.herodefense.gameplay.HeroAutoAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroDamageSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroStatCalculator;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.potions.AutoPotionSystem;
import com.amirrezahadipoor.herodefense.potions.HealthPotionSystem;
import com.amirrezahadipoor.herodefense.potions.PotionTier;
import com.amirrezahadipoor.herodefense.skills.SkillId;
import org.junit.jupiter.api.Test;

/** End-to-end proof that worn Mythics reach their combat levers. */
final class MythicWiringTest {
    private final HeroAutoAttackSystem system = new HeroAutoAttackSystem();

    @Test
    void crownCritMarksAndTheNextArrowHitsTheMarkHarder() {
        GameState state = stateWhoseNextRollCrits();
        equip(state, "HELMET", "crown_hollow_eye");
        Enemy target = enemy(state, 90f, 0f);
        target.health = target.maxHealth = 1_000_000f;
        state.aliveEnemies.add(target);

        HeroAttackUpdateResult fired = system.update(state, 0f);
        assertEquals(1, fired.shots());
        assertTrue(state.projectiles.get(0).critical);
        system.update(state, 0.2f);
        assertEquals(MythicEffects.CROWN_MARK_SECONDS, target.markRemainingSeconds, 1e-5f);

        for (int i = 0; i < 60 && state.projectiles.isEmpty(); i++) {
            system.update(state, 0.2f);
        }
        assertTrue(!state.projectiles.isEmpty(), "second arrow never fired");
        float arrow = state.projectiles.get(0).damage;
        float before = target.health;
        for (int i = 0; i < 60 && !state.projectiles.isEmpty(); i++) {
            system.update(state, 0.2f);
        }
        assertEquals(arrow * 1.25f, before - target.health, 0.01f);
    }

    @Test
    void emberlessCritRefundsPartOfTheShotCooldown() {
        GameState state = stateWhoseNextRollCrits();
        equip(state, "RING_2", "emberless_core");
        state.aliveEnemies.add(enemy(state, 90f, 0f));

        system.update(state, 0f);
        HeroAttackUpdateResult impact = system.update(state, 0.2f);
        assertEquals(1, impact.criticalHits());

        float interval = new HeroStatCalculator().attackIntervalSeconds(state);
        assertEquals(interval * 0.65f - 0.2f, state.hero.attackCooldownSeconds, 0.001f);
    }

    @Test
    void sunfallChainArcsStunWithoutAnyStunSkill() {
        GameState state = GameState.newRun(3313L);
        state.skillLevels.put(SkillId.CHAIN_LIGHTNING.saveKey(), 10);
        equip(state, "WEAPON", "sunfall_last_arrow");
        Enemy struck = enemy(state, 90f, 0f);
        Enemy neighbour = enemy(state, 90f, 120f);
        struck.health = struck.maxHealth = 1_000_000f;
        neighbour.health = neighbour.maxHealth = 1_000_000f;
        state.aliveEnemies.add(struck);
        state.aliveEnemies.add(neighbour);

        int arcs = 0;
        for (int i = 0; i < 400 && arcs == 0; i++) {
            arcs += system.update(state, 0.25f).chainArcs();
        }
        assertTrue(arcs > 0, "chain lightning never arced");
        assertTrue(neighbour.stunRemainingSeconds > 0f, "sunfall arc stunned nothing");
    }

    @Test
    void barkTenthLandedHitHealsThroughTheDamageSystem() {
        GameState state = GameState.newRun(3314L);
        equip(state, "ARMOR", "bark_first_root");
        state.hero.health = 50f;
        HeroDamageSystem damage = new HeroDamageSystem();
        for (int hit = 0; hit < 10; hit++) {
            damage.applyIncomingHit(state, 1f);
        }
        assertEquals(10, state.hero.mythicHitsTaken);
        assertEquals(40f + state.hero.maxHealth * 0.20f, state.hero.health, 1e-4f);
    }

    @Test
    void verdantAutoPotionStartsTheLifestealBuffWhichThenDecays() {
        GameState state = GameState.newRun(3315L);
        equip(state, "RING_1", "verdant_oath");
        state.hero.health = state.hero.maxHealth * 0.30f;
        state.healthPotions.set(0, 1);

        PotionTier used = new AutoPotionSystem(new HealthPotionSystem()).update(state);
        assertTrue(used != null, "auto-potion never fired");
        assertEquals(4f, state.hero.mythicLifestealRemainingSeconds, 1e-5f);
        assertEquals(0.05f, MythicEffects.verdantLifestealBonus(state), 1e-6f);

        system.update(state, 1f);
        assertEquals(3f, state.hero.mythicLifestealRemainingSeconds, 1e-5f);
    }

    @Test
    void windrunnerShortensTheAttackIntervalAsTheWaveRuns() {
        HeroStatCalculator stats = new HeroStatCalculator();
        GameState state = GameState.newRun(3316L);
        equip(state, "BOOTS", "windrunner_last_steps");
        state.waveElapsedSeconds = 0f;
        float fresh = stats.attackIntervalSeconds(state);
        state.waveElapsedSeconds = 25f;
        assertEquals(fresh / 1.25f, stats.attackIntervalSeconds(state), 1e-5f);
    }

    @Test
    void hollowEyeMarksDecayWhileTheMarkedKeepsMoving() {
        GameState state = GameState.newRun(3317L);
        Enemy foe = enemy(state, 300f, 0f);
        foe.movementSpeed = 50f;
        foe.markRemainingSeconds = 2f;
        state.aliveEnemies.add(foe);
        float x = foe.x;

        new EnemyMovementSystem().update(state, 0.5f);

        assertEquals(1.5f, foe.markRemainingSeconds, 1e-5f);
        assertTrue(foe.x != x, "marked foe must keep moving");
    }

    private static void equip(GameState state, String slot, String id) {
        state.equippedItems.put(slot, EquipmentCatalog.byId(id).createItem());
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
}
