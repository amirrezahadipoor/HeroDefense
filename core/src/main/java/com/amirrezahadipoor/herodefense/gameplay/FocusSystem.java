package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.GameState;

/**
 * The Hero's Focus meter (Phase 24.1): every landed hit charges it, critical
 * hits charge double, and a full meter unlocks the Ultimate. All math is
 * deterministic; rendering reads {@link #ratio} for the ring around the Hero.
 */
public final class FocusSystem {
    public static final float FOCUS_PER_HIT = 2f;
    public static final float CRITICAL_FOCUS_MULTIPLIER = 2f;

    private FocusSystem() {
    }

    /** Charges Focus for one volley's landed hits; chain arcs count as hits. */
    public static void addHits(GameState state, int hits, int criticalHits, int chainArcs) {
        if (state == null) return;
        float max = state.focusMax > 0f && Float.isFinite(state.focusMax) ? state.focusMax : 100f;
        if (state.focus >= max) return;
        int normal = Math.max(0, hits - Math.max(0, criticalHits));
        float gain = (normal + Math.max(0, criticalHits) * CRITICAL_FOCUS_MULTIPLIER
            + Math.max(0, chainArcs)) * FOCUS_PER_HIT;
        state.focus = Math.min(max, Math.max(0f, state.focus) + Math.max(0f, gain));
    }

    /** 0..1 charge of the Focus meter; 0 for null or degenerate state. */
    public static float ratio(GameState state) {
        if (state == null || !(state.focusMax > 0f) || !Float.isFinite(state.focusMax)) return 0f;
        if (!Float.isFinite(state.focus)) return 0f;
        return Math.max(0f, Math.min(1f, state.focus / state.focusMax));
    }

    /** True once the meter is full and the Ultimate tap target should show. */
    public static boolean isFull(GameState state) {
        return state != null && Float.isFinite(state.focus)
            && Float.isFinite(state.focusMax) && state.focusMax > 0f
            && state.focus >= state.focusMax;
    }
}
