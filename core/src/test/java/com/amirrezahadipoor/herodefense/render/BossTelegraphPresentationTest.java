package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.BossType;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Locks the boss telegraph warning: distinct identity colors and a readable pulse. */
final class BossTelegraphPresentationTest {
    @Test
    void everyBossWarnsInItsOwnIdentityColor() {
        Set<String> colors = new HashSet<>();
        for (BossType type : BossType.values()) {
            assertTrue(type.telegraphRed() >= 0f && type.telegraphRed() <= 1f, type.name());
            assertTrue(type.telegraphGreen() >= 0f && type.telegraphGreen() <= 1f, type.name());
            assertTrue(type.telegraphBlue() >= 0f && type.telegraphBlue() <= 1f, type.name());
            colors.add(type.telegraphRed() + "," + type.telegraphGreen() + "," + type.telegraphBlue());
        }
        assertTrue(colors.size() == BossType.values().length, colors.toString());
    }

    @Test
    void telegraphPulseStaysVisibleAndMovesOverTime() {
        for (float fraction : new float[] {0f, 0.25f, 0.5f, 0.75f, 1f}) {
            float min = 1f;
            float max = 0f;
            for (float time = 0f; time <= 2f; time += 0.05f) {
                float alpha = CombatEntityRenderer.telegraphAlpha(time, fraction);
                assertTrue(alpha >= 0.15f && alpha <= 0.95f,
                    "fraction=" + fraction + " time=" + time + " alpha=" + alpha);
                min = Math.min(min, alpha);
                max = Math.max(max, alpha);
            }
            assertTrue(max - min > 0.2f, "fraction=" + fraction + " never pulses");
        }
    }
}
