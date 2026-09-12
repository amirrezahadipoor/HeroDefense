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
    }

    public float lifeRatio() {
        return lifetimeSeconds <= 0f ? 0f : Math.max(0f, remainingSeconds / lifetimeSeconds);
    }
}
