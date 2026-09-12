package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Lazily draws only the currently equipped, Hero-rig-aligned attachment sheets. */
public final class EquipmentSpriteRenderer implements AutoCloseable {
    private static final EquipmentSlot[] LAYER_ORDER = {
        EquipmentSlot.BOOTS,
        EquipmentSlot.ARMOR,
        EquipmentSlot.RING_1,
        EquipmentSlot.RING_2,
        EquipmentSlot.HELMET,
        EquipmentSlot.WEAPON
    };

    private final Map<String, TextureAtlas> loadedAtlases = new HashMap<>();
    private final RarityGlowRenderer rarityGlowRenderer = new RarityGlowRenderer();

    public void draw(SpriteBatch batch, GameState state, int frameIndex, float runTimeSeconds) {
        Set<String> activeIds = new HashSet<>();
        for (EquipmentSlot slot : LAYER_ORDER) {
            Item item = state.equippedItems.get(slot.name());
            if (item == null || item.id == null || item.id.isEmpty()) {
                continue;
            }
            activeIds.add(item.id);
            TextureAtlas atlas = loadedAtlases.get(item.id);
            if (atlas == null) {
                String atlasPath = item.visualKey == null || item.visualKey.isEmpty()
                    ? EquipmentVisualContract.atlasPath(item.id)
                    : item.visualKey;
                atlas = new TextureAtlas(Gdx.files.internal(atlasPath));
                loadedAtlases.put(item.id, atlas);
            }
            String regionName = EquipmentVisualContract.regionName(item, state.hero.animationState);
            Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(regionName);
            if (frames.size == 0) {
                throw new IllegalStateException("Missing equipment clip: " + regionName);
            }
            TextureAtlas.AtlasRegion frame = frames.get(
                Math.min(frames.size - 1, Math.max(0, frameIndex))
            );
            rarityGlowRenderer.draw(
                batch,
                frame,
                HeroSpriteRenderer.frameX(state.hero),
                HeroSpriteRenderer.frameY(state.hero),
                HeroSpriteRenderer.FRAME_SIZE,
                HeroSpriteRenderer.FRAME_SIZE,
                VisualRarity.fromTier(item.tier),
                runTimeSeconds
            );
        }
        disposeUnequipped(activeIds);
    }

    private void disposeUnequipped(Set<String> activeIds) {
        Iterator<Map.Entry<String, TextureAtlas>> iterator = loadedAtlases.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TextureAtlas> entry = iterator.next();
            if (!activeIds.contains(entry.getKey())) {
                entry.getValue().dispose();
                iterator.remove();
            }
        }
    }

    @Override
    public void close() {
        for (TextureAtlas atlas : loadedAtlases.values()) {
            atlas.dispose();
        }
        loadedAtlases.clear();
        rarityGlowRenderer.close();
    }
}
