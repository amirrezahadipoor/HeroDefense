package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/** Draws Rare/Legendary/Mythic aura edges at runtime without modifying source sprites. */
public final class RarityGlowRenderer implements AutoCloseable {
    private final ShaderProgram glowShader;

    public RarityGlowRenderer() {
        ShaderProgram.pedantic = true;
        glowShader = new ShaderProgram(
            Gdx.files.internal("shaders/rarity-glow.vert"),
            Gdx.files.internal("shaders/rarity-glow.frag")
        );
        if (!glowShader.isCompiled()) {
            throw new IllegalStateException("Rarity glow shader failed to compile: " + glowShader.getLog());
        }
    }

    /** Must be called while {@code batch} is between begin/end. */
    public void draw(
        SpriteBatch batch,
        TextureRegion region,
        float x,
        float y,
        float width,
        float height,
        VisualRarity rarity,
        float runTimeSeconds
    ) {
        if (!rarity.isGlowing()) {
            batch.draw(region, x, y, width, height);
            return;
        }

        Texture texture = region.getTexture();
        batch.setShader(glowShader);
        glowShader.setUniformf("u_texelSize", 1f / texture.getWidth(), 1f / texture.getHeight());
        glowShader.setUniformf("u_glowColor", rarity.red(), rarity.green(), rarity.blue(), 1f);
        glowShader.setUniformf("u_time", runTimeSeconds);
        glowShader.setUniformf("u_intensity", rarity.intensity());
        batch.draw(region, x, y, width, height);
        batch.flush();
        batch.setShader(null);
    }

    @Override
    public void close() {
        glowShader.dispose();
    }
}
