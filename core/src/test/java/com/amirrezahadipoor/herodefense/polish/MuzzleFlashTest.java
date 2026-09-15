package com.amirrezahadipoor.herodefense.polish;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MuzzleFlashTest {
    @Test
    void muzzleFlashEmitsCoreAndRingAndMotes() {
        ParticleSystem ps = new ParticleSystem();
        ps.emitMuzzleFlash(100f, 100f, 3);
        long cores = ps.particles().stream().filter(pr -> pr.type == ParticleType.IMPACT_CORE).count();
        long rings = ps.particles().stream().filter(pr -> pr.type == ParticleType.CRITICAL_RING).count();
        long hits = ps.particles().stream().filter(pr -> pr.type == ParticleType.HIT).count();
        assertTrue(cores >= 1, "muzzle needs core");
        assertTrue(rings >= 1, "muzzle needs ring");
        assertTrue(hits >= 2, "muzzle needs fan motes");
    }

    @Test
    void fanOfArrowsIsVisibleViaShots() {
        ParticleSystem a = new ParticleSystem();
        ParticleSystem b = new ParticleSystem();
        a.emitMuzzleFlash(0f,0f,2);
        b.emitMuzzleFlash(0f,0f,4);
        long motesA = a.particles().stream().filter(pr -> pr.type == ParticleType.HIT).count();
        long motesB = b.particles().stream().filter(pr -> pr.type == ParticleType.HIT).count();
        assertTrue(motesB > motesA, "more shots -> more fan motes");
    }
}
