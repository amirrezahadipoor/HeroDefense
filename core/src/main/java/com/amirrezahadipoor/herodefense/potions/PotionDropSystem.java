package com.amirrezahadipoor.herodefense.potions;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.DropEntity;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Rolls an approximately eight-percent, wave-weighted potion reward per defeat. */
public final class PotionDropSystem {
    public static final float DROP_RATE = 0.08f;

    public int processDefeatedEnemies(GameState state) {
        if (state == null) return 0;
        int drops = 0;
        for (Enemy enemy : state.aliveEnemies) drops += rollOnce(state, enemy);
        for (Boss boss : state.aliveBosses) drops += rollOnce(state, boss);
        return drops;
    }

    public boolean isDrop(float roll) {
        if (roll < 0f || roll >= 1f || Float.isNaN(roll)) {
            throw new IllegalArgumentException("Potion roll must be in [0, 1)");
        }
        return roll < DROP_RATE;
    }

    /** Unlocks stronger tiers over the run, with higher unlocked tiers weighted more. */
    public PotionTier tierForRoll(int waveNumber, float roll) {
        if (roll < 0f || roll >= 1f || Float.isNaN(roll)) {
            throw new IllegalArgumentException("Tier roll must be in [0, 1)");
        }
        int wave = Math.max(1, Math.min(GameState.FINAL_WAVE, waveNumber));
        int unlocked = Math.min(PotionTier.values().length, 1 + (wave - 1) / 17);
        int totalWeight = unlocked * (unlocked + 1) / 2;
        float cursor = roll * totalWeight;
        for (int index = 0; index < unlocked; index++) {
            cursor -= index + 1;
            if (cursor < 0f) return PotionTier.values()[index];
        }
        return PotionTier.values()[unlocked - 1];
    }

    private int rollOnce(GameState state, Enemy enemy) {
        if (enemy == null || enemy.alive || enemy.potionDropRolled) return 0;
        enemy.potionDropRolled = true;
        if (!isDrop(state.nextCombatRandomFloat())) return 0;
        PotionTier tier = tierForRoll(state.waveNumber, state.nextCombatRandomFloat());
        DropEntity drop = new DropEntity(
            state.allocateEntityId(), "POTION", enemy.x, enemy.y, 1
        );
        drop.itemId = tier.name();
        drop.pickupDelaySeconds = 2.6f;
        state.drops.add(drop);
        return 1;
    }
}
