package com.amirrezahadipoor.herodefense.trials;

/**
 * The twelve Convergence Trials: paired risk/reward modifiers drafted before a run (pick 2
 * of 4 offered) and active for that run only. Each trial names its reward first — the green
 * line on the draft card — and its risk second, the red cost line.
 */
public enum TrialId {
    SWIFT_HOLLOW(
        "Swift Hollow", "Enemies move 25% faster", "+30% coin income", "speed"
    ),
    DRY_VEINS(
        "Dry Veins", "Potions never drop", "+1 talent point per level", "close"
    ),
    HEAVY_CROWNS(
        "Heavy Crowns", "Bosses deal 30% more damage", "Every boss drops a Rare+ item", "general_power"
    ),
    THIN_BLOOD(
        "Thin Blood", "Hero has 25% less max health", "Hero deals 25% more damage", "strength"
    ),
    GLASS_ARROWS(
        "Glass Arrows", "Hero deals 20% less damage", "Hero attacks 25% faster", "agility"
    ),
    IRON_TIDE(
        "Iron Tide", "+3 enemies every wave", "+25% experience", "wave"
    ),
    STONE_SKIN(
        "Stone Skin", "Enemies have 30% more health", "+50% item drop chance", "inventory"
    ),
    BOSS_BOUNTY(
        "Boss Bounty", "Bosses have 30% more health", "+30% Heartwood at Ascension", "coin"
    ),
    MISERS_PACT(
        "Miser's Pact", "Shop prices up 30%", "Begin the run with 200 coins", "shop"
    ),
    FAMISHED_EARTH(
        "Famished Earth", "-30% coin income", "+10% dodge chance", "dodge"
    ),
    BLOOD_PRICE(
        "Blood Price", "Hero takes 20% more damage", "+4% lifesteal", "lifesteal"
    ),
    HOLLOW_CALLING(
        "Hollow Calling", "Enemies deal 20% more damage", "Hero has 15% more max health", "health"
    );

    private final String title;
    private final String risk;
    private final String reward;
    private final String iconKey;

    TrialId(String title, String risk, String reward, String iconKey) {
        this.title = title;
        this.risk = risk;
        this.reward = reward;
        this.iconKey = iconKey;
    }

    public String title() {
        return title;
    }

    public String risk() {
        return risk;
    }

    public String reward() {
        return reward;
    }

    public String iconKey() {
        return iconKey;
    }

    /** Null-safe lookup; unknown or corrupt names resolve to null instead of throwing. */
    public static TrialId forName(String name) {
        if (name == null) {
            return null;
        }
        for (TrialId trial : values()) {
            if (trial.name().equals(name)) {
                return trial;
            }
        }
        return null;
    }
}
