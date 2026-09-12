package com.amirrezahadipoor.herodefense.audio;

import com.amirrezahadipoor.herodefense.settings.GameSettings;

/** Pure lifecycle/settings policy used by the platform audio owner. */
public final class AudioPlaybackPolicy {
    private AudioPlaybackPolicy() {
    }

    public static boolean shouldPlayMusic(GameSettings settings, boolean appBackgrounded) {
        return settings != null && settings.musicEnabled && !appBackgrounded;
    }

    public static boolean shouldPlayEffects(GameSettings settings, boolean appBackgrounded) {
        return settings != null && settings.soundEnabled && !appBackgrounded;
    }
}
