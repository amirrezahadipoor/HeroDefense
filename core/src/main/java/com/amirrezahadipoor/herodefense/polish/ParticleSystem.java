package com.amirrezahadipoor.herodefense.polish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded deterministic particle pool for core combat/economy feedback. */
public final class ParticleSystem {
    public static final int MAX_PARTICLES = 256;
    private final List<Particle> particles = new ArrayList<>();
    private int emissionSequence;

    public void emitHit(float x, float y, boolean critical) {
        emitBurst(ParticleType.HIT, x, y, critical ? 9 : 5, critical ? 120f : 80f, 0.22f, critical ? 7f : 5f);
    }

    public void emitDeath(float x, float y) {
        emitBurst(ParticleType.DEATH, x, y, 10, 95f, 0.48f, 7f);
    }

    public void emitCoins(float x, float y) {
        emitBurst(ParticleType.COIN, x, y, 5, 72f, 0.62f, 6f);
    }

    public void emitItemPickup(float x, float y) {
        emitBurst(ParticleType.ITEM_PICKUP, x, y, 12, 88f, 0.70f, 7f);
    }

    public void update(float deltaSeconds) {
        if (deltaSeconds <= 0f) return;
        for (Particle particle : particles) {
            particle.remainingSeconds -= deltaSeconds;
            particle.x += particle.velocityX * deltaSeconds;
            particle.y += particle.velocityY * deltaSeconds;
            particle.velocityX *= Math.max(0f, 1f - 3.2f * deltaSeconds);
            particle.velocityY += (particle.type == ParticleType.COIN ? 22f : -48f) * deltaSeconds;
        }
        particles.removeIf(particle -> particle.remainingSeconds <= 0f);
    }

    public List<Particle> particles() {
        return Collections.unmodifiableList(particles);
    }

    public void clear() {
        particles.clear();
    }

    private void emitBurst(
        ParticleType type,
        float x,
        float y,
        int count,
        float speed,
        float lifetime,
        float size
    ) {
        for (int index = 0; index < count; index++) {
            if (particles.size() >= MAX_PARTICLES) particles.remove(0);
            float angle = (emissionSequence++ * 2.3999632f + index * 1.37f) % ((float) Math.PI * 2f);
            float variation = 0.68f + (index % 4) * 0.11f;
            particles.add(new Particle(
                type,
                x,
                y,
                (float) Math.cos(angle) * speed * variation,
                (float) Math.sin(angle) * speed * variation + speed * 0.28f,
                lifetime * (0.85f + (index % 3) * 0.08f),
                size * (0.82f + (index % 3) * 0.10f)
            ));
        }
    }
}
