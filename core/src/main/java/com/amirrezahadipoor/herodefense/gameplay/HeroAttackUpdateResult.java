package com.amirrezahadipoor.herodefense.gameplay;

/** Combat events emitted by one Hero auto-attack update. */
public record HeroAttackUpdateResult(
    int shots,
    int hits,
    int criticalHits,
    float impactX,
    float impactY,
    int chainArcs,
    int stuns
) {
    public static final HeroAttackUpdateResult NONE =
        new HeroAttackUpdateResult(0, 0, 0, Float.NaN, Float.NaN, 0, 0);

    /** Legacy five-argument form kept for existing tests and call sites. */
    public HeroAttackUpdateResult(int shots, int hits, int criticalHits, float impactX, float impactY) {
        this(shots, hits, criticalHits, impactX, impactY, 0, 0);
    }

    public boolean hasImpact() {
        return hits > 0 && Float.isFinite(impactX) && Float.isFinite(impactY);
    }
}
