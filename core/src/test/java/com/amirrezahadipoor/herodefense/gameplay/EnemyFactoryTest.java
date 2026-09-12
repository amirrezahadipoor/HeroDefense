package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class EnemyFactoryTest {
    private final EnemyFactory factory = new EnemyFactory();

    @Test
    void createsAllFourDistinctRegularTypesAsMeleeAttackers() {
        GameState state = GameState.newRun(4L);
        for (EnemyType type : EnemyType.values()) {
            Enemy enemy = factory.create(state, type, 10f, 20f, 2);
            assertEquals(type.name(), enemy.enemyType);
            assertEquals(type.baseHealth(), enemy.health);
            assertEquals(type.baseDamage(), enemy.damage);
            assertEquals(type.attackRange(), enemy.attackRange);
            assertTrue(type.isMelee());
            assertTrue(enemy.attackRange <= 50f);
        }
        assertEquals(4, EnemyType.values().length);
    }

    @Test
    void archetypesHaveDifferentCombatProfilesAndMatchingAssetKeys() {
        assertTrue(EnemyType.GLOOM_WOLF.movementSpeed() > EnemyType.STONEKIN.movementSpeed());
        assertTrue(EnemyType.FUNGAL_BRUTE.baseHealth() > EnemyType.ROOTLING.baseHealth());
        assertEquals("fungal_brute", EnemyType.FUNGAL_BRUTE.assetKey());
    }
}
