package com.amirrezahadipoor.herodefense.polish;

/** Locked premium-v2 VFX restraint limits from the visual style guide, section 0.6. */
public final class VfxBudget {
    public static final int NORMAL_HIT_MAX_CORES = 1;
    public static final int NORMAL_HIT_MAX_MOTES = 6;
    public static final float NORMAL_HIT_MAX_LIFETIME_SECONDS = 0.25f;
    /** Critical and boss events may exceed the normal hit only through these multipliers. */
    public static final float CRITICAL_MULTIPLIER = 1.5f;
    public static final float BOSS_MULTIPLIER = 2.0f;
    /**
     * The Ultimate's higher, rate-limited budget: one blast per full Focus
     * meter, at most this multiple of a normal hit plus a capped beam fan.
     */
    public static final float ULTIMATE_MULTIPLIER = 3.0f;
    public static final int ULTIMATE_MAX_ARCS = 8;
    public static final int ULTIMATE_SPARKS = 8;
    /** Phase 18 skill effects: each arc/stun is cheaper than a normal hit so volleys stay bounded. */
    public static final int CRITICAL_SPARKS = 4;
    public static final int CHAIN_ARC_MAX_MOTES = 4;
    public static final int STUN_SPARKS = 3;
    public static final int AMBIENT_MOTE_COUNT = 14;
    public static final float AMBIENT_MAX_ALPHA = 0.22f;

    private VfxBudget() {
    }
}
