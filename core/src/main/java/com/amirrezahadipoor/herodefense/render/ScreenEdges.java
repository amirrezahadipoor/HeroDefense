package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.amirrezahadipoor.herodefense.WorldLayout;

/**
 * Process-wide snapshot of where the physical screen edges sit in world units. Renderers read
 * it to anchor bars and scrims to the real panel; touch layouts stay on the 1280 design grid.
 */
public final class ScreenEdges {
    private static float bottom = 0f;
    private static float top = WorldLayout.REFERENCE_HEIGHT;

    private ScreenEdges() {
    }

    public static void update(DisplayMetrics metrics) {
        bottom = metrics.bottomEdge();
        top = metrics.topEdge();
    }

    /** World y of the physical bottom edge; 0 on 16:9, negative on taller panels. */
    public static float bottom() {
        return bottom;
    }

    /** World y of the physical top edge; 1280 on 16:9, larger on taller panels. */
    public static float top() {
        return top;
    }

    public static float height() {
        return top - bottom;
    }

    /**
     * Draws a 9:16 backdrop so it covers the physical panel: scaled up uniformly around the
     * centre, never stretched, with the overflow cropped equally on the wider axis.
     */
    public static void drawCover(SpriteBatch batch, Texture texture) {
        float[] bounds = coverBounds(height());
        batch.draw(texture, bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    /** {x, y, width, height} for a 720x1280 image covering a 720-wide, visibleHeight-tall panel. */
    static float[] coverBounds(float visibleHeight) {
        float scale = Math.max(1f, visibleHeight / WorldLayout.REFERENCE_HEIGHT);
        float width = WorldLayout.REFERENCE_WIDTH * scale;
        float height = WorldLayout.REFERENCE_HEIGHT * scale;
        float x = (WorldLayout.REFERENCE_WIDTH - width) * 0.5f;
        float y = WorldLayout.REFERENCE_HEIGHT * 0.5f - height * 0.5f;
        return new float[] {x, y, width, height};
    }

    /** Resets to the 16:9 design area; used by unit tests. */
    static void reset() {
        bottom = 0f;
        top = WorldLayout.REFERENCE_HEIGHT;
    }
}
