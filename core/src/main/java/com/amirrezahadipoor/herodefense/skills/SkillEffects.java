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

    private SkillEffects() {
    }

    public static int level(GameState state, SkillId skill) {
        if (state == null || skill == null || state.skillLevels == null) return 0;
        Integer value = state.skillLevels.get(skill.saveKey());
        return value == null ? 0 : Math.max(0, Math.min(SkillId.MAX_LEVEL, value));
    }

    public static float chainChance(int level) {
        return level <= 0 ? 0f : CHAIN_BASE_CHANCE + CHAIN_CHANCE_PER_LEVEL * (clamp(level) - 1);
    }

    /** Number of additional enemies one lightning proc arcs to. */
    public static int chainTargets(int level) {
        if (level <= 0) return 0;
        return 1 + (clamp(level) - 1) / 3;
    }

    /** Expected extra arrows per volley; whole part guaranteed, fraction is a roll. */
    public static float extraArrows(int level) {
        return clamp(level) * MULTI_SHOT_ARROWS_PER_LEVEL;
    }

    public static float stunChance(int level) {
        return clamp(level) * STUN_CHANCE_PER_LEVEL;
    }

    public static float stunDuration(int level) {
        return level <= 0 ? 0f : STUN_BASE_DURATION + STUN_DURATION_PER_LEVEL * (clamp(level) - 1);
    }

    public static float criticalChance(int level) {
        return BASE_CRITICAL_CHANCE + clamp(level) * CRITICAL_CHANCE_PER_LEVEL;
    }

    public static float criticalMultiplier(int level) {
        return BASE_CRITICAL_MULTIPLIER + clamp(level) * CRITICAL_MULTIPLIER_PER_LEVEL;
    }

    public static float bonusRange(int level) {
        return clamp(level) * RANGE_PER_LEVEL;
    }

    /** Expected damage multiplier from criticals alone at a given mastery level. */
    public static float expectedCriticalMultiplier(int level) {
        return 1f + criticalChance(level) * (criticalMultiplier(level) - 1f);
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(SkillId.MAX_LEVEL, level));
    }
}
