package com.amirrezahadipoor.herodefense.polish;

/** Deterministic, bounded camera impulse used only for world rendering. */
public final class ScreenShakeSystem {
    private float duration;
    private float remaining;
    private float intensity;

    public void triggerHeroHit() {
        trigger(0.14f, 6f);
    }

    public void triggerBossKill() {
        trigger(0.34f, 14f);
    }

    public void update(float deltaSeconds) {
        if (deltaSeconds <= 0f || remaining <= 0f) return;
        remaining = Math.max(0f, remaining - deltaSeconds);
        if (remaining == 0f) {
            duration = 0f;
            intensity = 0f;
        }
    }

    public float offsetX() {
        if (remaining <= 0f || duration <= 0f) return 0f;
        float elapsed = duration - remaining;
        return (float) Math.sin(elapsed * 91f) * intensity * falloff();
    }

    public float offsetY() {
        if (remaining <= 0f || duration <= 0f) return 0f;
        float elapsed = duration - remaining;
        return (float) Math.cos(elapsed * 127f) * intensity * 0.72f * falloff();
    }

    public boolean active() {
        return remaining > 0f;
    }

    private void trigger(float nextDuration, float nextIntensity) {
        if (nextIntensity >= intensity || remaining <= 0f) {
            duration = nextDuration;
            remaining = nextDuration;
            intensity = nextIntensity;
        } else {
            remaining = Math.max(remaining, nextDuration);
            duration = Math.max(duration, remaining);
        }
    }

    private float falloff() {
        return remaining / duration;
    }
}
