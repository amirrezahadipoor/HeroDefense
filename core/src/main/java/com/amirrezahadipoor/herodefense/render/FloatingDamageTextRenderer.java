package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.polish.FloatingDamageText;
import com.amirrezahadipoor.herodefense.polish.FloatingDamageTextSystem;

/**
 * Damage numbers in the premium palette: ivory normal hits, oversized gold criticals,
 * cyan chain arcs, and a lilac STUN tag, each with a dark drop shadow for arena contrast.
 */
public final class FloatingDamageTextRenderer implements AutoCloseable {
    private final GlyphLayout layout = new GlyphLayout();

    public void draw(SpriteBatch batch, Matrix4 projection, FloatingDamageTextSystem system) {
        if (system.labels().isEmpty()) return;
        BitmapFont font = GameFonts.shared().font(GameFonts.Role.HEADING);
        float originalScaleX = font.getData().scaleX;
        float originalScaleY = font.getData().scaleY;
        batch.setProjectionMatrix(projection);
        batch.begin();
        for (FloatingDamageText label : system.labels()) {
            float scale = label.scale();
            font.getData().setScale(originalScaleX * scale, originalScaleY * scale);
            layout.setText(font, label.text);
            float x = label.x() - layout.width * 0.5f;
            float y = label.y();
            float alpha = alpha(label);
            font.setColor(0.04f, 0.075f, 0.09f, alpha * 0.9f);
            font.draw(batch, label.text, x + 2f, y - 2.5f);
            setColor(font, label.style, alpha);
            font.draw(batch, label.text, x, y);
        }
        font.getData().setScale(originalScaleX, originalScaleY);
        font.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    /** Fully opaque for the first 60% of life, then a linear fade. */
    static float alpha(FloatingDamageText label) {
        float life = label.lifeRatio();
        return life > 0.4f ? 1f : Math.max(0f, life / 0.4f);
    }

    private static void setColor(BitmapFont font, FloatingDamageText.Style style, float alpha) {
        switch (style) {
            case NORMAL -> font.setColor(0.953f, 0.894f, 0.737f, alpha);
            case SECONDARY -> font.setColor(0.682f, 0.737f, 0.682f, alpha);
            case CRITICAL -> font.setColor(0.918f, 0.776f, 0.427f, alpha);
            case CHAIN -> font.setColor(0.561f, 0.831f, 0.949f, alpha);
            case STUN -> font.setColor(0.792f, 0.686f, 0.945f, alpha);
        }
    }

    @Override
    public void close() {
        // Shared font owned by GameFonts.
    }
}
