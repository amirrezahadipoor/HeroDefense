package com.amirrezahadipoor.herodefense.gameplay;

/** Aggregate rewards granted once for all defeats resolved in one simulation update. */
public final class KillRewardResult {
    public static final KillRewardResult NONE = new KillRewardResult(0, 0, 0, 0);

    private final int kills;
    private final int coins;
    private final int experience;
    private final int levelsGained;

    public KillRewardResult(int kills, int coins, int experience, int levelsGained) {
        this.kills = kills;
        this.coins = coins;
        this.experience = experience;
        this.levelsGained = levelsGained;
    }

    public int kills() {
        return kills;
    }

    public int coins() {
        return coins;
    }

    public int experience() {
        return experience;
    }

    public int levelsGained() {
        return levelsGained;
    }
}
