package com.amirrezahadipoor.herodefense.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Inventory/equipment record; visualKey maps to a generated sprite variant. */
public final class Item {
    public String id = "";
    public String name = "";
    public String slot = "WEAPON";
    public String tier = "COMMON";
    public String iconKey = "";
    public String visualKey = "";
    public int sellPrice;
    /** Anvil reforge steps applied (0..ItemForgeSystem.MAX_UPGRADE); shown as a +N suffix. */
    public int upgradeLevel;
    public Map<String, Float> statBonuses = new LinkedHashMap<>();

    public Item() {
        // Required by libGDX Json.
    }

    public Item(String id, String name, String slot, String tier) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.tier = tier;
    }
}
