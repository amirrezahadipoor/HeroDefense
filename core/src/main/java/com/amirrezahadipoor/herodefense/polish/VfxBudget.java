package com.amirrezahadipoor.herodefense.polish;

/** Locked premium-v2 VFX restraint limits from the visual style guide, section 0.6. */
public final class VfxBudget {
    public static final int NORMAL_HIT_MAX_CORES = 1;
    public static final int NORMAL_HIT_MAX_MOTES = 6;
    public static final float NORMAL_HIT_MAX_LIFETIME_SECONDS = 0.25f;
    /** Critical and boss events may exceed the normal hit only through these multipliers. */
    public static final float CRITICAL_MULTIPLIER = 1.5f;
    public static final float BOSS_MULTIPLIER = 2.0f;
    public static final int AMBIENT_MOTE_COUNT = 14;
    public static final float AMBIENT_MAX_ALPHA = 0.22f;

    private VfxBudget() {
    }
}
