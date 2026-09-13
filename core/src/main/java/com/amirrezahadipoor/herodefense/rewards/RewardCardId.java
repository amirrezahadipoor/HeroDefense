package com.amirrezahadipoor.herodefense.rewards;

/** Extensible authored card pool used for deterministic three-card offers. */
public enum RewardCardId {
    STRENGTH("Might of Oak", "+ Strength", RewardEffectType.BASE_STAT, 1f),
    AGILITY("Windstep", "+ Agility", RewardEffectType.BASE_STAT, 1f),
    LUCK("Fortune Leaf", "+ Luck", RewardEffectType.BASE_STAT, 1f),
    DODGE("Fox Instinct", "+ Dodge", RewardEffectType.BASE_STAT, 1f),
    HEALTH("Heartwood", "+ Health", RewardEffectType.BASE_STAT, 1f),
    GENERAL_POWER("Verdant Power", "+8% all damage", RewardEffectType.GENERAL_POWER, 0.08f),
    COIN_INCOME("Golden Sap", "+15% coin income", RewardEffectType.COIN_INCOME, 0.15f),
    LIFESTEAL("Crimson Root", "+2% lifesteal", RewardEffectType.LIFESTEAL, 0.02f);

    private final String title;
    private final String description;
    private final RewardEffectType effectType;
    private final float baseMagnitude;

    RewardCardId(
        String title,
        String description,
        RewardEffectType effectType,
        float baseMagnitude
    ) {
        this.title = title;
        this.description = description;
        this.effectType = effectType;
        this.baseMagnitude = baseMagnitude;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public RewardEffectType effectType() {
        return effectType;
    }

    public float baseMagnitude() {
        return baseMagnitude;
    }

    /** Semantic Heartwood medallion shared by every reward-card presentation. */
    public String iconKey() {
        return switch (this) {
            case STRENGTH -> "strength";
            case AGILITY -> "agility";
            case LUCK -> "luck";
            case DODGE -> "dodge";
            case HEALTH -> "health";
            case GENERAL_POWER -> "general_power";
            case COIN_INCOME -> "coin";
            case LIFESTEAL -> "lifesteal";
        };
    }
}
