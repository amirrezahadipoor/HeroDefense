package com.amirrezahadipoor.herodefense.render;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.model.Item;

import java.util.ArrayList;
import java.util.List;

/** Builds complete, deterministic inventory detail/comparison data for rendering. */
public final class InventoryItemDetails {
    private InventoryItemDetails() {
    }

    public static Details inspect(GameState state, Item candidate) {
        if (state == null || candidate == null) return null;
        EquipmentSlot slot = EquipmentSlot.parse(candidate.slot);
        Item equipped = slot == null ? null : state.equippedItems.get(slot.name());
        boolean isEquipped = sameItem(candidate, equipped);
        List<StatComparison> stats = new ArrayList<>();
        for (HeroStat stat : HeroStat.values()) {
            float candidateValue = bonus(candidate, stat);
            float equippedValue = bonus(equipped, stat);
            if (candidateValue != 0f || equippedValue != 0f) {
                stats.add(new StatComparison(
                    stat,
                    candidateValue,
                    equippedValue,
                    candidateValue - equippedValue
                ));
            }
        }
        return new Details(
            candidate.name,
            candidate.tier,
            slot,
            isEquipped,
            equipped == null ? null : equipped.name,
            stats
        );
    }

    private static float bonus(Item item, HeroStat stat) {
        if (item == null || item.statBonuses == null) return 0f;
        Float value = item.statBonuses.get(stat.name());
        return value == null ? 0f : value;
    }

    private static boolean sameItem(Item first, Item second) {
        if (first == second) return first != null;
        return first != null && second != null && first.id != null && first.id.equals(second.id);
    }

    public record Details(
        String name,
        String rarity,
        EquipmentSlot slot,
        boolean equipped,
        String comparedItemName,
        List<StatComparison> stats
    ) {
        public Details {
            stats = List.copyOf(stats);
        }
    }

    public record StatComparison(
        HeroStat stat,
        float candidateValue,
        float equippedValue,
        float difference
    ) {
    }
}
