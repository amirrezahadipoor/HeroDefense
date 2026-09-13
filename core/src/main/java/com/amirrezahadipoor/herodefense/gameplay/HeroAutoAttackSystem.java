package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Hero;
import com.amirrezahadipoor.herodefense.model.Projectile;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.skills.SkillEffects;
import com.amirrezahadipoor.herodefense.skills.SkillId;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic nearest-target bow attacks for the stationary Hero, extended by the five
 * purchasable skills: Multi Shot volleys, Chain Lightning arcs, stunning arrows, critical
 * mastery, and bonus range. All rolls use the persisted combat RNG.
 */
public final class HeroAutoAttackSystem {
    public static final float ATTACK_RANGE = 420f;
    public static final float PROJECTILE_SPEED = 900f;
    public static final float CRITICAL_CHANCE = SkillEffects.BASE_CRITICAL_CHANCE;
    public static final float CRITICAL_DAMAGE_MULTIPLIER = SkillEffects.BASE_CRITICAL_MULTIPLIER;
    private static final int MAX_SHOTS_PER_UPDATE = 4;
    private static final int MAX_EXTRA_ARROWS = 4;

    /** Presentation events are capped so a maxed Multi Shot volley cannot flood a frame. */
    public static final int MAX_EVENTS_PER_UPDATE = 48;

    private final List<Enemy> scratchTargets = new ArrayList<>();
    private final List<CombatEvent> events = new ArrayList<>();

    private final HeroStatCalculator statCalculator;

    public HeroAutoAttackSystem() {
        this(new HeroStatCalculator());
    }

    public HeroAutoAttackSystem(HeroStatCalculator statCalculator) {
        this.statCalculator = statCalculator;
    }

    public HeroAttackUpdateResult update(GameState state, float deltaSeconds) {
        if (state == null || state.hero == null || !state.hero.alive || deltaSeconds < 0f) {
            return HeroAttackUpdateResult.NONE;
        }
        ImpactCounts impacts = updateProjectiles(state, deltaSeconds);

        Hero hero = state.hero;
        hero.attackCooldownSeconds -= deltaSeconds;
        Enemy target = findNearestTarget(state, hero.x, hero.y, attackRange(state));
        hero.currentTargetId = target == null ? -1L : target.id;
        if (target == null) {
            hero.attackCooldownSeconds = Math.max(0f, hero.attackCooldownSeconds);
            return new HeroAttackUpdateResult(
                0, impacts.hits, impacts.criticalHits, impacts.x, impacts.y,
                impacts.chainArcs, impacts.stuns, events
            );
        }

        int shots = 0;
        while (hero.attackCooldownSeconds <= 0f && shots < MAX_SHOTS_PER_UPDATE) {
            fire(state, hero, target);
            hero.attackCooldownSeconds += statCalculator.attackIntervalSeconds(state);
            shots++;
        }
        return new HeroAttackUpdateResult(
            shots, impacts.hits, impacts.criticalHits, impacts.x, impacts.y,
            impacts.chainArcs, impacts.stuns, events
        );
    }

    /** Bow reach including purchased Eagle Range levels. */
    public static float attackRange(GameState state) {
        return ATTACK_RANGE + SkillEffects.bonusRange(SkillEffects.level(state, SkillId.LONG_RANGE));
    }

    public Enemy findNearestTarget(GameState state, float x, float y, float range) {
        float maximumDistanceSquared = range * range;
        Enemy nearest = null;
        float nearestDistanceSquared = maximumDistanceSquared;
        for (Enemy enemy : state.aliveEnemies) {
            float distance = validDistanceSquared(enemy, x, y);
            if (isBetterTarget(enemy, distance, nearest, nearestDistanceSquared)) {
                nearest = enemy;
                nearestDistanceSquared = distance;
            }
        }
        for (Boss boss : state.aliveBosses) {
            float distance = validDistanceSquared(boss, x, y);
            if (isBetterTarget(boss, distance, nearest, nearestDistanceSquared)) {
                nearest = boss;
                nearestDistanceSquared = distance;
            }
        }
        return nearest;
    }

    private static float validDistanceSquared(Enemy enemy, float x, float y) {
        if (enemy == null || !enemy.alive || !enemy.active) {
            return Float.POSITIVE_INFINITY;
        }
        return enemy.distanceSquaredTo(x, y);
    }

