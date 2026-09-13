package com.amirrezahadipoor.herodefense.audio;

import java.util.EnumMap;
import java.util.Map;

/** Pure per-cue rate limiter; testable without a platform audio backend. */
public final class AudioThrottle {
    private final Map<AudioCue, Float> sinceLastPlay = new EnumMap<>(AudioCue.class);

    /** Returns true and arms the cue's interval when it may play now. */
    public boolean allow(AudioCue cue) {
        if (cue == null) return false;
        Float elapsed = sinceLastPlay.get(cue);
        if (elapsed != null && elapsed < cue.minIntervalSeconds()) return false;
        sinceLastPlay.put(cue, 0f);
        return true;
    }

    public void advance(float deltaSeconds) {
        if (deltaSeconds <= 0f) return;
        sinceLastPlay.replaceAll((cue, elapsed) -> elapsed + deltaSeconds);
    }
}
