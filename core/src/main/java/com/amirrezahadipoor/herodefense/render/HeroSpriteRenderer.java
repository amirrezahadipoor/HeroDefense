package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.amirrezahadipoor.herodefense.gameplay.HeroAnimationController;
import com.amirrezahadipoor.herodefense.model.Hero;
import com.amirrezahadipoor.herodefense.model.HeroAnimationState;

import java.util.EnumMap;
import java.util.Map;

/** Draws the procedural Blender Hero atlas at its fixed gameplay anchor. */
public final class HeroSpriteRenderer implements AutoCloseable {
    private static final String ATLAS_PATH = "generated/sprites/hero.atlas";
    static final float FRAME_SIZE = 192f;
    private static final float FEET_OFFSET_FROM_FRAME_BOTTOM = 23f;

    private final TextureAtlas atlas;
    private final Map<HeroAnimationState, Array<TextureAtlas.AtlasRegion>> frames =
        new EnumMap<>(HeroAnimationState.class);

    public HeroSpriteRenderer() {
        atlas = new TextureAtlas(Gdx.files.internal(ATLAS_PATH));
        register(HeroAnimationState.IDLE, "hero_idle", HeroAnimationController.IDLE_FRAMES);
        register(HeroAnimationState.ATTACK, "hero_attack", HeroAnimationController.ATTACK_FRAMES);
        register(HeroAnimationState.HIT, "hero_hit", HeroAnimationController.HIT_FRAMES);
        register(HeroAnimationState.DEATH, "hero_death", HeroAnimationController.DEATH_FRAMES);
    }

    public void draw(SpriteBatch batch, Hero hero, int frameIndex) {
        Array<TextureAtlas.AtlasRegion> clip = frames.get(hero.animationState);
        TextureAtlas.AtlasRegion frame = clip.get(Math.min(clip.size - 1, Math.max(0, frameIndex)));
        batch.draw(
            frame,
            frameX(hero),
            frameY(hero),
            FRAME_SIZE,
            FRAME_SIZE
        );
    }

    static float frameX(Hero hero) {
        return hero.x - FRAME_SIZE * 0.5f;
    }

    static float frameY(Hero hero) {
        return hero.y - FEET_OFFSET_FROM_FRAME_BOTTOM;
    }

    private void register(HeroAnimationState state, String regionName, int expectedFrames) {
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(regionName);
        if (regions.size != expectedFrames) {
            atlas.dispose();
            throw new IllegalStateException(
                "Expected " + expectedFrames + " frames for " + regionName + ", found " + regions.size
            );
        }
        frames.put(state, regions);
    }

    @Override
    public void close() {
        atlas.dispose();
    }
}
