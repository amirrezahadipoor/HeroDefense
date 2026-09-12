package com.amirrezahadipoor.herodefense.polish;

public final class TouchPulse {
    public enum Kind { TAP, CARD_SELECTION }

    public final float x;
    public final float y;
    public final Kind kind;
    public final float lifetimeSeconds;
    public float remainingSeconds;

    TouchPulse(float x, float y, Kind kind, float lifetimeSeconds) {
        this.x = x;
        this.y = y;
        this.kind = kind;
        this.lifetimeSeconds = lifetimeSeconds;
        this.remainingSeconds = lifetimeSeconds;
    }

    public float progress() {
        return 1f - Math.max(0f, remainingSeconds / lifetimeSeconds);
    }
}
