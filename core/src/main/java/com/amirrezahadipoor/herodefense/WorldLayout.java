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
    /** Grove site for the Wave-50 planting (left flank, reuses sapling atlas). */
    public static final float GROVE_TREE_50_X = 142f;
    public static final float GROVE_TREE_50_Y = 738f;
    /** Grove site for the Wave-150 planting (mid-right, reuses sapling atlas). */
    public static final float GROVE_TREE_150_X = 360f + 118f;
    public static final float GROVE_TREE_150_Y = 738f;

    /** Returns the world position for a planted grove tree by its planting order (0 = 50, 1 = 100, 2 = 150). */
    public static float groveTreeX(int index) {
        return switch (index) {
            case 0 -> GROVE_TREE_50_X;
            case 1 -> SECOND_TREE_X;
            case 2 -> GROVE_TREE_150_X;
            default -> SECOND_TREE_X;
        };
    }

    public static float groveTreeY(int index) {
        return switch (index) {
            case 0 -> GROVE_TREE_50_Y;
            case 1 -> SECOND_TREE_Y;
            case 2 -> GROVE_TREE_150_Y;
            default -> SECOND_TREE_Y;
        };
    }

    private WorldLayout() {
    }
}
