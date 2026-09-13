package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class UiFrameRendererTest {
    @Test
    void statePriorityIsDisabledThenPressedThenSelectedThenNormal() {
        UiFrameRenderer renderer = new UiFrameRenderer();
        assertEquals(UiFrameRenderer.State.NORMAL,
            renderer.resolve(true, false, 10f, 20f, 100f, 80f));
        assertEquals(UiFrameRenderer.State.SELECTED,
            renderer.resolve(true, true, 10f, 20f, 100f, 80f));

        renderer.press(50f, 50f);
        assertEquals(UiFrameRenderer.State.PRESSED,
            renderer.resolve(true, true, 10f, 20f, 100f, 80f));
        assertEquals(UiFrameRenderer.State.DISABLED,
            renderer.resolve(false, true, 10f, 20f, 100f, 80f));

        renderer.movePress(400f, 400f);
        assertEquals(UiFrameRenderer.State.SELECTED,
            renderer.resolve(true, true, 10f, 20f, 100f, 80f));
        renderer.release();
        assertEquals(UiFrameRenderer.State.NORMAL,
            renderer.resolve(true, false, 10f, 20f, 100f, 80f));
        renderer.close();
    }

    @Test
    void boundaryTouchesCountAsPressedForTouchMatchedVisualFeedback() {
        UiFrameRenderer renderer = new UiFrameRenderer();
        renderer.press(110f, 100f);
        assertEquals(UiFrameRenderer.State.PRESSED,
            renderer.resolve(true, false, 10f, 20f, 100f, 80f));
        renderer.release();
    }
}
