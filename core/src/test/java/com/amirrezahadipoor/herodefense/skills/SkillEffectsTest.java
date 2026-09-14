package com.amirrezahadipoor.herodefense.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Enemy;
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
        for (int level = 1; level <= SkillId.CORE_LEVELS; level++) {
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
    void levelReadsPersistedMapAndRejectsNegatives() {
        GameState state = GameState.newRun(7L);
        assertEquals(0, SkillEffects.level(state, SkillId.MULTI_SHOT));
        state.skillLevels.put(SkillId.MULTI_SHOT.saveKey(), 99);
        state.validateAndRepair();
        assertEquals(99, SkillEffects.level(state, SkillId.MULTI_SHOT));
        state.skillLevels.put(SkillId.MULTI_SHOT.saveKey(), -4);
        state.validateAndRepair();
        assertEquals(0, SkillEffects.level(state, SkillId.MULTI_SHOT));
    }

    @Test
    void endlessLevelsKeepGrowingWithDiminishingReturnsAndHardCeilings() {
        assertEquals(10f, SkillEffects.effectiveLevel(10), 1e-6f);
        assertEquals(15f, SkillEffects.effectiveLevel(20), 1e-6f);
        assertEquals(17.5f, SkillEffects.effectiveLevel(30), 1e-6f);
        assertTrue(SkillEffects.effectiveLevel(1000) <= 20f && SkillEffects.effectiveLevel(1000) > 19.9f);
        for (int level = 11; level <= 60; level++) {
            assertTrue(SkillEffects.effectiveLevel(level) > SkillEffects.effectiveLevel(level - 1));
            assertTrue(SkillEffects.criticalMultiplier(level) > SkillEffects.criticalMultiplier(level - 1));
        }
        float gainCore = SkillEffects.criticalMultiplier(10) - SkillEffects.criticalMultiplier(9);
        float gainEndless = SkillEffects.criticalMultiplier(11) - SkillEffects.criticalMultiplier(10);
        assertEquals(gainCore * 0.5f, gainEndless, 1e-5f);
        assertTrue(SkillEffects.chainChance(1000) <= SkillEffects.CHAIN_CHANCE_CAP);
        assertTrue(SkillEffects.stunChance(1000) <= SkillEffects.STUN_CHANCE_CAP);
        assertTrue(SkillEffects.criticalChance(1000) <= SkillEffects.CRITICAL_CHANCE_CAP);
        assertTrue(SkillEffects.extraArrows(1000) <= SkillEffects.EXTRA_ARROWS_CAP);
        assertTrue(SkillEffects.chainTargets(1000) <= SkillEffects.CHAIN_TARGETS_CAP);
        assertTrue(SkillEffects.bonusRange(1000) <= SkillEffects.BONUS_RANGE_CAP);
    }

    @Test
    void evolutionQueryIgnoresForeignIdsAndMissingMaps() {
        assertNull(SkillEffects.evolution(null, SkillId.CHAIN_LIGHTNING));
        GameState state = GameState.newRun(6L);
        assertNull(SkillEffects.evolution(state, null));
        assertNull(SkillEffects.evolution(state, SkillId.CHAIN_LIGHTNING));

        state.skillEvolutions.put(SkillId.CHAIN_LIGHTNING.saveKey(), "hornet_volley");
        assertNull(SkillEffects.evolution(state, SkillId.CHAIN_LIGHTNING));

        state.skillEvolutions.put(SkillId.CHAIN_LIGHTNING.saveKey(), "storm_chain");
        assertEquals(SkillEvolution.STORM_CHAIN,
            SkillEffects.evolution(state, SkillId.CHAIN_LIGHTNING));
    }

    @Test
    void evolutionBonusesStayNeutralUntilTheirForkIsTaken() {
        GameState plain = GameState.newRun(7L);
        assertEquals(0, SkillEffects.stormChainTargetsBonus(plain));
        assertEquals(0f, SkillEffects.vampiricHealShare(plain));
        assertEquals(0f, SkillEffects.hornetExtraArrows(plain));
        assertEquals(SkillEffects.MULTI_SHOT_DAMAGE_SHARE,
            SkillEffects.secondaryArrowShare(plain));
        assertEquals(0f, SkillEffects.deepRootsDurationBonus(plain));
        assertEquals(0f, SkillEffects.executionerMultiplierBonus(plain));
        assertEquals(0f, SkillEffects.keenEyeChanceBonus(plain));
        assertEquals(0f, SkillEffects.farstriderRangeBonus(plain));
        assertEquals(1f, SkillEffects.starfallVictimMultiplier(plain, null));
        assertEquals(1f, SkillEffects.deadeyeMultiplier(plain, 10_000f));
        assertEquals(1f, SkillEffects.deadeyeMultiplier(null, 10_000f));

        GameState storm = GameState.newRun(8L);
        storm.skillEvolutions.put(SkillId.CHAIN_LIGHTNING.saveKey(), "storm_chain");
        assertEquals(2, SkillEffects.stormChainTargetsBonus(storm));
        assertTrue(SkillEffects.stormChainStuns(storm));
        assertEquals(0f, SkillEffects.vampiricHealShare(storm));

        GameState vamp = GameState.newRun(9L);
        vamp.skillEvolutions.put(SkillId.CHAIN_LIGHTNING.saveKey(), "vampiric_chain");
        assertEquals(0.30f, SkillEffects.vampiricHealShare(vamp), 1e-6f);

        GameState multi = GameState.newRun(10L);
        multi.skillEvolutions.put(SkillId.MULTI_SHOT.saveKey(), "true_flight");
        assertEquals(1f, SkillEffects.secondaryArrowShare(multi), 1e-6f);
        assertEquals(0f, SkillEffects.hornetExtraArrows(multi));

        GameState stun = GameState.newRun(11L);
        stun.skillEvolutions.put(SkillId.STUN_CHANCE.saveKey(), "starfall");
        Enemy victim = new Enemy(1L, "ROOTLING", 0f, 0f);
        assertEquals(1f, SkillEffects.starfallVictimMultiplier(stun, victim), 1e-6f);
        victim.stunRemainingSeconds = 1f;
        assertEquals(1.25f, SkillEffects.starfallVictimMultiplier(stun, victim), 1e-6f);
        assertEquals(0f, SkillEffects.deepRootsDurationBonus(stun));

        GameState deadeye = GameState.newRun(12L);
        deadeye.skillEvolutions.put(SkillId.LONG_RANGE.saveKey(), "deadeye");
        assertEquals(1f, SkillEffects.deadeyeMultiplier(deadeye, 350f), 1e-6f);
        assertEquals(1.25f, SkillEffects.deadeyeMultiplier(deadeye, 350.01f), 1e-6f);
        assertEquals(0f, SkillEffects.farstriderRangeBonus(deadeye));
    }
}
