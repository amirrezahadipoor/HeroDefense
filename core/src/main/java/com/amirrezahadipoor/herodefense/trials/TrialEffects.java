package com.amirrezahadipoor.herodefense.trials;

import java.util.Collection;

/**
 * Pure queries over a run's active trials. Every lever in the game reads trial modifiers
 * through these static methods; a null or empty trial list always yields the identity
 * value, so untrialled runs behave exactly as before. Queries naming enemies apply to
 * regular enemies only; boss effects live in the two trials that name bosses explicitly.
 */
public final class TrialEffects {
    private TrialEffects() {}

    private static boolean has(Collection<String> trials, TrialId trial) {
        return trials != null && trials.contains(trial.name());
    }

    /** Enemy move speed; Swift Hollow quickens the horde. */
    public static float enemySpeedMultiplier(Collection<String> trials) {
        return has(trials, TrialId.SWIFT_HOLLOW) ? 1.25f : 1f;
    }

    /** Coin income; Swift Hollow and Miser's Pact pay more, Famished Earth pays less. */
    public static float coinIncomeMultiplier(Collection<String> trials) {
        float mult = 1f;
        if (has(trials, TrialId.SWIFT_HOLLOW)) {
            mult *= 1.3f;
        }
        if (has(trials, TrialId.MISERS_PACT)) {
            mult *= 1.3f;
        }
        if (has(trials, TrialId.FAMISHED_EARTH)) {
            mult *= 0.7f;
        }
        return mult;
    }

    /** Dry Veins dries every potion drop. */
    public static boolean potionsDrop(Collection<String> trials) {
        return !has(trials, TrialId.DRY_VEINS);
    }

    /**
     * Bonus talent points for reaching the level; Dry Veins grants one more every fourth
     * level, roughly +25% talent income instead of a run-trivializing double.
     */
    public static int bonusTalentPointsForLevel(Collection<String> trials, int level) {
        return has(trials, TrialId.DRY_VEINS) && level % 4 == 0 ? 1 : 0;
    }

    /** Boss damage; Heavy Crowns makes crowns hit harder. */
    public static float bossDamageMultiplier(Collection<String> trials) {
        return has(trials, TrialId.HEAVY_CROWNS) ? 1.3f : 1f;
    }

    /** Heavy Crowns: every boss drops a Rare-or-better item. */
    public static boolean bossAlwaysDropsRarePlus(Collection<String> trials) {
        return has(trials, TrialId.HEAVY_CROWNS);
    }

    /** Hero max health; Thin Blood thins it, Hollow Calling swells it. */
    public static float heroMaxHealthMultiplier(Collection<String> trials) {
        float mult = 1f;
        if (has(trials, TrialId.THIN_BLOOD)) {
            mult *= 0.8f;
        }
        if (has(trials, TrialId.HOLLOW_CALLING)) {
            mult *= 1.15f;
        }
        return mult;
    }

    /** Hero damage; Thin Blood sharpens it, Glass Arrows dulls it. */
    public static float heroDamageMultiplier(Collection<String> trials) {
        float mult = 1f;
        if (has(trials, TrialId.THIN_BLOOD)) {
            mult *= 1.2f;
        }
        if (has(trials, TrialId.GLASS_ARROWS)) {
            mult *= 0.8f;
        }
        return mult;
    }

    /** Hero attack speed; Glass Arrows quickens the draw. */
    public static float heroAttackSpeedMultiplier(Collection<String> trials) {
        return has(trials, TrialId.GLASS_ARROWS) ? 1.25f : 1f;
    }

    /** Extra regular enemies per wave; Iron Tide floods the arena. */
    public static int extraEnemiesPerWave(Collection<String> trials) {
        return has(trials, TrialId.IRON_TIDE) ? 3 : 0;
    }

    /** Experience gain; Iron Tide pays the deluge in wisdom. */
    public static float experienceMultiplier(Collection<String> trials) {
        return has(trials, TrialId.IRON_TIDE) ? 1.25f : 1f;
    }

    /** Enemy health; Stone Skin hardens every hide. */
    public static float enemyHealthMultiplier(Collection<String> trials) {
        return has(trials, TrialId.STONE_SKIN) ? 1.2f : 1f;
    }

    /** Item drop chance; Stone Skin cracks open double spoils. */
    public static float itemDropChanceMultiplier(Collection<String> trials) {
        return has(trials, TrialId.STONE_SKIN) ? 2f : 1f;
    }

    /** Boss health; Boss Bounty fattens every crown. */
    public static float bossHealthMultiplier(Collection<String> trials) {
        return has(trials, TrialId.BOSS_BOUNTY) ? 1.3f : 1f;
    }

    /** Heartwood at Ascension; Boss Bounty sweetens the harvest. */
    public static float heartwoodMultiplier(Collection<String> trials) {
        return has(trials, TrialId.BOSS_BOUNTY) ? 1.3f : 1f;
    }

    /** Shop prices; Miser's Pact gouges every shelf. */
    public static float shopPriceMultiplier(Collection<String> trials) {
        return has(trials, TrialId.MISERS_PACT) ? 1.3f : 1f;
    }

    /** Dodge chance bonus; Famished Earth teaches hunger-dodges. */
    public static float dodgeChanceBonus(Collection<String> trials) {
        return has(trials, TrialId.FAMISHED_EARTH) ? 0.10f : 0f;
    }

    /** Damage the hero takes; Blood Price makes every wound deeper. */
    public static float damageTakenMultiplier(Collection<String> trials) {
        return has(trials, TrialId.BLOOD_PRICE) ? 1.15f : 1f;
    }

    /** Lifesteal bonus; Blood Price pays wounds back in blood. */
    public static float lifestealBonus(Collection<String> trials) {
        return has(trials, TrialId.BLOOD_PRICE) ? 0.03f : 0f;
    }

    /** Enemy damage; Hollow Calling lends the horde its voice. */
    public static float enemyDamageMultiplier(Collection<String> trials) {
        return has(trials, TrialId.HOLLOW_CALLING) ? 1.2f : 1f;
    }
}
