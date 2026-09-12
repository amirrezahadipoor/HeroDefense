package com.amirrezahadipoor.herodefense.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

final class EntityStructureTest {
    @Test
    void enemyDamageTransitionsToDeadOnlyAtZeroHealth() {
        Enemy enemy = new Enemy(2L, "ROOTLING", 10f, 20f);
        enemy.health = enemy.maxHealth = 12f;
        enemy.receiveDamage(5f);
        assertEquals(7f, enemy.health);
        enemy.receiveDamage(99f);
        assertFalse(enemy.alive);
        assertFalse(enemy.active);
    }

    @Test
    void entityDistanceUsesArenaCoordinates() {
        Hero hero = new Hero(1L, 0f, 0f);
        assertEquals(25f, hero.distanceSquaredTo(3f, 4f));
    }
}
