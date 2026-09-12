package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.BossType;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Creates a boss with its authored identity; wave multipliers are applied separately. */
public final class BossFactory {
    public Boss create(
        GameState state,
        BossType type,
        float x,
        float y,
        int bossNumber,
        int spawnLane
    ) {
        if (state == null || type == null || bossNumber < 1) {
            throw new IllegalArgumentException("Valid state, boss type, and boss number are required");
        }
        Boss boss = new Boss(state.allocateEntityId(), type.name(), x, y, bossNumber);
        boss.uniqueAttack = type.uniqueAttack();
        boss.movementSpeed = type.movementSpeed();
        boss.attackRange = type.attackRange();
        boss.attackIntervalSeconds = type.attackIntervalSeconds();
        boss.spawnLane = spawnLane;
        return boss;
    }
}
