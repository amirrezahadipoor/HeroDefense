package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class CodexOverlayRendererTest {
    @Test
    void shortTextStaysOnOneLine() {
        List<String> lines = CodexOverlayRenderer.wrapLines("Hello grove", s -> s.length() * 10.0, 500f);
        assertEquals(List.of("Hello grove"), lines);
    }

    @Test
    void longTextWrapsGreedilyAtWordBoundaries() {
        List<String> lines = CodexOverlayRenderer.wrapLines(
            "aa bb cc dd", s -> s.length() * 10.0, 55f);
        assertEquals(List.of("aa bb", "cc dd"), lines);
    }

    @Test
    void paragraphBreaksSurviveAsBlankLines() {
        List<String> lines = CodexOverlayRenderer.wrapLines(
            "first\n\nsecond", s -> s.length() * 10.0, 500f);
        assertEquals(List.of("first", "", "second"), lines);
    }

    @Test
    void oversizedWordsKeepTheirOwnLineInsteadOfVanishing() {
        List<String> lines = CodexOverlayRenderer.wrapLines(
            "ok pneumonoultramicroscopicsilicovolcanoconiosis ok", s -> s.length() * 10.0, 100f);
        assertEquals(3, lines.size());
        assertEquals("ok", lines.get(0));
        assertTrue(lines.get(2).equals("ok"));
    }

    @Test
    void nullTextWrapsToNothing() {
        assertTrue(CodexOverlayRenderer.wrapLines(null, s -> 1.0, 100f).isEmpty());
    }
}
