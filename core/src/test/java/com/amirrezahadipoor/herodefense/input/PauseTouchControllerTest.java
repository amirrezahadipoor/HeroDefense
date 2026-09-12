package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.GameFlowController;
import com.amirrezahadipoor.herodefense.GameScreenState;
import org.junit.jupiter.api.Test;

final class PauseTouchControllerTest {
    @Test
    void tapsFreezeAndResumeTheSimulationGate() {
        GameFlowController flow = new GameFlowController();
        PauseTouchController touch = new PauseTouchController();
        flow.transitionTo(GameScreenState.PLAYING);
        assertTrue(flow.simulationRunning());

        assertTrue(touch.tap(flow, 630f, 1115f));
        assertEquals(GameScreenState.PAUSED, flow.state());
        assertFalse(flow.simulationRunning());

        assertTrue(touch.tap(flow, 360f, 600f));
        assertEquals(GameScreenState.PLAYING, flow.state());
        assertTrue(flow.simulationRunning());
    }
}
