package com.amirrezahadipoor.herodefense.trials;

import com.amirrezahadipoor.herodefense.model.GameState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pre-run Convergence draft: offers four trials from the run seed and binds exactly two.
 * The offer and the picks so far live in {@link GameState}, so a save closed mid-draft
 * replays the draft instead of stranding the run.
 */
public final class TrialDraftSystem {
    public static final int OFFER_COUNT = 4;
    public static final int PICK_COUNT = 2;
    private static final long OFFER_SALT = 0xC0E4CE92D192ED03L;

    /** Deals a fresh deterministic offer, discarding any previous offer, picks, or pair. */
    public void prepareOffer(GameState state) {
        if (state == null) {
            return;
        }
        List<TrialId> pool = new ArrayList<>(List.of(TrialId.values()));
        Collections.shuffle(pool, new Random(state.runSeed ^ OFFER_SALT));
        state.pendingTrialOffer.clear();
        state.trialDraftPicks.clear();
        state.activeTrials.clear();
        for (int i = 0; i < Math.min(OFFER_COUNT, pool.size()); i++) {
            state.pendingTrialOffer.add(pool.get(i).name());
        }
    }

    /**
     * Picks the offered trial at the index. Returns true only when the pick completes the
     * pair and binds it to the run; out-of-range and duplicate picks are ignored.
     */
    public boolean chooseTrial(GameState state, int offerIndex) {
        if (state == null || state.pendingTrialOffer == null
            || offerIndex < 0 || offerIndex >= state.pendingTrialOffer.size()) {
            return false;
        }
        if (state.trialDraftPicks == null || state.activeTrials == null) {
            return false;
        }
        if (state.trialDraftPicks.size() >= PICK_COUNT) {
            return false;
        }
        String picked = state.pendingTrialOffer.get(offerIndex);
        if (picked == null || state.trialDraftPicks.contains(picked)) {
            return false;
        }
        state.trialDraftPicks.add(picked);
        if (state.trialDraftPicks.size() < PICK_COUNT) {
            return false;
        }
        state.activeTrials.clear();
        state.activeTrials.addAll(state.trialDraftPicks);
        state.pendingTrialOffer.clear();
        state.trialDraftPicks.clear();
        return true;
    }
}
