package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class OpeningCinematicTest {
    @Test
    void zoomsInSpeaksThreeEnglishLinesAndZoomsBackOut() {
        OpeningCinematic opening = new OpeningCinematic();
        assertEquals(OpeningCinematic.Phase.IDLE, opening.phase());
        assertEquals(1f, opening.cameraZoom());
        opening.begin();

        assertEquals(OpeningCinematic.Phase.ZOOM_IN, opening.phase());
        assertNull(opening.line());
        advance(opening, OpeningCinematic.ZOOM_IN_SECONDS * 0.5f);
        assertTrue(opening.cameraZoom() < 1f && opening.cameraZoom() > OpeningCinematic.CLOSE_ZOOM);
        assertTrue(opening.cloudAlpha() > 0f && opening.cloudAlpha() < OpeningCinematic.CLOUD_MAX_ALPHA);

        advance(opening, OpeningCinematic.ZOOM_IN_SECONDS * 0.5f + 0.4f);
        assertEquals(OpeningCinematic.Phase.LINE_ONE, opening.phase());
        assertEquals("Can you protect the World Tree?!", opening.line());
        assertEquals(OpeningCinematic.CLOSE_ZOOM, opening.cameraZoom(), 0.0001f);
        assertEquals(1f, opening.cameraFocus(), 0.0001f);
        assertEquals(1f, opening.lineAlpha(), 0.0001f);

        advance(opening, OpeningCinematic.LINE_ONE_SECONDS);
        assertEquals("Can you?", opening.line());
        advance(opening, OpeningCinematic.LINE_TWO_SECONDS);
        assertEquals("Are you sure?!", opening.line());
        advance(opening, OpeningCinematic.LINE_THREE_SECONDS);
        assertEquals(OpeningCinematic.Phase.ZOOM_OUT, opening.phase());
        assertNull(opening.line());
        advance(opening, OpeningCinematic.ZOOM_OUT_SECONDS * 0.5f);
        assertTrue(opening.cameraZoom() > OpeningCinematic.CLOSE_ZOOM && opening.cameraZoom() < 1f);

        boolean finished = false;
        for (int step = 0; step < 100 && !finished; step++) finished = opening.update(0.05f);
        assertTrue(finished);
        assertFalse(opening.isActive());
        assertEquals(OpeningCinematic.Phase.DONE, opening.phase());
        assertEquals(1f, opening.cameraZoom());
        assertEquals(0f, opening.cloudAlpha());
        assertEquals(0f, opening.cameraFocus());
    }

    @Test
    void linesFadeInAndOutInsteadOfPopping() {
        OpeningCinematic opening = new OpeningCinematic();
        opening.begin();
        advance(opening, OpeningCinematic.ZOOM_IN_SECONDS + 0.02f);
        assertTrue(opening.lineAlpha() < 0.2f);
        advance(opening, OpeningCinematic.LINE_ONE_SECONDS - 0.06f);
        assertTrue(opening.lineAlpha() < 0.3f);
    }

    @Test
    void tapSkipsAndHitchesCannotSkip() {
        OpeningCinematic opening = new OpeningCinematic();
        opening.begin();
        assertFalse(opening.update(9f));
        assertTrue(opening.elapsedSeconds() <= 0.11f);
        opening.skip();
        assertTrue(opening.update(0f));
        assertFalse(opening.update(0.05f));
    }

    @Test
    void eachAscensionTierSpeaksItsOwnBeats() {
        assertTierLines(0, "Can you protect the World Tree?!", "Can you?", "Are you sure?!");
        assertTierLines(1, "Again, the dark comes.", "Again, I stand.", "This time — further.");
        assertTierLines(
            2,
            "The Hollow remembers me now.",
            "Good. Let it be afraid.",
            "Roots first. Then flesh. Then the Tree. Not today."
        );
        assertTierLines(
            3,
            "Another dawn. Another siege.",
            "The Tree does not ask twice.",
            "Neither do I."
        );
    }

    @Test
    void tierThreeAndBeyondReuseOneSet() {
        String[] tier3 = OpeningCinematic.linesForTier(3);
        String[] tier9 = OpeningCinematic.linesForTier(9);
        assertEquals(tier3[0], tier9[0]);
        assertEquals(tier3[1], tier9[1]);
        assertEquals(tier3[2], tier9[2]);
    }

    @Test
    void defaultBeginKeepsTheShippedTierZeroLines() {
        OpeningCinematic opening = new OpeningCinematic();
        opening.begin();
        advance(opening, OpeningCinematic.ZOOM_IN_SECONDS + 0.1f);
        assertEquals("Can you protect the World Tree?!", opening.line());
    }

    private static void assertTierLines(int tier, String one, String two, String three) {
        OpeningCinematic opening = new OpeningCinematic();
        opening.begin(tier);
        advance(opening, OpeningCinematic.ZOOM_IN_SECONDS + 0.1f);
        assertEquals(one, opening.line());
        advance(opening, OpeningCinematic.LINE_ONE_SECONDS);
        assertEquals(two, opening.line());
        advance(opening, OpeningCinematic.LINE_TWO_SECONDS);
        assertEquals(three, opening.line());
    }

    private static void advance(OpeningCinematic opening, float seconds) {
        float remaining = seconds;
        while (remaining > 0f) {
            float step = Math.min(0.05f, remaining);
            opening.update(step);
            remaining -= step;
        }
    }
}
