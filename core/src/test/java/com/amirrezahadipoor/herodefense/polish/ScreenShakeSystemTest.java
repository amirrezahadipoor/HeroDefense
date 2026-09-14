package com.amirrezahadipoor.herodefense.polish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ScreenShakeSystemTest {
    @Test
    void heroImpulseIsLightBoundedAndEndsQuickly() {
        ScreenShakeSystem shake = new ScreenShakeSystem();
        shake.triggerHeroHit();
        assertTrue(shake.active());
        assertTrue(Math.abs(shake.offsetX()) <= 6f);
        assertTrue(Math.abs(shake.offsetY()) <= 6f);
        shake.update(0.15f);
        assertFalse(shake.active());
        assertEquals(0f, shake.offsetX());
        assertEquals(0f, shake.offsetY());
    }

    @Test
    void bossKillUsesAReadableButStillRestrainedImpulse() {
        ScreenShakeSystem shake = new ScreenShakeSystem();
        shake.triggerBossKill();
        shake.update(0.05f);
        assertTrue(shake.active());
        assertTrue(Math.abs(shake.offsetX()) <= 14f);
        shake.update(0.30f);
        assertFalse(shake.active());
    }

    @Test
    void ultimateKickIsTheStrongestButStillEnds() {
        ScreenShakeSystem shake = new ScreenShakeSystem();
        shake.triggerUltimate();
        assertTrue(shake.active());
        shake.update(0.05f);
        assertTrue(shake.active());
        assertTrue(Math.abs(shake.offsetX()) <= 20f);
        assertTrue(Math.abs(shake.offsetY()) <= 20f);
        shake.update(0.30f);
        assertTrue(shake.active(), "ultimate outlasts a boss kill");
        shake.update(0.15f);
        assertFalse(shake.active());
        assertEquals(0f, shake.offsetX());
        assertEquals(0f, shake.offsetY());
    }
}
