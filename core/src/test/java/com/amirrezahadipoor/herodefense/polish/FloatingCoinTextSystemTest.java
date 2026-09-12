package com.amirrezahadipoor.herodefense.polish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FloatingCoinTextSystemTest {
    @Test
    void positiveCoinAwardsRiseFadeAndExpireWithoutAffectingGameState() {
        FloatingCoinTextSystem system = new FloatingCoinTextSystem();
        system.emit(360f, 745f, 0);
        assertTrue(system.labels().isEmpty());

        system.emit(360f, 745f, 27);
        assertEquals(1, system.labels().size());
        FloatingCoinText label = system.labels().get(0);
        assertEquals(27, label.amount);
        float startingY = label.y;

        system.update(0.25f);
        assertTrue(label.y > startingY);
        assertTrue(label.lifeRatio() > 0f && label.lifeRatio() < 1f);
        system.update(FloatingCoinTextSystem.LIFETIME_SECONDS);
        assertTrue(system.labels().isEmpty());
    }

    @Test
    void labelPoolRemainsBoundedDuringBurstKills() {
        FloatingCoinTextSystem system = new FloatingCoinTextSystem();
        for (int index = 0; index < FloatingCoinTextSystem.MAX_LABELS + 5; index++) {
            system.emit(360f, 745f, index + 1);
        }
        assertEquals(FloatingCoinTextSystem.MAX_LABELS, system.labels().size());
    }
}
