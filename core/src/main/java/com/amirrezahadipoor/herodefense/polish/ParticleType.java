package com.amirrezahadipoor.herodefense.polish;

/** Every reviewed particle family; rings expand while motes shrink over their lifetime. */
public enum ParticleType {
    HIT,
    IMPACT_CORE,
    CRITICAL_RING,
    CRITICAL_SPARK,
    DEATH,
    DEATH_RING,
    COIN,
    ITEM_PICKUP,
    COLLECTION_SPARKLE,
    BOSS_SHOCKWAVE,
    BOSS_DUST,
    TREE_LEAF,
    /** Jagged cyan beam from a struck foe to a chain-lightning victim; uses the segment fields. */
    CHAIN_BEAM,
    /** Bright flash at the end of a chain arc. */
    CHAIN_FLASH,
    /** Lilac star that orbits a stunned enemy's head. */
    STUN_SPARK,
    /** Falling water droplet from the ceremony watering can. */
    WATER_DROP;

    public boolean isRing() {
        return this == CRITICAL_RING || this == DEATH_RING || this == BOSS_SHOCKWAVE;
    }

    public boolean isBeam() {
        return this == CHAIN_BEAM;
    }
}
