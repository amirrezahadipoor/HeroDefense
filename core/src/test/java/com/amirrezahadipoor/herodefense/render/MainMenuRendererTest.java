package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class MainMenuRendererTest {
    @Test
    void mainMenuCurrencyLabelAlwaysShowsThePersistedTotal() {
        assertEquals("$ 417", MainMenuRenderer.coinTotalLabel(417));
        assertEquals("$ 0", MainMenuRenderer.coinTotalLabel(-1));
    }
}
