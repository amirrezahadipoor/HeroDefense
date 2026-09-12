package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.items.EquipmentDefinition;
import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.DropEntity;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.ItemTier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Rolls at most one low-chance equipment drop per defeated enemy, modified by Luck. */
public final class ItemDropSystem {
    public static final float COMMON_RATE = 0.06f;
    public static final float UNCOMMON_RATE = 0.03f;
    public static final float RARE_RATE = 0.008f;
    public static final float LEGENDARY_RATE = 0.0015f;

    private final HeroStatCalculator statCalculator;
    private final Map<ItemTier, List<EquipmentDefinition>> byTier = new EnumMap<>(ItemTier.class);

    public ItemDropSystem() {
        this(new HeroStatCalculator());
    }

    public ItemDropSystem(HeroStatCalculator statCalculator) {
        this.statCalculator = statCalculator;
        for (ItemTier tier : ItemTier.values()) byTier.put(tier, new ArrayList<>());
        for (EquipmentDefinition item : EquipmentCatalog.all()) byTier.get(item.tier()).add(item);
    }

    public int processDefeatedEnemies(GameState state) {
        if (state == null || state.hero == null) return 0;
        int drops = 0;
        for (Enemy enemy : state.aliveEnemies) drops += rollOnce(state, enemy);
        for (Boss boss : state.aliveBosses) drops += rollOnce(state, boss);
        return drops;
    }

    public ItemTier tierForRoll(float roll, float luckMultiplier) {
        if (roll < 0f || roll >= 1f || Float.isNaN(roll)) {
            throw new IllegalArgumentException("Drop roll must be in [0, 1)");
        }
        float multiplier = Math.max(0f, luckMultiplier);
        float threshold = LEGENDARY_RATE * multiplier;
        if (roll < threshold) return ItemTier.LEGENDARY;
        threshold += RARE_RATE * multiplier;
        if (roll < threshold) return ItemTier.RARE;
        threshold += UNCOMMON_RATE * multiplier;
        if (roll < threshold) return ItemTier.UNCOMMON;
        threshold += COMMON_RATE * multiplier;
        return roll < threshold ? ItemTier.COMMON : null;
    }

    private int rollOnce(GameState state, Enemy enemy) {
        if (enemy == null || enemy.alive || enemy.itemDropRolled) return 0;
        enemy.itemDropRolled = true;
        ItemTier tier = tierForRoll(
            state.nextCombatRandomFloat(),
            statCalculator.dropChanceMultiplier(state)
        );
        if (tier == null) return 0;

        List<EquipmentDefinition> choices = byTier.get(tier);
        int index = Math.min(
            choices.size() - 1,
            (int) (state.nextCombatRandomFloat() * choices.size())
        );
        EquipmentDefinition selected = choices.get(index);
        DropEntity drop = new DropEntity(
            state.allocateEntityId(), "ITEM", enemy.x, enemy.y, 1
        );
        drop.itemId = selected.id();
        drop.pickupDelaySeconds = 0.65f;
        state.drops.add(drop);
        return 1;
    }
}
