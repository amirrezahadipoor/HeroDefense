package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.PlantingCeremony;
import org.junit.jupiter.api.Test;

final class CeremonyLinesTest {
    @Test
    void beatsMatchStoryContentVerbatim() {
        assertEquals(
            "One root should not hold this alone.",
            CeremonyLines.lineFor(PlantingCeremony.Phase.WALK_OUT)
        );
        assertEquals(
            "Then another one. Grow angry if you must.",
            CeremonyLines.lineFor(PlantingCeremony.Phase.PLANT)
        );
        assertEquals(
            "I will hold the line. That is my job.",
            CeremonyLines.lineFor(PlantingCeremony.Phase.WATER)
        );
        assertEquals(
            "The grove remembers your gift.",
            CeremonyLines.lineFor(PlantingCeremony.Phase.GROW)
        );
        assertEquals(
            "Now hold the grove.",
            CeremonyLines.lineFor(PlantingCeremony.Phase.WALK_BACK)
        );
        assertNull(CeremonyLines.lineFor(PlantingCeremony.Phase.IDLE));
        assertNull(CeremonyLines.lineFor(PlantingCeremony.Phase.DONE));
        assertNull(CeremonyLines.lineFor(null));
    }

    @Test
    void onlyTheGrowthBeatSpeaksInTheTreeVoice() {
        assertTrue(CeremonyLines.isTreeVoice(PlantingCeremony.Phase.GROW));
        assertFalse(CeremonyLines.isTreeVoice(PlantingCeremony.Phase.WALK_OUT));
        assertFalse(CeremonyLines.isTreeVoice(PlantingCeremony.Phase.PLANT));
        assertFalse(CeremonyLines.isTreeVoice(PlantingCeremony.Phase.WATER));
        assertFalse(CeremonyLines.isTreeVoice(PlantingCeremony.Phase.WALK_BACK));
        assertFalse(CeremonyLines.isTreeVoice(null));
    }
}
