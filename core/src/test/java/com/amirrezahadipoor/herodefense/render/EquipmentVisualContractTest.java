package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.items.EquipmentDefinition;
import com.amirrezahadipoor.herodefense.model.HeroAnimationState;
import com.amirrezahadipoor.herodefense.model.Item;
import org.junit.jupiter.api.Test;

final class EquipmentVisualContractTest {
    @Test
    void everyRuntimeItemMapsToItsReviewedAtlasAndFourHeroClips() {
        for (EquipmentDefinition definition : EquipmentCatalog.all()) {
            Item item = definition.createItem();
            assertEquals(EquipmentVisualContract.atlasPath(item.id), item.visualKey);
            for (HeroAnimationState state : HeroAnimationState.values()) {
                assertEquals(
                    item.id + "_" + state.name().toLowerCase(java.util.Locale.ROOT),
                    EquipmentVisualContract.regionName(item, state)
                );
            }
        }
    }
}
