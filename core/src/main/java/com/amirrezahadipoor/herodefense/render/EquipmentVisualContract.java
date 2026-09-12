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
        return item.id + "_" + state.name().toLowerCase(Locale.ROOT);
    }
}
