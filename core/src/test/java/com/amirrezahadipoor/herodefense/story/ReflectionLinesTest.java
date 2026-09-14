package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

final class ReflectionLinesTest {
    @Test
    void reflectionsMatchStoryContentVerbatim() {
        assertEquals(
            "The wolves run from something deeper than me. That should worry me more than it does.",
            ReflectionLines.lineForWave(25)
        );
        assertEquals(
            "Half of what I've killed today, I might have known once. I try not to think about that.",
            ReflectionLines.lineForWave(50)
        );
        assertEquals(
            "The ground past the tree line doesn't feel like ground anymore.",
            ReflectionLines.lineForWave(75)
        );
        assertEquals(
            "Two trees now. Twice as much to lose. I don't recommend the trade, but I'd make it again.",
            ReflectionLines.lineForWave(125)
        );
        assertEquals(
            "It doesn't send its strongest first anymore. It's stopped being patient.",
            ReflectionLines.lineForWave(150)
        );
        assertEquals(
            "Whatever's left out there, it's the last of it. Or it wants me to think that.",
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
