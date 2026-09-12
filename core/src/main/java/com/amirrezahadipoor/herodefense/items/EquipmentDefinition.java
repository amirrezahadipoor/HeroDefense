package com.amirrezahadipoor.herodefense.items;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.model.ItemTier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Complete authored identity and gameplay data for one equipment item. */
public final class EquipmentDefinition {
    private final String id;
    private final String name;
    private final EquipmentSlot slot;
    private final ItemTier tier;
    private final String iconPath;
    private final Map<HeroStat, Integer> statBonuses;

    public EquipmentDefinition(
        String id,
        String name,
        EquipmentSlot slot,
        ItemTier tier,
        String iconPath,
        Map<HeroStat, Integer> statBonuses
    ) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.tier = tier;
        this.iconPath = iconPath;
        this.statBonuses = Collections.unmodifiableMap(new LinkedHashMap<>(statBonuses));
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public EquipmentSlot slot() {
        return slot;
    }

    public ItemTier tier() {
        return tier;
    }

    public String iconPath() {
        return iconPath;
    }

    public Map<HeroStat, Integer> statBonuses() {
        return statBonuses;
    }

    public Item createItem() {
        Item item = new Item(id, name, slot.name(), tier.name());
        item.iconKey = iconPath;
        item.visualKey = "generated/equipment/" + id + ".atlas";
        for (Map.Entry<HeroStat, Integer> bonus : statBonuses.entrySet()) {
            item.statBonuses.put(bonus.getKey().name(), bonus.getValue().floatValue());
        }
        return item;
    }
}
