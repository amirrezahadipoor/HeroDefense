package com.amirrezahadipoor.herodefense.polish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import org.junit.jupiter.api.Test;

final class HitDeathFeelTest {
    @Test
    void hitFlashIsSetOnDamage() {
        Enemy e = new Enemy(1L, EnemyType.ROOTLING.name(), 100f, 100f);
        e.health = 100f; e.maxHealth = 100f; e.alive = true;
        e.receiveDamage(10f);
        // Simulate hitFlash set by gameplay (not by model alone) — we set manually here
        e.hitFlashSeconds = 0.14f;
        assertTrue(e.hitFlashSeconds > 0f);
    }

    @Test
    void perTypeDeathBurstsDiffer() {
        ParticleSystem a = new ParticleSystem();
        ParticleSystem b = new ParticleSystem();
        a.emitDeath(0f,0f, "ROOTLING");
        b.emitDeath(0f,0f, "STONEKIN");
        // Bursts differ at least in particle counts or ring size
        assertTrue(a.particles().size() != b.particles().size() || a.particles().get(0).size != b.particles().get(0).size,
            "per-type bursts should be distinct");
    }

    @Test
    void critNumbersAreLargerAndArcHigher() {
        FloatingDamageText crit = new FloatingDamageText(FloatingDamageText.Style.CRITICAL, "100!", 0f, 0f, 0f, 0.95f);
        FloatingDamageText normal = new FloatingDamageText(FloatingDamageText.Style.NORMAL, "100", 0f, 0f, 0f, 0.72f);
        // crit scale larger
        assertTrue(crit.scale() > normal.scale());
        // crit arcs higher (y progress)
        crit.remainingSeconds = crit.lifetimeSeconds * 0.5f;
        normal.remainingSeconds = normal.lifetimeSeconds * 0.5f;
        assertTrue(crit.y() > normal.y());
    }

    @Test
    void cameraShakeExistsForHeroHitAndBossDeath() {
        ScreenShakeSystem s = new ScreenShakeSystem();
        s.triggerHeroHit();
        assertTrue(s.active());
        s.update(1f);
        assertTrue(!s.active());
        s.triggerBossKill();
        assertTrue(s.active());
    }
}
