package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import java.lang.reflect.Field;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Phase 29.4: hero renders only boots + weapon for silhouette readability. */
final class HeroEquipmentLayerOrderTest {

    @Test
    void layerOrderContainsOnlyBootsAndWeaponInOrder() throws Exception {
        Field f = EquipmentSpriteRenderer.class.getDeclaredField("LAYER_ORDER");
        f.setAccessible(true);
        EquipmentSlot[] order = (EquipmentSlot[]) f.get(null);
        assertEquals(2, order.length, "LAYER_ORDER must be boots+weapon only");
        assertEquals(EquipmentSlot.BOOTS, order[0]);
        assertEquals(EquipmentSlot.WEAPON, order[1]);
        Set<EquipmentSlot> allowed = Set.of(EquipmentSlot.BOOTS, EquipmentSlot.WEAPON);
        for (EquipmentSlot s : order) {
            assertTrue(allowed.contains(s), "unexpected slot " + s);
        }
        // Ensure dropped slots are not rendered
        assertTrue(java.util.Arrays.stream(order).noneMatch(s -> s == EquipmentSlot.ARMOR));
        assertTrue(java.util.Arrays.stream(order).noneMatch(s -> s == EquipmentSlot.HELMET));
        assertTrue(java.util.Arrays.stream(order).noneMatch(s -> s == EquipmentSlot.RING_1));
        assertTrue(java.util.Arrays.stream(order).noneMatch(s -> s == EquipmentSlot.RING_2));
    }

    @Test
    void weaponIsTopmostLayer() throws Exception {
        Field f = EquipmentSpriteRenderer.class.getDeclaredField("LAYER_ORDER");
        f.setAccessible(true);
        EquipmentSlot[] order = (EquipmentSlot[]) f.get(null);
        assertEquals(EquipmentSlot.WEAPON, order[order.length - 1], "weapon must be topmost");
    }
}
