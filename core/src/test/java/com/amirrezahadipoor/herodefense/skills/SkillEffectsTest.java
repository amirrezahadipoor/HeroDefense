package com.amirrezahadipoor.herodefense.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class SkillEffectsTest {
    @Test
    void levelZeroLeavesBaselineCombatUntouched() {
        assertEquals(0f, SkillEffects.chainChance(0));
        assertEquals(0f, SkillEffects.extraArrows(0));
        assertEquals(0f, SkillEffects.stunChance(0));
        assertEquals(SkillEffects.BASE_CRITICAL_CHANCE, SkillEffects.criticalChance(0));
        assertEquals(SkillEffects.BASE_CRITICAL_MULTIPLIER, SkillEffects.criticalMultiplier(0));
        assertEquals(0f, SkillEffects.bonusRange(0));
    }

    @Test
    void criticalMasteryDoublesChanceAndReachesTwoAndAHalfTimesDamageAtMaxLevel() {
        assertEquals(SkillEffects.BASE_CRITICAL_CHANCE * 2f, SkillEffects.criticalChance(10), 1e-6f);
        assertEquals(2.5f, SkillEffects.criticalMultiplier(10), 1e-6f);
    }

    @Test
    void everyEffectGrowsMonotonicallyThroughTenLevels() {
        for (int level = 1; level <= SkillId.MAX_LEVEL; level++) {
            assertTrue(SkillEffects.chainChance(level) > SkillEffects.chainChance(level - 1));
            assertTrue(SkillEffects.extraArrows(level) > SkillEffects.extraArrows(level - 1));
            assertTrue(SkillEffects.stunChance(level) > SkillEffects.stunChance(level - 1));
            assertTrue(SkillEffects.stunDuration(level) >= SkillEffects.stunDuration(level - 1));
            assertTrue(SkillEffects.criticalChance(level) > SkillEffects.criticalChance(level - 1));
            assertTrue(SkillEffects.criticalMultiplier(level) > SkillEffects.criticalMultiplier(level - 1));
            assertTrue(SkillEffects.bonusRange(level) > SkillEffects.bonusRange(level - 1));
            assertTrue(SkillEffects.chainTargets(level) >= SkillEffects.chainTargets(level - 1));
        }
        assertEquals(1, SkillEffects.chainTargets(1));
        assertEquals(4, SkillEffects.chainTargets(10));
        assertEquals(3f, SkillEffects.extraArrows(10), 1e-6f);
    }

    @Test
    void levelReadsPersistedMapAndClampsGarbage() {
        GameState state = GameState.newRun(7L);
        assertEquals(0, SkillEffects.level(state, SkillId.MULTI_SHOT));
        state.skillLevels.put(SkillId.MULTI_SHOT.saveKey(), 99);
        state.validateAndRepair();
        assertEquals(SkillId.MAX_LEVEL, SkillEffects.level(state, SkillId.MULTI_SHOT));
    }
}
