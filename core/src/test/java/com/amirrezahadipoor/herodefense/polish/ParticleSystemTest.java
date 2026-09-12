package com.amirrezahadipoor.herodefense.polish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ParticleSystemTest {
    @Test
    void emitsAllRequiredFeedbackKindsAndExpiresThem() {
        ParticleSystem particles = new ParticleSystem();
        particles.emitHit(1f, 2f, false);
        particles.emitDeath(1f, 2f);
        particles.emitCoins(1f, 2f);
        particles.emitItemPickup(1f, 2f);
        assertTrue(particles.particles().stream().anyMatch(p -> p.type == ParticleType.HIT));
        assertTrue(particles.particles().stream().anyMatch(p -> p.type == ParticleType.DEATH));
        assertTrue(particles.particles().stream().anyMatch(p -> p.type == ParticleType.COIN));
        assertTrue(particles.particles().stream().anyMatch(p -> p.type == ParticleType.ITEM_PICKUP));
        particles.update(1f);
        assertTrue(particles.particles().isEmpty());
    }

    @Test
    void repeatedBurstsStayWithinMobileParticleCap() {
        ParticleSystem particles = new ParticleSystem();
        for (int i = 0; i < 100; i++) particles.emitDeath(0f, 0f);
        assertEquals(ParticleSystem.MAX_PARTICLES, particles.particles().size());
    }
}
