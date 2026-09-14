package com.amirrezahadipoor.herodefense.potions;

import com.amirrezahadipoor.herodefense.items.MythicEffects;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Automatically consumes the weakest available potion below a configurable HP ratio. */
public final class AutoPotionSystem {
    public static final float DEFAULT_THRESHOLD = 0.35f;

    private final HealthPotionSystem potionSystem;
    private final float threshold;

    public AutoPotionSystem(HealthPotionSystem potionSystem) {
        this(potionSystem, DEFAULT_THRESHOLD);
    }

    public AutoPotionSystem(HealthPotionSystem potionSystem, float threshold) {
        if (threshold <= 0f || threshold >= 1f || Float.isNaN(threshold)) {
            throw new IllegalArgumentException("Potion threshold must be between zero and one");
        }
        this.potionSystem = potionSystem;
        this.threshold = threshold;
    }

    public float threshold() {
        return threshold;
    }

    public PotionTier update(GameState state) {
        if (state == null || state.hero == null || !state.hero.alive
            || state.hero.health / state.hero.maxHealth >= threshold) {
            return null;
        }
        for (PotionTier tier : PotionTier.values()) {
            if (state.healthPotions.get(tier.inventoryIndex()) > 0 && potionSystem.use(state, tier)) {
                if (MythicEffects.hasVerdantOath(state)) {
                    state.hero.mythicLifestealRemainingSeconds =
                        MythicEffects.VERDANT_LIFESTEAL_SECONDS;
                }
                return tier;
            }
        }
        return null;
    }
}
