package com.amirrezahadipoor.herodefense.render;

import com.amirrezahadipoor.herodefense.model.HeroAnimationState;
import com.amirrezahadipoor.herodefense.model.Item;

import java.util.Locale;

/** Naming bridge between equipped item records and generated attachment atlases. */
public final class EquipmentVisualContract {
    private EquipmentVisualContract() {
    }

    public static String atlasPath(String itemId) {
        return "generated/equipment/" + itemId + ".atlas";
    }

    public static String regionName(Item item, HeroAnimationState state) {
        return artId(item) + "_" + state.name().toLowerCase(Locale.ROOT);
    }

    /** Atlas region prefix: derived from the item's visual atlas so borrowed art resolves. */
    static String artId(Item item) {
        String key = item.visualKey;
        if (key != null && key.startsWith("generated/equipment/") && key.endsWith(".atlas")) {
            return key.substring("generated/equipment/".length(), key.length() - ".atlas".length());
        }
        return item.id;
    }
}
