package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.model.HeroStats;
import com.amirrezahadipoor.herodefense.items.AffixEffects;
import com.amirrezahadipoor.herodefense.items.EquipmentSetBonus;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.trials.TrialEffects;

/** Combines permanent Hero points with stat bonuses from all six equipped items. */
public final class HeroStatCalculator {
    public int points(GameState state, HeroStat stat) {
        int points = basePoints(state, stat);
        for (Item item : state.equippedItems.values()) {
            if (item == null || item.statBonuses == null) {
                continue;
            }
            Float bonus = item.statBonuses.get(stat.name());
            if (bonus != null && bonus > 0f) {
                points += Math.round(bonus);
            }
        }
        return points;
    }

    public float damage(GameState state) {
        return (HeroStats.BASE_DAMAGE
            + points(state, HeroStat.STRENGTH) * HeroStats.DAMAGE_PER_STRENGTH)
            * TrialEffects.heroDamageMultiplier(state.activeTrials)
            * AffixEffects.damageMultiplier(state)
            * EquipmentSetBonus.damageMultiplier(state);
    }

    public float attackIntervalSeconds(GameState state) {
        float attacksPerSecond = (HeroStats.BASE_ATTACKS_PER_SECOND
            + points(state, HeroStat.AGILITY) * HeroStats.ATTACK_SPEED_PER_AGILITY)
            * TrialEffects.heroAttackSpeedMultiplier(state.activeTrials)
            * AffixEffects.attackSpeedMultiplier(state)
            * EquipmentSetBonus.attackSpeedMultiplier(state);
        return 1f / attacksPerSecond;
    }

    public float dropChanceMultiplier(GameState state) {
        return (float) Math.pow(
            HeroStats.DROP_MULTIPLIER_PER_LUCK,
            points(state, HeroStat.LUCK)
        ) * AffixEffects.dropChanceMultiplier(state);
    }

    public float dodgeChance(GameState state) {
        return Math.min(
            HeroStats.MAX_DODGE_CHANCE,
            points(state, HeroStat.DODGE) * HeroStats.DODGE_CHANCE_PER_POINT
                + TrialEffects.dodgeChanceBonus(state.activeTrials)
                + AffixEffects.dodgeBonus(state)
        );
    }

    public float maxHealth(GameState state) {
        return (HeroStats.BASE_MAX_HEALTH
            + points(state, HeroStat.HEALTH) * HeroStats.MAX_HEALTH_PER_POINT)
            * TrialEffects.heroMaxHealthMultiplier(state.activeTrials)
            * AffixEffects.maxHealthMultiplier(state)
            * EquipmentSetBonus.maxHealthMultiplier(state);
    }

    private static int basePoints(GameState state, HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> state.hero.stats.strength;
            case AGILITY -> state.hero.stats.agility;
            case LUCK -> state.hero.stats.luck;
            case DODGE -> state.hero.stats.dodge;
            case HEALTH -> state.hero.stats.health;
        };
    }
}
