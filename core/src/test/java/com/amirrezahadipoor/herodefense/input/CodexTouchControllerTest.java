package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class CodexTouchControllerTest {
    @Test
    void openStartsUnselectedAtTheTop() {
        CodexTouchController controller = new CodexTouchController();
        assertFalse(controller.isOpen());
        controller.open();
        assertTrue(controller.isOpen());
        assertEquals(-1, controller.selectedIndex());
        assertEquals(0, controller.firstVisibleIndex());
    }

    @Test
    void tappingARowSelectsItsEntry() {
        CodexTouchController controller = new CodexTouchController();
        controller.open();
        GameState state = GameState.newRun(31L);
        assertEquals(CodexTouchController.Action.SELECTED, controller.tap(state, 360f, 973f));
        assertEquals(0, controller.selectedIndex());
        assertEquals(CodexTouchController.Action.SELECTED, controller.tap(state, 360f, 553f));
        assertEquals(5, controller.selectedIndex());
        assertEquals(CodexTouchController.Action.NONE, controller.tap(state, 360f, 500f));
        assertEquals(5, controller.selectedIndex());
    }

    @Test
    void tappingCloseClosesAndIgnoresFurtherTaps() {
        CodexTouchController controller = new CodexTouchController();
        controller.open();
        GameState state = GameState.newRun(32L);
        assertEquals(CodexTouchController.Action.CLOSED, controller.tap(state, 620f, 1160f));
        assertFalse(controller.isOpen());
        assertEquals(CodexTouchController.Action.NONE, controller.tap(state, 360f, 973f));
    }

    @Test
    void dragScrollsTheListWithinItsBounds() {
        CodexTouchController controller = new CodexTouchController();
        controller.open();
        GameState state = GameState.newRun(33L);
        controller.drag(state, 55f);
        assertEquals(1, controller.firstVisibleIndex());
        controller.drag(state, 55f * 100f);
        assertEquals(24, controller.firstVisibleIndex());
        controller.drag(state, -55f * 100f);
        assertEquals(0, controller.firstVisibleIndex());

        controller.drag(state, 55f * 24f);
        assertEquals(CodexTouchController.Action.SELECTED, controller.tap(state, 360f, 973f));
        assertEquals(24, controller.selectedIndex());
    }

    @Test
    void closedControllerIgnoresDrag() {
        CodexTouchController controller = new CodexTouchController();
        controller.drag(GameState.newRun(34L), 500f);
        assertEquals(0, controller.firstVisibleIndex());
    }
}
