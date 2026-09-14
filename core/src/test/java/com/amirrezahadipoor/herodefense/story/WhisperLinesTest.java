package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class WhisperLinesTest {
    @Test
    void poolHoldsSixSingleSentenceTreeLines() {
        assertEquals(6, WhisperLines.LINES.size());
        for (String line : WhisperLines.LINES) {
            long terminals = line.chars().filter(c -> c == '.' || c == '!' || c == '?').count();
            assertEquals(1, terminals, line);
        }
    }

    @Test
    void firstUnusedWalksThePoolInOrderThenStaysSilent() {
        Map<String, Boolean> used = new LinkedHashMap<>();
        for (String expected : WhisperLines.LINES) {
            String next = WhisperLines.firstUnused(used);
            assertEquals(expected, next);
            WhisperLines.markUsed(used, next);
        }
        assertNull(WhisperLines.firstUnused(used));
    }
}
