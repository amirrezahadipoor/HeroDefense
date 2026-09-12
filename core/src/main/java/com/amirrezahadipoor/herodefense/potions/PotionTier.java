package com.amirrezahadipoor.herodefense.potions;

/** Six health-potion tiers matching generated icon suffixes and Phase 14 heals. */
public enum PotionTier {
    TIER_1(0, 0.15f),
    TIER_2(1, 0.25f),
    TIER_3(2, 0.40f),
    TIER_4(3, 0.60f),
    TIER_5(4, 0.80f),
    TIER_6(5, 1.00f);

    private final int inventoryIndex;
    private final float maxHealthFraction;

    PotionTier(int inventoryIndex, float maxHealthFraction) {
        this.inventoryIndex = inventoryIndex;
        this.maxHealthFraction = maxHealthFraction;
    }

    public int inventoryIndex() {
        return inventoryIndex;
    }

    public float maxHealthFraction() {
        return maxHealthFraction;
    }

    public String iconPath() {
        return "generated/icons/health_potion_" + (inventoryIndex + 1) + ".png";
    }
}
