package com.amirrezahadipoor.herodefense.items;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.ItemTier;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable runtime roster matching the 40 reviewed procedural equipment variants. */
public final class EquipmentCatalog {
    private static final List<EquipmentDefinition> ALL = Collections.unmodifiableList(Arrays.asList(
        item("ashwood_bow", EquipmentSlot.WEAPON, ItemTier.COMMON),
        item("militia_sabre", EquipmentSlot.WEAPON, ItemTier.COMMON),
        item("thorn_spear", EquipmentSlot.WEAPON, ItemTier.COMMON),
        item("leather_cap", EquipmentSlot.HELMET, ItemTier.COMMON),
        item("scout_hood", EquipmentSlot.HELMET, ItemTier.COMMON),
        item("padded_vest", EquipmentSlot.ARMOR, ItemTier.COMMON),
        item("bark_tunic", EquipmentSlot.ARMOR, ItemTier.COMMON),
        item("trail_boots", EquipmentSlot.BOOTS, ItemTier.COMMON),
        item("hide_greaves", EquipmentSlot.BOOTS, ItemTier.COMMON),
        item("copper_leaf_ring", EquipmentSlot.RING_1, ItemTier.COMMON),
        item("river_pebble_ring", EquipmentSlot.RING_2, ItemTier.COMMON),
        item("acorn_band", EquipmentSlot.RING_1, ItemTier.COMMON),
        item("hunter_loop", EquipmentSlot.RING_2, ItemTier.COMMON),
        item("twine_circle", EquipmentSlot.RING_1, ItemTier.COMMON),
        item("moonwood_longbow", EquipmentSlot.WEAPON, ItemTier.UNCOMMON),
        item("verdant_glaive", EquipmentSlot.WEAPON, ItemTier.UNCOMMON),
        item("fern_guard", EquipmentSlot.HELMET, ItemTier.UNCOMMON),
        item("antler_circlet", EquipmentSlot.HELMET, ItemTier.UNCOMMON),
        item("ranger_mail", EquipmentSlot.ARMOR, ItemTier.UNCOMMON),
        item("mossweave_coat", EquipmentSlot.ARMOR, ItemTier.UNCOMMON),
        item("windstep_boots", EquipmentSlot.BOOTS, ItemTier.UNCOMMON),
        item("rootguard_sabatons", EquipmentSlot.BOOTS, ItemTier.UNCOMMON),
        item("jade_sap_ring", EquipmentSlot.RING_1, ItemTier.UNCOMMON),
        item("hawk_eye_band", EquipmentSlot.RING_2, ItemTier.UNCOMMON),
        item("silver_briar_ring", EquipmentSlot.RING_1, ItemTier.UNCOMMON),
        item("dewstone_loop", EquipmentSlot.RING_2, ItemTier.UNCOMMON),
        item("starfall_bow", EquipmentSlot.WEAPON, ItemTier.RARE),
        item("golem_splitter", EquipmentSlot.WEAPON, ItemTier.RARE),
        item("owlguard_helm", EquipmentSlot.HELMET, ItemTier.RARE),
        item("crystalbark_plate", EquipmentSlot.ARMOR, ItemTier.RARE),
        item("shadeleaf_mantle", EquipmentSlot.ARMOR, ItemTier.RARE),
        item("stormrunner_boots", EquipmentSlot.BOOTS, ItemTier.RARE),
        item("sapphire_luck_ring", EquipmentSlot.RING_1, ItemTier.RARE),
        item("bloodroot_signet", EquipmentSlot.RING_2, ItemTier.RARE),
        item("echo_band", EquipmentSlot.RING_1, ItemTier.RARE),
        item("worldbranch", EquipmentSlot.WEAPON, ItemTier.LEGENDARY),
        item("crown_of_first_leaves", EquipmentSlot.HELMET, ItemTier.LEGENDARY),
        item("heartwood_aegis", EquipmentSlot.ARMOR, ItemTier.LEGENDARY),
        item("boots_of_three_winds", EquipmentSlot.BOOTS, ItemTier.LEGENDARY),
        item("eternal_seed", EquipmentSlot.RING_2, ItemTier.LEGENDARY)
    ));
    private static final Map<String, EquipmentDefinition> BY_ID = indexById();

    private EquipmentCatalog() {
    }

    public static List<EquipmentDefinition> all() {
        return ALL;
    }

    public static EquipmentDefinition byId(String id) {
        return BY_ID.get(id);
    }

    private static EquipmentDefinition item(String id, EquipmentSlot slot, ItemTier tier) {
        return new EquipmentDefinition(id, slot, tier);
    }

    private static Map<String, EquipmentDefinition> indexById() {
        Map<String, EquipmentDefinition> result = new LinkedHashMap<>();
        for (EquipmentDefinition definition : ALL) {
            if (result.put(definition.id(), definition) != null) {
                throw new IllegalStateException("Duplicate equipment id: " + definition.id());
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
