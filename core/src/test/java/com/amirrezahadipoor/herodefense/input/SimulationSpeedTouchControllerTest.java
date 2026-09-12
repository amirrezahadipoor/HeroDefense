package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class SimulationSpeedTouchControllerTest {
    @Test
    void speedTargetCyclesExactlyOneTwoThreeAndBackToOne() {
        GameState state = GameState.newRun(51L);
        SimulationSpeedTouchController touch = new SimulationSpeedTouchController();
        assertTrue(touch.tap(state, 490f, 1115f));
        assertEquals(2f, state.simulationSpeed);
        assertTrue(touch.tap(state, 490f, 1115f));
        assertEquals(3f, state.simulationSpeed);
        assertTrue(touch.tap(state, 490f, 1115f));
        assertEquals(1f, state.simulationSpeed);
    }

    @Test
    void tapsOutsideSpeedTargetDoNotMutateRun() {
        GameState state = GameState.newRun(52L);
        assertFalse(new SimulationSpeedTouchController().tap(state, 10f, 10f));
        assertEquals(1f, state.simulationSpeed);
    }
}
