package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.BossType;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class BossSpecialAttackSystemTest {
    private final BossFactory factory = new BossFactory();
    private final BossSpecialAttackSystem specials = new BossSpecialAttackSystem(new HeroDamageSystem());

    @Test
    void golemGroundSlamDealsOneHeavyHit() {
        Scenario scenario = scenario(BossType.ANCIENT_GOLEM, 0f);
        specials.update(scenario.state, 0f);
        assertEquals(984f, scenario.state.hero.health);
        assertEquals(1, scenario.boss.specialUseCount);
    }

    @Test
    void matriarchThornCageDamagesAndDelaysHeroAttack() {
        Scenario scenario = scenario(BossType.THORN_MATRIARCH, 0f);
        specials.update(scenario.state, 0f);
        assertEquals(995f, scenario.state.hero.health);
        assertEquals(2f, scenario.state.hero.attackCooldownSeconds);
    }

    @Test
    void wyrmFlameSweepMakesTwoSeparateDodgeAwareHits() {
        Scenario scenario = scenario(BossType.EMBER_WYRM, 0f);
        long before = scenario.state.combatRandomState;
        specials.update(scenario.state, 0f);
        assertEquals(989f, scenario.state.hero.health);
        assertTrue(before != scenario.state.combatRandomState);
    }

    @Test
    void knightVoidChargeClosesDistanceBeforeStriking() {
        Scenario scenario = scenario(BossType.VOID_KNIGHT, 300f);
        specials.update(scenario.state, 0f);
        float distance = (float) Math.sqrt(scenario.boss.distanceSquaredTo(
            scenario.state.hero.x, scenario.state.hero.y
        ));
        assertEquals(scenario.boss.attackRange, distance, 0.001f);
        assertEquals(987.5f, scenario.state.hero.health);
    }

    private Scenario scenario(BossType type, float xOffset) {
        GameState state = GameState.newRun(type.ordinal() + 100L);
        state.hero.maxHealth = 1_000f;
        state.hero.health = 1_000f;
        Boss boss = factory.create(
            state, type, state.hero.x + xOffset, state.hero.y, type.ordinal() + 1, 0
        );
        boss.damage = 10f;
        state.aliveBosses.add(boss);
        return new Scenario(state, boss);
    }

    private record Scenario(GameState state, Boss boss) {
    }
}
