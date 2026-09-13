package com.amirrezahadipoor.herodefense.polish;

/** One short-lived deterministic world-space feedback mote. */
public final class Particle {
    public final ParticleType type;
    public float x;
    public float y;
    public float velocityX;
    public float velocityY;
    public float remainingSeconds;
    public final float lifetimeSeconds;
    public final float size;
    /** Segment end for beams; unused (0) for point particles. */
    public float endX;
    public float endY;
    /** Deterministic per-particle phase used by orbiting/jittering renderers. */
    public final float seed;

    Particle(
        ParticleType type,
        float x,
        float y,
        float velocityX,
        float velocityY,
        float lifetimeSeconds,
        float size
    ) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.remainingSeconds = lifetimeSeconds;
        this.lifetimeSeconds = lifetimeSeconds;
        this.size = size;
        this.seed = (Math.abs(x * 0.37f + y * 0.91f + size * 3.1f)) % 6.2831f;
    }

    public float lifeRatio() {
        return lifetimeSeconds <= 0f ? 0f : Math.max(0f, remainingSeconds / lifetimeSeconds);
    }
}
