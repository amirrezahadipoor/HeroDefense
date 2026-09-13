package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.HashMap;
import java.util.Map;

/** Lazily owns reviewed Blender-rendered UI icon textures. */
public final class UiIconRenderer implements AutoCloseable {
    private final Map<String, Texture> textures = new HashMap<>();

    public void draw(SpriteBatch batch, String key, float x, float y, float size) {
        draw(batch, key, x, y, size, UiFrameRenderer.State.NORMAL);
    }

    public void draw(
        SpriteBatch batch,
        String key,
        float x,
        float y,
        float size,
        UiFrameRenderer.State state
    ) {
        if (batch == null || key == null || key.isBlank()) return;
        float originalColor = batch.getPackedColor();
        switch (state) {
            case PRESSED -> batch.setColor(0.72f, 0.82f, 0.76f, 1f);
            case SELECTED -> batch.setColor(1f, 0.96f, 0.78f, 1f);
            case DISABLED -> batch.setColor(0.48f, 0.52f, 0.50f, 0.62f);
            case NORMAL -> batch.setColor(1f, 1f, 1f, 1f);
        }
        Texture texture = textures.computeIfAbsent(key, this::load);
        batch.draw(texture, x, y, size, size);
        batch.setPackedColor(originalColor);
    }

    private Texture load(String key) {
        Texture texture = new Texture(Gdx.files.internal("generated/icons/ui_" + key + ".png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    @Override
    public void close() {
        for (Texture texture : textures.values()) texture.dispose();
        textures.clear();
    }
}
