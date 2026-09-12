package com.amirrezahadipoor.herodefense.model;

/** Rarity power targets and their whole-stat budgets for authored equipment. */
public enum ItemTier {
    COMMON(0.05f, 1),
    UNCOMMON(0.12f, 2),
    RARE(0.25f, 4),
    LEGENDARY(0.45f, 7);

    private final float relativePower;
    private final int statPointBudget;

    ItemTier(float relativePower, int statPointBudget) {
        this.relativePower = relativePower;
        this.statPointBudget = statPointBudget;
    }

    public float relativePower() {
        return relativePower;
    }

    public int statPointBudget() {
        return statPointBudget;
    }
}
