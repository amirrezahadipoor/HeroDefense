package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Creates regular melee enemies from the typed gameplay/asset catalog. */
public final class EnemyFactory {
    public Enemy create(GameState state, EnemyType type, float x, float y, int spawnLane) {
        if (state == null || type == null) {
            throw new IllegalArgumentException("State and enemy type are required");
        }
        Enemy enemy = new Enemy(state.allocateEntityId(), type.name(), x, y);
        enemy.health = type.baseHealth();
        enemy.maxHealth = type.baseHealth();
        enemy.damage = type.baseDamage();
        enemy.movementSpeed = type.movementSpeed();
        enemy.attackRange = type.attackRange();
        enemy.attackIntervalSeconds = type.attackIntervalSeconds();
        enemy.spawnLane = spawnLane;
        return enemy;
    }
}
