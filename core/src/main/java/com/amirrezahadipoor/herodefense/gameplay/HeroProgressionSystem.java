package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Hero;
import com.amirrezahadipoor.herodefense.model.HeroStat;

/** XP, level-cap, and one-point touch talent allocation rules. */
public final class HeroProgressionSystem {
    public static final int LEVEL_CAP = 100;

    public int experienceRequiredForNextLevel(int level) {
        if (level >= LEVEL_CAP) {
            return 0;
        }
        return 50 + Math.max(1, level) * 25;
    }

    /** Returns the number of levels gained. Each level awards exactly one point. */
    public int grantExperience(GameState state, int experience) {
        if (state == null || experience <= 0 || state.heroLevel >= LEVEL_CAP) {
            return 0;
        }
        long available = (long) state.heroExperience + experience;
        int levelsGained = 0;
        while (state.heroLevel < LEVEL_CAP) {
            int required = experienceRequiredForNextLevel(state.heroLevel);
            if (available < required) {
                break;
            }
            available -= required;
            state.heroLevel++;
            state.unspentTalentPoints++;
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
        hero.maxHealth = hero.stats.maxHealth();
        if (stat == HeroStat.HEALTH) {
            hero.health = Math.min(hero.maxHealth, hero.health + hero.maxHealth - previousMaxHealth);
        }
        state.unspentTalentPoints--;
        return true;
    }
}
