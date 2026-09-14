package com.amirrezahadipoor.herodefense.items;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The two 4-piece equipment sets and their 2/4-piece bonuses. Membership is read from the
 * catalog ({@link EquipmentDefinition#setId()}), so saves need no schema change: forged,
 * renamed, or heirloom items resolve through their stable catalog id.
 */
public final class EquipmentSetBonus {
    public static final String VERDANT_COVENANT = "verdant_covenant";
    public static final String BASTION_OATH = "bastion_oath";

    public record SetBonus(
        String setId,
        String displayName,
        int pieces,
        float attackSpeedBonus,
        int chainTargetsBonus,
        float maxHealthBonus,
        float damageBonus
    ) {
    }

    private static final List<SetBonus> ALL = List.of(
        new SetBonus(VERDANT_COVENANT, "Verdant Covenant", 4, 0.05f, 1, 0f, 0f),
        new SetBonus(BASTION_OATH, "Bastion Oath", 4, 0f, 0, 0.05f, 0.10f)
    );

    private EquipmentSetBonus() {
    }

    public static List<SetBonus> all() {
        return ALL;
    }

    public static SetBonus byId(String setId) {
        if (setId == null) return null;
        for (SetBonus set : ALL) {
            if (set.setId().equals(setId)) return set;
        }
        return null;
    }

    /** Equipped pieces per set id, derived live from the catalog. Never null. */
    public static Map<String, Integer> equippedCounts(GameState state) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SetBonus set : ALL) counts.put(set.setId(), 0);
        if (state == null || state.equippedItems == null) return counts;
        for (Item item : state.equippedItems.values()) {
            if (item == null || item.id == null) continue;
            EquipmentDefinition definition = EquipmentCatalog.byId(item.id);
            if (definition == null) continue;
            String setId = definition.setId();
            if (setId != null && counts.containsKey(setId)) {
                counts.put(setId, counts.get(setId) + 1);
            }
        }
        return counts;
    }

    /** Set ids with all four pieces equipped. */
    public static List<String> completedSets(GameState state) {
        List<String> completed = new ArrayList<>();
        for (Map.Entry<String, Integer> count : equippedCounts(state).entrySet()) {
            SetBonus set = byId(count.getKey());
            if (set != null && count.getValue() >= set.pieces()) completed.add(set.setId());
        }
        return Collections.unmodifiableList(completed);
    }

    private static boolean twoPieceActive(GameState state, String setId) {
        return equippedCounts(state).getOrDefault(setId, 0) >= 2;
    }

    private static boolean fourPieceActive(GameState state, String setId) {
        return equippedCounts(state).getOrDefault(setId, 0) >= 4;
    }

    /** Verdant Covenant (2): +5% attack speed. */
    public static float attackSpeedMultiplier(GameState state) {
        return twoPieceActive(state, VERDANT_COVENANT)
            ? 1f + byId(VERDANT_COVENANT).attackSpeedBonus() : 1f;
    }

    /** Verdant Covenant (4): Chain Lightning arcs to one more target. */
    public static int chainTargetsBonus(GameState state) {
        return fourPieceActive(state, VERDANT_COVENANT)
            ? byId(VERDANT_COVENANT).chainTargetsBonus() : 0;
    }

    /** Bastion Oath (2): +5% max health. */
    public static float maxHealthMultiplier(GameState state) {
        return twoPieceActive(state, BASTION_OATH)
            ? 1f + byId(BASTION_OATH).maxHealthBonus() : 1f;
    }

    /** Bastion Oath (4): +10% damage. */
    public static float damageMultiplier(GameState state) {
        return fourPieceActive(state, BASTION_OATH)
            ? 1f + byId(BASTION_OATH).damageBonus() : 1f;
    }
}
