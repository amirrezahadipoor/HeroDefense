package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.trials.TrialDraftSystem;

/** Converts trial-card taps into draft picks; true only when the pair is bound. */
public final class TrialDraftTouchController {
    private final TrialDraftSystem draft;

    public TrialDraftTouchController(TrialDraftSystem draft) {
        this.draft = draft;
    }

    public boolean tap(GameState state, float x, float y) {
        int choiceIndex = TrialDraftTouchLayout.cardIndexAt(x, y);
        return choiceIndex >= 0 && draft.chooseTrial(state, choiceIndex);
    }
}
