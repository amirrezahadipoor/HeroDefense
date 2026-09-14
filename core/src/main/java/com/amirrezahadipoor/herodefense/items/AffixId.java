package com.amirrezahadipoor.herodefense.items;

/**
 * Minor random affixes rolled onto every Rare and Legendary drop. Each affix is a small
 * single-lane bonus; Common and Uncommon items stay affix-free so early loot stays simple.
 * Stun and Chain affixes only add chance while their skill is learned.
 */
public enum AffixId {
    CRIT_CHANCE("+3% Critical Chance", 0.03f),
    CRIT_DAMAGE("+12% Critical Damage", 0.12f),
    LIFESTEAL("+2% Lifesteal", 0.02f),
    COINS_ON_KILL("+3 Coins per Kill", 3f),
    DAMAGE("+4% Damage", 0.04f),
    ATTACK_SPEED("+3% Attack Speed", 0.03f),
    MAX_HEALTH("+4% Max Health", 0.04f),
    DODGE("+2% Dodge Chance", 0.02f),
    EXPERIENCE("+6% Experience", 0.06f),
    ITEM_FIND("+12% Item Find", 0.12f),
    POTION_POWER("+20% Potion Healing", 0.20f),
    STUN_CHANCE("+3% Stun Chance", 0.03f),
    CHAIN_CHANCE("+4% Chain Chance", 0.04f),
    MULTISHOT("+0.2 Extra Arrows", 0.20f),
    BOSS_DAMAGE("+8% Boss Damage", 0.08f);

    private final String display;
    private final float value;

    AffixId(String display, float value) {
        this.display = display;
        this.value = value;
    }

    public String display() {
        return display;
    }

    public float value() {
        return value;
    }

    /** Null-safe lookup; unknown or corrupt ids resolve to null instead of throwing. */
    public static AffixId forName(String name) {
        if (name == null) {
            return null;
        }
        for (AffixId affix : values()) {
            if (affix.name().equals(name)) {
                return affix;
            }
        }
        return null;
    }
}
