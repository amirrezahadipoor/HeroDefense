package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.trials.TrialEffects;

/** Creates regular melee enemies from the typed gameplay/asset catalog. */
public final class EnemyFactory {
    private final DifficultyCurve difficultyCurve;

    public EnemyFactory() {
        this(new DifficultyCurve());
    }

    public EnemyFactory(DifficultyCurve difficultyCurve) {
        this.difficultyCurve = difficultyCurve;
    }

    public Enemy create(GameState state, EnemyType type, float x, float y, int spawnLane) {
        if (state == null || type == null) {
            throw new IllegalArgumentException("State and enemy type are required");
        }
        Enemy enemy = new Enemy(state.allocateEntityId(), type.name(), x, y);
        enemy.health = type.baseHealth();
        enemy.maxHealth = type.baseHealth();
        enemy.damage = type.baseDamage();
        enemy.movementSpeed = type.movementSpeed()
            * TrialEffects.enemySpeedMultiplier(state.activeTrials);
        enemy.attackRange = type.attackRange();
        enemy.attackIntervalSeconds = type.attackIntervalSeconds();
        enemy.spawnLane = spawnLane;
        return enemy;
    }

    public Enemy createForWave(
        GameState state,
        EnemyType type,
        float x,
        float y,
        int spawnLane,
        int waveNumber
    ) {
        Enemy enemy = create(state, type, x, y, spawnLane);
        difficultyCurve.applyToRegularEnemy(enemy, type, waveNumber, state.ascensionTier);
        float healthMult = TrialEffects.enemyHealthMultiplier(state.activeTrials);
        enemy.health *= healthMult;
        enemy.maxHealth *= healthMult;
        enemy.damage *= TrialEffects.enemyDamageMultiplier(state.activeTrials);
        return enemy;
    }
}
