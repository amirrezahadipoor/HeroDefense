package com.amirrezahadipoor.herodefense.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.amirrezahadipoor.herodefense.settings.GameSettings;

import java.util.EnumMap;
import java.util.Map;

/** Owns libGDX Music/Sound resources, looping watchdog, settings, and lifecycle pause. */
public final class GameAudioManager implements AutoCloseable {
    public static final String MUSIC_PATH = "audio/music/world_tree_vigil.ogg";
    private static final float MUSIC_VOLUME = 0.28f;

    private final Music music;
    private final Map<AudioCue, Sound> effects = new EnumMap<>(AudioCue.class);
    private GameSettings settings;
    private boolean appBackgrounded;

    private final AudioThrottle throttle = new AudioThrottle();

    public GameAudioManager(GameSettings settings) {
        this.settings = settings;
        music = Gdx.audio.newMusic(Gdx.files.internal(MUSIC_PATH));
        music.setLooping(true);
        music.setVolume(MUSIC_VOLUME);
        for (AudioCue cue : AudioCue.values()) {
            effects.put(cue, Gdx.audio.newSound(Gdx.files.internal(cue.path())));
        }
        update(settings);
    }

    /** Called each render so an interrupted looping track is restarted when policy permits. */
    public void update(GameSettings updatedSettings) {
        settings = updatedSettings;
        if (AudioPlaybackPolicy.shouldPlayMusic(settings, appBackgrounded)) {
            if (!music.isPlaying()) music.play();
        } else if (music.isPlaying()) {
            music.pause();
        }
    }

    public void play(AudioCue cue) {
        if (cue == null || !AudioPlaybackPolicy.shouldPlayEffects(settings, appBackgrounded)) return;
        if (!throttle.allow(cue)) return;
        Sound sound = effects.get(cue);
        if (sound != null) sound.play(cue.volume());
    }

    /** Advance the per-cue rate limiter with real (not simulation) time. */
    public void tick(float realDeltaSeconds) {
        throttle.advance(realDeltaSeconds);
    }

    public void pauseForBackground() {
        appBackgrounded = true;
        if (music.isPlaying()) music.pause();
        for (Sound sound : effects.values()) sound.stop();
    }

    public void resumeFromBackground() {
        appBackgrounded = false;
        update(settings);
    }

    @Override
    public void close() {
        music.stop();
        music.dispose();
        for (Sound sound : effects.values()) sound.dispose();
        effects.clear();
    }
}
