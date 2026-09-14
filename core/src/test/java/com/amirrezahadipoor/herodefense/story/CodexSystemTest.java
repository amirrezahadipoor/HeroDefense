package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CodexSystemTest {
    private final CodexSystem codex = new CodexSystem();

    @Test
    void freshRunUnlocksOnlyTheWaveOneEntry() {
        GameState state = GameState.newRun(7L);
        List<String> unlocked = codex.unlockForWaveReached(state);
        assertEquals(List.of("codex_01"), unlocked);
        assertTrue(codex.isUnlocked(state, "codex_01"));
        assertEquals(1, codex.unlockedCount(state));
    }

    @Test
    void waveUnlocksAreCumulativeAndIdempotent() {
        GameState state = GameState.newRun(7L);
        state.waveNumber = 30;
        assertEquals(List.of("codex_01", "codex_02", "codex_03", "codex_04"), codex.unlockForWaveReached(state));
        assertTrue(codex.unlockForWaveReached(state).isEmpty());
        state.waveNumber = 100;
        assertEquals(
            List.of("codex_05", "codex_06", "codex_07", "codex_08"), codex.unlockForWaveReached(state));
        assertEquals(8, codex.unlockedCount(state));
    }

    @Test
    void bossKillUnlocksOnlyItsOwnIdentityEntry() {
        GameState state = GameState.newRun(7L);
        assertEquals(List.of("codex_11"), codex.unlockForBossKill(state, "EMBER_WYRM"));
        assertTrue(codex.unlockForBossKill(state, "EMBER_WYRM").isEmpty());
        assertTrue(codex.unlockForBossKill(state, "ANCIENT_GOLEM").size() == 1);
        assertFalse(codex.isUnlocked(state, "codex_10"));
        assertFalse(codex.isUnlocked(state, "codex_12"));
    }

    @Test
    void eliteKillUnlocksOnlyItsOwnAffixEntry() {
        GameState state = GameState.newRun(7L);
        assertEquals(List.of("codex_14"), codex.unlockForEliteKill(state, "rootward_ward"));
        assertTrue(codex.unlockForEliteKill(state, "rootward_ward").isEmpty());
        assertFalse(codex.isUnlocked(state, "codex_13"));
        assertFalse(codex.isUnlocked(state, "codex_15"));
    }

    @Test
    void ascensionUnlocksFollowCompletedTierThresholds() {
        GameState state = GameState.newRun(7L);
        assertTrue(codex.unlockForAscension(state).isEmpty());
        state.ascensionTier = 1;
        assertEquals(List.of("codex_16"), codex.unlockForAscension(state));
        state.ascensionTier = 5;
        assertEquals(List.of("codex_17", "codex_18", "codex_19"), codex.unlockForAscension(state));
        state.ascensionTier = 10;
        assertEquals(List.of("codex_20"), codex.unlockForAscension(state));
        assertEquals(5, codex.unlockedCount(state));
    }

    @Test
    void unknownIdsAndNullStatesStayLocked() {
        GameState state = GameState.newRun(7L);
        assertFalse(codex.unlock(state, "codex_99"));
        assertFalse(codex.unlock(state, null));
        assertTrue(codex.unlockForWaveReached(null).isEmpty());
        assertTrue(codex.unlockForBossKill(state, null).isEmpty());
        assertTrue(codex.unlockForBossKill(state, "NOPE").isEmpty());
        assertTrue(codex.unlockForEliteKill(state, "nope").isEmpty());
        assertEquals(0, codex.unlockedCount(null));
    }
}
