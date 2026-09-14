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

    private static Enemy deadElite(GameState state, String affix, float x, float y) {
        Enemy elite = new Enemy(state.allocateEntityId(), "ROOTLING", x, y);
        elite.alive = false;
        elite.eliteAffix = affix;
        state.aliveEnemies.add(elite);
        return elite;
    }
}
