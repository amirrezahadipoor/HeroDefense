package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.amirrezahadipoor.herodefense.gameplay.PlantingCeremony;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the Hero from the reviewed planting-ceremony atlas (walk / plant / water clips rendered
 * on the combat rig with the seed pouch and watering can). Frame placement matches
 * {@link HeroSpriteRenderer} exactly so the hand-off to the combat idle never jumps.
 */
public final class CeremonyHeroRenderer implements AutoCloseable {
    static final String ATLAS_PATH = "generated/sprites/hero_ceremony.atlas";

    private final TextureAtlas atlas;
    private final Map<String, Array<TextureAtlas.AtlasRegion>> clips = new HashMap<>();

    public CeremonyHeroRenderer() {
        atlas = new TextureAtlas(Gdx.files.internal(ATLAS_PATH));
        register("walk", PlantingCeremony.WALK_FRAMES);
        register("plant", PlantingCeremony.PLANT_FRAMES);
        register("water", PlantingCeremony.WATER_FRAMES);
    }

    public void draw(SpriteBatch batch, PlantingCeremony ceremony) {
        Array<TextureAtlas.AtlasRegion> clip = clips.get(ceremony.heroClip());
        TextureAtlas.AtlasRegion frame = clip.get(
            Math.min(clip.size - 1, Math.max(0, ceremony.heroFrame()))
        );
        float size = HeroSpriteRenderer.FRAME_SIZE;
        float x = ceremony.heroX() - size * 0.5f;
        float y = ceremony.heroY() - HeroSpriteRenderer.FEET_OFFSET_FROM_FRAME_BOTTOM;
        if (ceremony.heroFacesRight()) {
            batch.draw(frame, x, y, size, size);
        } else {
            // Mirror horizontally for the walk back; the source clip faces the sapling.
            batch.draw(frame, x + size, y, -size, size);
        }
    }

    private void register(String clip, int expectedFrames) {
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions("hero_ceremony_" + clip);
        if (regions.size != expectedFrames) {
            atlas.dispose();
            throw new IllegalStateException(
                "Expected " + expectedFrames + " frames for hero_ceremony_" + clip
                    + ", found " + regions.size
            );
        }
        clips.put(clip, regions);
    }

    @Override
    public void close() {
        atlas.dispose();
    }
}
