package com.amirrezahadipoor.herodefense.skills;

import com.amirrezahadipoor.herodefense.model.GameState;

/**
 * Pure per-level skill numbers. Every combat system reads through here so the shop, the
 * simulator, and the renderer can never disagree about what a level does.
 */
public final class SkillEffects {
    /** Chain Lightning: chance per arrow hit to arc, arcs per proc, damage share per arc. */
    public static final float CHAIN_BASE_CHANCE = 0.10f;
    public static final float CHAIN_CHANCE_PER_LEVEL = 0.05f;
    public static final float CHAIN_DAMAGE_SHARE = 0.55f;
    public static final float CHAIN_RADIUS = 210f;

    /** Multi Shot: extra arrows per volley (fractional part is a chance for one more). */
    public static final float MULTI_SHOT_ARROWS_PER_LEVEL = 0.30f;
    public static final float MULTI_SHOT_DAMAGE_SHARE = 0.70f;

    /** Stun: chance per arrow hit, duration in seconds. Bosses resist half the duration. */
    public static final float STUN_CHANCE_PER_LEVEL = 0.02f;
    public static final float STUN_BASE_DURATION = 0.50f;
    public static final float STUN_DURATION_PER_LEVEL = 0.05f;
    public static final float BOSS_STUN_RESISTANCE = 0.5f;

    /** Critical Mastery: at level 10 the base chance doubles and the multiplier reaches 2.5x. */
    public static final float BASE_CRITICAL_CHANCE = 0.05f;
    public static final float BASE_CRITICAL_MULTIPLIER = 1.75f;
    public static final float CRITICAL_CHANCE_PER_LEVEL = 0.005f;
    public static final float CRITICAL_MULTIPLIER_PER_LEVEL = 0.075f;

    /** Eagle Range: bow range added per level over the 420-unit base. */
    public static final float RANGE_PER_LEVEL = 22f;

    /**
     * Endless levels (Phase 18): past {@link SkillId#CORE_LEVELS} each further level is worth a
     * geometrically shrinking share of a core level, so growth continues forever but converges.
     * Chance-type effects also hit hard ceilings so no roll ever becomes a certainty.
     */
    public static final float ENDLESS_DECAY_PER_TEN_LEVELS = 0.5f;
    public static final float CHAIN_CHANCE_CAP = 0.90f;
    public static final float STUN_CHANCE_CAP = 0.45f;
    public static final float STUN_DURATION_CAP = 2.5f;
    public static final float CRITICAL_CHANCE_CAP = 0.60f;
    public static final int CHAIN_TARGETS_CAP = 8;
    public static final float EXTRA_ARROWS_CAP = 6f;
    public static final float BONUS_RANGE_CAP = 520f;

    private SkillEffects() {
    }

    public static int level(GameState state, SkillId skill) {
        if (state == null || skill == null || state.skillLevels == null) return 0;
        Integer value = state.skillLevels.get(skill.saveKey());
        return value == null ? 0 : Math.max(0, value);
    }

    /**
     * Effective "core-equivalent" level: levels 1..10 count fully; each block of ten beyond
     * that is worth half the previous block (10 → 10, 20 → 15, 30 → 17.5, ∞ → 20).
     */
    public static float effectiveLevel(int level) {
        int clamped = Math.max(0, level);
        if (clamped <= SkillId.CORE_LEVELS) return clamped;
        float effective = SkillId.CORE_LEVELS;
        float weight = 1f;
        int remaining = clamped - SkillId.CORE_LEVELS;
        while (remaining > 0) {
            weight *= ENDLESS_DECAY_PER_TEN_LEVELS;
            int block = Math.min(SkillId.CORE_LEVELS, remaining);
            effective += block * weight;
            remaining -= block;
        }
        return effective;
    }

    public static float chainChance(int level) {
        if (level <= 0) return 0f;
        return Math.min(CHAIN_CHANCE_CAP,
            CHAIN_BASE_CHANCE + CHAIN_CHANCE_PER_LEVEL * (effectiveLevel(level) - 1f));
    }

    /** Number of additional enemies one lightning proc arcs to. */
    public static int chainTargets(int level) {
        if (level <= 0) return 0;
        return Math.min(CHAIN_TARGETS_CAP, 1 + (Math.round(effectiveLevel(level)) - 1) / 3);
    }

    /** Expected extra arrows per volley; whole part guaranteed, fraction is a roll. */
    public static float extraArrows(int level) {
        return Math.min(EXTRA_ARROWS_CAP, effectiveLevel(level) * MULTI_SHOT_ARROWS_PER_LEVEL);
    }

    public static float stunChance(int level) {
        return Math.min(STUN_CHANCE_CAP, effectiveLevel(level) * STUN_CHANCE_PER_LEVEL);
    }

    public static float stunDuration(int level) {
        if (level <= 0) return 0f;
        return Math.min(STUN_DURATION_CAP,
            STUN_BASE_DURATION + STUN_DURATION_PER_LEVEL * (effectiveLevel(level) - 1f));
    }

    public static float criticalChance(int level) {
        return Math.min(CRITICAL_CHANCE_CAP,
            BASE_CRITICAL_CHANCE + effectiveLevel(level) * CRITICAL_CHANCE_PER_LEVEL);
    }

    public static float criticalMultiplier(int level) {
        return BASE_CRITICAL_MULTIPLIER + effectiveLevel(level) * CRITICAL_MULTIPLIER_PER_LEVEL;
    }

    public static float bonusRange(int level) {
        return Math.min(BONUS_RANGE_CAP, effectiveLevel(level) * RANGE_PER_LEVEL);
    }

    /** Expected damage multiplier from criticals alone at a given mastery level. */
    public static float expectedCriticalMultiplier(int level) {
        return 1f + criticalChance(level) * (criticalMultiplier(level) - 1f);
    }

}
