package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.IncomingHitResult;
import org.junit.jupiter.api.Test;

final class HeroDamageSystemTest {
    private final HeroDamageSystem system = new HeroDamageSystem();

    @Test
    void everyPositiveIncomingHitConsumesOnePersistedDodgeRoll() {
        GameState state = GameState.newRun(99L);
        long randomStateBefore = state.combatRandomState;

        IncomingHitResult result = system.applyIncomingHit(state, 12f);

        assertEquals(IncomingHitResult.DAMAGED, result);
        assertEquals(88f, state.hero.health);
        assertNotEquals(randomStateBefore, state.combatRandomState);
    }

    @Test
    void ignoredValuesDoNotConsumeARoll() {
        GameState state = GameState.newRun(100L);
        long randomStateBefore = state.combatRandomState;

        assertEquals(IncomingHitResult.IGNORED, system.applyIncomingHit(state, 0f));
        assertEquals(randomStateBefore, state.combatRandomState);
    }
}
