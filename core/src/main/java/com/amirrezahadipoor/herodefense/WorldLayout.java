package com.amirrezahadipoor.herodefense;

/** Fixed portrait world-space contract used by rendering, touch hit tests, and assets. */
public final class WorldLayout {
    public static final float REFERENCE_WIDTH = 720f;
    public static final float REFERENCE_HEIGHT = 1280f;
    public static final float HERO_CENTER_X = REFERENCE_WIDTH * 0.5f;
    public static final float HERO_CENTER_Y = 600f;
    public static final float WORLD_TREE_X = HERO_CENTER_X;
    public static final float WORLD_TREE_Y = 755f;
    /** The second Heartwood planted at Wave 100 stands to the right of the first. */
    public static final float SECOND_TREE_X = 578f;
    public static final float SECOND_TREE_Y = 738f;

    private WorldLayout() {
    }
}
