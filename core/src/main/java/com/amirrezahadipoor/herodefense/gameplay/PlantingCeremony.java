package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.WorldLayout;
import com.amirrezahadipoor.herodefense.model.GameState;

/**
 * Deterministic timeline of the Wave 100 planting ceremony. The Hero walks from the anchor to
 * the planting spot beside the World Tree, kneels and plants the seed, waters it, watches the
 * sapling grow, then walks back and is anchored again. Combat is frozen throughout; the player
 * never steers the Hero and may only skip with a tap.
 *
 * <p>Everything here is a pure function of elapsed seconds so a save reloaded mid-ceremony
 * simply replays it from the start, and identical inputs produce identical frames on every device.
 */
public final class PlantingCeremony {
    public enum Phase { IDLE, WALK_OUT, PLANT, WATER, GROW, WALK_BACK, DONE }

    public static final int WALK_FRAMES = 8;
    public static final int PLANT_FRAMES = 10;
    public static final int WATER_FRAMES = 10;
    public static final int GROW_FRAMES = 12;
    public static final float WALK_FPS = 10f;
    public static final float GESTURE_FPS = 8f;
    public static final float GROW_FPS = 8f;

    public static final float WALK_OUT_SECONDS = 1.6f;
    public static final float PLANT_SECONDS = PLANT_FRAMES / GESTURE_FPS;
    public static final float WATER_SECONDS = WATER_FRAMES / GESTURE_FPS;
    public static final float GROW_SECONDS = GROW_FRAMES / GROW_FPS;
    public static final float WALK_BACK_SECONDS = 1.6f;
    public static final float TOTAL_SECONDS =
        WALK_OUT_SECONDS + PLANT_SECONDS + WATER_SECONDS + GROW_SECONDS + WALK_BACK_SECONDS;
    public static final float SHORT_TOTAL_SECONDS =
        WALK_OUT_SECONDS + PLANT_SECONDS + WALK_BACK_SECONDS;
    /** Seed becomes visible at this fraction of the plant clip (the press frames). */
    public static final float SEED_PLANTED_AT = 0.55f;
    private static final float MAX_STEP_SECONDS = 0.10f;
    private static final float LINE_FADE_IN_SECONDS = 0.3f;
    private static final float LINE_FADE_OUT_SECONDS = 0.4f;

    /** Where the Hero stands while planting: just left of the sapling, a step forward. */
    public static final float STAND_X = WorldLayout.SECOND_TREE_X - 78f;
    public static final float STAND_Y = WorldLayout.SECOND_TREE_Y - 38f;

    private float elapsedSeconds;
    private boolean active;
    private boolean shortMode;
    private int groveIndex = 1;
    private float standX = STAND_X;
    private float standY = STAND_Y;
    private float treeX = WorldLayout.SECOND_TREE_X;
    private float treeY = WorldLayout.SECOND_TREE_Y;

    public void begin() {
        begin(false, 1);
    }

    /** Begins the ceremony, short (3-beat) for waves 50/150, full (5-beat) for 100. */
    public void begin(boolean shortCeremony, int groveIndex) {
        elapsedSeconds = 0f;
        active = true;
        shortMode = shortCeremony;
        this.groveIndex = Math.max(0, Math.min(2, groveIndex));
        this.treeX = WorldLayout.groveTreeX(this.groveIndex);
        this.treeY = WorldLayout.groveTreeY(this.groveIndex);
        standX = treeX - 78f;
        standY = treeY - 38f;
    }

    public int groveIndex() {
        return groveIndex;
    }

    public float treeX() {
        return treeX;
    }

    public float treeY() {
        return treeY;
    }

    public boolean isShort() {
        return shortMode;
    }

    /** Advances presentation time; returns true on the frame the ceremony completes. */
    public boolean update(float deltaSeconds) {
        if (!active) return false;
        float safe = Float.isFinite(deltaSeconds) ? Math.max(0f, Math.min(MAX_STEP_SECONDS, deltaSeconds)) : 0f;
        float total = shortMode ? SHORT_TOTAL_SECONDS : TOTAL_SECONDS;
        elapsedSeconds = Math.min(total, elapsedSeconds + safe);
        if (elapsedSeconds >= total) {
            active = false;
            return true;
        }
        return false;
    }

    /** A touch skips straight to the end; the next update reports completion. */
    public void skip() {
        if (active) elapsedSeconds = shortMode ? SHORT_TOTAL_SECONDS : TOTAL_SECONDS;
    }

    public boolean isActive() {
        return active;
    }

    public float elapsedSeconds() {
        return elapsedSeconds;
    }

    public Phase phase() {
        return phaseAt(elapsedSeconds, active, shortMode);
    }

    static Phase phaseAt(float seconds, boolean active) {
        return phaseAt(seconds, active, false);
    }

    static Phase phaseAt(float seconds, boolean active, boolean shortMode) {
        float total = shortMode ? SHORT_TOTAL_SECONDS : TOTAL_SECONDS;
        if (!active && seconds <= 0f) return Phase.IDLE;
        if (seconds >= total) return Phase.DONE;
        float t = seconds;
        if (t < WALK_OUT_SECONDS) return Phase.WALK_OUT;
        t -= WALK_OUT_SECONDS;
        if (t < PLANT_SECONDS) return Phase.PLANT;
        t -= PLANT_SECONDS;
        if (!shortMode) {
            if (t < WATER_SECONDS) return Phase.WATER;
            t -= WATER_SECONDS;
            if (t < GROW_SECONDS) return Phase.GROW;
        }
        return Phase.WALK_BACK;
    }

