package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.polish.TouchFeedbackSystem;
import com.amirrezahadipoor.herodefense.polish.TouchPulse;

/** Restrained dual-ring touch confirmation that never blankets gameplay or labels. */
public final class TouchFeedbackRenderer implements AutoCloseable {
    private final ShapeRenderer shapes = new ShapeRenderer();

    public void draw(Matrix4 projection, TouchFeedbackSystem feedback) {
        if (feedback.pulses().isEmpty()) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glLineWidth(3f);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (TouchPulse pulse : feedback.pulses()) {
            float progress = pulse.progress();
            float radius = radius(pulse.kind, progress);
            float alpha = alpha(progress);
            if (pulse.kind == TouchPulse.Kind.CARD_SELECTION) {
                shapes.setColor(0.95f, 0.75f, 0.30f, alpha);
            } else {
                shapes.setColor(0.45f, 0.82f, 0.65f, alpha);
            }
            shapes.circle(pulse.x, pulse.y, radius, 32);
            shapes.circle(pulse.x, pulse.y, Math.max(3f, radius - 8f), 32);
        }
        shapes.end();
        Gdx.gl.glLineWidth(1f);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    static float radius(TouchPulse.Kind kind, float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        float start = kind == TouchPulse.Kind.CARD_SELECTION ? 36f : 22f;
        float travel = kind == TouchPulse.Kind.CARD_SELECTION ? 64f : 34f;
        return start + clamped * travel;
    }

    static float alpha(float progress) {
        return (1f - Math.max(0f, Math.min(1f, progress))) * 0.58f;
    }

    @Override
    public void close() {
        shapes.dispose();
    }
}
