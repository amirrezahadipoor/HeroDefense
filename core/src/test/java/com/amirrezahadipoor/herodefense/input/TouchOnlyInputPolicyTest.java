package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class TouchOnlyInputPolicyTest {
    @Test
    void controllerDeclaresPointerCallbacksAndNoKeyboardOrMouseOnlyBindings() {
        Set<String> declared = Arrays.stream(TouchInputController.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

        assertTrue(declared.contains("touchDown"));
        assertTrue(declared.contains("touchDragged"));
        assertTrue(declared.contains("touchUp"));
        assertFalse(declared.contains("keyDown"));
        assertFalse(declared.contains("keyUp"));
        assertFalse(declared.contains("keyTyped"));
        assertFalse(declared.contains("mouseMoved"));
        assertFalse(declared.contains("scrolled"));
    }
}
