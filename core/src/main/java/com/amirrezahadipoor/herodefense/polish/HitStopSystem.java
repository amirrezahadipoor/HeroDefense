package com.amirrezahadipoor.herodefense.polish;

/** Briefly removes real frame time from combat after a critical impact. */
public final class HitStopSystem {
    public static final float CRITICAL_HIT_STOP_SECONDS = 0.045f;
    private float remainingSeconds;

    public void triggerCriticalHit() {
        remainingSeconds = Math.max(remainingSeconds, CRITICAL_HIT_STOP_SECONDS);
    }

    /** Returns the real-time slice still available to simulation after the freeze. */
    public float consume(float frameDeltaSeconds) {
        if (frameDeltaSeconds <= 0f) return 0f;
        float frozen = Math.min(frameDeltaSeconds, remainingSeconds);
        remainingSeconds = Math.max(0f, remainingSeconds - frameDeltaSeconds);
        return frameDeltaSeconds - frozen;
    }

    public boolean active() {
        return remainingSeconds > 0f;
    }

    public void clear() {
        remainingSeconds = 0f;
    }
}
