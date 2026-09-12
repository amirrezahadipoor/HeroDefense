package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.IncomingHitResult;

/** Routes every positive enemy hit through one persisted Dodge roll. */
public final class HeroDamageSystem {
    public IncomingHitResult applyIncomingHit(GameState state, float damage) {
        if (state == null || state.hero == null || !state.hero.alive || damage <= 0f) {
            return IncomingHitResult.IGNORED;
        }
        float dodgeRoll = state.nextCombatRandomFloat();
        return state.hero.receiveIncomingHit(damage, dodgeRoll);
    }
}
