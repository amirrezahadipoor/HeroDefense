package com.amirrezahadipoor.herodefense.model;

/** Platform-independent base for anything positioned in the combat arena. */
public abstract class ArenaEntity {
    public long id;
    public float x;
    public float y;
    public boolean active = true;

    protected ArenaEntity() {
        // Required by libGDX Json.
    }

    protected ArenaEntity(long id, float x, float y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public float distanceSquaredTo(float targetX, float targetY) {
        float dx = targetX - x;
        float dy = targetY - y;
        return dx * dx + dy * dy;
    }
}
