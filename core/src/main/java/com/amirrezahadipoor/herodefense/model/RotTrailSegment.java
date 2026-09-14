package com.amirrezahadipoor.herodefense.model;

/** One fading patch of weeping-rot ground; heroes standing in it take damage over time. */
public final class RotTrailSegment extends ArenaEntity {
    public float remainingSeconds;
    public float damagePerSecond;
    public long sourceId;

    public RotTrailSegment() {
        super();
    }

    public RotTrailSegment(
        long id, float x, float y, float remainingSeconds, float damagePerSecond, long sourceId
    ) {
        super(id, x, y);
        this.remainingSeconds = remainingSeconds;
        this.damagePerSecond = damagePerSecond;
        this.sourceId = sourceId;
    }
}
