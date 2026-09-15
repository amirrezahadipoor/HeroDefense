package com.amirrezahadipoor.herodefense.story;

import com.amirrezahadipoor.herodefense.model.GameState;
import java.util.List;

/**
 * Branching end-of-run epilogues, verbatim (§6). A = flawless victory, B = hard-fought
 * victory, C/D/E = falls before wave 50 / at 50–149 / at 150+. On wins only, the two
 * tier-independent §2.5 transition lines follow the epilogue before the Ascend prompt.
 */
public enum Epilogue {
    A(List.of(
        "Two hundred waves. No step lost.",
        "The Hollow needs a new plan.",
        "Till then, the Tree and I stand."
    )),
    B(List.of(
        "Two hundred waves. All were close.",
        "I do not recall it all. I recall not letting go.",
        "That is enough. It has to be."
    )),
    C(List.of(
        "Not to the middle.",
        "The Tree falls soft and quiet this early.",
        "Next time it will be loud."
    )),
    D(List.of(
        "Close to the second root.",
        "I went farther than last time. Far is not far enough.",
        "Again."
    )),
    E(List.of(
        "One tree stood when I fell. That must count.",
        "The Hollow paid past wave one hundred. It just lasted a bit more.",
        "Next time it pays for all."
    ));

    /** Tier-independent Ascension transition, shown after the epilogue on wins only (§2.5). */
    public static final List<String> TRANSITION = List.of(
        "The Hollow is not gone. It is quiet while it learns to fall again.",
        "Rise again. The Tree will still stand."
    );

    private final List<String> lines;

    Epilogue(List<String> lines) {
        this.lines = lines;
    }

    public List<String> lines() {
        return lines;
    }

    /**
     * Ending to show: the id recorded when the run ended wins, so a later detour (root
     * network, reload) can never reselect; saves without an id select live.
     */
    public static Epilogue endingFor(GameState state) {
        if (state.epilogueId != null) {
            for (Epilogue epilogue : values()) {
                if (epilogue.name().equals(state.epilogueId)) {
                    return epilogue;
                }
            }
        }
        return select(state);
    }

    /**
     * Selects the epilogue for a finished run. A needs Wave 200 cleared with no Hero death,
     * fewer than 3 potions used, and finishing HP at or above 30% of max — B is every other
     * Wave-200 clear (a Hero death always ends the run, so the doc's "died and revived" clause
     * can never occur). Losses split by Game Over wave: C below 50, D at 50–149, E at 150+
     * (a Wave-200 death is E).
     */
    public static Epilogue select(GameState state) {
        if (state.runComplete) {
            boolean flawless = !state.heroDiedThisRun
                && state.potionsUsedThisRun < 3
                && state.hero != null
                && state.hero.maxHealth > 0f
                && state.hero.health / state.hero.maxHealth >= 0.3f;
            return flawless ? A : B;
        }
        int wave = Math.max(1, state.waveNumber);
        if (wave < 50) {
            return C;
        }
        if (wave < 150) {
            return D;
        }
        return E;
    }
}
