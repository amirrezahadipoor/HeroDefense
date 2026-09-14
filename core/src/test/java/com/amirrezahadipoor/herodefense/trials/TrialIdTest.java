package com.amirrezahadipoor.herodefense.trials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class TrialIdTest {
    @Test
    void twelveTrialsCarryTitlesRiskRewardAndIcons() {
        assertEquals(12, TrialId.values().length);
        Set<String> iconKeys = new HashSet<>();
        for (TrialId trial : TrialId.values()) {
            assertFalse(trial.title().isBlank());
            assertFalse(trial.risk().isBlank());
            assertFalse(trial.reward().isBlank());
            assertFalse(trial.iconKey().isBlank());
            assertTrue(iconKeys.add(trial.iconKey()));
            assertEquals(trial, TrialId.forName(trial.name()));
        }
    }

    @Test
    void forNameResolvesUnknownNamesToNull() {
        assertNull(TrialId.forName(null));
        assertNull(TrialId.forName(""));
        assertNull(TrialId.forName("NOT_A_TRIAL"));
    }
}
