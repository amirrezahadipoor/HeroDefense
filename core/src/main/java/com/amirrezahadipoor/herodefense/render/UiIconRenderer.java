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
        if (batch == null || key == null || key.isBlank()) return;
        Texture texture = textures.computeIfAbsent(key, this::load);
        batch.draw(texture, x, y, size, size);
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
