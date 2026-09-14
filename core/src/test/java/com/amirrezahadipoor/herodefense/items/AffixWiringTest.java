package com.amirrezahadipoor.herodefense.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.DropPickupSystem;
import com.amirrezahadipoor.herodefense.gameplay.EnemyFactory;
import com.amirrezahadipoor.herodefense.gameplay.HeroProgressionSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroStatCalculator;
import com.amirrezahadipoor.herodefense.gameplay.KillRewardSystem;
import com.amirrezahadipoor.herodefense.model.DropEntity;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.potions.HealthPotionSystem;
import com.amirrezahadipoor.herodefense.potions.PotionTier;
import org.junit.jupiter.api.Test;

/** End-to-end proof that equipped affixes reach their gameplay levers. */
final class AffixWiringTest {
    @Test
    void damageAndSurvivalAffixesReshapeHeroStats() {
        HeroStatCalculator stats = new HeroStatCalculator();
        GameState plain = GameState.newRun(1L);
        GameState affixed = GameState.newRun(1L);
        equip(affixed, EquipmentSlot.WEAPON, AffixId.DAMAGE);
        equip(affixed, EquipmentSlot.HELMET, AffixId.MAX_HEALTH);
        equip(affixed, EquipmentSlot.BOOTS, AffixId.DODGE);
        assertEquals(stats.damage(plain) * 1.04f, stats.damage(affixed), 0.001f);
        assertEquals(stats.maxHealth(plain) * 1.04f, stats.maxHealth(affixed), 0.001f);
        assertEquals(stats.dodgeChance(plain) + 0.02f, stats.dodgeChance(affixed), 0.0001f);
    }

    @Test
    void coinsOnKillAffixPaysPerKill() {
        KillRewardSystem rewards = new KillRewardSystem(new HeroProgressionSystem());
        GameState plain = defeatedRootling(2L);
        GameState affixed = defeatedRootling(2L);
        equip(affixed, EquipmentSlot.RING_1, AffixId.COINS_ON_KILL);
        int base = rewards.processDefeatedEnemies(plain).coins();
        int paid = rewards.processDefeatedEnemies(affixed).coins();
        assertEquals(base + 3, paid);
    }

    @Test
    void potionPowerAffixDeepensHealing() {
        HealthPotionSystem potions = new HealthPotionSystem();
        GameState affixed = GameState.newRun(3L);
        equip(affixed, EquipmentSlot.ARMOR, AffixId.POTION_POWER);
        affixed.hero.health = 10f;
        potions.add(affixed, PotionTier.TIER_1, 1);
        assertTrue(potions.use(affixed, PotionTier.TIER_1));
        assertEquals(10f + affixed.hero.maxHealth * 0.15f * 1.20f, affixed.hero.health, 0.01f);
    }

    @Test
    void rarePickupsArriveAffixedWhileCommonsStayClean() {
        DropPickupSystem pickup = new DropPickupSystem();
        GameState state = GameState.newRun(4L);
        DropEntity rare = drop(state, "starfall_bow");
        DropEntity common = drop(state, "ashwood_bow");
        state.drops.add(rare);
        state.drops.add(common);
        assertEquals(2, pickup.update(state, 30f, null));
        assertEquals(2, state.inventory.size());
        for (Item item : state.inventory) {
            if ("starfall_bow".equals(item.id)) {
                assertTrue(AffixId.forName(item.affixId) != null);
            } else {
                assertEquals("", item.affixId);
            }
        }
    }

    private static void equip(GameState state, EquipmentSlot slot, AffixId affix) {
        Item item = new Item("probe", "Probe", slot.name(), "RARE");
        item.affixId = affix.name();
        state.equippedItems.put(slot.name(), item);
    }

    private static GameState defeatedRootling(long seed) {
        GameState state = GameState.newRun(seed);
        state.waveNumber = 1;
        Enemy enemy = new EnemyFactory().create(state, EnemyType.ROOTLING, 0f, 0f, 0);
        enemy.receiveDamage(Float.MAX_VALUE);
        state.aliveEnemies.add(enemy);
        return state;
    }

    private static DropEntity drop(GameState state, String itemId) {
        DropEntity drop = new DropEntity(state.allocateEntityId(), "ITEM", 0f, 0f, 1);
        drop.itemId = itemId;
        drop.pickupDelaySeconds = 0f;
        return drop;
    }
}
