package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.BossType;
import com.amirrezahadipoor.herodefense.model.GameState;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class BossFactoryTest {
    @Test
    void catalogContainsFourDistinctDesignAndAttackIdentities() {
        GameState state = GameState.newRun(9L);
        BossFactory factory = new BossFactory();
        Set<String> assets = new HashSet<>();
        Set<String> attacks = new HashSet<>();

        int number = 1;
        for (BossType type : BossType.values()) {
            Boss boss = factory.create(state, type, 1f, 2f, number++, 0);
            assertEquals(type, boss.bossDefinition());
            assertEquals(type.uniqueAttack(), boss.uniqueAttack);
            assets.add(type.assetKey());
            attacks.add(type.uniqueAttack());
        }

        assertEquals(4, BossType.values().length);
        assertEquals(4, assets.size());
        assertEquals(4, attacks.size());
        assertNotEquals(BossType.ANCIENT_GOLEM.assetKey(), BossType.EMBER_WYRM.assetKey());
    }
}
