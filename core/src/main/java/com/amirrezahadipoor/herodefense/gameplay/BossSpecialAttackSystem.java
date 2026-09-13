package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.BossType;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Executes four mechanically distinct boss specials, all through Dodge-aware damage. */
public final class BossSpecialAttackSystem {
    private final HeroDamageSystem heroDamageSystem;

    public BossSpecialAttackSystem(HeroDamageSystem heroDamageSystem) {
        this.heroDamageSystem = heroDamageSystem;
    }

    public void update(GameState state, float deltaSeconds) {
        if (state == null || state.hero == null || !state.hero.alive || deltaSeconds < 0f) {
            return;
        }
        for (Boss boss : state.aliveBosses) {
            if (boss == null || !boss.alive || !boss.active || !state.hero.alive) {
                continue;
            }
            boss.specialCooldownSeconds -= deltaSeconds;
            float triggerRange = triggerRange(boss.bossDefinition());
            if (boss.specialCooldownSeconds <= 0f
                && !boss.stunned()
                && boss.distanceSquaredTo(state.hero.x, state.hero.y) <= triggerRange * triggerRange) {
                execute(state, boss);
                boss.specialCooldownSeconds += cooldown(boss.bossDefinition());
                boss.specialUseCount++;
                boss.specialAnimationSeconds = 0.5f;
            } else {
                boss.specialAnimationSeconds = Math.max(0f, boss.specialAnimationSeconds - deltaSeconds);
            }
        }
    }

    private void execute(GameState state, Boss boss) {
        switch (boss.bossDefinition()) {
            case ANCIENT_GOLEM -> heroDamageSystem.applyIncomingHit(state, boss.damage * 1.6f);
            case THORN_MATRIARCH -> {
                heroDamageSystem.applyIncomingHit(state, boss.damage * 0.5f);
                state.hero.attackCooldownSeconds = Math.max(state.hero.attackCooldownSeconds, 2f);
            }
            case EMBER_WYRM -> {
                heroDamageSystem.applyIncomingHit(state, boss.damage * 0.55f);
                heroDamageSystem.applyIncomingHit(state, boss.damage * 0.55f);
            }
            case VOID_KNIGHT -> {
                chargeToMeleeRange(state, boss);
                heroDamageSystem.applyIncomingHit(state, boss.damage * 1.25f);
            }
        }
    }

    private static void chargeToMeleeRange(GameState state, Boss boss) {
        float dx = boss.x - state.hero.x;
        float dy = boss.y - state.hero.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0f) {
            dx = 1f;
            dy = 0f;
            length = 1f;
        }
        boss.x = state.hero.x + dx / length * boss.attackRange;
        boss.y = state.hero.y + dy / length * boss.attackRange;
    }

    private static float triggerRange(BossType type) {
        return switch (type) {
            case ANCIENT_GOLEM -> 145f;
            case THORN_MATRIARCH -> 210f;
            case EMBER_WYRM -> 250f;
            case VOID_KNIGHT -> 480f;
        };
    }

    private static float cooldown(BossType type) {
        return switch (type) {
            case ANCIENT_GOLEM -> 5.5f;
            case THORN_MATRIARCH -> 5f;
            case EMBER_WYRM -> 4.5f;
            case VOID_KNIGHT -> 4f;
        };
    }
}
