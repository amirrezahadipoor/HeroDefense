package com.amirrezahadipoor.herodefense.items;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.model.ItemTier;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable runtime roster of 46 equipment variants. The Hero is a pure archer: every weapon
 * is a bow. All bows now have own art per Phase 29.2 (no borrow), and the six Mythics
 * same-slot Rare/Legendary art until their own glow tier lands.
 */
public final class EquipmentCatalog {
    private static final List<EquipmentDefinition> ALL = Collections.unmodifiableList(Arrays.asList(
        item("ashwood_bow", "Ashwood Bow", EquipmentSlot.WEAPON, ItemTier.COMMON, HeroStat.STRENGTH, 1, HeroStat.AGILITY, 0),
        bow("yew_shortbow", "Yew Shortbow", ItemTier.COMMON, HeroStat.AGILITY, 1, HeroStat.STRENGTH, 0, "yew_shortbow"),
        bow("thornwood_bow", "Thornwood Bow", ItemTier.COMMON, HeroStat.LUCK, 1, HeroStat.STRENGTH, 0, "thornwood_bow"),
        item("leather_cap", "Leather Cap", EquipmentSlot.HELMET, ItemTier.COMMON, HeroStat.DODGE, 1, HeroStat.HEALTH, 0),
        item("scout_hood", "Scout Hood", EquipmentSlot.HELMET, ItemTier.COMMON, HeroStat.HEALTH, 1, HeroStat.DODGE, 0),
        item("padded_vest", "Padded Vest", EquipmentSlot.ARMOR, ItemTier.COMMON, HeroStat.HEALTH, 1, HeroStat.STRENGTH, 0),
        item("bark_tunic", "Bark Tunic", EquipmentSlot.ARMOR, ItemTier.COMMON, HeroStat.STRENGTH, 1, HeroStat.HEALTH, 0),
        item("trail_boots", "Trail Boots", EquipmentSlot.BOOTS, ItemTier.COMMON, HeroStat.AGILITY, 1, HeroStat.DODGE, 0),
        item("hide_greaves", "Hide Greaves", EquipmentSlot.BOOTS, ItemTier.COMMON, HeroStat.DODGE, 1, HeroStat.AGILITY, 0),
        item("copper_leaf_ring", "Copper Leaf Ring", EquipmentSlot.RING_1, ItemTier.COMMON, HeroStat.LUCK, 1, HeroStat.DODGE, 0),
        item("river_pebble_ring", "River Pebble Ring", EquipmentSlot.RING_2, ItemTier.COMMON, HeroStat.LUCK, 1, HeroStat.HEALTH, 0),
        item("acorn_band", "Acorn Band", EquipmentSlot.RING_1, ItemTier.COMMON, HeroStat.LUCK, 1, HeroStat.STRENGTH, 0),
        item("hunter_loop", "Hunter Loop", EquipmentSlot.RING_2, ItemTier.COMMON, HeroStat.LUCK, 1, HeroStat.AGILITY, 0),
        item("twine_circle", "Twine Circle", EquipmentSlot.RING_1, ItemTier.COMMON, HeroStat.LUCK, 1, HeroStat.DODGE, 0),
        item("moonwood_longbow", "Moonwood Longbow", EquipmentSlot.WEAPON, ItemTier.UNCOMMON, HeroStat.AGILITY, 2, HeroStat.STRENGTH, 0),
        bow("verdant_recurve", "Verdant Recurve", ItemTier.UNCOMMON, HeroStat.STRENGTH, 2, HeroStat.AGILITY, 0, "verdant_recurve", "verdant_covenant"),
        item("fern_guard", "Fern Guard", EquipmentSlot.HELMET, ItemTier.UNCOMMON, HeroStat.DODGE, 2, HeroStat.HEALTH, 0, "verdant_covenant"),
        item("antler_circlet", "Antler Circlet", EquipmentSlot.HELMET, ItemTier.UNCOMMON, HeroStat.HEALTH, 2, HeroStat.DODGE, 0),
        item("ranger_mail", "Ranger Mail", EquipmentSlot.ARMOR, ItemTier.UNCOMMON, HeroStat.HEALTH, 2, HeroStat.STRENGTH, 0),
        item("mossweave_coat", "Mossweave Coat", EquipmentSlot.ARMOR, ItemTier.UNCOMMON, HeroStat.STRENGTH, 2, HeroStat.HEALTH, 0, "verdant_covenant"),
        item("windstep_boots", "Windstep Boots", EquipmentSlot.BOOTS, ItemTier.UNCOMMON, HeroStat.AGILITY, 2, HeroStat.DODGE, 0, "verdant_covenant"),
        item("rootguard_sabatons", "Rootguard Sabatons", EquipmentSlot.BOOTS, ItemTier.UNCOMMON, HeroStat.DODGE, 2, HeroStat.AGILITY, 0),
        item("jade_sap_ring", "Jade Sap Ring", EquipmentSlot.RING_1, ItemTier.UNCOMMON, HeroStat.LUCK, 2, HeroStat.STRENGTH, 0),
        item("hawk_eye_band", "Hawk Eye Band", EquipmentSlot.RING_2, ItemTier.UNCOMMON, HeroStat.LUCK, 2, HeroStat.HEALTH, 0),
        item("silver_briar_ring", "Silver Briar Ring", EquipmentSlot.RING_1, ItemTier.UNCOMMON, HeroStat.LUCK, 2, HeroStat.DODGE, 0),
        item("dewstone_loop", "Dewstone Loop", EquipmentSlot.RING_2, ItemTier.UNCOMMON, HeroStat.LUCK, 2, HeroStat.AGILITY, 0),
        item("starfall_bow", "Starfall Bow", EquipmentSlot.WEAPON, ItemTier.RARE, HeroStat.AGILITY, 3, HeroStat.STRENGTH, 1),
        bow("golemsbane_warbow", "Golemsbane Warbow", ItemTier.RARE, HeroStat.STRENGTH, 3, HeroStat.AGILITY, 1, "golemsbane_warbow", "bastion_oath"),
        item("owlguard_helm", "Owlguard Helm", EquipmentSlot.HELMET, ItemTier.RARE, HeroStat.DODGE, 3, HeroStat.HEALTH, 1, "bastion_oath"),
        item("crystalbark_plate", "Crystalbark Plate", EquipmentSlot.ARMOR, ItemTier.RARE, HeroStat.HEALTH, 3, HeroStat.STRENGTH, 1, "bastion_oath"),
        item("shadeleaf_mantle", "Shadeleaf Mantle", EquipmentSlot.ARMOR, ItemTier.RARE, HeroStat.STRENGTH, 3, HeroStat.HEALTH, 1),
        item("stormrunner_boots", "Stormrunner Boots", EquipmentSlot.BOOTS, ItemTier.RARE, HeroStat.AGILITY, 3, HeroStat.DODGE, 1, "bastion_oath"),
        item("sapphire_luck_ring", "Sapphire Luck Ring", EquipmentSlot.RING_1, ItemTier.RARE, HeroStat.LUCK, 3, HeroStat.STRENGTH, 1),
        item("bloodroot_signet", "Bloodroot Signet", EquipmentSlot.RING_2, ItemTier.RARE, HeroStat.LUCK, 3, HeroStat.HEALTH, 1),
        item("echo_band", "Echo Band", EquipmentSlot.RING_1, ItemTier.RARE, HeroStat.LUCK, 3, HeroStat.DODGE, 1),
        item("worldbranch", "Worldbranch", EquipmentSlot.WEAPON, ItemTier.LEGENDARY, HeroStat.AGILITY, 5, HeroStat.STRENGTH, 2),
        item("crown_of_first_leaves", "Crown Of First Leaves", EquipmentSlot.HELMET, ItemTier.LEGENDARY, HeroStat.HEALTH, 5, HeroStat.DODGE, 2),
        item("heartwood_aegis", "Heartwood Aegis", EquipmentSlot.ARMOR, ItemTier.LEGENDARY, HeroStat.HEALTH, 5, HeroStat.STRENGTH, 2),
        item("boots_of_three_winds", "Boots Of Three Winds", EquipmentSlot.BOOTS, ItemTier.LEGENDARY, HeroStat.DODGE, 5, HeroStat.AGILITY, 2),
        item("eternal_seed", "Eternal Seed", EquipmentSlot.RING_2, ItemTier.LEGENDARY, HeroStat.LUCK, 5, HeroStat.AGILITY, 2),
        mythic("sunfall_last_arrow", "Sunfall, the Last Arrow", EquipmentSlot.WEAPON, HeroStat.AGILITY, 2, HeroStat.STRENGTH, 1, "sunfall_last_arrow"),
        mythic("crown_hollow_eye", "Crown of the Hollow Eye", EquipmentSlot.HELMET, HeroStat.HEALTH, 2, HeroStat.DODGE, 1, "crown_hollow_eye"),
        mythic("bark_first_root", "Bark of the First Root", EquipmentSlot.ARMOR, HeroStat.HEALTH, 2, HeroStat.STRENGTH, 1, "bark_first_root"),
        mythic("windrunner_last_steps", "Windrunner's Last Steps", EquipmentSlot.BOOTS, HeroStat.DODGE, 2, HeroStat.AGILITY, 1, "windrunner_last_steps"),
        mythic("verdant_oath", "Verdant Oath", EquipmentSlot.RING_1, HeroStat.LUCK, 2, HeroStat.STRENGTH, 1, "verdant_oath"),
        mythic("emberless_core", "Emberless Core", EquipmentSlot.RING_2, HeroStat.LUCK, 2, HeroStat.AGILITY, 1, "emberless_core")
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

    private static EquipmentDefinition item(
        String id,
        String name,
        EquipmentSlot slot,
        ItemTier tier,
        HeroStat primary,
        int primaryAmount,
        HeroStat secondary,
        int secondaryAmount
    ) {
        return item(id, name, slot, tier, primary, primaryAmount, secondary, secondaryAmount, "");
    }

    private static EquipmentDefinition item(
        String id,
        String name,
        EquipmentSlot slot,
        ItemTier tier,
        HeroStat primary,
        int primaryAmount,
        HeroStat secondary,
        int secondaryAmount,
        String setId
    ) {
        Map<HeroStat, Integer> bonuses = new LinkedHashMap<>();
        bonuses.put(primary, primaryAmount);
        if (secondaryAmount > 0) {
            bonuses.put(secondary, secondaryAmount);
        }
        return new EquipmentDefinition(
            id,
            name,
            slot,
            tier,
            "generated/icons/equipment_" + id + ".png",
            bonuses,
            id,
            setId
        );
    }

    /** A bow whose art is borrowed from an already-reviewed bow of the same tier. */
    private static EquipmentDefinition bow(
        String id,
        String name,
        ItemTier tier,
        HeroStat primary,
        int primaryAmount,
        HeroStat secondary,
        int secondaryAmount,
        String artId
    ) {
        return bow(id, name, tier, primary, primaryAmount, secondary, secondaryAmount, artId, "");
    }

    private static EquipmentDefinition bow(
        String id,
        String name,
        ItemTier tier,
        HeroStat primary,
        int primaryAmount,
        HeroStat secondary,
        int secondaryAmount,
        String artId,
        String setId
    ) {
        Map<HeroStat, Integer> bonuses = new LinkedHashMap<>();
        bonuses.put(primary, primaryAmount);
        if (secondaryAmount > 0) {
            bonuses.put(secondary, secondaryAmount);
        }
        return new EquipmentDefinition(
            id,
            name,
            EquipmentSlot.WEAPON,
            tier,
            "generated/icons/equipment_" + artId + ".png",
            bonuses,
            artId,
            setId
        );
    }

    /**
     * A Mythic: small authored stats (the passive is the power) drawn with a reviewed
     * Legendary's atlas and icon until the Mythic glow tier lands in Phase 23.3c.
     */
    private static EquipmentDefinition mythic(
        String id,
        String name,
        EquipmentSlot slot,
        HeroStat primary,
        int primaryAmount,
        HeroStat secondary,
        int secondaryAmount,
        String artId
    ) {
        Map<HeroStat, Integer> bonuses = new LinkedHashMap<>();
        bonuses.put(primary, primaryAmount);
        if (secondaryAmount > 0) {
            bonuses.put(secondary, secondaryAmount);
        }
        return new EquipmentDefinition(
            id,
            name,
            slot,
            ItemTier.MYTHIC,
            "generated/icons/equipment_" + artId + ".png",
            bonuses,
            artId,
            ""
        );
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
