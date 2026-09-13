package com.amirrezahadipoor.herodefense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class GameFlowControllerTest {
    @Test
    void exposesEveryRequiredStateAndStartsAtMenu() {
        assertEquals(10, GameScreenState.values().length);
        assertEquals(GameScreenState.MENU, new GameFlowController().state());
    }

    @Test
    void pausesAndReturnsToPlaying() {
        GameFlowController flow = new GameFlowController();
        flow.transitionTo(GameScreenState.PLAYING);
        flow.transitionTo(GameScreenState.PAUSED);
        flow.returnFromOverlay();
        assertEquals(GameScreenState.PLAYING, flow.state());
    }

    @Test
    void inventoryFreezesPlayAndReturnsToItsExactOpeningState() {
        GameFlowController flow = new GameFlowController();
        flow.transitionTo(GameScreenState.PLAYING);
        flow.transitionTo(GameScreenState.INVENTORY);
        assertEquals(GameScreenState.INVENTORY, flow.state());
        assertFalse(flow.simulationRunning());
        flow.returnFromOverlay();
        assertEquals(GameScreenState.PLAYING, flow.state());

        flow.transitionTo(GameScreenState.PAUSED);
        flow.transitionTo(GameScreenState.INVENTORY);
        flow.returnFromOverlay();
        assertEquals(GameScreenState.PAUSED, flow.state());
        flow.returnFromOverlay();
        assertEquals(GameScreenState.PLAYING, flow.state());
    }

    @Test
    void rejectsInvalidMenuToGameOverTransition() {
        GameFlowController flow = new GameFlowController();
        assertThrows(IllegalStateException.class, () -> flow.transitionTo(GameScreenState.GAME_OVER));
    }

    @Test
    void cinematicIsReachableOnlyFromCombatOrCardChoiceAndReturnsToCombat() {
        GameFlowController flow = new GameFlowController();
        flow.transitionTo(GameScreenState.PLAYING);
        flow.transitionTo(GameScreenState.CARD_CHOICE);
        flow.transitionTo(GameScreenState.CINEMATIC);
        assertFalse(flow.simulationRunning());
        assertFalse(flow.canTransitionTo(GameScreenState.PAUSED));
        assertFalse(flow.canTransitionTo(GameScreenState.SHOP));
        assertFalse(flow.canTransitionTo(GameScreenState.GAME_OVER));
        flow.transitionTo(GameScreenState.PLAYING);
        flow.transitionTo(GameScreenState.CINEMATIC);
        assertEquals(GameScreenState.CINEMATIC, flow.state());
    }
}
