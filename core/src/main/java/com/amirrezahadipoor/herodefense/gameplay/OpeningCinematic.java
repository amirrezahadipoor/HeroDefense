package com.amirrezahadipoor.herodefense.gameplay;

/**
 * Deterministic timeline of the new-run opening: the camera pushes in on the Hero, a dark
 * cloud rolls over the arena, the Hero speaks three lines in white, the cloud clears, and the
 * camera eases back to the standard framing before Wave 1 starts. Presentation only; a tap
 * skips straight to the end. Never shown on Continue.
 */
public final class OpeningCinematic {
    public enum Phase { IDLE, ZOOM_IN, LINE_ONE, LINE_TWO, LINE_THREE, ZOOM_OUT, DONE }

    public static final String LINE_ONE = "Can you protect the World Tree?!";
    public static final String LINE_TWO = "Can you?";
    public static final String LINE_THREE = "Are you sure?!";
    /** Ascension tier 1 opening, verbatim (§1). */
    public static final String TIER1_LINE_ONE = "Dark comes again.";
    public static final String TIER1_LINE_TWO = "I stand again.";
    public static final String TIER1_LINE_THREE = "This time I go far.";
    /** Ascension tier 2 opening, verbatim (§1). */
    public static final String TIER2_LINE_ONE = "The Hollow knows me now.";
    public static final String TIER2_LINE_TWO = "Good. Let it fear.";
    public static final String TIER2_LINE_THREE = "Roots first. Then flesh. Then Tree. Not today.";
    /** Tier 3 and every tier after reuse this one set as-is (§1). */
    public static final String TIER3_LINE_ONE = "New dawn. New fight.";
    public static final String TIER3_LINE_TWO = "The Tree asks once.";
    public static final String TIER3_LINE_THREE = "So do I.";

    public static final float ZOOM_IN_SECONDS = 1.1f;
    public static final float LINE_ONE_SECONDS = 2.2f;
    public static final float LINE_TWO_SECONDS = 1.5f;
    public static final float LINE_THREE_SECONDS = 1.9f;
    public static final float ZOOM_OUT_SECONDS = 1.1f;
    public static final float TOTAL_SECONDS =
        ZOOM_IN_SECONDS + LINE_ONE_SECONDS + LINE_TWO_SECONDS + LINE_THREE_SECONDS + ZOOM_OUT_SECONDS;
    /** Camera zoom at the tightest point (fraction of the normal view). */
    public static final float CLOSE_ZOOM = 0.58f;
    /** Cloud reaches full darkness by the end of the push-in and lifts during the pull-out. */
    public static final float CLOUD_MAX_ALPHA = 0.72f;
    /** Each line fades in over this much of its slot and out over the last part. */
    private static final float LINE_FADE_SECONDS = 0.28f;
    private static final float MAX_STEP_SECONDS = 0.10f;

    private float elapsedSeconds;
    private boolean active;
    private String[] tierLines = linesForTier(0);

    public void begin() {
        begin(0);
    }

    /** Starts the opening speaking the line set for {@code ascensionTier}. */
    public void begin(int ascensionTier) {
        tierLines = linesForTier(ascensionTier);
        elapsedSeconds = 0f;
        active = true;
    }

    /** Line set for a tier: 0 shipped, 1 and 2 their own, 3+ one shared set (§1). */
    public static String[] linesForTier(int ascensionTier) {
        if (ascensionTier == 1) {
            return new String[] {TIER1_LINE_ONE, TIER1_LINE_TWO, TIER1_LINE_THREE};
        }
        if (ascensionTier == 2) {
            return new String[] {TIER2_LINE_ONE, TIER2_LINE_TWO, TIER2_LINE_THREE};
        }
        if (ascensionTier >= 3) {
            return new String[] {TIER3_LINE_ONE, TIER3_LINE_TWO, TIER3_LINE_THREE};
        }
        return new String[] {LINE_ONE, LINE_TWO, LINE_THREE};
    }

    /** Advances presentation time; returns true on the frame the opening completes. */
    public boolean update(float deltaSeconds) {
        if (!active) return false;
        float safe = Float.isFinite(deltaSeconds) ? Math.max(0f, Math.min(MAX_STEP_SECONDS, deltaSeconds)) : 0f;
        elapsedSeconds = Math.min(TOTAL_SECONDS, elapsedSeconds + safe);
        if (elapsedSeconds >= TOTAL_SECONDS) {
            active = false;
            return true;
        }
        return false;
    }

