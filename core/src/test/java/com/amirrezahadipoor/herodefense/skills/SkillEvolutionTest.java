package com.amirrezahadipoor.herodefense.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SkillEvolutionTest {
    @Test
    void everySkillForksIntoExactlyTwoDocumentedEvolutions() {
        Set<String> ids = new HashSet<>();
        for (SkillId skill : SkillId.values()) {
            List<SkillEvolution> options = SkillEvolution.forSkill(skill);
            assertEquals(2, options.size(), skill.name());
            for (SkillEvolution evolution : options) {
                assertEquals(skill, evolution.skill());
                assertTrue(ids.add(evolution.id()), "duplicate id " + evolution.id());
                assertTrue(evolution.displayName() != null && !evolution.displayName().isBlank());
                assertTrue(evolution.description() != null && !evolution.description().isBlank());
                assertEquals(evolution, SkillEvolution.parse(evolution.id()));
            }
        }
        assertEquals(10, ids.size());
        assertTrue(SkillEvolution.forSkill(null).isEmpty());
        assertNull(SkillEvolution.parse(null));
        assertNull(SkillEvolution.parse("not_an_evolution"));
    }

    @Test
    void chainForkMatchesTheRoadmapNames() {
        List<SkillEvolution> options = SkillEvolution.forSkill(SkillId.CHAIN_LIGHTNING);
        assertEquals("Storm Chain", options.get(0).displayName());
        assertEquals("Vampiric Chain", options.get(1).displayName());
    }

    @Test
    void forkShortLabelsStayCompactForTheShopRow() {
        for (SkillEvolution evolution : SkillEvolution.values()) {
            assertTrue(evolution.forkShort() != null && !evolution.forkShort().isBlank());
            assertTrue(evolution.forkShort().length() <= 12,
                evolution.name() + ": " + evolution.forkShort());
        }
    }

    @Test
    void simPicksTheDocumentedHigherDpsOptionPerSkill() {
        assertEquals(
            SkillEvolution.STORM_CHAIN, SkillEvolution.simPick(SkillId.CHAIN_LIGHTNING)
        );
        assertEquals(SkillEvolution.HORNET_VOLLEY, SkillEvolution.simPick(SkillId.MULTI_SHOT));
        assertEquals(SkillEvolution.STARFALL, SkillEvolution.simPick(SkillId.STUN_CHANCE));
        assertEquals(
            SkillEvolution.EXECUTIONER, SkillEvolution.simPick(SkillId.CRITICAL_MASTERY)
        );
        assertEquals(SkillEvolution.DEADEYE, SkillEvolution.simPick(SkillId.LONG_RANGE));
        assertNull(SkillEvolution.simPick(null));
    }
}
