package com.amirrezahadipoor.herodefense;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ShopFlowTest {
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
