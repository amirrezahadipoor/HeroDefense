package com.amirrezahadipoor.herodefense.trials;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.story.CodexSystem;
import java.util.ArrayList;
import java.util.List;

/**
 * The two locked Convergence Trials: Heavy Crowns opens at Codex mastery (10 entries) and
 * Boss Bounty after the first Ascension, so the drafting pool itself is discovered over
 * time. Conditions are monotonic, so an unlocked trial never locks again.
 */
public final class TrialLockConditions {
    private static final CodexSystem CODEX = new CodexSystem();

    private TrialLockConditions() {}

    public static boolean isUnlocked(GameState state, TrialId trial) {
        if (trial == null) {
            return false;
        }
        return switch (trial) {
            case HEAVY_CROWNS -> state != null
                && CODEX.unlockedCount(state) >= CodexSystem.MASTERY_LEVEL;
            case BOSS_BOUNTY -> state != null && state.ascensionTier >= 1;
            default -> true;
        };
    }

    /** Trials eligible for the draft offer, in canonical order. */
    public static List<TrialId> unlockedPool(GameState state) {
        List<TrialId> pool = new ArrayList<>();
        for (TrialId trial : TrialId.values()) {
            if (isUnlocked(state, trial)) {
                pool.add(trial);
            }
        }
        return pool;
    }

    /** Stamps the persistent unlock record for every condition currently met. */
    public static void refreshUnlockRecord(GameState state) {
        if (state == null || state.trialUnlocked == null) {
            return;
        }
        for (TrialId trial : TrialId.values()) {
            if (isUnlocked(state, trial)) {
                state.trialUnlocked.put(trial.name(), true);
            }
        }
    }
}
