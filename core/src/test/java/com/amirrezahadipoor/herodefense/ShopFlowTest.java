package com.amirrezahadipoor.herodefense;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ShopFlowTest {
    @Test
    void settingsOpenAndCloseFromMainMenu() {
        GameFlowController flow = new GameFlowController();
        flow.transitionTo(GameScreenState.SETTINGS);
        assertEquals(GameScreenState.SETTINGS, flow.state());
        flow.transitionTo(GameScreenState.MENU);
        assertEquals(GameScreenState.MENU, flow.state());
    }

    @Test
    void inGameShopReturnsToFrozenPauseOverlay() {
        GameFlowController flow = new GameFlowController();
        flow.transitionTo(GameScreenState.PLAYING);
        flow.transitionTo(GameScreenState.PAUSED);
        flow.transitionTo(GameScreenState.SHOP);
        flow.returnFromOverlay();
        assertEquals(GameScreenState.PAUSED, flow.state());
    }
}
