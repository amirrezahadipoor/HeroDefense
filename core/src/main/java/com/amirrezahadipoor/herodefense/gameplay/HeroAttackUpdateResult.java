package com.amirrezahadipoor.herodefense.gameplay;

/** Combat events emitted by one Hero auto-attack update. */
public record HeroAttackUpdateResult(
    int shots,
    int hits,
    int criticalHits,
    float impactX,
    float impactY
) {
    public static final HeroAttackUpdateResult NONE =
        new HeroAttackUpdateResult(0, 0, 0, Float.NaN, Float.NaN);

    public boolean hasImpact() {
        return hits > 0 && Float.isFinite(impactX) && Float.isFinite(impactY);
    }
}
