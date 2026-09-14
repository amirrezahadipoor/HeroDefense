package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.EliteAffix;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Resolves Elite affix combat: blightburst death blasts (25.2a), live affixes in 25.2b. */
public final class EliteAffixSystem {
    public static final float BLIGHT_BLAST_RADIUS = 150f;
    public static final float BLIGHT_BLAST_DAMAGE_MULT = 1f;

    private final HeroDamageSystem heroDamageSystem;

    public EliteAffixSystem(HeroDamageSystem heroDamageSystem) {
        this.heroDamageSystem = heroDamageSystem;
    }

    public void update(GameState state, float deltaSeconds) {
        if (state == null || state.hero == null || state.aliveEnemies == null || deltaSeconds < 0f) {
            return;
        }
        for (Enemy enemy : state.aliveEnemies) {
            if (enemy == null || enemy.alive || enemy.eliteAffix == null || enemy.affixResolved) {
                continue;
            }
            enemy.affixResolved = true;
            if (EliteAffix.BLIGHTBURST.id().equals(enemy.eliteAffix)
                && state.hero.alive
                && enemy.distanceSquaredTo(state.hero.x, state.hero.y)
                    <= BLIGHT_BLAST_RADIUS * BLIGHT_BLAST_RADIUS) {
                heroDamageSystem.applyIncomingHit(
                    state, enemy.damage * BLIGHT_BLAST_DAMAGE_MULT);
            }
        }
    }
}
