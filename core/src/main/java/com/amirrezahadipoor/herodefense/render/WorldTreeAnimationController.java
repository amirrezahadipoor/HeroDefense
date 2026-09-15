package com.amirrezahadipoor.herodefense.render;

import com.amirrezahadipoor.herodefense.model.GameState;

/**
 * Selects reviewed World Tree frames while allowing the destruction clip to finish after the
 * gameplay simulation has stopped. This controller owns presentation time only; it never mutates
 * game state.
 */
public final class WorldTreeAnimationController {
    public static final int IDLE_FRAME_COUNT = 6;
    public static final int DESTROY_FRAME_COUNT = 10;
    public static final float FRAME_RATE = 12f;
    public static final float DAMAGED_HEALTH_RATIO = 0.40f;
    private static final float MAX_PRESENTATION_DELTA = 0.10f;

    public enum VisualState {
        HEALTHY,
        DAMAGED,
        DESTROYING,
        DESTROYED
    }

    public record Selection(VisualState state, int frameIndex) {
        public Selection {
            if (state == null) throw new IllegalArgumentException("state is required");
            if (frameIndex < 0) throw new IllegalArgumentException("frame index cannot be negative");
        }
    }

    private boolean destructionStarted;
    private float destructionSeconds;

    public Selection select(GameState state, float loopTimeSeconds, float presentationDeltaSeconds) {
        if (!isDestroyed(state)) {
            destructionStarted = false;
            destructionSeconds = 0f;
            int frame = loopFrame(loopTimeSeconds);
            return new Selection(
                isDamaged(state) ? VisualState.DAMAGED : VisualState.HEALTHY,
                frame
            );
        }

        if (!destructionStarted) {
            destructionStarted = true;
            destructionSeconds = 0f;
        } else {
            float safeDelta = Float.isFinite(presentationDeltaSeconds)
                ? Math.max(0f, Math.min(MAX_PRESENTATION_DELTA, presentationDeltaSeconds))
                : 0f;
            destructionSeconds += safeDelta;
        }
        int frame = Math.min(
            DESTROY_FRAME_COUNT - 1,
            Math.max(0, (int) (destructionSeconds * FRAME_RATE))
        );
        VisualState visualState = frame == DESTROY_FRAME_COUNT - 1
            ? VisualState.DESTROYED
            : VisualState.DESTROYING;
        return new Selection(visualState, frame);
    }

    static boolean isDestroyed(GameState state) {
        if (state == null || state.hero == null || !state.hero.alive) return true;
        if (state.worldTreeHealth > 0f) return false;
        for (int i = 0; i < state.plantedTreesCount; i++) {
            if (state.getTreeHealth(i + 1) > 0f) return false;
        }
        return true;
    }

    static boolean isDamaged(GameState state) {
        if (isDestroyed(state)) return true;
        float cur = state.worldTreeHealth;
        float max = Math.max(1f, state.worldTreeMaxHealth);
        float ratio = cur / max;
        for (int i = 0; i < state.plantedTreesCount; i++) {
            float c = state.getTreeHealth(i + 1);
            float m = Math.max(1f, state.getTreeMaxHealth(i + 1));
            ratio = Math.min(ratio, c / m);
        }
        return ratio <= DAMAGED_HEALTH_RATIO;
    }

    /** Grove health ratio across all standing trees (0..1). */
    public static float groveHealthRatio(GameState state) {
        if (state == null) return 0f;
        float totalCur = Math.max(0f, state.worldTreeHealth);
        float totalMax = Math.max(1f, state.worldTreeMaxHealth);
        for (int i = 0; i < state.plantedTreesCount; i++) {
            totalCur += Math.max(0f, state.getTreeHealth(i + 1));
            totalMax += Math.max(1f, state.getTreeMaxHealth(i + 1));
        }
        return Math.max(0f, Math.min(1f, totalCur / totalMax));
    }

    static int loopFrame(float loopTimeSeconds) {
        if (!Float.isFinite(loopTimeSeconds)) return 0;
        return Math.floorMod((int) (loopTimeSeconds * FRAME_RATE), IDLE_FRAME_COUNT);
    }
}
