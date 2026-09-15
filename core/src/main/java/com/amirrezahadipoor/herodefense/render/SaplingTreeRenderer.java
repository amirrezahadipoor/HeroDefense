package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.amirrezahadipoor.herodefense.WorldLayout;
import com.amirrezahadipoor.herodefense.gameplay.PlantingCeremony;

/**
 * Grove saplings planted at waves 50/100/150. Each site reuses the same sapling atlas
 * (grow + idle) anchored at its WorldLayout grove position with aura scaling.
 */
public final class SaplingTreeRenderer implements AutoCloseable {
    static final String ATLAS_PATH = "generated/sprites/world_tree_sapling.atlas";
    public static final int IDLE_FRAMES = 6;
    static final float IDLE_FRAME_RATE = 10f;
    /** Rendered in the tree frame class (256 px) but drawn smaller than the 330 px Heartwood. */
    static final float DRAW_SIZE = 258f;
    static final float FEET_OFFSET = 24f;

    private final TextureAtlas atlas;
    private final Array<TextureAtlas.AtlasRegion> growFrames;
    private final Array<TextureAtlas.AtlasRegion> idleFrames;

    public SaplingTreeRenderer() {
        atlas = new TextureAtlas(Gdx.files.internal(ATLAS_PATH));
        growFrames = require("world_tree_sapling_grow", PlantingCeremony.GROW_FRAMES);
        idleFrames = require("world_tree_sapling_idle", IDLE_FRAMES);
    }

    /** Growth frame during the ceremony (call only while {@code ceremony.saplingVisible()}). */
    public void drawGrowing(SpriteBatch batch, PlantingCeremony ceremony) {
        int frame = Math.min(growFrames.size - 1, Math.max(0, ceremony.saplingGrowFrame()));
        drawFrameAt(batch, growFrames.get(frame), ceremony.treeX(), ceremony.treeY());
    }

    /** Fully grown idle sway once the run has moved past the ceremony. */
    public void drawIdle(SpriteBatch batch, float loopTimeSeconds) {
        drawFrameAt(batch, idleFrames.get(idleFrame(loopTimeSeconds)), WorldLayout.SECOND_TREE_X, WorldLayout.SECOND_TREE_Y);
    }

    /** Idle sway at a specific world position (used for grove sites). */
    public void drawIdleAt(SpriteBatch batch, float loopTimeSeconds, float x, float y) {
        drawAura(batch, x, y, loopTimeSeconds);
        drawFrameAt(batch, idleFrames.get(idleFrame(loopTimeSeconds)), x, y);
    }

    /** Draws all already-planted grove trees idle at their anchored sites. */
    public void drawGroveIdle(SpriteBatch batch, com.amirrezahadipoor.herodefense.model.GameState state, float loopTimeSeconds) {
        for (int i = 0; i < state.plantedTreesCount; i++) {
            float x = WorldLayout.groveTreeX(i);
            float y = WorldLayout.groveTreeY(i);
            drawIdleAt(batch, loopTimeSeconds, x, y);
        }
    }

    static int idleFrame(float loopTimeSeconds) {
        return Math.floorMod((int) (loopTimeSeconds * IDLE_FRAME_RATE), IDLE_FRAMES);
    }

    private static void drawFrame(SpriteBatch batch, TextureAtlas.AtlasRegion region) {
        drawFrameAt(batch, region, WorldLayout.SECOND_TREE_X, WorldLayout.SECOND_TREE_Y);
    }

    private static void drawFrameAt(SpriteBatch batch, TextureAtlas.AtlasRegion region, float x, float y) {
        batch.draw(
            region,
            x - DRAW_SIZE * 0.5f,
            y - FEET_OFFSET,
            DRAW_SIZE,
            DRAW_SIZE
        );
    }

    private void drawAura(SpriteBatch batch, float x, float y, float loopTimeSeconds) {
        float pulse = 0.85f + 0.15f * (float) Math.sin(loopTimeSeconds * 2.1f + x * 0.01f);
        float alpha = 0.14f * pulse;
        float prev = batch.getPackedColor();
        batch.setColor(0.55f, 0.95f, 0.65f, alpha);
        float auraSize = DRAW_SIZE * 0.95f;
        // soft aura behind the trunk reusing the idle frame at low alpha
        TextureAtlas.AtlasRegion auraFrame = idleFrames.get(0);
        batch.draw(auraFrame, x - auraSize * 0.5f, y - FEET_OFFSET + 6f, auraSize, auraSize);
        batch.setPackedColor(prev);
    }

    private Array<TextureAtlas.AtlasRegion> require(String region, int expected) {
        Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(region);
        if (frames.size != expected) {
            atlas.dispose();
            throw new IllegalStateException(
                "Expected " + expected + " frames for " + region + ", found " + frames.size
            );
        }
        return frames;
    }

    @Override
    public void close() {
        atlas.dispose();
    }
}
