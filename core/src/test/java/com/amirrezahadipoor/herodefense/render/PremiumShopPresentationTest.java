package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.input.StatShopTouchLayout;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks explicit affordability, benefit, paused context, and touch-safe cards. */
final class PremiumShopPresentationTest {
    @Test
    void affordabilityLabelsNeverDependOnColorAlone() {
        assertEquals("MAXED", StatShopOverlayRenderer.affordabilityLabel(true, false, 0, 0));
        assertEquals("AFFORDABLE",
            StatShopOverlayRenderer.affordabilityLabel(false, true, 55, 80));
        assertEquals("NEED $ 20",
            StatShopOverlayRenderer.affordabilityLabel(false, false, 100, 80));
    }

    @Test
    void everyStatExplainsItsPermanentBenefit() {
        for (HeroStat stat : HeroStat.values()) {
            String benefit = StatShopOverlayRenderer.statBenefit(stat);
            assertTrue(benefit.startsWith("+1 "), stat.name());
            assertTrue(benefit.length() >= 10, stat.name());
        }
    }

    @Test
    void purchaseCardsAndCloseRemainGenerousTouchTargets() {
        assertTrue(StatShopTouchLayout.ROW_WIDTH >= 610f);
        assertTrue(StatShopTouchLayout.ROW_HEIGHT >= 135f);
        assertTrue(StatShopTouchLayout.CLOSE_SIZE >= 100f);
    }

    @Test
    void rendererBindsGeneratedStatesAndExplicitContext() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/amirrezahadipoor/herodefense/render/StatShopOverlayRenderer.java"
        ));
        for (String required : new String[] {
            "WORLD TREE ARMORY", "COMBAT PAUSED", "Close returns to Pause",
            "Close returns to battle", "AFFORDABLE", "NEED $ ", "MAXED",
            "LEVEL ", "feedbackMessage()", "UiFrameRenderer.Kind.BUTTON",
            "UiFrameRenderer.Kind.PANEL", "MainMenuRenderer.pressedOffset"
        }) {
            assertTrue(source.contains(required), required);
        }
    }
}
