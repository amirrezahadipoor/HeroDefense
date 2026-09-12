package com.amirrezahadipoor.herodefense.model;

/** A touch-visible reward waiting to be collected into run state. */
public final class DropEntity extends ArenaEntity {
    public String dropType = "COIN";
    public String itemId = "";
    public int quantity = 1;
    public float pickupDelaySeconds;
    public DropCollectionStage collectionStage = DropCollectionStage.GROUND;
    public float homingElapsedSeconds;
    public boolean collectionEffectEmitted;

    public DropEntity() {
        super();
    }

    public DropEntity(long id, String dropType, float x, float y, int quantity) {
        super(id, x, y);
        this.dropType = dropType;
        this.quantity = quantity;
    }
}
