package com.amirrezahadipoor.herodefense.trials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.story.CodexSystem;
import com.amirrezahadipoor.herodefense.story.LoreCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TrialLockConditionsTest {
    private final CodexSystem codex = new CodexSystem();
    private final TrialDraftSystem draft = new TrialDraftSystem();

    @Test
    void freshRunsLockExactlyHeavyCrownsAndBossBounty() {
        GameState state = GameState.newRun(1L);
        List<TrialId> pool = TrialLockConditions.unlockedPool(state);
        assertEquals(10, pool.size());
        assertFalse(pool.contains(TrialId.HEAVY_CROWNS));
        assertFalse(pool.contains(TrialId.BOSS_BOUNTY));
        assertFalse(TrialId.HEAVY_CROWNS.lockHint().isBlank());
        assertFalse(TrialId.BOSS_BOUNTY.lockHint().isBlank());
        for (TrialId trial : pool) {
            assertTrue(trial.lockHint().isBlank());
            assertTrue(TrialLockConditions.isUnlocked(state, trial));
        }
    }

    @Test
    void codexMasteryUnlocksHeavyCrowns() {
        GameState state = GameState.newRun(2L);
        List<String> ids = LoreCatalog.all().stream().map(e -> e.id()).toList();
        for (int i = 0; i < CodexSystem.MASTERY_LEVEL - 1; i++) {
            codex.unlock(state, ids.get(i));
        }
        assertFalse(TrialLockConditions.isUnlocked(state, TrialId.HEAVY_CROWNS));
        codex.unlock(state, ids.get(CodexSystem.MASTERY_LEVEL - 1));
        assertTrue(TrialLockConditions.isUnlocked(state, TrialId.HEAVY_CROWNS));
    }

    @Test
    void firstAscensionUnlocksBossBounty() {
        GameState state = GameState.newRun(3L);
        assertFalse(TrialLockConditions.isUnlocked(state, TrialId.BOSS_BOUNTY));
        state.ascensionTier = 1;
        assertTrue(TrialLockConditions.isUnlocked(state, TrialId.BOSS_BOUNTY));
    }

    @Test
    void offersNeverIncludeLockedTrials() {
        for (long seed = 1L; seed <= 25L; seed++) {
            GameState state = GameState.newRun(seed);
            draft.prepareOffer(state);
            assertEquals(TrialDraftSystem.OFFER_COUNT, state.pendingTrialOffer.size());
            assertFalse(state.pendingTrialOffer.contains(TrialId.HEAVY_CROWNS.name()));
            assertFalse(state.pendingTrialOffer.contains(TrialId.BOSS_BOUNTY.name()));
        }
    }

    @Test
    void prepareOfferStampsThePersistentUnlockRecord() {
        GameState state = GameState.newRun(4L);
        state.ascensionTier = 2;
        draft.prepareOffer(state);
        assertTrue(Boolean.TRUE.equals(state.trialUnlocked.get(TrialId.BOSS_BOUNTY.name())));
        assertTrue(Boolean.TRUE.equals(state.trialUnlocked.get(TrialId.SWIFT_HOLLOW.name())));
        assertFalse(Boolean.TRUE.equals(state.trialUnlocked.get(TrialId.HEAVY_CROWNS.name())));
    }
}