    private static boolean isBetterTarget(
        Enemy candidate,
        float candidateDistance,
        Enemy current,
        float currentDistance
    ) {
        if (candidateDistance > currentDistance) {
            return false;
        }
        return candidateDistance < currentDistance
            || (candidateDistance != Float.POSITIVE_INFINITY && current != null && candidate.id < current.id)
            || (candidateDistance != Float.POSITIVE_INFINITY && current == null);
    }

    private void fire(GameState state, Hero hero, Enemy target) {
        hero.beginAttackAnimation();
        launch(state, hero, target, false);

        float extra = SkillEffects.extraArrows(SkillEffects.level(state, SkillId.MULTI_SHOT));
        int extraArrows = (int) extra;
        if (state.nextCombatRandomFloat() < extra - extraArrows) extraArrows++;
        extraArrows = Math.min(MAX_EXTRA_ARROWS, extraArrows);
        if (extraArrows == 0) return;

        collectTargetsByDistance(state, hero.x, hero.y, attackRange(state), target);
        for (int index = 0; index < extraArrows; index++) {
            // Spread across other foes when available; otherwise stack on the primary target.
            Enemy extraTarget = scratchTargets.isEmpty()
                ? target
                : scratchTargets.get(index % scratchTargets.size());
            launch(state, hero, extraTarget, true);
        }
    }

    private void launch(GameState state, Hero hero, Enemy target, boolean secondary) {
        Projectile projectile = new Projectile(
            state.allocateEntityId(), hero.id, target.id, hero.x, hero.y
        );
        int mastery = SkillEffects.level(state, SkillId.CRITICAL_MASTERY);
        projectile.critical = state.nextCombatRandomFloat() < SkillEffects.criticalChance(mastery);
        projectile.secondary = secondary;
        projectile.damage = statCalculator.damage(state)
            * (1f + effectValue(state, BossRewardCardSystem.GENERAL_POWER_KEY))
            * (projectile.critical ? SkillEffects.criticalMultiplier(mastery) : 1f)
            * (secondary ? SkillEffects.MULTI_SHOT_DAMAGE_SHARE : 1f);
        float distance = (float) Math.sqrt(hero.distanceSquaredTo(target.x, target.y));
        projectile.remainingLifetimeSeconds = distance / PROJECTILE_SPEED + 0.25f;
        setVelocityToward(projectile, target);
        state.projectiles.add(projectile);
    }

    /** Fills {@link #scratchTargets} with living foes in range other than {@code exclude}, nearest first. */
    private void collectTargetsByDistance(GameState state, float x, float y, float range, Enemy exclude) {
        scratchTargets.clear();
        float maximum = range * range;
        for (Enemy enemy : state.aliveEnemies) {
            if (enemy != exclude && validDistanceSquared(enemy, x, y) <= maximum) scratchTargets.add(enemy);
        }
        for (Boss boss : state.aliveBosses) {
            if (boss != exclude && validDistanceSquared(boss, x, y) <= maximum) scratchTargets.add(boss);
        }
        scratchTargets.sort((a, b) -> {
            int byDistance = Float.compare(a.distanceSquaredTo(x, y), b.distanceSquaredTo(x, y));
            return byDistance != 0 ? byDistance : Long.compare(a.id, b.id);
        });
    }

