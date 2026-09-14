package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.ascension.RootNetworkCatalog;
import com.amirrezahadipoor.herodefense.ascension.RootNodeDefinition;

/** Touch mapping for Root Network nodes laid along the tree. */
public final class RootNetworkTouchLayout {
    public static final float CLOSE_X = 20f;
    public static final float CLOSE_Y = 1140f;
    public static final float CLOSE_W = 120f;
    public static final float CLOSE_H = 96f;
    public static final float NODE_RADIUS = 36f;

    private RootNetworkTouchLayout() {}

    public static boolean closeAt(float x, float y) {
        return x >= CLOSE_X && x <= CLOSE_X + CLOSE_W && y >= CLOSE_Y && y <= CLOSE_Y + CLOSE_H;
    }

    public static String nodeAt(float x, float y) {
        for (RootNodeDefinition def : RootNetworkCatalog.all()) {
            float dx = x - def.x();
            float dy = y - def.y();
            if (dx * dx + dy * dy <= NODE_RADIUS * NODE_RADIUS) {
                return def.id();
            }
        }
        return null;
    }
}
