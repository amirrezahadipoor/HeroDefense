package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class EliteAffixSystemTest {
    private final EliteAffixSystem affixes = new EliteAffixSystem(new HeroDamageSystem());

    @Test
    void blightburstDetonatesOnceWithinRadius() {
        GameState state = GameState.newRun(11L);
        state.hero.maxHealth = 1_000f;
        state.hero.health = 1_000f;
        Enemy elite = deadElite(state, "blightburst", state.hero.x, state.hero.y);
        elite.damage = 10f;
        long before = state.combatRandomState;
        affixes.update(state, 0.1f);
        assertTrue(elite.affixResolved);
        assertTrue(state.hero.health < 1_000f);
        assertTrue(before != state.combatRandomState);
        float afterBlast = state.hero.health;
        affixes.update(state, 0.1f);
        assertEquals(afterBlast, state.hero.health);
    }

    @Test
    void blightburstIsHarmlessBeyondBlastRadiusButStillResolves() {
        GameState state = GameState.newRun(11L);
        state.hero.maxHealth = 1_000f;
        state.hero.health = 1_000f;
        float far = EliteAffixSystem.BLIGHT_BLAST_RADIUS + 50f;
        Enemy elite = deadElite(state, "blightburst", state.hero.x + far, state.hero.y);
        elite.damage = 10f;
        affixes.update(state, 0.1f);
        assertTrue(elite.affixResolved);
        assertEquals(1_000f, state.hero.health);
    }

    @Test
    void otherAffixDeathsResolveQuietlyAndRegularsAreIgnored() {
        GameState state = GameState.newRun(11L);
        state.hero.maxHealth = 1_000f;
        state.hero.health = 1_000f;
        Enemy rootward = deadElite(state, "rootward_ward", state.hero.x, state.hero.y);
        Enemy weeping = deadElite(state, "weeping_rot", state.hero.x, state.hero.y);
        Enemy regular = new Enemy(3L, "ROOTLING", state.hero.x, state.hero.y);
        regular.alive = false;
        state.aliveEnemies.add(regular);
        affixes.update(state, 0.1f);
        assertTrue(rootward.affixResolved);
        assertTrue(weeping.affixResolved);
        assertTrue(!regular.affixResolved);
        assertEquals(1_000f, state.hero.health);
    }

    @Test
    void rootwardShieldCyclesAndTurnsDamageAside() {
        GameState state = GameState.newRun(11L);
        Enemy elite = liveElite(state, "rootward_ward");
        elite.maxHealth = 500f;
        elite.health = 500f;
        elite.affixTimerSeconds = EliteAffixSystem.ROOTWARD_SHIELD_PERIOD
            - EliteAffixSystem.ROOTWARD_FIRST_SHIELD_DELAY;
        affixes.update(state, EliteAffixSystem.ROOTWARD_FIRST_SHIELD_DELAY);
        assertEquals(EliteAffixSystem.ROOTWARD_SHIELD_DURATION,
            elite.affixShieldRemainingSeconds, 1e-6f);
        elite.receiveDamage(100f);
        assertEquals(500f, elite.health);
        affixes.update(state, EliteAffixSystem.ROOTWARD_SHIELD_DURATION);
        assertEquals(0f, elite.affixShieldRemainingSeconds, 1e-6f);
        elite.receiveDamage(100f);
        assertEquals(400f, elite.health);
    }

    @Test
    void stunFreezesAffixClocks() {
        GameState state = GameState.newRun(11L);
        Enemy rootward = liveElite(state, "rootward_ward");
        rootward.affixTimerSeconds = 4f;
        rootward.stunRemainingSeconds = 10f;
        Enemy weeping = liveElite(state, "weeping_rot");
        weeping.stunRemainingSeconds = 10f;
        affixes.update(state, 2f);
        assertEquals(4f, rootward.affixTimerSeconds, 1e-6f);
        assertEquals(0f, rootward.affixShieldRemainingSeconds, 1e-6f);
        assertTrue(state.rotTrail.isEmpty());
        rootward.stunRemainingSeconds = 0f;
        affixes.update(state, 2f);
        assertEquals(EliteAffixSystem.ROOTWARD_SHIELD_DURATION,
            rootward.affixShieldRemainingSeconds, 1e-6f);
    }

    @Test
    void weepingLaysTrailAndBillsStandingHeroesOncePerSource() {
        GameState state = GameState.newRun(11L);
        state.hero.maxHealth = 1_000f;
        state.hero.health = 1_000f;
        Enemy elite = liveElite(state, "weeping_rot");
        elite.damage = 100f;
        long before = state.combatRandomState;
        affixes.update(state, EliteAffixSystem.WEEPING_TRAIL_INTERVAL);
        assertEquals(1, state.rotTrail.size());
        float tick = 100f * EliteAffixSystem.WEEPING_DAMAGE_SHARE
            * EliteAffixSystem.WEEPING_TRAIL_INTERVAL;
        assertEquals(1_000f - tick, state.hero.health, 1e-3f);
        affixes.update(state, EliteAffixSystem.WEEPING_TRAIL_INTERVAL);
        assertEquals(2, state.rotTrail.size());
        assertEquals(1_000f - 2f * tick, state.hero.health, 1e-3f);
        assertEquals(before, state.combatRandomState);
    }

    @Test
    void twoWeepingElitesWoundTwiceWhileOneNeverStacks() {
        GameState state = GameState.newRun(11L);
        state.hero.maxHealth = 1_000f;
        state.hero.health = 1_000f;
        Enemy first = liveElite(state, "weeping_rot");
        first.damage = 100f;
        Enemy second = liveElite(state, "weeping_rot");
        second.damage = 100f;
        affixes.update(state, EliteAffixSystem.WEEPING_TRAIL_INTERVAL);
        affixes.update(state, EliteAffixSystem.WEEPING_TRAIL_INTERVAL);
        assertEquals(4, state.rotTrail.size());
        float tick = 100f * EliteAffixSystem.WEEPING_DAMAGE_SHARE
            * EliteAffixSystem.WEEPING_TRAIL_INTERVAL;
        assertEquals(1_000f - 4f * tick, state.hero.health, 1e-3f);
    }

    @Test
    void rotSegmentsExpireAfterTheirLifetime() {
        GameState state = GameState.newRun(11L);
        Enemy elite = liveElite(state, "weeping_rot");
        affixes.update(state, EliteAffixSystem.WEEPING_TRAIL_INTERVAL);
        affixes.update(state, EliteAffixSystem.WEEPING_TRAIL_INTERVAL);
        assertEquals(2, state.rotTrail.size());
        elite.alive = false;
        affixes.update(state, EliteAffixSystem.WEEPING_SEGMENT_LIFETIME);
        assertTrue(state.rotTrail.isEmpty());
    }

    @Test
    void quietWavesSpendNoRandomness() {
        GameState state = GameState.newRun(11L);
        for (int index = 0; index < 3; index++) {
            state.aliveEnemies.add(new Enemy(10L + index, "ROOTLING", 100f, 100f));
        }
        Enemy corpse = new Enemy(20L, "ROOTLING", 100f, 100f);
        corpse.alive = false;
        state.aliveEnemies.add(corpse);
        long before = state.combatRandomState;
        affixes.update(state, 1f);
        assertEquals(before, state.combatRandomState);
        assertTrue(state.rotTrail.isEmpty());
    }

    private static Enemy liveElite(GameState state, String affix) {
        Enemy elite = new Enemy(state.allocateEntityId(), "ROOTLING", state.hero.x, state.hero.y);
        elite.eliteAffix = affix;
        state.aliveEnemies.add(elite);
        return elite;
    }

    private static Enemy deadElite(GameState state, String affix, float x, float y) {
        Enemy elite = new Enemy(state.allocateEntityId(), "ROOTLING", x, y);
        elite.alive = false;
        elite.eliteAffix = affix;
        state.aliveEnemies.add(elite);
        return elite;
    }
}
