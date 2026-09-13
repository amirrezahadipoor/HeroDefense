package com.amirrezahadipoor.herodefense.polish;

/**
 * Pure time-driven arena spores. Positions derive only from run time and index, so ambient motion
 * costs no state, no allocation, and stays deterministic for review captures.
 */
public final class AmbientMoteField {
    public static final int COUNT = VfxBudget.AMBIENT_MOTE_COUNT;
    private static final float ARENA_LEFT = 40f;
    private static final float ARENA_WIDTH = 640f;
    private static final float ARENA_BOTTOM = 180f;
    private static final float ARENA_HEIGHT = 820f;

    private AmbientMoteField() {
    }

    public static float x(int index, float timeSeconds) {
        float base = ARENA_LEFT + ARENA_WIDTH * fraction(index * 7 + 3);
        float sway = 26f * (float) Math.sin(timeSeconds * (0.35f + fraction(index) * 0.25f) + index);
        return base + sway;
    }

    public static float y(int index, float timeSeconds) {
        float speed = 9f + fraction(index * 5 + 1) * 8f;
        float travel = (timeSeconds * speed + fraction(index * 11) * ARENA_HEIGHT) % ARENA_HEIGHT;
        return ARENA_BOTTOM + travel;
    }

    public static float size(int index) {
        return 2.5f + fraction(index * 3 + 2) * 2.5f;
    }

    /** Fades in near the ground and out before the canopy; never exceeds the ambient budget. */
    public static float alpha(int index, float timeSeconds) {
        float progress = (y(index, timeSeconds) - ARENA_BOTTOM) / ARENA_HEIGHT;
        float envelope = Math.min(1f, Math.min(progress / 0.15f, (1f - progress) / 0.25f));
        float twinkle = 0.75f + 0.25f * (float) Math.sin(timeSeconds * 1.7f + index * 2.1f);
        return Math.max(0f, VfxBudget.AMBIENT_MAX_ALPHA * envelope * twinkle);
    }

    private static float fraction(int seed) {
        int mixed = seed * 0x9E3779B1;
        mixed ^= mixed >>> 15;
        mixed *= 0x85EBCA6B;
        mixed ^= mixed >>> 13;
        return (mixed & 0xFFFF) / 65536f;
    }
}
