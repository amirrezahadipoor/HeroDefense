package com.amirrezahadipoor.herodefense.render;

import com.amirrezahadipoor.herodefense.polish.ParticleType;

/** Test-only bridge to package-private ParticleRenderer shape math (no GL needed). */
public final class ParticleRendererContract {
    private ParticleRendererContract() {
    }

    public static float ringRadius(float fullRadius, float lifeRatio) {
        return ParticleRenderer.ringRadius(fullRadius, lifeRatio);
    }

    public static float moteScale(ParticleType type, float lifeRatio) {
        return ParticleRenderer.moteScale(type, lifeRatio);
    }

    public static float beamJitter(float phase, int index) {
        return ParticleRenderer.beamJitter(phase, index);
    }
}
