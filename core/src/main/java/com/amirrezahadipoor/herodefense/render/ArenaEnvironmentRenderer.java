package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.amirrezahadipoor.herodefense.WorldLayout;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Draws the reviewed Blender-rendered forest floor, props, and defended World Tree. */
public final class ArenaEnvironmentRenderer implements AutoCloseable {
    private static final float TREE_SIZE = 330f;
    private static final float TREE_FEET_OFFSET = 31f;

    private static final float[][] CRYSTAL_PLACEMENTS = {
        {18f, 150f, 0f},
        {574f, 210f, 1f},
        {24f, 805f, 2f},
        {570f, 850f, 0f}
    };

    private final Texture[] ground = new Texture[3];
    private final Texture[] crystals = new Texture[3];
    private final TextureAtlas healthyTreeAtlas;
    private final TextureAtlas damagedTreeAtlas;
    private final Array<TextureAtlas.AtlasRegion> healthyTreeFrames;
    private final Array<TextureAtlas.AtlasRegion> damagedTreeFrames;
    private final Array<TextureAtlas.AtlasRegion> destroyedTreeFrames;
    private final WorldTreeAnimationController treeAnimation =
        new WorldTreeAnimationController();

    public ArenaEnvironmentRenderer() {
        for (int index = 0; index < 3; index++) {
            ground[index] = texture("generated/environment/ground_tile_" + index + ".png");
            crystals[index] = texture("generated/environment/crystal_prop_" + index + ".png");
        }
        healthyTreeAtlas = new TextureAtlas(
            Gdx.files.internal("generated/sprites/world_tree_healthy.atlas")
        );
        damagedTreeAtlas = new TextureAtlas(
            Gdx.files.internal("generated/sprites/world_tree_damaged.atlas")
        );
        healthyTreeFrames = requireFrames(
            healthyTreeAtlas,
            "world_tree_healthy_idle",
            WorldTreeAnimationController.IDLE_FRAME_COUNT
        );
        damagedTreeFrames = requireFrames(
            damagedTreeAtlas,
            "world_tree_damaged_idle",
            WorldTreeAnimationController.IDLE_FRAME_COUNT
        );
        destroyedTreeFrames = requireFrames(
            damagedTreeAtlas,
            "world_tree_damaged_destroy",
            WorldTreeAnimationController.DESTROY_FRAME_COUNT
        );
    }

    public void draw(
        SpriteBatch batch,
        GameState state,
        float runTimeSeconds,
        float presentationDeltaSeconds
    ) {
        drawGround(batch);
        drawCrystals(batch);
        drawWorldTree(batch, state, runTimeSeconds, presentationDeltaSeconds);
    }

    private void drawGround(SpriteBatch batch) {
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 4; column++) {
                int variant = (row * 2 + column) % ground.length;
                float x = -32f + column * 196f + (row % 2) * 34f;
                float y = 35f + row * 172f;
                batch.draw(ground[variant], x, y, 224f, 164f);
            }
        }
    }

    private void drawCrystals(SpriteBatch batch) {
        for (float[] placement : CRYSTAL_PLACEMENTS) {
            int variant = Math.round(placement[2]);
            batch.draw(crystals[variant], placement[0], placement[1], 128f, 128f);
        }
    }

    private void drawWorldTree(
        SpriteBatch batch,
        GameState state,
        float runTimeSeconds,
        float presentationDeltaSeconds
    ) {
        WorldTreeAnimationController.Selection selection = treeAnimation.select(
            state, runTimeSeconds, presentationDeltaSeconds
        );
        Array<TextureAtlas.AtlasRegion> frames = switch (selection.state()) {
            case HEALTHY -> healthyTreeFrames;
            case DAMAGED -> damagedTreeFrames;
            case DESTROYING, DESTROYED -> destroyedTreeFrames;
        };
        batch.draw(
            frames.get(selection.frameIndex()),
            WorldLayout.WORLD_TREE_X - TREE_SIZE * 0.5f,
            WorldLayout.WORLD_TREE_Y - TREE_FEET_OFFSET,
            TREE_SIZE,
            TREE_SIZE
        );
    }

    private static Texture texture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private static Array<TextureAtlas.AtlasRegion> requireFrames(
        TextureAtlas atlas,
        String region,
        int expected
    ) {
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
        for (Texture texture : ground) texture.dispose();
        for (Texture texture : crystals) texture.dispose();
        healthyTreeAtlas.dispose();
        damagedTreeAtlas.dispose();
    }
}
