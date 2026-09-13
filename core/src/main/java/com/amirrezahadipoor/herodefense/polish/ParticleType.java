package com.amirrezahadipoor.herodefense.polish;

/** Every reviewed particle family; rings expand while motes shrink over their lifetime. */
public enum ParticleType {
    HIT,
    IMPACT_CORE,
    CRITICAL_RING,
    DEATH,
    DEATH_RING,
    COIN,
    ITEM_PICKUP,
    COLLECTION_SPARKLE,
    BOSS_SHOCKWAVE,
    BOSS_DUST,
    TREE_LEAF;

    public boolean isRing() {
        return this == CRITICAL_RING || this == DEATH_RING || this == BOSS_SHOCKWAVE;
    }
}
