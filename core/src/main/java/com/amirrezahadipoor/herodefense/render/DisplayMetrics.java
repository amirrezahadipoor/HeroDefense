package com.amirrezahadipoor.herodefense.render;

import com.amirrezahadipoor.herodefense.WorldLayout;

/**
 * Pure-Java description of how the fixed 720-wide world maps onto a real phone panel.
 *
 * <p>The world width is always exactly the screen width. Taller-than-16:9 panels reveal extra
 * world height above and below the 1280-unit design area instead of black bars; that extra
 * height is reported so the HUD can anchor to the true screen edges and the arena can fill it.
 */
public final class DisplayMetrics {
    /** Never let the visible world shrink below the design height; 4:3 tablets letterbox the width instead. */
    public static final float MIN_WORLD_HEIGHT = WorldLayout.REFERENCE_HEIGHT;
    /** Hard cap on how much extra height we expose so 21:9 panels do not stretch the arena forever. */
    public static final float MAX_WORLD_HEIGHT = 1_720f;

    private final int screenWidth;
    private final int screenHeight;
    private final float density;

    public DisplayMetrics(int screenWidth, int screenHeight, float density) {
        this.screenWidth = Math.max(1, screenWidth);
        this.screenHeight = Math.max(1, screenHeight);
        this.density = Float.isFinite(density) && density > 0f ? density : 1f;
    }

    /** World units visible vertically once the width is pinned to 720. */
    public float worldHeight() {
        float natural = WorldLayout.REFERENCE_WIDTH * screenHeight / screenWidth;
        return Math.max(MIN_WORLD_HEIGHT, Math.min(MAX_WORLD_HEIGHT, natural));
    }

    /** Extra height beyond the 1280 design area, split evenly above and below. */
    public float verticalOverflow() {
        return worldHeight() - WorldLayout.REFERENCE_HEIGHT;
    }

    /** World y of the bottom screen edge (negative on tall phones). */
    public float bottomEdge() {
        return -verticalOverflow() * 0.5f;
    }

    /** World y of the top screen edge (above 1280 on tall phones). */
    public float topEdge() {
        return WorldLayout.REFERENCE_HEIGHT + verticalOverflow() * 0.5f;
    }

    /** Physical pixels per world unit; 1.5 on a 1080-wide panel. */
    public float pixelsPerWorldUnit() {
        return screenWidth / WorldLayout.REFERENCE_WIDTH;
    }

    /**
     * Converts a device-independent size (Android sp/dp) into pixels, then into world units.
     * Rendering text at this size keeps a 16sp label physically 16sp on every phone.
     */
    public float worldUnitsForDp(float dp) {
        return dp * density / pixelsPerWorldUnit();
    }

    /** Pixel size the glyph atlas must be rasterised at for a world-space size to stay crisp. */
    public int glyphPixelsForWorldUnits(float worldUnits) {
        return Math.max(8, Math.round(worldUnits * pixelsPerWorldUnit()));
    }

    public int screenWidth() {
        return screenWidth;
    }

    public int screenHeight() {
        return screenHeight;
    }

    public float density() {
        return density;
    }
}
