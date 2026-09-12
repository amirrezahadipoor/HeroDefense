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
            assertTrue(budget.multiplier(boss) >= previous);
            previous = budget.multiplier(boss);
        }
    }

    @Test
    void latePercentageAndStatCardsAreStrongerThanEarlyCards() {
        assertTrue(
            budget.magnitude(RewardCardId.GENERAL_POWER, 20)
                > budget.magnitude(RewardCardId.GENERAL_POWER, 1)
        );
        assertTrue(budget.statPoints(20) > budget.statPoints(1));
        assertEquals("+16% all damage", budget.description(RewardCardId.GENERAL_POWER, 20));
    }
}
