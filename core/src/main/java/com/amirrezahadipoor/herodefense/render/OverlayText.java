package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Shared premium-v2 overlay typography: warm parchment text with a fixed forest shadow. */
final class OverlayText implements AutoCloseable {
    static final Color GOLD = Color.valueOf("EAC66D");
    static final Color IVORY = Color.valueOf("F3E4BC");
    static final Color SUBTLE = Color.valueOf("AEBCAE");
    static final Color POSITIVE = Color.valueOf("69C884");
    static final Color NEGATIVE = Color.valueOf("DF6A65");
    static final Color MUTED = Color.valueOf("777D76");

    private static final float SHADOW_OFFSET_X = 1.5f;
    private static final float SHADOW_OFFSET_Y = -2f;

    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    OverlayText() {
        font.getRegion().getTexture().setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );
    }

    void draw(SpriteBatch batch, String text, float x, float y, float scale, Color color) {
        draw(batch, text, x, y, scale, color, 1f);
    }

    void draw(
        SpriteBatch batch, String text, float x, float y, float scale, Color color, float alpha
    ) {
        if (text == null || text.isEmpty()) return;
        float resolvedAlpha = Math.max(0f, Math.min(1f, color.a * alpha));
        font.getData().setScale(scale);
        font.setColor(0.003f, 0.010f, 0.009f, resolvedAlpha);
        font.draw(batch, text, x + SHADOW_OFFSET_X, y + SHADOW_OFFSET_Y);
        font.setColor(color.r, color.g, color.b, resolvedAlpha);
        font.draw(batch, text, x, y);
    }

    void drawCentered(
        SpriteBatch batch, String text, float centerX, float y, float scale, Color color
    ) {
        drawCentered(batch, text, centerX, y, scale, color, 1f);
    }

    void drawCentered(
        SpriteBatch batch,
        String text,
        float centerX,
        float y,
        float scale,
        Color color,
        float alpha
    ) {
        draw(batch, text, centerX - width(text, scale) * 0.5f, y, scale, color, alpha);
    }

    void drawRightAligned(
        SpriteBatch batch, String text, float rightX, float y, float scale, Color color
    ) {
        drawRightAligned(batch, text, rightX, y, scale, color, 1f);
    }

    void drawRightAligned(
        SpriteBatch batch,
        String text,
        float rightX,
        float y,
        float scale,
        Color color,
        float alpha
    ) {
        draw(batch, text, rightX - width(text, scale), y, scale, color, alpha);
    }

    float width(String text, float scale) {
        if (text == null || text.isEmpty()) return 0f;
        font.getData().setScale(scale);
        layout.setText(font, text);
        return layout.width;
    }

    @Override
    public void close() {
        font.dispose();
    }
}
