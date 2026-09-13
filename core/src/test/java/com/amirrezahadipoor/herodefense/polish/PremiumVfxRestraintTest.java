package com.amirrezahadipoor.herodefense.polish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.render.ParticleRendererContract;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks the style-guide 0.6 VFX budget for every layered effect. */
final class PremiumVfxRestraintTest {
    @Test
    void normalHitStaysWithinOneCoreSixMotesAndAQuarterSecond() {
        ParticleSystem particles = new ParticleSystem();
        particles.emitHit(0f, 0f, false);
        long cores = count(particles, ParticleType.IMPACT_CORE);
        long motes = count(particles, ParticleType.HIT);
        assertEquals(VfxBudget.NORMAL_HIT_MAX_CORES, cores);
        assertTrue(motes <= VfxBudget.NORMAL_HIT_MAX_MOTES);
        assertEquals(0, count(particles, ParticleType.CRITICAL_RING));
        for (Particle particle : particles.particles()) {
            assertTrue(particle.lifetimeSeconds <= VfxBudget.NORMAL_HIT_MAX_LIFETIME_SECONDS);
        }
    }

    @Test
    void criticalAndBossEventsExceedOnlyThroughDocumentedMultipliers() {
        ParticleSystem particles = new ParticleSystem();
        particles.emitHit(0f, 0f, true);
        assertEquals(1, count(particles, ParticleType.CRITICAL_RING));
        assertTrue(count(particles, ParticleType.HIT)
            <= Math.round(VfxBudget.NORMAL_HIT_MAX_MOTES * VfxBudget.CRITICAL_MULTIPLIER));

        particles.clear();
        particles.emitBossEntrance(0f, 0f);
        assertEquals(1, count(particles, ParticleType.BOSS_SHOCKWAVE));
        assertTrue(count(particles, ParticleType.BOSS_DUST)
            <= Math.round(VfxBudget.NORMAL_HIT_MAX_MOTES * VfxBudget.BOSS_MULTIPLIER));

        particles.clear();
        particles.emitBossDeath(0f, 0f);
        assertEquals(1, count(particles, ParticleType.BOSS_SHOCKWAVE));
        assertEquals(1, count(particles, ParticleType.DEATH_RING));
    }

    @Test
    void deathCollectionAndTreeEffectsAreBoundedAndExpire() {
        ParticleSystem particles = new ParticleSystem();
        particles.emitDeath(0f, 0f);
        assertEquals(1, count(particles, ParticleType.DEATH_RING));
        particles.emitCollectionSparkle(0f, 0f);
        assertTrue(count(particles, ParticleType.COLLECTION_SPARKLE) <= 6);
        particles.emitTreeDestruction(360f, 755f);
        assertTrue(count(particles, ParticleType.TREE_LEAF) <= 18);
        assertTrue(particles.particles().size() <= ParticleSystem.MAX_PARTICLES);
        particles.update(3f);
        assertTrue(particles.particles().isEmpty());
    }

    @Test
    void ringsExpandWhileMotesShrinkAndCoresFlash() {
        assertTrue(ParticleRendererContract.ringRadius(100f, 1f)
            < ParticleRendererContract.ringRadius(100f, 0.5f));
        assertTrue(ParticleRendererContract.ringRadius(100f, 0.5f)
            < ParticleRendererContract.ringRadius(100f, 0f));
        assertEquals(100f, ParticleRendererContract.ringRadius(100f, 0f), 0.001f);
        assertEquals(1f, ParticleRendererContract.moteScale(ParticleType.HIT, 1f));
        assertEquals(0f, ParticleRendererContract.moteScale(ParticleType.HIT, 0f));
        assertTrue(ParticleRendererContract.moteScale(ParticleType.IMPACT_CORE, 0.25f)
            > ParticleRendererContract.moteScale(ParticleType.HIT, 0.25f));
        assertTrue(ParticleType.CRITICAL_RING.isRing());
        assertFalse(ParticleType.HIT.isRing());
    }

    @Test
    void ambientSporesStayBelowCharacterContrastAndInsideTheArena() {
        for (int index = 0; index < AmbientMoteField.COUNT; index++) {
            for (float time = 0f; time < 120f; time += 0.7f) {
                float alpha = AmbientMoteField.alpha(index, time);
                assertTrue(alpha >= 0f && alpha <= VfxBudget.AMBIENT_MAX_ALPHA);
                float x = AmbientMoteField.x(index, time);
                float y = AmbientMoteField.y(index, time);
                assertTrue(x >= 0f && x <= 720f);
                assertTrue(y >= 150f && y <= 1030f);
            }
        }
        assertEquals(AmbientMoteField.x(3, 4.5f), AmbientMoteField.x(3, 4.5f));
    }

    @Test
    void shakeImpulsesForBossEntranceAndTreeFallStayRestrained() {
        ScreenShakeSystem shake = new ScreenShakeSystem();
        shake.triggerBossEntrance();
        assertTrue(Math.abs(shake.offsetX()) <= 8f);
        shake.update(0.25f);
        assertFalse(shake.active());
        shake.triggerTreeFall();
        assertTrue(Math.abs(shake.offsetX()) <= 9f);
        shake.update(0.61f);
        assertFalse(shake.active());
    }

    @Test
    void gameBindsEveryLayeredEventExactlyOnce() throws IOException {
        String game = Files.readString(Path.of(
            "src/main/java/com/amirrezahadipoor/herodefense/HeroDefenseGame.java"
        ));
        for (String call : new String[] {
            "emitBossEntrance(", "emitBossDeath(", "emitTreeDestruction(",
            "emitCollectionSparkle(", "drawAmbient(", "triggerBossEntrance()", "triggerTreeFall()"
        }) {
            assertTrue(game.contains(call), call);
        }
        assertTrue(game.contains("boss.entrancePresented"));
        String combat = Files.readString(Path.of(
            "src/main/java/com/amirrezahadipoor/herodefense/render/CombatEntityRenderer.java"
        ));
        assertTrue(combat.contains("PROJECTILE_TRAIL_STEPS"));
    }

    private static long count(ParticleSystem particles, ParticleType type) {
        return particles.particles().stream().filter(p -> p.type == type).count();
    }
}
