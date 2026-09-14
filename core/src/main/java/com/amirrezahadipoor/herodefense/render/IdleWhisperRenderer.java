package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.model.GameState;
import java.util.List;

/**
 * Shared single-line story overlay: a dim veil plus one centered white line with a fade
 * in/out envelope. First used by the §8 idle whisper; wave title cards and reflection lines
 * reuse it in Phase 21.3. A tap dismisses it; otherwise it fades out on its own.
 */
public final class IdleWhisperRenderer implements AutoCloseable {
    public static final float SHOW_SECONDS = 4.0f;
    private static final float FADE_IN_SECONDS = 0.45f;
    private static final float FADE_OUT_SECONDS = 0.7f;
    private static final float VEIL_ALPHA = 0.45f;
    static final float LINE_Y = GameState.ARENA_CENTER_Y + 250f;
    static final float LINE_SCALE = 1.6f;
    static final float LINE_STRIDE = 46f;
    static final float MAX_LINE_WIDTH = 640f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();

    public void draw(SpriteBatch batch, Matrix4 projection, String line, float elapsedSeconds) {
        if (line == null) {
            return;
        }
        float alpha = alphaFor(elapsedSeconds);
        if (alpha <= 0.001f) {
            return;
        }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.03f, 0.04f, VEIL_ALPHA * alpha);
        shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
        shapes.end();
        List<String> rows = CodexOverlayRenderer.wrapLines(line, this::lineWidth, MAX_LINE_WIDTH);
        batch.setProjectionMatrix(projection);
        batch.begin();
        float y = LINE_Y;
        for (String row : rows) {
            text.drawCentered(batch, row, 360f, y, LINE_SCALE, Color.WHITE, alpha);
            y -= LINE_STRIDE;
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
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
        shapes.dispose();
        text.close();
    }
}
