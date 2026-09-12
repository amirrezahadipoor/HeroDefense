package com.amirrezahadipoor.herodefense.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.settings.GameSettings;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

final class AudioContractTest {
    @Test
    void everyRequiredCueHasAUniqueOggAssetAndSafeVolume() {
        assertEquals(5, AudioCue.values().length);
        assertEquals(
            AudioCue.values().length,
            new HashSet<>(Arrays.stream(AudioCue.values()).map(AudioCue::path).toList()).size()
        );
        for (AudioCue cue : AudioCue.values()) {
            assertTrue(cue.path().startsWith("audio/sfx/"));
            assertTrue(cue.path().endsWith(".ogg"));
            assertTrue(cue.volume() > 0f && cue.volume() <= 1f);
        }
        assertEquals("audio/music/world_tree_vigil.ogg", GameAudioManager.MUSIC_PATH);
    }

    @Test
    void independentTogglesAndBackgroundingGatePlayback() {
        GameSettings settings = new GameSettings();
        assertTrue(AudioPlaybackPolicy.shouldPlayMusic(settings, false));
        assertTrue(AudioPlaybackPolicy.shouldPlayEffects(settings, false));
        settings.musicEnabled = false;
        assertFalse(AudioPlaybackPolicy.shouldPlayMusic(settings, false));
        assertTrue(AudioPlaybackPolicy.shouldPlayEffects(settings, false));
        settings.soundEnabled = false;
        assertFalse(AudioPlaybackPolicy.shouldPlayEffects(settings, false));
        settings.musicEnabled = true;
        assertFalse(AudioPlaybackPolicy.shouldPlayMusic(settings, true));
    }
}
