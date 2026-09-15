package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

final class ReflectionLinesTest {
    @Test
    void reflectionsMatchStoryContentVerbatim() {
        assertEquals(
            "Wolves fear something deeper than me. That should scare me more.",
            ReflectionLines.lineForWave(25)
        );
        assertEquals(
            "Half of what I killed, I once knew. I try not to think of it.",
            ReflectionLines.lineForWave(50)
        );
        assertEquals(
            "Ground past the tree line feels wrong. Not ground at all.",
            ReflectionLines.lineForWave(75)
        );
        assertEquals(
            "Two trees now. Twice to lose. Bad trade. I would still make it.",
            ReflectionLines.lineForWave(125)
        );
        assertEquals(
            "It no longer sends weak first. It is done waiting.",
            ReflectionLines.lineForWave(150)
        );
        assertEquals(
            "What is left may be the last. Or it wants me to think so.",
            ReflectionLines.lineForWave(175)
        );
    }

    @Test
    void otherWavesStaySilent() {
        for (int wave : new int[] {1, 5, 10, 15, 20, 24, 26, 100, 101, 200}) {
            assertNull(ReflectionLines.lineForWave(wave), "wave " + wave);
        }
    }
}
