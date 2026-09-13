package com.amirrezahadipoor.herodefense.polish;

/** One short-lived damage / status pop-up anchored above a struck enemy. */
public final class FloatingDamageText {
    public enum Style { NORMAL, CRITICAL, CHAIN, STUN, SECONDARY }

    public final Style style;
    public final String text;
    public final float originX;
    public final float originY;
    public final float driftX;
    public final float lifetimeSeconds;
    public float remainingSeconds;

    FloatingDamageText(Style style, String text, float x, float y, float driftX, float lifetimeSeconds) {
        this.style = style;
        this.text = text;
        this.originX = x;
        this.originY = y;
        this.driftX = driftX;
        this.lifetimeSeconds = lifetimeSeconds;
        this.remainingSeconds = lifetimeSeconds;
    }

    public float lifeRatio() {
        if (lifetimeSeconds <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, remainingSeconds / lifetimeSeconds));
    }

    /** 0 at spawn, 1 at expiry. */
    public float progress() {
        return 1f - lifeRatio();
    }

    /** Fast rise that decelerates, so numbers pop then hang before fading. */
    public float x() {
        return originX + driftX * progress();
    }

    public float y() {
        float p = progress();
        return originY + FloatingDamageTextSystem.RISE_DISTANCE * (1f - (1f - p) * (1f - p));
    }

    /** Criticals punch in oversized and settle; others ease in briefly. */
    public float scale() {
        float p = progress();
        float base = switch (style) {
            case CRITICAL -> 1.35f;
            case STUN -> 1.05f;
            case CHAIN -> 0.92f;
            case SECONDARY -> 0.78f;
            case NORMAL -> 0.88f;
        };
        float punch = style == Style.CRITICAL ? 0.45f : 0.18f;
        float pop = p < 0.12f ? 1f + punch * (1f - p / 0.12f) : 1f;
        return base * pop;
    }
}
