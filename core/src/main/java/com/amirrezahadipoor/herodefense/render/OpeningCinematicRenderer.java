package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.gameplay.OpeningCinematic;
import com.amirrezahadipoor.herodefense.model.GameState;

/**
 * Screen-space layer for the new-run opening: a dark storm cloud rolling over the arena and the
 * Hero's spoken line in white. Drawn with the un-zoomed camera so text keeps its physical size.
 */
public final class OpeningCinematicRenderer implements AutoCloseable {
    static final Color SPEECH = Color.WHITE;
    /** Speech sits a little above the Hero's head in the zoomed framing. */
    static final float SPEECH_Y = GameState.ARENA_CENTER_Y + 250f;
    static final float SPEECH_SCALE = 1.6f;
    private static final int CLOUD_PUFFS = 9;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();

    public void draw(SpriteBatch batch, Matrix4 projection, OpeningCinematic opening) {
        float cloud = opening.cloudAlpha();
        if (cloud > 0.001f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapes.setProjectionMatrix(projection);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            // Whole-arena gloom first, then rolling puffs that drift in from the top-left.
            shapes.setColor(0.02f, 0.03f, 0.04f, cloud * 0.55f);
            shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
            float drift = opening.elapsedSeconds() * 26f;
            for (int index = 0; index < CLOUD_PUFFS; index++) {
                float px = -140f + index * 118f + drift * (0.6f + (index % 3) * 0.2f);
                px = ((px + 300f) % 1320f) - 300f;
                float py = ScreenEdges.top() - 120f - (index % 4) * 150f
                    + (float) Math.sin(opening.elapsedSeconds() * 0.9f + index) * 14f;
                float radius = 170f + (index % 3) * 40f;
                shapes.setColor(0.05f, 0.06f, 0.08f, cloud * 0.9f);
                shapes.ellipse(px - radius, py - radius * 0.42f, radius * 2f, radius * 0.84f, 48);
                shapes.setColor(0.10f, 0.11f, 0.14f, cloud * 0.45f);
                shapes.ellipse(px - radius * 0.7f, py - radius * 0.20f, radius * 1.4f, radius * 0.5f, 40);
            }
            shapes.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
        String line = opening.line();
        float alpha = opening.lineAlpha();
        if (line != null && alpha > 0.001f) {
            batch.setProjectionMatrix(projection);
            batch.begin();
            text.drawCentered(batch, line, 360f, SPEECH_Y, SPEECH_SCALE, SPEECH, alpha);
            batch.end();
        }
    }

    @Override
    public void close() {
        shapes.dispose();
        text.close();
    }
}
