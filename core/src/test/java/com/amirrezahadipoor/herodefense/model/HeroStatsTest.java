package com.amirrezahadipoor.herodefense.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HeroStatsTest {
    @Test
    void eachBaseStatControlsItsDeclaredDerivedValue() {
        HeroStats stats = new HeroStats();
        stats.strength = 3;
        stats.agility = 4;
        stats.luck = 5;
        stats.dodge = 6;
        stats.health = 7;

        assertEquals(16f, stats.damage());
        assertEquals(1.12f, stats.attacksPerSecond(), 0.0001f);
        assertEquals(1f / 1.12f, stats.attackIntervalSeconds(), 0.0001f);
        assertEquals((float) Math.pow(1.02, 5), stats.dropChanceMultiplier(), 0.0001f);
        assertEquals(0.03f, stats.dodgeChance(), 0.0001f);
        assertEquals(170f, stats.maxHealth());
    }

    @Test
    void repairRejectsNegativeStatsAndDodgeHasASafeCap() {
        HeroStats stats = new HeroStats();
        stats.strength = -20;
        stats.agility = -1;
        stats.luck = -2;
        stats.health = -3;
        stats.dodge = 1_000;

        stats.validateAndRepair();

        assertEquals(0, stats.strength);
        assertEquals(0, stats.agility);
        assertEquals(0, stats.luck);
        assertEquals(0, stats.health);
        assertTrue(stats.dodgeChance() <= HeroStats.MAX_DODGE_CHANCE);
    }
}
