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
        "Two hundred waves. Not one step lost.",
        "The Hollow will need a better plan than waves.",
        "Until it finds one — the Tree stands, and so do I."
    )),
    B(List.of(
        "Two hundred waves. Every one of them close.",
        "I don't remember all of it clearly. I remember not letting go.",
        "That's enough. It has to be."
    )),
    C(List.of(
        "Not even to the middle.",
        "The Tree falls quietly, when it falls this early. Almost gently.",
        "It will not be this quiet next time."
    )),
    D(List.of(
        "So close to the second root.",
        "I got further than the fall before. That is not the same as far enough.",
        "Again, then."
    )),
    E(List.of(
        "One tree still stood when I fell. That has to count for something.",
        "The Hollow paid for every wave past a hundred. It just outlasted me by a few.",
        "Next time, it pays for all two hundred."
    ));

    /** Tier-independent Ascension transition, shown after the epilogue on wins only (§2.5). */
    public static final List<String> TRANSITION = List.of(
        "The Hollow isn't gone. It's only quiet — for as long as it takes to remember how to fall again.",
        "Rise again? The Tree will still be standing when you do."
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
