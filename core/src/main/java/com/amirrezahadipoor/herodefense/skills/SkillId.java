package com.amirrezahadipoor.herodefense.skills;

import java.util.Locale;

/**
 * Five expensive, coin-only combat skills. The first {@link #CORE_LEVELS} levels are the
 * linearly priced core progression; beyond that levels are endless with geometric pricing
 * and diminishing effect. Every level's effect is a pure function of the level so saves stay
 * deterministic.
 */
public enum SkillId {
    CHAIN_LIGHTNING("Chain Lightning", "Arrows arc lightning to nearby foes"),
    MULTI_SHOT("Multi Shot", "Loose extra arrows at more targets"),
    STUN_CHANCE("Stunning Shot", "Arrows may freeze enemies in place"),
    CRITICAL_MASTERY("Critical Mastery", "Sharper crits: more often, harder"),
    LONG_RANGE("Eagle Range", "Bow reaches farther across the arena");

    /** Levels priced on the base curve and counted at full effect. */
    public static final int CORE_LEVELS = 10;
    /** Endless: past the core levels each purchase multiplies the price again. */
    public static final float ENDLESS_PRICE_GROWTH = 1.45f;

    private final String displayName;
    private final String summary;

    SkillId(String displayName, String summary) {
        this.displayName = displayName;
        this.summary = summary;
    }

    public String displayName() {
        return displayName;
    }

    public String summary() {
        return summary;
    }

    /** Reviewed control-medallion icon key rendered by the Blender UI pipeline. */
    public String iconKey() {
        return "skill_" + name().toLowerCase(Locale.ROOT);
    }

    /** Stable save-file key; never rename. */
    public String saveKey() {
        return name();
    }

    public static SkillId parse(String value) {
        try {
            return SkillId.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException error) {
            return null;
        }
    }
}
