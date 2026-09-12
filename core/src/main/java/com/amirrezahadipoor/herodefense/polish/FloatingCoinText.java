package com.amirrezahadipoor.herodefense.polish;

/** One short-lived golden currency label anchored over the Hero. */
public final class FloatingCoinText {
    public final int amount;
    public final float x;
    public float y;
    public final float lifetimeSeconds;
    public float remainingSeconds;

    FloatingCoinText(int amount, float x, float y, float lifetimeSeconds) {
        this.amount = amount;
        this.x = x;
        this.y = y;
        this.lifetimeSeconds = lifetimeSeconds;
        this.remainingSeconds = lifetimeSeconds;
    }

    public float lifeRatio() {
        if (lifetimeSeconds <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, remainingSeconds / lifetimeSeconds));
    }
}
