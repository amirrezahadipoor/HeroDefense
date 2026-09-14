package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.items.MythicEffects;
import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.polish.VfxBudget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Hero's Ultimate (Phase 24.1): at full Focus one tap strikes every foe on
 * screen for a multiple of the Hero's attack damage and drains Focus to zero.
 * Kills pay out through the normal sweep ({@link KillRewardSystem}); the beam
 * fan, blast, shake, and sound are emitted by the caller from the result, so
 * the simulator can fire the same Ultimate without any presentation.
 */
public final class HeroUltimateSystem {
    public static final float ULTIMATE_DAMAGE_MULTIPLIER = 4f;
    /** Blast anchor matches the Focus ring center on the Hero's frame. */
    public static final float BLAST_Y_OFFSET = 73f;

    private final HeroStatCalculator statCalculator;

    public HeroUltimateSystem() {
        this(new HeroStatCalculator());
    }

    public HeroUltimateSystem(HeroStatCalculator statCalculator) {
        this.statCalculator = statCalculator;
    }

    /** Fires the Ultimate; returns {@link UltimateResult#NONE} below full Focus. */
    public UltimateResult fire(GameState state) {
        if (state == null || state.hero == null || !FocusSystem.isFull(state)) {
            return UltimateResult.NONE;
        }
        float damage = Math.max(0f, statCalculator.damage(state)) * ULTIMATE_DAMAGE_MULTIPLIER;
        state.focus = 0f;
        List<Enemy> foes = new ArrayList<>();
        if (state.aliveEnemies != null) {
            for (Enemy enemy : state.aliveEnemies) {
                if (enemy != null && enemy.alive) foes.add(enemy);
            }
        }
        if (state.aliveBosses != null) {
            for (Boss boss : state.aliveBosses) {
                if (boss != null && boss.alive) foes.add(boss);
            }
        }
        for (Enemy foe : foes) {
            foe.receiveDamage(damage * MythicEffects.crownMarkDamageMultiplier(foe));
        }
        foes.sort(Comparator.comparingDouble(
            foe -> distanceSquared(state.hero.x, state.hero.y, foe.x, foe.y)
        ));
        List<Enemy> arcs = foes.subList(0, Math.min(VfxBudget.ULTIMATE_MAX_ARCS, foes.size()));
        return new UltimateResult(
            foes.size(), damage, arcs, state.hero.x, state.hero.y + BLAST_Y_OFFSET
        );
    }

    private static float distanceSquared(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return dx * dx + dy * dy;
    }
}
