package com.amirrezahadipoor.herodefense.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RewardPowerBudgetTest {
    private final RewardPowerBudget budget = new RewardPowerBudget();

    @Test
    void budgetGrowsSmoothlyFromBossOneThroughBossTwenty() {
        assertEquals(1f, budget.multiplier(1));
        assertEquals(1.95f, budget.multiplier(20), 0.0001f);
        float previous = 0f;
        for (int boss = 1; boss <= 20; boss++) {
            float current = budget.multiplier(boss);
            assertEquals(1f + (boss - 1) * 0.05f, current, 0.0001f);
            assertTrue(current >= previous);
            if (boss > 1) assertEquals(0.05f, current - previous, 0.0001f);
            for (RewardCardId card : RewardCardId.values()) {
                float magnitude = budget.magnitude(card, boss);
                assertTrue(magnitude > 0f, card + " at Boss " + boss);
                assertTrue(!budget.description(card, boss).isBlank());
            }
            previous = current;
        }
    }

    @Test
    void latePercentageAndStatCardsAreStrongerThanEarlyCards() {
        assertTrue(
            budget.magnitude(RewardCardId.GENERAL_POWER, 20)
                > budget.magnitude(RewardCardId.GENERAL_POWER, 1)
        );
        assertEquals(1, budget.statPoints(1));
        assertEquals(2, budget.statPoints(20));
        assertEquals(
            1.95f,
            budget.magnitude(RewardCardId.GENERAL_POWER, 20)
                / budget.magnitude(RewardCardId.GENERAL_POWER, 1),
            0.0001f
        );
        assertEquals("+16% all damage", budget.description(RewardCardId.GENERAL_POWER, 20));
    }
}
