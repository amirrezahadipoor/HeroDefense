package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.settings.GameSettings;

/** Applies device-setting toggles from taps and reports close separately. */
public final class SettingsTouchController {
    public SettingsTouchLayout.Action tap(GameSettings settings, float x, float y) {
        SettingsTouchLayout.Action action = SettingsTouchLayout.actionAt(x, y);
        if (settings == null) return SettingsTouchLayout.Action.NONE;
        if (action == SettingsTouchLayout.Action.TOGGLE_SOUND) {
            settings.soundEnabled = !settings.soundEnabled;
        } else if (action == SettingsTouchLayout.Action.TOGGLE_MUSIC) {
            settings.musicEnabled = !settings.musicEnabled;
        }
        return action;
    }
}
