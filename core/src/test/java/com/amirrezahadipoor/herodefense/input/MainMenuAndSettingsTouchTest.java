package com.amirrezahadipoor.herodefense.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.settings.GameSettings;
import org.junit.jupiter.api.Test;

final class MainMenuAndSettingsTouchTest {
    @Test
    void mainMenuExposesAllThreeActionsAndDisablesMissingContinue() {
        assertEquals(
            MainMenuTouchLayout.Action.NEW_GAME,
            MainMenuTouchLayout.actionAt(360f, 760f, false)
        );
        assertEquals(
            MainMenuTouchLayout.Action.NONE,
            MainMenuTouchLayout.actionAt(360f, 570f, false)
        );
        assertEquals(
            MainMenuTouchLayout.Action.CONTINUE,
            MainMenuTouchLayout.actionAt(360f, 570f, true)
        );
        assertEquals(
            MainMenuTouchLayout.Action.SETTINGS,
            MainMenuTouchLayout.actionAt(360f, 380f, false)
        );
    }

    @Test
    void settingsUseOnlyLargeTapToggles() {
        GameSettings settings = new GameSettings();
        SettingsTouchController touch = new SettingsTouchController();
        assertEquals(
            SettingsTouchLayout.Action.TOGGLE_SOUND,
            touch.tap(settings, 360f, 775f)
        );
        assertFalse(settings.soundEnabled);
        assertEquals(
            SettingsTouchLayout.Action.TOGGLE_MUSIC,
            touch.tap(settings, 360f, 575f)
        );
        assertFalse(settings.musicEnabled);
        assertEquals(SettingsTouchLayout.Action.CLOSE, touch.tap(settings, 620f, 1170f));
        assertTrue(SettingsTouchLayout.ROW_HEIGHT >= 96f);
    }
}
