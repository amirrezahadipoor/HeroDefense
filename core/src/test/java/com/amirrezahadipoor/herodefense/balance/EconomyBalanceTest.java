package com.amirrezahadipoor.herodefense.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.KillRewardSystem;
import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.items.EquipmentDefinition;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.model.ItemTier;
import com.amirrezahadipoor.herodefense.shop.StatShopSystem;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class EconomyBalanceTest {
    private final StatShopSystem shop = new StatShopSystem();

    @Test
    void eachBossCoinRewardBuysRoughlyOneContemporaryStatUpgrade() {
        GameState state = GameState.newRun(0xEC0L);
        for (int bossNumber = 1; bossNumber <= 20; bossNumber++) {
            int contemporaryLevel = bossNumber - 1;
            int reward = KillRewardSystem.bossCoinReward(bossNumber);
            for (HeroStat stat : HeroStat.values()) {
                state.shopUpgradeLevels.put(stat.name(), contemporaryLevel);
                int price = shop.price(state, stat);
                String context = "Boss " + bossNumber + ", " + stat;
                assertTrue(price <= reward, context + " was not affordable");
                assertTrue(
                    reward <= Math.round(price * 1.40f),
                    context + " reward bought substantially more than one upgrade"
                );
            }
        }
    }

    @Test
    void everyItemUsesTheMonotonicTierResaleBudget() {
        Map<ItemTier, Integer> expected = Map.of(
            ItemTier.COMMON, 12,
            ItemTier.UNCOMMON, 30,
            ItemTier.RARE, 75,
            ItemTier.LEGENDARY, 180,
            ItemTier.MYTHIC, 400
        );
        for (EquipmentDefinition definition : EquipmentCatalog.all()) {
            assertEquals(
                expected.get(definition.tier()),
                definition.createItem().sellPrice,
                definition.id()
            );
        }
    }
}
