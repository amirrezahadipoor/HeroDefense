package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.ascension.RootNetworkSystem;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Touch controller for Root Network following Shop pattern. */
public final class RootNetworkTouchController {
    public enum Action { NONE, CLOSED, PURCHASED }

    private final RootNetworkSystem system;

    public RootNetworkTouchController(RootNetworkSystem system) {
        this.system = system;
    }

    public Action tap(GameState state, float x, float y) {
        if (RootNetworkTouchLayout.closeAt(x, y)) return Action.CLOSED;
        String nodeId = RootNetworkTouchLayout.nodeAt(x, y);
        if (nodeId != null && system.purchase(state, nodeId)) {
            return Action.PURCHASED;
        }
        return Action.NONE;
    }
}
