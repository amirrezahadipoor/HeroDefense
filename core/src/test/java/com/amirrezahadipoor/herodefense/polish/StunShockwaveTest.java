package com.amirrezahadipoor.herodefense.polish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class StunShockwaveTest {
    @Test
    void stunEmitsShockwaveRingPlusStars() {
        ParticleSystem ps = new ParticleSystem();
        ps.emitStunSparks(100f, 100f, 1.0f);
        long rings = ps.particles().stream().filter(p -> p.type == ParticleType.CRITICAL_RING).count();
        long stars = ps.particles().stream().filter(p -> p.type == ParticleType.STUN_SPARK).count();
        assertEquals(2, rings, "shockwave needs 2 rings");
        assertEquals(VfxBudget.STUN_SPARKS, stars, "stars count");
    }

    @Test
    void starsAreUpgraded() {
        ParticleSystem ps = new ParticleSystem();
        ps.emitStunSparks(0f,0f,1.5f);
        for (var p : ps.particles()) if (p.type == ParticleType.STUN_SPARK) {
            assertTrue(p.size > 5f, "upgraded stars larger");
        }
    }
}
