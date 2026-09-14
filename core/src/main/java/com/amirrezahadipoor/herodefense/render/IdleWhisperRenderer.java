package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.model.GameState;
import java.util.List;

/**
 * Shared single-line story overlay: one centered shadowed line with a fade in/out envelope
 * and no arena veil, so beats never darken the scene beneath them. First used by the §8
 * idle whisper; wave title cards, reflection lines, and ceremony beats reuse it. A tap
 * dismisses it; otherwise it fades out on its own.
 */
public final class IdleWhisperRenderer implements AutoCloseable {
    public static final float SHOW_SECONDS = 4.0f;
    private static final float FADE_IN_SECONDS = 0.45f;
    private static final float FADE_OUT_SECONDS = 0.7f;
    static final float LINE_Y = GameState.ARENA_CENTER_Y + 250f;
    static final float LINE_SCALE = 1.6f;
    static final float LINE_STRIDE = 46f;
    static final float MAX_LINE_WIDTH = 640f;

    private final OverlayText text = new OverlayText();

    public void draw(SpriteBatch batch, Matrix4 projection, String line, float elapsedSeconds) {
        draw(batch, projection, line, alphaFor(elapsedSeconds), false);
    }

    /** Same lines treatment with an explicit alpha; treeVoice tints leaf-green. */
    public void draw(
        SpriteBatch batch, Matrix4 projection, String line, float alpha, boolean treeVoice
    ) {
        if (line == null) {
            return;
        }
        if (alpha <= 0.001f) {
            return;
        }
        List<String> rows = CodexOverlayRenderer.wrapLines(line, this::lineWidth, MAX_LINE_WIDTH);
        batch.setProjectionMatrix(projection);
        batch.begin();
        Color voice = treeVoice ? OverlayText.POSITIVE : Color.WHITE;
        float y = LINE_Y;
        for (String row : rows) {
            text.drawCentered(batch, row, 360f, y, LINE_SCALE, voice, alpha);
            y -= LINE_STRIDE;
        }
        batch.end();
    }

    private double lineWidth(String row) {
        return text.width(row, LINE_SCALE);
    }

    /** 0..1 envelope: quick fade in, hold, then fade out over the last fraction of a second. */
    static float alphaFor(float elapsedSeconds) {
        float in = elapsedSeconds / FADE_IN_SECONDS;
        float out = (SHOW_SECONDS - elapsedSeconds) / FADE_OUT_SECONDS;
        return Math.max(0f, Math.min(1f, Math.min(in, out)));
    }

    @Override
    public void close() {
        text.close();
    }
}
