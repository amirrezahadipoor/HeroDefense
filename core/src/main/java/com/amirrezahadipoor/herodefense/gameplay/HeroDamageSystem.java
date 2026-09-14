package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.items.MythicEffects;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.IncomingHitResult;
import com.amirrezahadipoor.herodefense.trials.TrialEffects;

/** Routes every positive enemy hit through one persisted Dodge roll. */
public final class HeroDamageSystem {
    private final HeroStatCalculator statCalculator;

    public HeroDamageSystem() {
        this(new HeroStatCalculator());
    }

    public HeroDamageSystem(HeroStatCalculator statCalculator) {
        this.statCalculator = statCalculator;
    }

    public IncomingHitResult applyIncomingHit(GameState state, float damage) {
        if (state == null || state.hero == null || !state.hero.alive || damage <= 0f) {
            return IncomingHitResult.IGNORED;
        }
        return applyIncomingHitWithRoll(state, damage, state.nextCombatRandomFloat());
    }

    /**
     * Applies a hit with a pre-rolled dodge die (telegraphed boss specials roll at
     * trigger time so the combat stream never shifts, then land at detonation).
     */
    public IncomingHitResult applyIncomingHitWithRoll(GameState state, float damage, float dodgeRoll) {
        if (state == null || state.hero == null || !state.hero.alive || damage <= 0f) {
            return IncomingHitResult.IGNORED;
        }
        IncomingHitResult result = state.hero.receiveIncomingHit(
            damage * TrialEffects.damageTakenMultiplier(state.activeTrials),
            dodgeRoll,
            statCalculator.dodgeChance(state)
        );
        if (result == IncomingHitResult.DAMAGED || result == IncomingHitResult.KILLED) {
            MythicEffects.onLandedHitTaken(state);
        }
        return result;
    }
}
