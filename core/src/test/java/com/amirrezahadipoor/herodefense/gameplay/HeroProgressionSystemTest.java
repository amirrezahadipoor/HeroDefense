package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import org.junit.jupiter.api.Test;

final class HeroProgressionSystemTest {
    private final HeroProgressionSystem system = new HeroProgressionSystem();

    @Test
    void everyEarnedLevelAwardsExactlyOnePointAndCannotExceedTheCap() {
        GameState state = GameState.newRun(1L);

        int levels = system.grantExperience(state, Integer.MAX_VALUE);

        assertEquals(HeroProgressionSystem.LEVEL_CAP - 1, levels);
        assertEquals(HeroProgressionSystem.LEVEL_CAP, state.heroLevel);
        assertEquals(HeroProgressionSystem.LEVEL_CAP - 1, state.unspentTalentPoints);
        assertEquals(0, state.heroExperience);
        assertEquals(0, system.grantExperience(state, 1_000));
    }

    @Test
    void allocationConsumesOnePointAndHealthPointRaisesCurrentAndMaximumHp() {
        GameState state = GameState.newRun(2L);
        state.unspentTalentPoints = 2;
        state.hero.health = 60f;

        assertTrue(system.allocateTalentPoint(state, HeroStat.HEALTH));
        assertEquals(110f, state.hero.maxHealth);
        assertEquals(70f, state.hero.health);
        assertEquals(1, state.unspentTalentPoints);
        assertTrue(system.allocateTalentPoint(state, HeroStat.STRENGTH));
        assertEquals(12f, state.hero.damagePerAttack());
        assertFalse(system.allocateTalentPoint(state, HeroStat.LUCK));
    }

    @Test
    void levelsPastOneHundredCostProgressivelyMoreExperience() {
        int atHundred = system.experienceRequiredForNextLevel(100);
        int atHundredOne = system.experienceRequiredForNextLevel(101);
        int atOneFifty = system.experienceRequiredForNextLevel(150);
        assertEquals(50 + 100 * 25, atHundred);
        assertTrue(atHundredOne > 50 + 101 * 25);
        assertTrue(atOneFifty > atHundredOne * 3);
        assertEquals(0, system.experienceRequiredForNextLevel(HeroProgressionSystem.LEVEL_CAP));
    }
}
