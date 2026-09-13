package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ProjectileTrailTest {
    @Test
    void trailFadesMonotonicallyAndStaysShort() {
        assertTrue(CombatEntityRenderer.PROJECTILE_TRAIL_STEPS <= 3);
        float previous = 1f;
        for (int step = 1; step <= CombatEntityRenderer.PROJECTILE_TRAIL_STEPS; step++) {
            float alpha = CombatEntityRenderer.projectileTrailAlpha(step);
            assertTrue(alpha < previous);
            assertTrue(alpha >= 0f && alpha < 0.6f);
            previous = alpha;
        }
        assertEquals(0f, CombatEntityRenderer.projectileTrailAlpha(4));
    }
}