    public void skip() {
        if (active) elapsedSeconds = TOTAL_SECONDS;
    }

    public boolean isActive() {
        return active;
    }

    public float elapsedSeconds() {
        return elapsedSeconds;
    }

    public Phase phase() {
        if (!active && elapsedSeconds <= 0f) return Phase.IDLE;
        if (elapsedSeconds >= TOTAL_SECONDS) return Phase.DONE;
        float t = elapsedSeconds;
        if (t < ZOOM_IN_SECONDS) return Phase.ZOOM_IN;
        t -= ZOOM_IN_SECONDS;
        if (t < LINE_ONE_SECONDS) return Phase.LINE_ONE;
        t -= LINE_ONE_SECONDS;
        if (t < LINE_TWO_SECONDS) return Phase.LINE_TWO;
        t -= LINE_TWO_SECONDS;
        if (t < LINE_THREE_SECONDS) return Phase.LINE_THREE;
        return Phase.ZOOM_OUT;
    }

    /** Seconds since the current phase began. */
    public float phaseSeconds() {
        float t = elapsedSeconds;
        return switch (phase()) {
            case ZOOM_IN -> t;
            case LINE_ONE -> t - ZOOM_IN_SECONDS;
            case LINE_TWO -> t - ZOOM_IN_SECONDS - LINE_ONE_SECONDS;
            case LINE_THREE -> t - ZOOM_IN_SECONDS - LINE_ONE_SECONDS - LINE_TWO_SECONDS;
            case ZOOM_OUT -> t - ZOOM_IN_SECONDS - LINE_ONE_SECONDS - LINE_TWO_SECONDS - LINE_THREE_SECONDS;
            default -> 0f;
        };
    }

    private float phaseDuration() {
        return switch (phase()) {
            case ZOOM_IN -> ZOOM_IN_SECONDS;
            case LINE_ONE -> LINE_ONE_SECONDS;
            case LINE_TWO -> LINE_TWO_SECONDS;
            case LINE_THREE -> LINE_THREE_SECONDS;
            case ZOOM_OUT -> ZOOM_OUT_SECONDS;
            default -> 1f;
        };
    }

    /** 0..1 progress within the current phase. */
    public float phaseProgress() {
        return Math.max(0f, Math.min(1f, phaseSeconds() / phaseDuration()));
    }

    /** Camera zoom: 1 = normal framing, {@link #CLOSE_ZOOM} = tight on the Hero. */
    public float cameraZoom() {
        return switch (phase()) {
            case ZOOM_IN -> lerp(1f, CLOSE_ZOOM, ease(phaseProgress()));
            case LINE_ONE, LINE_TWO, LINE_THREE -> CLOSE_ZOOM;
            case ZOOM_OUT -> lerp(CLOSE_ZOOM, 1f, ease(phaseProgress()));
            default -> 1f;
        };
    }

    /** How far the camera has moved from the arena centre toward the Hero (0..1). */
    public float cameraFocus() {
        return switch (phase()) {
            case ZOOM_IN -> ease(phaseProgress());
            case LINE_ONE, LINE_TWO, LINE_THREE -> 1f;
            case ZOOM_OUT -> 1f - ease(phaseProgress());
            default -> 0f;
        };
    }

    /** Opacity of the dark cloud over the arena. */
    public float cloudAlpha() {
        return switch (phase()) {
            case ZOOM_IN -> CLOUD_MAX_ALPHA * ease(phaseProgress());
            case LINE_ONE, LINE_TWO, LINE_THREE -> CLOUD_MAX_ALPHA;
            case ZOOM_OUT -> CLOUD_MAX_ALPHA * (1f - ease(phaseProgress()));
            default -> 0f;
        };
    }

    /** The line currently spoken, or null. */
    public String line() {
        return switch (phase()) {
            case LINE_ONE -> tierLines[0];
            case LINE_TWO -> tierLines[1];
            case LINE_THREE -> tierLines[2];
            default -> null;
        };
    }

    /** Opacity of the spoken line: quick fade in, hold, quick fade out. */
    public float lineAlpha() {
        if (line() == null) return 0f;
        float t = phaseSeconds();
        float remaining = phaseDuration() - t;
        return Math.max(0f, Math.min(1f, Math.min(t / LINE_FADE_SECONDS, remaining / LINE_FADE_SECONDS)));
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static float ease(float t) {
        return t * t * (3f - 2f * t);
    }
}
