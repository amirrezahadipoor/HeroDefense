package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.GameFlowController;
import com.amirrezahadipoor.herodefense.GameScreenState;

/** Applies pause and resume transitions exclusively from HUD/menu taps. */
public final class PauseTouchController {
    public boolean tap(GameFlowController flow, float x, float y) {
        if (flow == null) return false;
        if (flow.state() == GameScreenState.PLAYING && HudTouchLayout.pauseAt(x, y)) {
            flow.transitionTo(GameScreenState.PAUSED);
            return true;
        }
        if (flow.state() == GameScreenState.PAUSED && PauseTouchLayout.resumeAt(x, y)) {
            flow.returnFromOverlay();
            return true;
        }
        return false;
    }
}