    /** Seconds since the current phase started. */
    public float phaseSeconds() {
        float t = elapsedSeconds;
        switch (phase()) {
            case WALK_OUT: return t;
            case PLANT: return t - WALK_OUT_SECONDS;
            case WATER: return t - WALK_OUT_SECONDS - PLANT_SECONDS;
            case GROW: return t - WALK_OUT_SECONDS - PLANT_SECONDS - WATER_SECONDS;
            case WALK_BACK: return shortMode ? t - WALK_OUT_SECONDS - PLANT_SECONDS
                : t - WALK_OUT_SECONDS - PLANT_SECONDS - WATER_SECONDS - GROW_SECONDS;
            default: return 0f;
        }
    }

    /** 0..1 progress within the current phase. */
    public float phaseProgress() {
        float duration = switch (phase()) {
            case WALK_OUT -> WALK_OUT_SECONDS;
            case PLANT -> PLANT_SECONDS;
            case WATER -> WATER_SECONDS;
            case GROW -> GROW_SECONDS;
            case WALK_BACK -> WALK_BACK_SECONDS;
            default -> 1f;
        };
        return Math.max(0f, Math.min(1f, phaseSeconds() / duration));
    }

    /** Opacity of the spoken beat: quick fade in, hold, quick fade out within its phase. */
    public float lineAlpha() {
        switch (phase()) {
            case WALK_OUT:
            case PLANT:
            case WATER:
            case GROW:
            case WALK_BACK:
                float duration = phaseDuration();
                float t = phaseSeconds();
                return Math.max(0f, Math.min(1f, Math.min(
                    t / LINE_FADE_IN_SECONDS, (duration - t) / LINE_FADE_OUT_SECONDS)));
            default:
                return 0f;
        }
    }

    private float phaseDuration() {
        return switch (phase()) {
            case WALK_OUT -> WALK_OUT_SECONDS;
            case PLANT -> PLANT_SECONDS;
            case WATER -> WATER_SECONDS;
            case GROW -> GROW_SECONDS;
            case WALK_BACK -> WALK_BACK_SECONDS;
            default -> 1f;
        };
    }

    public float heroX() {
        return switch (phase()) {
            case WALK_OUT -> lerp(GameState.ARENA_CENTER_X, standX, ease(phaseProgress()));
            case PLANT, WATER, GROW -> standX;
            case WALK_BACK -> lerp(standX, GameState.ARENA_CENTER_X, ease(phaseProgress()));
            default -> GameState.ARENA_CENTER_X;
        };
    }

    public float heroY() {
        return switch (phase()) {
            case WALK_OUT -> lerp(GameState.ARENA_CENTER_Y, standY, ease(phaseProgress()));
            case PLANT, WATER, GROW -> standY;
            case WALK_BACK -> lerp(standY, GameState.ARENA_CENTER_Y, ease(phaseProgress()));
            default -> GameState.ARENA_CENTER_Y;
        };
    }

    /** True when the Hero faces the sapling (right); false when walking back (left). */
    public boolean heroFacesRight() {
        return phase() != Phase.WALK_BACK;
    }

    /** Frame index into the clip named by {@link #heroClip()}. */
    public int heroFrame() {
        return switch (phase()) {
            case WALK_OUT, WALK_BACK -> (int) (phaseSeconds() * WALK_FPS) % WALK_FRAMES;
            case PLANT -> Math.min(PLANT_FRAMES - 1, (int) (phaseSeconds() * GESTURE_FPS));
            case WATER -> Math.min(WATER_FRAMES - 1, (int) (phaseSeconds() * GESTURE_FPS));
            case GROW -> WATER_FRAMES - 1;
            default -> 0;
        };
    }

    /**
     * Ceremony-atlas clip: "walk", "plant", or "water". While the sapling grows the Hero holds the
     * final water pose (can lowered) so the props never pop between clips.
     */
    public String heroClip() {
        return switch (phase()) {
            case WALK_OUT, WALK_BACK -> "walk";
            case PLANT -> "plant";
            case WATER, GROW -> "water";
            default -> "walk";
        };
    }

    /** Whether the sapling sprite exists yet (seed pressed into the soil). */
    public boolean saplingVisible() {
        Phase phase = phase();
        if (phase == Phase.PLANT) return phaseProgress() >= SEED_PLANTED_AT;
        return phase == Phase.WATER || phase == Phase.GROW || phase == Phase.WALK_BACK || phase == Phase.DONE;
    }

    /** Frame of the grow clip: sprout until GROW starts, then the growth ramp, then fully grown. */
    public int saplingGrowFrame() {
        if (shortMode) {
            return switch (phase()) {
                case WALK_BACK, DONE -> GROW_FRAMES - 1;
                default -> 0;
            };
        }
        return switch (phase()) {
            case GROW -> Math.min(GROW_FRAMES - 1, (int) (phaseSeconds() * GROW_FPS));
            case WALK_BACK, DONE -> GROW_FRAMES - 1;
            default -> 0;
        };
    }

    /** Water droplets fall during the pour hold (frames 4..8 of the water clip). */
    public boolean pouring() {
        if (shortMode) return false;
        if (phase() != Phase.WATER) return false;
        int frame = heroFrame();
        return frame >= 4 && frame <= 8;
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /** Smoothstep so the walk starts and stops without a snap. */
    private static float ease(float t) {
        return t * t * (3f - 2f * t);
    }
}
