package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** Locks the verbatim §4 "Whispering Wounds" fragments and their I/II alternation. */
final class EliteFragmentsTest {
    @Test
    void oddKillsShowFragmentOneAndEvenKillsFragmentTwo() {
        assertEquals(
            "It doesn't die so much as let go. Whatever was holding it together was never its own to keep.",
            EliteFragments.fragmentFor("blightburst", 1));
        assertEquals("The burst isn't rage. It's relief.",
            EliteFragments.fragmentFor("blightburst", 2));
        assertEquals(
            "The shield isn't armor. It's a root, briefly remembering what it was for.",
            EliteFragments.fragmentFor("rootward_ward", 3));
        assertEquals(
            "Even corrupted, something in it still tries to protect something. It's just no longer sure what.",
            EliteFragments.fragmentFor("rootward_ward", 4));
        assertEquals("The ground it crosses doesn't heal. Not yet. Maybe not ever.",
            EliteFragments.fragmentFor("weeping_rot", 5));
        assertEquals(
            "Every trail leads back the same direction, if you follow it far enough: toward the Tree.",
            EliteFragments.fragmentFor("weeping_rot", 6));
    }

    @Test
    void unknownAffixesShowNothingAndBadCountsClampToOne() {
        assertNull(EliteFragments.fragmentFor("unknown_affix", 1));
        assertNull(EliteFragments.fragmentFor(null, 1));
        assertEquals(EliteFragments.fragmentFor("blightburst", 1),
            EliteFragments.fragmentFor("blightburst", 0));
    }
}
