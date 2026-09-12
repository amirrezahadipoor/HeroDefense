package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.polish.Particle;
import com.amirrezahadipoor.herodefense.polish.ParticleSystem;
import com.amirrezahadipoor.herodefense.polish.ParticleType;

/** Small low-cost polygon motes rendered in shaken world space. */
public final class ParticleRenderer implements AutoCloseable {
    private final ShapeRenderer shapes = new ShapeRenderer();

    public void draw(Matrix4 projection, ParticleSystem particles) {
        if (particles.particles().isEmpty()) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Particle particle : particles.particles()) {
            setColor(particle.type, particle.lifeRatio());
            shapes.circle(particle.x, particle.y, particle.size * particle.lifeRatio(), 6);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void setColor(ParticleType type, float alpha) {
        switch (type) {
            case HIT -> shapes.setColor(0.94f, 0.70f, 0.25f, alpha);
            case DEATH -> shapes.setColor(0.40f, 0.31f, 0.50f, alpha * 0.85f);
            case COIN -> shapes.setColor(0.84f, 0.68f, 0.30f, alpha);
            case ITEM_PICKUP -> shapes.setColor(0.45f, 0.76f, 0.40f, alpha);
        }
    }

    @Override
    public void close() {
        shapes.dispose();
    }
}
