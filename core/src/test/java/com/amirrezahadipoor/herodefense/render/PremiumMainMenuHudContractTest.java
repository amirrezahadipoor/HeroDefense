package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.amirrezahadipoor.herodefense.input.HudTouchLayout;
import com.amirrezahadipoor.herodefense.input.MainMenuTouchLayout;
import com.amirrezahadipoor.herodefense.polish.TouchPulse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks the readable, touch-first premium hierarchy of the primary surfaces. */
final class PremiumMainMenuHudContractTest {
    private static final Path REPOSITORY = Path.of("..").normalize();

    @Test
    void mainMenuKeepsLargeTouchBoundsAndMovesPressedContents() {
        assertTrue(MainMenuTouchLayout.BUTTON_WIDTH >= 480f);
        assertTrue(MainMenuTouchLayout.BUTTON_HEIGHT >= 140f);
        assertEquals(-4f, MainMenuRenderer.pressedOffset(UiFrameRenderer.State.PRESSED));
        assertEquals(0f, MainMenuRenderer.pressedOffset(UiFrameRenderer.State.NORMAL));
        assertEquals(0f, MainMenuRenderer.pressedOffset(UiFrameRenderer.State.DISABLED));
        assertTrue(MainMenuRenderer.TITLE_PANEL_WIDTH >= 600f);
        assertTrue(MainMenuRenderer.TITLE_PANEL_HEIGHT >= 240f);
        assertTrue(MainMenuRenderer.COIN_PANEL_WIDTH >= 176f);
    }

    @Test
    void segmentedHudReducesTopOcclusionWithoutShrinkingTouchTargets() {
        float oldOpaqueTopArea = 688f * 215f;
        assertTrue(HudRenderer.occupiedTopArea() <= oldOpaqueTopArea * 0.80f);
        assertTrue(HudTouchLayout.BUTTON_WIDTH >= 120f);
        assertTrue(HudTouchLayout.BUTTON_HEIGHT >= 100f);
        assertTrue(HudTouchLayout.UTILITY_BUTTON_WIDTH >= 150f);
        assertTrue(HudTouchLayout.UTILITY_BUTTON_HEIGHT >= 104f);
        assertTrue(HudRenderer.HEALTH_BAR_WIDTH >= 580f);
        assertTrue(HudRenderer.HEALTH_BAR_HEIGHT >= 24f);
    }

    @Test
    void healthTreatmentClampsAndChangesAtReadableThresholds() {
        assertEquals(0f, HudRenderer.healthRatio(-10f, 100f));
        assertEquals(0.5f, HudRenderer.healthRatio(50f, 100f));
        assertEquals(1f, HudRenderer.healthRatio(150f, 100f));
        assertEquals(0f, HudRenderer.healthRatio(50f, 0f));
        Color critical = HudRenderer.healthColor(0.24f);
        Color wounded = HudRenderer.healthColor(0.25f);
        Color healthy = HudRenderer.healthColor(0.50f);
        assertNotEquals(critical, wounded);
        assertNotEquals(wounded, healthy);
        assertNotEquals(critical, healthy);
    }

    @Test
    void restrainedTouchRingsExpandAndFadeWithoutAFullDisc() {
        assertEquals(22f, TouchFeedbackRenderer.radius(TouchPulse.Kind.TAP, 0f));
        assertEquals(56f, TouchFeedbackRenderer.radius(TouchPulse.Kind.TAP, 1f));
        assertEquals(36f, TouchFeedbackRenderer.radius(TouchPulse.Kind.CARD_SELECTION, 0f));
        assertEquals(100f, TouchFeedbackRenderer.radius(TouchPulse.Kind.CARD_SELECTION, 1f));
        assertEquals(0.58f, TouchFeedbackRenderer.alpha(0f));
        assertEquals(0f, TouchFeedbackRenderer.alpha(1f));
    }

    @Test
    void sourceBindsReviewedBackdropFramesAndEveryPrimaryIcon() throws IOException {
        String menu = Files.readString(REPOSITORY.resolve(
            "core/src/main/java/com/amirrezahadipoor/herodefense/render/MainMenuRenderer.java"
        ));
        assertTrue(menu.contains("generated/environment/arena_backdrop.png"));
        assertTrue(menu.contains("UiFrameRenderer.Kind.PANEL"));
        assertTrue(menu.contains("UiFrameRenderer.Kind.BUTTON"));
        for (String key : new String[] {"coin", "new_game", "continue", "settings"}) {
            assertTrue(menu.contains("\"" + key + "\""), key);
        }

        String hud = Files.readString(REPOSITORY.resolve(
            "core/src/main/java/com/amirrezahadipoor/herodefense/render/HudRenderer.java"
        ));
        for (String key : new String[] {
            "health", "wave", "coin", "speed", "pause", "inventory", "shop"
        }) {
            assertTrue(hud.contains("\"" + key + "\""), key);
        }
        assertTrue(hud.contains("state.simulationSpeed > 1f"));
        assertTrue(hud.contains("MainMenuRenderer.pressedOffset"));
    }
}
