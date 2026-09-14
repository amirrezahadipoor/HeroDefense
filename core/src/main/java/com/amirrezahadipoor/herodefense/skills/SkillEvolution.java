package com.amirrezahadipoor.herodefense.skills;

import java.util.ArrayList;
import java.util.List;

/**
 * Level-10 capstones (Phase 24.2): each skill forks into one of two Evolutions instead of
 * continuing the flat endless curve. Stored in {@code GameState.skillEvolutions} by stable
 * id; the choice is one-time per skill per run and resets with the skill shop.
 */
public enum SkillEvolution {
    STORM_CHAIN(
        SkillId.CHAIN_LIGHTNING, "storm_chain", "Storm Chain", "+2 arcs+stun",
        "+2 arc targets; arcs Stun (20%, 1.0s)"
    ),
    VAMPIRIC_CHAIN(
        SkillId.CHAIN_LIGHTNING, "vampiric_chain", "Vampiric Chain", "heal 30%",
        "Arcs heal the Hero for 30% of damage dealt"
    ),
    HORNET_VOLLEY(
        SkillId.MULTI_SHOT, "hornet_volley", "Hornet Volley", "+2 arrows", "+2 extra arrows per volley"
    ),
    TRUE_FLIGHT(
        SkillId.MULTI_SHOT, "true_flight", "True Flight", "100% arrow",
        "Secondary arrows deal full damage"
    ),
    DEEP_ROOTS(
        SkillId.STUN_CHANCE, "deep_roots", "Deep Roots", "+1.2s stun", "Stuns last +1.2s longer"
    ),
    STARFALL(
        SkillId.STUN_CHANCE, "starfall", "Starfall", "stun +25%", "Stunned foes take +25% damage"
    ),
    EXECUTIONER(
        SkillId.CRITICAL_MASTERY, "executioner", "Executioner", "crit x+0.5",
        "Critical multiplier +0.5"
    ),
    KEEN_EYE(
        SkillId.CRITICAL_MASTERY, "keen_eye", "Keen Eye", "crit +10%", "Critical chance +10%"
    ),
    FARSTRIDER(
        SkillId.LONG_RANGE, "farstrider", "Farstrider", "range +150", "Bonus range +150"
    ),
    DEADEYE(
        SkillId.LONG_RANGE, "deadeye", "Deadeye", "+25% far", "+25% damage beyond 350 units"
    );

    private final SkillId skill;
    private final String id;
    private final String displayName;
    private final String forkShort;
    private final String description;

    SkillEvolution(SkillId skill, String id, String displayName, String forkShort, String description) {
        this.skill = skill;
        this.id = id;
        this.displayName = displayName;
        this.forkShort = forkShort;
        this.description = description;
    }

    public SkillId skill() {
        return skill;
    }

    /** Stable save-file id; never rename. */
    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Compact effect label (<= 12 chars) for the shop fork row, where each option
     * shares one line with its side ("LEFT: Storm Chain: +2 arcs+stun").
     */
    public String forkShort() {
        return forkShort;
    }

    public String description() {
        return description;
    }

    /** The two fork options for a skill, in display order. */
    public static List<SkillEvolution> forSkill(SkillId skill) {
        List<SkillEvolution> options = new ArrayList<>(2);
        if (skill == null) return options;
        for (SkillEvolution evolution : values()) {
            if (evolution.skill == skill) options.add(evolution);
        }
        return options;
    }

    public static SkillEvolution parse(String id) {
        if (id == null) return null;
        for (SkillEvolution evolution : values()) {
            if (evolution.id.equals(id)) return evolution;
        }
        return null;
    }

    /**
     * The simulator's deterministic Evolution policy (higher-DPS pick):
     * Storm, Hornet, Starfall, Executioner, Deadeye.
     */
    public static SkillEvolution simPick(SkillId skill) {
        if (skill == null) return null;
        return switch (skill) {
            case CHAIN_LIGHTNING -> STORM_CHAIN;
            case MULTI_SHOT -> HORNET_VOLLEY;
            case STUN_CHANCE -> STARFALL;
            case CRITICAL_MASTERY -> EXECUTIONER;
            case LONG_RANGE -> DEADEYE;
        };
    }
}
