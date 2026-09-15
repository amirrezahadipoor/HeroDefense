package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Phase 30.1: real arrows rotate onto velocity vector; lock math. */
final class ProjectileRotationTest {

    private static final float EPS = 0.01f;

    @Test
    void rotationMatchesAtan2InDegrees() {
        assertEquals(0f, CombatEntityRenderer.projectileRotation(1f, 0f), EPS);
        assertEquals(90f, CombatEntityRenderer.projectileRotation(0f, 1f), EPS);
        assertEquals(180f, CombatEntityRenderer.projectileRotation(-1f, 0f), EPS);
        assertEquals(-90f, CombatEntityRenderer.projectileRotation(0f, -1f), EPS);
        assertEquals(45f, CombatEntityRenderer.projectileRotation(1f, 1f), EPS);
        assertEquals(135f, CombatEntityRenderer.projectileRotation(-1f, 1f), EPS);
        assertEquals(-135f, CombatEntityRenderer.projectileRotation(-1f, -1f), EPS);
        assertEquals(-45f, CombatEntityRenderer.projectileRotation(1f, -1f), EPS);
    }

    @Test
    void zeroVelocityDefaultsToZero() {
        assertEquals(0f, CombatEntityRenderer.projectileRotation(0f, 0f), EPS);
    }

    @Test
    void secondaryAndCritVariantsKeepSameRotationMath() {
        // Variants must not affect angle — only sprite size changes.
        float vx = 3f, vy = 4f;
        float expected = 53.1301f;
        assertEquals(expected, CombatEntityRenderer.projectileRotation(vx, vy), 0.05f);
        assertEquals(expected, CombatEntityRenderer.projectileRotation(vx * 0.5f, vy * 0.5f), 0.05f);
    }

    @Test
    void trailAlphaAndHeatStillBounded() {
        // Regression: trail helpers untouched by arrow refactor
        assertEquals(0.55f, CombatEntityRenderer.projectileTrailAlpha(0), 1e-6f);
        assertEquals(0f, CombatEntityRenderer.projectileTrailAlpha(4), 1e-6f);
        assertEquals(0f, CombatEntityRenderer.trailHeat(0), 1e-6f);
        assertEquals(1f, CombatEntityRenderer.trailHeat(10), 1e-6f);
    }
}