    private ImpactCounts updateProjectiles(GameState state, float deltaSeconds) {
        int hits = 0;
        int criticalHits = 0;
        int chainArcs = 0;
        int stuns = 0;
        int chainLevel = SkillEffects.level(state, SkillId.CHAIN_LIGHTNING);
        int stunLevel = SkillEffects.level(state, SkillId.STUN_CHANCE);
        float lifesteal = effectValue(state, BossRewardCardSystem.LIFESTEAL_KEY);
        float impactX = Float.NaN;
        float impactY = Float.NaN;
        events.clear();
        for (Projectile projectile : state.projectiles) {
            if (projectile == null || !projectile.active || projectile.sourceId != state.hero.id) {
                continue;
            }
            Enemy target = findTargetById(state, projectile.targetId);
            projectile.remainingLifetimeSeconds -= deltaSeconds;
            if (target == null || projectile.remainingLifetimeSeconds < 0f) {
                projectile.active = false;
                continue;
            }

            float distanceSquared = projectile.distanceSquaredTo(target.x, target.y);
            float travel = PROJECTILE_SPEED * deltaSeconds;
            if (distanceSquared <= travel * travel) {
                projectile.x = target.x;
                projectile.y = target.y;
                float healthBefore = target.health;
                target.receiveDamage(projectile.damage);
                hits++;
                impactX = target.x;
                impactY = target.y;
                if (projectile.critical) criticalHits++;
                float damageDealt = Math.max(0f, healthBefore - target.health);
                emit(CombatEvent.hit(target.x, target.y + 40f, projectile.damage,
                    projectile.critical, projectile.secondary));
                if (stunLevel > 0 && target.alive
                    && state.nextCombatRandomFloat() < SkillEffects.stunChance(stunLevel)) {
                    float duration = SkillEffects.stunDuration(stunLevel);
                    if (target instanceof Boss) duration *= SkillEffects.BOSS_STUN_RESISTANCE;
                    target.stunRemainingSeconds = Math.max(target.stunRemainingSeconds, duration);
                    stuns++;
                    emit(CombatEvent.stun(target.x, target.y + 70f, duration));
                }
                if (chainLevel > 0 && !projectile.secondary
                    && state.nextCombatRandomFloat() < SkillEffects.chainChance(chainLevel)) {
                    int arcs = chainLightning(state, target, projectile.damage, chainLevel);
                    chainArcs += arcs;
                    damageDealt += arcs * projectile.damage * SkillEffects.CHAIN_DAMAGE_SHARE;
                }
                if (lifesteal > 0f && state.hero.alive) {
                    state.hero.health = Math.min(
                        state.hero.maxHealth,
                        state.hero.health + damageDealt * lifesteal
                    );
                }
                projectile.active = false;
            } else {
                setVelocityToward(projectile, target);
                projectile.x += projectile.velocityX * deltaSeconds;
                projectile.y += projectile.velocityY * deltaSeconds;
            }
        }
        state.projectiles.removeIf(projectile -> projectile == null || !projectile.active);
        return new ImpactCounts(hits, criticalHits, impactX, impactY, chainArcs, stuns);
    }

    private void emit(CombatEvent event) {
        if (events.size() < MAX_EVENTS_PER_UPDATE) events.add(event);
    }

    /** Arcs a share of the arrow's damage to the nearest foes around the struck target. */
    private int chainLightning(GameState state, Enemy struck, float arrowDamage, int level) {
        collectTargetsByDistance(state, struck.x, struck.y, SkillEffects.CHAIN_RADIUS, struck);
        int arcs = Math.min(SkillEffects.chainTargets(level), scratchTargets.size());
        float arcDamage = arrowDamage * SkillEffects.CHAIN_DAMAGE_SHARE;
        for (int index = 0; index < arcs; index++) {
            Enemy victim = scratchTargets.get(index);
            victim.receiveDamage(arcDamage);
            emit(CombatEvent.arc(struck.x, struck.y + 40f, victim.x, victim.y + 40f, arcDamage));
        }
        return arcs;
    }

    private static Enemy findTargetById(GameState state, long id) {
        for (Enemy enemy : state.aliveEnemies) {
            if (enemy != null && enemy.id == id && enemy.alive) {
                return enemy;
            }
        }
        for (Boss boss : state.aliveBosses) {
            if (boss != null && boss.id == id && boss.alive) {
                return boss;
            }
        }
        return null;
    }

    private static float effectValue(GameState state, String key) {
        Float value = state.permanentEffects.get(key);
        return value == null ? 0f : Math.max(0f, value);
    }

    private static void setVelocityToward(Projectile projectile, Enemy target) {
        float dx = target.x - projectile.x;
        float dy = target.y - projectile.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0f) {
            projectile.velocityX = 0f;
            projectile.velocityY = 0f;
            return;
        }
        projectile.velocityX = dx / length * PROJECTILE_SPEED;
        projectile.velocityY = dy / length * PROJECTILE_SPEED;
    }

    private record ImpactCounts(
        int hits, int criticalHits, float x, float y, int chainArcs, int stuns
    ) {
    }
}
