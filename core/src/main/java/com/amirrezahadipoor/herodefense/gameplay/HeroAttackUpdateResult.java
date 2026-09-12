package com.amirrezahadipoor.herodefense.gameplay;

/** Combat events emitted by one Hero auto-attack update. */
public record HeroAttackUpdateResult(int shots, int hits, int criticalHits) {
    public static final HeroAttackUpdateResult NONE = new HeroAttackUpdateResult(0, 0, 0);
}
