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
        assertEquals(11, AudioCue.values().length);
        assertEquals(
            AudioCue.values().length,
            new HashSet<>(Arrays.stream(AudioCue.values()).map(AudioCue::path).toList()).size()
        );
        for (AudioCue cue : AudioCue.values()) {
            assertTrue(cue.path().startsWith("audio/sfx/"));
            assertTrue(cue.path().endsWith(".ogg"));
            assertTrue(cue.volume() > 0f && cue.volume() <= 1f);
            assertTrue(cue.minIntervalSeconds() >= 0.05f, cue + " must be rate limited");
            assertTrue(java.nio.file.Files.isRegularFile(
                java.nio.file.Paths.get("..", "android", "assets", cue.path())), cue.path());
        }
        assertEquals("audio/music/world_tree_vigil.ogg", GameAudioManager.MUSIC_PATH);
    }

    @Test
    void everyCommittedEffectIsRecordedInTheLicenseLedgerWithItsHash() throws Exception {
        String ledger = java.nio.file.Files.readString(
            java.nio.file.Paths.get("..", "docs", "audio", "AUDIO_LICENSES.md"));
        for (AudioCue cue : AudioCue.values()) {
            java.nio.file.Path file = java.nio.file.Paths.get("..", "android", "assets", cue.path());
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(java.nio.file.Files.readAllBytes(file));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            assertTrue(ledger.contains("`" + cue.path() + "`"), cue.path() + " missing from ledger");
            assertTrue(ledger.contains(hex.toString()), cue.path() + " hash missing from ledger");
        }
    }

    @Test
    void throttleBlocksRepeatsInsideTheCueIntervalOnly() {
        AudioThrottle throttle = new AudioThrottle();
        assertTrue(throttle.allow(AudioCue.CRITICAL));
        assertFalse(throttle.allow(AudioCue.CRITICAL));
        assertTrue(throttle.allow(AudioCue.KILL), "other cues are independent");
        throttle.advance(AudioCue.CRITICAL.minIntervalSeconds());
        assertTrue(throttle.allow(AudioCue.CRITICAL));
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
