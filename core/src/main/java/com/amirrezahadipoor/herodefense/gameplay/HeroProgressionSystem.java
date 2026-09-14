package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.items.AffixEffects;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Hero;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.trials.TrialEffects;

/** XP, level-cap, and one-point touch talent allocation rules. */
public final class HeroProgressionSystem {
    /** Raised from 100 in Phase 19 so the endless half (waves 101–200) keeps awarding points. */
    public static final int LEVEL_CAP = 200;
    /** Levels past this one cost progressively more XP so the second half is not a level flood. */
    public static final int CORE_LEVELS = 100;
    public static final float LATE_LEVEL_GROWTH = 1.03f;

    public int experienceRequiredForNextLevel(int level) {
        if (level >= LEVEL_CAP) {
            return 0;
        }
        int base = 50 + Math.max(1, level) * 25;
        int late = Math.max(0, level - CORE_LEVELS);
        return late == 0 ? base : (int) Math.min(Integer.MAX_VALUE / 4, base * Math.pow(LATE_LEVEL_GROWTH, late));
    }

    /** Returns the number of levels gained. Each level awards one point, plus trial bonuses. */
    public int grantExperience(GameState state, int experience) {
        if (state == null || experience <= 0 || state.heroLevel >= LEVEL_CAP) {
            return 0;
        }
        int scaled = Math.max(1, Math.round(
            experience * TrialEffects.experienceMultiplier(state.activeTrials)
                * AffixEffects.experienceMultiplier(state)
        ));
        long available = (long) state.heroExperience + scaled;
        int levelsGained = 0;
        while (state.heroLevel < LEVEL_CAP) {
            int required = experienceRequiredForNextLevel(state.heroLevel);
            if (available < required) {
                break;
            }
            available -= required;
            state.heroLevel++;
            state.unspentTalentPoints += 1
                + TrialEffects.bonusTalentPointsForLevel(state.activeTrials, state.heroLevel);
            levelsGained++;
        }
        state.heroExperience = state.heroLevel == LEVEL_CAP
            ? 0
            : (int) Math.min(Integer.MAX_VALUE, available);
        return levelsGained;
    }

    public boolean allocateTalentPoint(GameState state, HeroStat stat) {
        if (state == null || state.hero == null || stat == null || state.unspentTalentPoints <= 0) {
            return false;
        }
        Hero hero = state.hero;
        float previousMaxHealth = hero.maxHealth;
        switch (stat) {
            case STRENGTH -> hero.stats.strength++;
            case AGILITY -> hero.stats.agility++;
            case LUCK -> hero.stats.luck++;
            case DODGE -> hero.stats.dodge++;
            case HEALTH -> hero.stats.health++;
        }
        hero.maxHealth = hero.stats.maxHealth()
            * TrialEffects.heroMaxHealthMultiplier(state.activeTrials);
        if (stat == HeroStat.HEALTH) {
            hero.health = Math.min(hero.maxHealth, hero.health + hero.maxHealth - previousMaxHealth);
        }
        state.unspentTalentPoints--;
        return true;
    }
}
