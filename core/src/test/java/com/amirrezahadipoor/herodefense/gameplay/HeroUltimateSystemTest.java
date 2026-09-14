package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class HeroUltimateSystemTest {
    private final HeroUltimateSystem ultimate = new HeroUltimateSystem();

    @Test
    void belowFullFocusFiresNothingAndKeepsCharge() {
        assertEquals(UltimateResult.NONE, ultimate.fire(null));
        GameState state = GameState.newRun(511L);
        state.aliveEnemies.add(enemy(state, 90f, 0f));
        float before = state.aliveEnemies.get(0).health;
        state.focus = state.focusMax - 1f;

        UltimateResult result = ultimate.fire(state);

        assertEquals(UltimateResult.NONE, result);
        assertFalse(result.fired());
        assertEquals(state.focusMax - 1f, state.focus, 1e-6f);
        assertEquals(before, state.aliveEnemies.get(0).health, 1e-6f);
    }

    @Test
    void fullFocusStrikesEveryFoeAndDrainsToZero() {
        assertEquals(2f, HeroUltimateSystem.ULTIMATE_DAMAGE_MULTIPLIER);
        GameState state = GameState.newRun(512L);
        for (int i = 0; i < 10; i++) {
            state.aliveEnemies.add(enemy(state, 90f + i * 30f, 0f));
        }
        Boss boss = new Boss(
            state.allocateEntityId(), "ANCIENT_GOLEM", state.hero.x + 120f, state.hero.y, 1
        );
        boss.health = boss.maxHealth = 1_000_000f;
        state.aliveBosses.add(boss);
        state.focus = state.focusMax;
        float expected =
            new HeroStatCalculator().damage(state) * HeroUltimateSystem.ULTIMATE_DAMAGE_MULTIPLIER;

        UltimateResult result = ultimate.fire(state);

        assertTrue(result.fired());
        assertEquals(11, result.foesHit());
        assertEquals(expected, result.damageEach(), 1e-4f);
        assertEquals(0f, state.focus, 1e-6f);
        for (Enemy foe : state.aliveEnemies) {
            assertEquals(1_000_000f - expected, foe.health, 0.01f);
        }
        assertEquals(1_000_000f - expected, boss.health, 0.01f);
        assertEquals(state.hero.x, result.blastX(), 1e-6f);
        assertEquals(state.hero.y + HeroUltimateSystem.BLAST_Y_OFFSET, result.blastY(), 1e-6f);
    }

    @Test
    void beamFanCapsAtEightNearestAndMarkedFoesTakeMore() {
        GameState state = GameState.newRun(513L);
        for (int i = 0; i < 10; i++) {
            state.aliveEnemies.add(enemy(state, 90f + i * 30f, 0f));
        }
        state.aliveEnemies.get(0).markRemainingSeconds = 4f;
        state.focus = state.focusMax;

        UltimateResult result = ultimate.fire(state);

        assertEquals(8, result.arcTargets().size());
        assertEquals(state.aliveEnemies.get(0), result.arcTargets().get(0));
        assertEquals(state.aliveEnemies.get(7), result.arcTargets().get(7));
        float marked = 1_000_000f - state.aliveEnemies.get(0).health;
        float plain = 1_000_000f - state.aliveEnemies.get(1).health;
        assertEquals(plain * 1.25f, marked, 0.01f);
    }

    @Test
    void emptyFieldStillConsumesTheMeter() {
        GameState state = GameState.newRun(514L);
        state.focus = state.focusMax;

        UltimateResult result = ultimate.fire(state);

        assertTrue(result.fired());
        assertEquals(0, result.foesHit());
        assertTrue(result.arcTargets().isEmpty());
        assertEquals(0f, state.focus, 1e-6f);
    }

    @Test
    void ultimateKillsPayOutThroughTheNormalSweep() {
        GameState state = GameState.newRun(515L);
        for (int i = 0; i < 3; i++) {
            Enemy weak = enemy(state, 90f + i * 30f, 0f);
            weak.health = 1f;
            state.aliveEnemies.add(weak);
        }
        state.focus = state.focusMax;
        ultimate.fire(state);

        KillRewardResult rewards = new KillRewardSystem(new HeroProgressionSystem())
            .processDefeatedEnemies(state);

        assertEquals(3, rewards.kills());
        assertTrue(state.coins > 0);
    }

    @Test
    void ultimateDamageScalesWithHeroLevelAndEquippedMythics() {
        assertEquals(2f, HeroUltimateSystem.damageMultiplier(null), 1e-6f);
        GameState state = GameState.newRun(516L);
        assertEquals(2f, HeroUltimateSystem.damageMultiplier(state), 1e-6f);

        state.heroLevel = 101;
        state.equippedItems.put(
            "WEAPON", EquipmentCatalog.byId("sunfall_last_arrow").createItem()
        );
        state.equippedItems.put(
            "HELMET", EquipmentCatalog.byId("crown_hollow_eye").createItem()
        );
        // 2 x (1 + 100 x 0.03) x (1 + 2 x 0.15) = 2 x 4 x 1.3 = 10.4.
        assertEquals(10.4f, HeroUltimateSystem.damageMultiplier(state), 1e-4f);

        state.aliveEnemies.add(enemy(state, 90f, 0f));
        state.focus = state.focusMax;
        float expected = new HeroStatCalculator().damage(state) * 10.4f;
        UltimateResult result = ultimate.fire(state);
        assertEquals(expected, result.damageEach(), 0.5f);
        assertEquals(1_000_000f - expected, state.aliveEnemies.get(0).health, 0.5f);
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
