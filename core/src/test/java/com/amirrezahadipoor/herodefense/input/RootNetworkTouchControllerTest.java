package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.ascension.RootNetworkSystem;
import com.amirrezahadipoor.herodefense.input.RootNetworkTouchController.Action;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

/** Phase 27.0: Root Network tap paths across wallet/node states. */
final class RootNetworkTouchControllerTest {
    private final RootNetworkTouchController controller =
        new RootNetworkTouchController(new RootNetworkSystem());

    @Test
    void closeTapCloses() {
        Action action = controller.tap(
            new GameState(),
            RootNetworkTouchLayout.CLOSE_X + 10f,
            RootNetworkTouchLayout.CLOSE_Y + 10f);
        assertEquals(Action.CLOSED, action);
    }

    @Test
    void affordableNodeTapPurchases() {
        GameState state = new GameState();
        state.heartwood = 100;
        Action action = controller.tap(state, 360f, 280f);
        assertEquals(Action.PURCHASED, action);
        assertEquals(85, state.heartwood);
        assertTrue(state.rootNodesPurchased.getOrDefault("root_strength_1", false));
    }

    @Test
    void brokeNodeTapStaysSilent() {
        GameState state = new GameState();
        state.heartwood = 0;
        assertEquals(Action.NONE, controller.tap(state, 360f, 280f));
    }

    @Test
    void ownedNodeRetapStaysSilent() {
        GameState state = new GameState();
        state.heartwood = 100;
        assertEquals(Action.PURCHASED, controller.tap(state, 360f, 280f));
        assertEquals(Action.NONE, controller.tap(state, 360f, 280f));
    }

    @Test
    void emptyArenaTapStaysSilent() {
        assertEquals(Action.NONE, controller.tap(new GameState(), 10f, 10f));
    }
}
