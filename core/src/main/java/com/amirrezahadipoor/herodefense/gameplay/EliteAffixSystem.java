package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.EliteAffix;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.RotTrailSegment;

import java.util.Iterator;

/** Runs Elite affix combat: blightburst death blasts plus the live rootward/weeping clocks. */
public final class EliteAffixSystem {
    public static final float BLIGHT_BLAST_RADIUS = 150f;
    public static final float BLIGHT_BLAST_DAMAGE_MULT = 1f;
    public static final float ROOTWARD_SHIELD_PERIOD = 6f;
    public static final float ROOTWARD_SHIELD_DURATION = 2f;
    public static final float ROOTWARD_FIRST_SHIELD_DELAY = 2f;
    public static final float WEEPING_TRAIL_INTERVAL = 0.5f;
    public static final float WEEPING_SEGMENT_LIFETIME = 3f;
    public static final float WEEPING_SEGMENT_RADIUS = 55f;
    public static final float WEEPING_DAMAGE_SHARE = 0.35f;

    private final HeroDamageSystem heroDamageSystem;

    public EliteAffixSystem(HeroDamageSystem heroDamageSystem) {
        this.heroDamageSystem = heroDamageSystem;
    }

    public void update(GameState state, float deltaSeconds) {
        if (state == null || state.hero == null || state.aliveEnemies == null || deltaSeconds < 0f) {
            return;
        }
        for (Enemy enemy : state.aliveEnemies) {
            if (enemy == null || enemy.eliteAffix == null) continue;
            if (!enemy.alive) {
                resolveDeath(state, enemy);
                continue;
            }
            if (enemy.stunned()) continue;
            if (EliteAffix.ROOTWARD_WARD.id().equals(enemy.eliteAffix)) {
                updateRootward(enemy, deltaSeconds);
            } else if (EliteAffix.WEEPING_ROT.id().equals(enemy.eliteAffix)) {
                updateWeeping(state, enemy, deltaSeconds);
            }
        }
        updateTrail(state, deltaSeconds);
    }

    private void resolveDeath(GameState state, Enemy enemy) {
        if (enemy.affixResolved) return;
        enemy.affixResolved = true;
        if (EliteAffix.BLIGHTBURST.id().equals(enemy.eliteAffix)
            && state.hero.alive
            && enemy.distanceSquaredTo(state.hero.x, state.hero.y)
                <= BLIGHT_BLAST_RADIUS * BLIGHT_BLAST_RADIUS) {
            heroDamageSystem.applyIncomingHit(
                state, enemy.damage * BLIGHT_BLAST_DAMAGE_MULT);
        }
    }

    private static void updateRootward(Enemy enemy, float deltaSeconds) {
        if (enemy.affixShieldRemainingSeconds > 0f) {
            enemy.affixShieldRemainingSeconds =
                Math.max(0f, enemy.affixShieldRemainingSeconds - deltaSeconds);
            return;
        }
        enemy.affixTimerSeconds += deltaSeconds;
        if (enemy.affixTimerSeconds >= ROOTWARD_SHIELD_PERIOD) {
            enemy.affixTimerSeconds = 0f;
            enemy.affixShieldRemainingSeconds = ROOTWARD_SHIELD_DURATION;
        }
    }

    private static void updateWeeping(GameState state, Enemy enemy, float deltaSeconds) {
        enemy.affixTimerSeconds += deltaSeconds;
        if (enemy.affixTimerSeconds < WEEPING_TRAIL_INTERVAL || state.rotTrail == null) return;
        enemy.affixTimerSeconds -= WEEPING_TRAIL_INTERVAL;
        state.rotTrail.add(new RotTrailSegment(state.allocateEntityId(), enemy.x, enemy.y,
            WEEPING_SEGMENT_LIFETIME, enemy.damage * WEEPING_DAMAGE_SHARE, enemy.id));
    }

    /**
     * Ticks rot patches out and bills standing heroes once per frame: each elite's
     * rot wounds once no matter how its patches overlap; two elites wound twice.
     */
    private void updateTrail(GameState state, float deltaSeconds) {
        if (state.rotTrail == null) return;
        Iterator<RotTrailSegment> segments = state.rotTrail.iterator();
        while (segments.hasNext()) {
            RotTrailSegment segment = segments.next();
            if (segment == null) {
                segments.remove();
                continue;
            }
            segment.remainingSeconds -= deltaSeconds;
            if (segment.remainingSeconds <= 0f) segments.remove();
        }
        float totalDps = 0f;
        for (int index = 0; index < state.rotTrail.size(); index++) {
            RotTrailSegment segment = state.rotTrail.get(index);
            if (coversHero(state, segment) && strongestOfSource(state, index)) {
                totalDps += segment.damagePerSecond;
            }
        }
        if (totalDps > 0f && state.hero.alive) {
            heroDamageSystem.applyEnvironmentalHit(state, totalDps * deltaSeconds);
        }
    }

    private boolean coversHero(GameState state, RotTrailSegment segment) {
        return state.hero.alive && segment != null
            && segment.distanceSquaredTo(state.hero.x, state.hero.y)
                <= WEEPING_SEGMENT_RADIUS * WEEPING_SEGMENT_RADIUS;
    }

    /** Exactly one covering patch bills per source: strongest wins, ties go low-index. */
    private boolean strongestOfSource(GameState state, int index) {
        RotTrailSegment segment = state.rotTrail.get(index);
        for (int other = 0; other < state.rotTrail.size(); other++) {
            if (other == index) continue;
            RotTrailSegment rival = state.rotTrail.get(other);
            if (rival == null || rival.sourceId != segment.sourceId || !coversHero(state, rival)) {
                continue;
            }
            if (rival.damagePerSecond > segment.damagePerSecond
                || (rival.damagePerSecond == segment.damagePerSecond && other < index)) {
                return false;
            }
        }
        return true;
    }
}
