package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.model.GameState;

/** Cycles the persisted simulation multiplier 1x -> 2x -> 3x -> 1x from one tap target. */
public final class SimulationSpeedTouchController {
    public boolean tap(GameState state, float x, float y) {
        if (state == null || !HudTouchLayout.speedAt(x, y)) return false;
        if (state.simulationSpeed == 1f) {
            state.simulationSpeed = 2f;
        } else if (state.simulationSpeed == 2f) {
            state.simulationSpeed = 3f;
        } else {
            state.simulationSpeed = 1f;
        }
        return true;
    }
}
