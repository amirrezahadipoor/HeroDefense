package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.polish.TouchFeedbackSystem;
import com.amirrezahadipoor.herodefense.polish.TouchPulse;

/** Expanding translucent tap/card confirmation circles drawn above every screen. */
public final class TouchFeedbackRenderer implements AutoCloseable {
    private final ShapeRenderer shapes = new ShapeRenderer();

    public void draw(Matrix4 projection, TouchFeedbackSystem feedback) {
        if (feedback.pulses().isEmpty()) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (TouchPulse pulse : feedback.pulses()) {
            float progress = pulse.progress();
            float radius = (pulse.kind == TouchPulse.Kind.CARD_SELECTION ? 36f : 22f)
                + progress * (pulse.kind == TouchPulse.Kind.CARD_SELECTION ? 64f : 34f);
            float alpha = (1f - progress) * 0.42f;
            if (pulse.kind == TouchPulse.Kind.CARD_SELECTION) {
                shapes.setColor(0.95f, 0.75f, 0.30f, alpha);
            } else {
                shapes.setColor(0.45f, 0.82f, 0.65f, alpha);
            }
            shapes.circle(pulse.x, pulse.y, radius, 24);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void close() {
        shapes.dispose();
    }
}
