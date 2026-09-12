package com.amirrezahadipoor.herodefense.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class RewardCardPoolTest {
    @Test
    void poolContainsAllFiveStatsAndEveryRequiredExtensibleEffectCategory() {
        Set<String> ids = Arrays.stream(RewardCardId.values())
            .map(Enum::name)
            .collect(Collectors.toSet());
        assertTrue(ids.containsAll(Set.of("STRENGTH", "AGILITY", "LUCK", "DODGE", "HEALTH")));

        EnumSet<RewardEffectType> categories = Arrays.stream(RewardCardId.values())
            .map(RewardCardId::effectType)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(RewardEffectType.class)));
        assertEquals(EnumSet.allOf(RewardEffectType.class), categories);
        assertEquals(8, RewardCardId.values().length);
    }
}
