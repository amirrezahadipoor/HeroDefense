package com.amirrezahadipoor.herodefense.rewards;

/** Initial reward IDs; the effect pool expands in the next roadmap item. */
public enum RewardCardId {
    STRENGTH("Strength", "+ Strength"),
    AGILITY("Agility", "+ Agility"),
    LUCK("Luck", "+ Luck"),
    DODGE("Dodge", "+ Dodge"),
    HEALTH("Health", "+ Health");

    private final String title;
    private final String description;

    RewardCardId(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }
}
