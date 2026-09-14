package com.amirrezahadipoor.herodefense.ascension;

import java.util.Collections;
import java.util.List;

/** Immutable definition of one Root Network node. */
public final class RootNodeDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final int cost;
    private final RootNodeBonusType bonusType;
    private final int bonusAmount;
    private final float x;
    private final float y;
    private final List<String> requires;

    public RootNodeDefinition(
        String id,
        String name,
        String description,
        int cost,
        RootNodeBonusType bonusType,
        int bonusAmount,
        float x,
        float y,
        List<String> requires
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.bonusType = bonusType;
        this.bonusAmount = bonusAmount;
        this.x = x;
        this.y = y;
        this.requires = requires == null ? Collections.emptyList() : Collections.unmodifiableList(requires);
    }

    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public int cost() { return cost; }
    public RootNodeBonusType bonusType() { return bonusType; }
    public int bonusAmount() { return bonusAmount; }
    public float x() { return x; }
    public float y() { return y; }
    public List<String> requires() { return requires; }
}
