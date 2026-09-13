package com.amirrezahadipoor.herodefense.skills;

import java.util.Locale;

/**
 * Five expensive, coin-only combat skills. Each has exactly {@link #MAX_LEVEL} purchasable
 * levels; every level's effect is a pure function of the level so saves stay deterministic.
 */
public enum SkillId {
    CHAIN_LIGHTNING("Chain Lightning", "Arrows arc lightning to nearby foes"),
    MULTI_SHOT("Multi Shot", "Loose extra arrows at more targets"),
    STUN_CHANCE("Stunning Shot", "Arrows may freeze enemies in place"),
    CRITICAL_MASTERY("Critical Mastery", "Sharper crits: more often, harder"),
    LONG_RANGE("Eagle Range", "Bow reaches farther across the arena");

    public static final int MAX_LEVEL = 10;

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
