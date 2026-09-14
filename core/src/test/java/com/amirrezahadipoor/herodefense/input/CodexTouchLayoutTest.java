package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class CodexTouchLayoutTest {
    @Test
    void sixVisibleRowsCoverTheListBandWithoutOverlap() {
        assertEquals(6, CodexTouchLayout.VISIBLE_ROWS);
        assertEquals(0, CodexTouchLayout.visibleRowAt(360f, 973f));
        assertEquals(5, CodexTouchLayout.visibleRowAt(360f, 553f));
        assertEquals(2, CodexTouchLayout.visibleRowAt(360f, 805f));
        assertEquals(-1, CodexTouchLayout.visibleRowAt(10f, 973f));
        assertEquals(-1, CodexTouchLayout.visibleRowAt(710f, 973f));
        assertEquals(-1, CodexTouchLayout.visibleRowAt(360f, 500f));
    }

    @Test
    void gapsBetweenRowsBelongToNoRow() {
        float rowZeroBottom = CodexTouchLayout.rowBottom(0);
        float rowOneTop = CodexTouchLayout.rowBottom(1) + CodexTouchLayout.LIST_ROW_HEIGHT;
        float gapMiddle = (rowZeroBottom + rowOneTop) * 0.5f;
        assertTrue(gapMiddle < rowZeroBottom);
        assertTrue(gapMiddle > rowOneTop);
        assertEquals(-1, CodexTouchLayout.visibleRowAt(360f, gapMiddle));
    }

    @Test
    void closeTargetSitsTopRight() {
        assertTrue(CodexTouchLayout.closeAt(620f, 1160f));
        assertFalse(CodexTouchLayout.closeAt(360f, 1160f));
        assertFalse(CodexTouchLayout.closeAt(620f, 1000f));
    }
}
