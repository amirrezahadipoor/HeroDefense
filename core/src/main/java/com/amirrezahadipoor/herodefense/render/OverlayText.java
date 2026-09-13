package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Shared premium-v2 typography: density-true Nunito glyphs, warm parchment fills, and a fixed
 * forest shadow. Legacy call sites still pass a scale; it is mapped onto a typographic role so
 * every label is physically at least 11sp on the device instead of 5sp of blurred bitmap font.
 */
final class OverlayText implements AutoCloseable {
    static final Color GOLD = Color.valueOf("EAC66D");
    static final Color IVORY = Color.valueOf("F7EBCB");
    static final Color SUBTLE = Color.valueOf("C6D2C4");
    static final Color POSITIVE = Color.valueOf("7ED898");
    static final Color NEGATIVE = Color.valueOf("EA7F79");
    static final Color MUTED = Color.valueOf("97A096");

    private static final float SHADOW_OFFSET_X = 1.5f;
    private static final float SHADOW_OFFSET_Y = -2.5f;
    private static final float SHADOW_ALPHA = 0.85f;

    private final GlyphLayout layout = new GlyphLayout();

    OverlayText() {
    }

    void draw(SpriteBatch batch, String text, float x, float y, float scale, Color color) {
        draw(batch, text, x, y, scale, color, 1f);
    }

    void draw(
        SpriteBatch batch, String text, float x, float y, float scale, Color color, float alpha
    ) {
        draw(batch, text, x, y, GameFonts.Role.forLegacyScale(scale), color, alpha);
    }

    void draw(
        SpriteBatch batch,
        String text,
        float x,
        float y,
        GameFonts.Role role,
        Color color,
        float alpha
    ) {
        if (text == null || text.isEmpty()) return;
        float resolvedAlpha = Math.max(0f, Math.min(1f, color.a * alpha));
        BitmapFont font = GameFonts.shared().font(role);
        font.setColor(0.003f, 0.010f, 0.009f, resolvedAlpha * SHADOW_ALPHA);
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
        GameFonts.Role role = GameFonts.Role.forLegacyScale(scale);
        draw(batch, text, centerX - width(text, role) * 0.5f, y, role, color, alpha);
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
        GameFonts.Role role = GameFonts.Role.forLegacyScale(scale);
        draw(batch, text, rightX - width(text, role), y, role, color, alpha);
    }

    float width(String text, float scale) {
        return width(text, GameFonts.Role.forLegacyScale(scale));
    }

    float width(String text, GameFonts.Role role) {
        if (text == null || text.isEmpty()) return 0f;
        layout.setText(GameFonts.shared().font(role), text);
        return layout.width;
    }

    /** Cap-to-baseline height of the role, used by callers that stack lines. */
    float lineHeight(GameFonts.Role role) {
        return GameFonts.shared().font(role).getLineHeight();
    }

    @Override
    public void close() {
        // Fonts are owned by GameFonts.shared(); nothing to release per renderer.
    }
}
