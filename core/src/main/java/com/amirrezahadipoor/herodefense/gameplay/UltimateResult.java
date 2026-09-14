package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Enemy;

import java.util.List;

/** Outcome of one Ultimate firing: damage applied plus the VFX fan anchors. */
public record UltimateResult(
    int foesHit,
    float damageEach,
    List<Enemy> arcTargets,
    float blastX,
    float blastY
) {
    public static final UltimateResult NONE =
        new UltimateResult(0, 0f, List.of(), 0f, 0f);

    public UltimateResult {
        arcTargets = arcTargets == null ? List.of() : List.copyOf(arcTargets);
    }

    public boolean fired() {
        return damageEach > 0f;
    }
}
