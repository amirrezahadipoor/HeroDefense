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

    // X, Y, draw size, variant. Smaller upper props reinforce portrait depth.
    private static final float[][] CRYSTAL_PLACEMENTS = {
        {-8f, 120f, 172f, 0f},
        {556f, 205f, 164f, 1f},
        {4f, 820f, 148f, 2f},
        {568f, 884f, 136f, 0f}
    };

    private final Texture backdrop;
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
        backdrop = texture("generated/environment/arena_backdrop.png");
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
        batch.draw(
            backdrop, 0f, 0f, WorldLayout.REFERENCE_WIDTH, WorldLayout.REFERENCE_HEIGHT
        );
        drawGround(batch);
        drawCrystals(batch);
        drawWorldTree(batch, state, runTimeSeconds, presentationDeltaSeconds);
    }

    private void drawGround(SpriteBatch batch) {
        float originalColor = batch.getPackedColor();
        for (int row = 0; row < 8; row++) {
            float depth = row / 7f;
            float width = 252f - depth * 42f;
            float height = 184f - depth * 32f;
            float shade = 0.96f - depth * 0.23f;
            batch.setColor(shade * 0.92f, shade, shade * 0.95f, 0.96f);
            for (int column = 0; column < 4; column++) {
                int variant = (row * 2 + column) % ground.length;
                float x = -46f + column * 193f + (row % 2) * 31f;
                float y = 10f + row * 143f;
                batch.draw(ground[variant], x, y, width, height);
            }
        }
        batch.setPackedColor(originalColor);
    }

    private void drawCrystals(SpriteBatch batch) {
        for (float[] placement : CRYSTAL_PLACEMENTS) {
            float size = placement[2];
            int variant = Math.round(placement[3]);
            batch.draw(crystals[variant], placement[0], placement[1], size, size);
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
        backdrop.dispose();
        for (Texture texture : ground) texture.dispose();
        for (Texture texture : crystals) texture.dispose();
        healthyTreeAtlas.dispose();
        damagedTreeAtlas.dispose();
    }
}
