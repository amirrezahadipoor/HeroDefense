package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.DropCollectionStage;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.ItemTier;
import org.junit.jupiter.api.Test;

final class ItemDropSystemTest {
    private final ItemDropSystem drops = new ItemDropSystem();

    @Test
    void baseRollBandsMatchRoadmapRatesAndPrioritizeRarestTier() {
        assertEquals(0.06f, ItemDropSystem.COMMON_RATE);
        assertEquals(0.03f, ItemDropSystem.UNCOMMON_RATE);
        assertEquals(0.008f, ItemDropSystem.RARE_RATE);
        assertEquals(0.0015f, ItemDropSystem.LEGENDARY_RATE);

        float legendaryEnd = ItemDropSystem.LEGENDARY_RATE;
        float rareEnd = legendaryEnd + ItemDropSystem.RARE_RATE;
        float uncommonEnd = rareEnd + ItemDropSystem.UNCOMMON_RATE;
        float commonEnd = uncommonEnd + ItemDropSystem.COMMON_RATE;
        assertEquals(ItemTier.LEGENDARY, drops.tierForRoll(0f, 1f));
        assertEquals(ItemTier.RARE, drops.tierForRoll(legendaryEnd, 1f));
        assertEquals(ItemTier.UNCOMMON, drops.tierForRoll(rareEnd, 1f));
        assertEquals(ItemTier.COMMON, drops.tierForRoll(uncommonEnd, 1f));
        assertNull(drops.tierForRoll(commonEnd, 1f));
    }

    @Test
    void luckMultipliesDropRatesAndEachDefeatRollsOnlyOnce() {
        GameState state = GameState.newRun(91L);
        state.hero.stats.luck = 2;
        assertEquals(
            (float) Math.pow(1.02, 2),
            new HeroStatCalculator().dropChanceMultiplier(state),
            0.0001f
        );
        state.hero.stats.luck = 200; // Guarantees total weighted chance above 100% for this test.
        Enemy enemy = new EnemyFactory().create(state, EnemyType.ROOTLING, 1f, 2f, 0);
        enemy.receiveDamage(Float.MAX_VALUE);
        state.aliveEnemies.add(enemy);

        assertEquals(1, drops.processDefeatedEnemies(state));
        assertEquals(1, state.drops.size());
        assertNotNull(EquipmentCatalog.byId(state.drops.get(0).itemId));
        assertEquals(0, drops.processDefeatedEnemies(state));
        assertEquals(1, state.drops.size());

        DropPickupSystem pickup = new DropPickupSystem();
        // Drops linger visibly on the ground (2.6s) before homing to the inventory.
        assertEquals(0, pickup.update(state, 2.4f));
        assertEquals(DropCollectionStage.GROUND, state.drops.get(0).collectionStage);
        assertEquals(0, pickup.update(state, 0.25f));
        assertEquals(DropCollectionStage.HOMING, state.drops.get(0).collectionStage);
        assertEquals(0, state.inventory.size());
        assertEquals(0, pickup.update(state, 0.3f));
        assertEquals(1, pickup.update(state, 0.11f));
        assertEquals(1, state.inventory.size());
        assertEquals(0, state.drops.size());
    }

    @Test
    void silentWatcherDefeatRollsNoItemDrop() {
        GameState state = GameState.newRun(92L);
        state.hero.stats.luck = 200; // Would guarantee a drop for a real foe.
        Enemy watcher = new EnemyFactory().create(state, EnemyType.ROOTLING, 1f, 2f, 0);
        watcher.silentWatcher = true;
        watcher.receiveDamage(Float.MAX_VALUE);
        state.aliveEnemies.add(watcher);

        assertEquals(0, drops.processDefeatedEnemies(state));
        assertEquals(0, state.drops.size());
    }
}
