package com.amirrezahadipoor.herodefense.model;

/**
 * The fixed Elite affix pool (Phase 25.2): blightburst explodes on death,
 * rootward_ward periodically shields, weeping_rot leaves a damaging trail.
 */
public enum EliteAffix {
    BLIGHTBURST("blightburst"),
    ROOTWARD_WARD("rootward_ward"),
    WEEPING_ROT("weeping_rot");

    private final String id;

    EliteAffix(String id) {
        this.id = id;
    }

    /** Stable id used by saves, codex triggers, and kill counts; never rename. */
    public String id() {
        return id;
    }

    public static EliteAffix fromId(String id) {
        if (id == null) return null;
        for (EliteAffix affix : values()) {
            if (affix.id.equals(id)) return affix;
        }
        return null;
    }
}
