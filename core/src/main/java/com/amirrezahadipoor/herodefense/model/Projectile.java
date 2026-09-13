package com.amirrezahadipoor.herodefense.model;

/** Optional visual/gameplay projectile emitted by Hero attacks or boss abilities. */
public final class Projectile extends ArenaEntity {
    public long sourceId;
    public long targetId;
    public float velocityX;
    public float velocityY;
    public float damage;
    public float remainingLifetimeSeconds;
    public boolean critical;
    /** Extra arrows from Multi Shot deal a reduced share and never chain again. */
    public boolean secondary;

    public Projectile() {
        super();
    }

    public Projectile(long id, long sourceId, long targetId, float x, float y) {
        super(id, x, y);
        this.sourceId = sourceId;
        this.targetId = targetId;
    }
}
