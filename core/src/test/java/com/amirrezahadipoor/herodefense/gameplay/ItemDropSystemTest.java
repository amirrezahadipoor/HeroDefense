package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.ItemTier;
import org.junit.jupiter.api.Test;

final class ItemDropSystemTest {
    private final ItemDropSystem drops = new ItemDropSystem();

    @Test
    void baseRollBandsMatchRoadmapRatesAndPrioritizeRarestTier() {
        assertEquals(ItemTier.LEGENDARY, drops.tierForRoll(0f, 1f));
        assertEquals(ItemTier.RARE, drops.tierForRoll(0.002f, 1f));
        assertEquals(ItemTier.UNCOMMON, drops.tierForRoll(0.01f, 1f));
        assertEquals(ItemTier.COMMON, drops.tierForRoll(0.05f, 1f));
        assertNull(drops.tierForRoll(0.10f, 1f));
    }

    @Test
    void luckMultipliesDropRatesAndEachDefeatRollsOnlyOnce() {
        GameState state = GameState.newRun(91L);
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
        assertEquals(0, pickup.update(state, 0.5f));
        assertEquals(1, pickup.update(state, 0.2f));
        assertEquals(1, state.inventory.size());
        assertEquals(0, state.drops.size());
    }
}
