package com.amirrezahadipoor.herodefense.polish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ChainLightningBranchTest {
    @Test
    void shortArcHasNoBranchLongArcBranches() {
        ParticleSystem ps = new ParticleSystem();
        ps.emitChainArc(0f, 0f, 50f, 0f); // len 50 <140, no branch
        long beams = ps.particles().stream().filter(p -> p.type == ParticleType.CHAIN_BEAM).count();
        assertEquals(1, beams, "short arc should be single beam");
        ps.clear();
        ps.emitChainArc(0f, 0f, 200f, 0f); // len 200 >140, branch
        beams = ps.particles().stream().filter(p -> p.type == ParticleType.CHAIN_BEAM).count();
        assertEquals(2, beams, "long arc should branch into 2 beams");
        long flashes = ps.particles().stream().filter(p -> p.type == ParticleType.CHAIN_FLASH).count();
        assertEquals(1, flashes, "one flash per target");
    }

    @Test
    void branchIsDeterministic() {
        ParticleSystem a = new ParticleSystem();
        ParticleSystem b = new ParticleSystem();
        a.emitChainArc(10f, 20f, 210f, 40f);
        b.emitChainArc(10f, 20f, 210f, 40f);
        assertEquals(a.particles().size(), b.particles().size());
        for (int i=0;i<a.particles().size();i++) {
            assertEquals(a.particles().get(i).endX, b.particles().get(i).endX, 1e-6f);
            assertEquals(a.particles().get(i).endY, b.particles().get(i).endY, 1e-6f);
        }
    }

    @Test
    void jitterIsDeterministic() {
        float j1 = com.amirrezahadipoor.herodefense.render.ParticleRenderer.beamJitter(1.2f, 3);
        float j2 = com.amirrezahadipoor.herodefense.render.ParticleRenderer.beamJitter(1.2f, 3);
        assertEquals(j1, j2, 1e-6f);
        float j3 = com.amirrezahadipoor.herodefense.render.ParticleRenderer.beamJitter(2.4f, 3);
        assertTrue(Math.abs(j3 - j1) > 1e-3f, "different phase should jitter differently");
    }
}
