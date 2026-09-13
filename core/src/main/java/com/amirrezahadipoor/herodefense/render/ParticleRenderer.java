package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.polish.AmbientMoteField;
import com.amirrezahadipoor.herodefense.polish.Particle;
import com.amirrezahadipoor.herodefense.polish.ParticleSystem;
import com.amirrezahadipoor.herodefense.polish.ParticleType;

/** Layered low-cost polygon VFX: ambient spores, expanding rings, cores, and shrinking motes. */
public final class ParticleRenderer implements AutoCloseable {
    static final float RING_THICKNESS = 3f;
    private final ShapeRenderer shapes = new ShapeRenderer();

    /** Ambient layer drawn beneath actors; alpha never exceeds the ambient budget. */
    public void drawAmbient(Matrix4 projection, float timeSeconds) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int index = 0; index < AmbientMoteField.COUNT; index++) {
            shapes.setColor(0.62f, 0.86f, 0.58f, AmbientMoteField.alpha(index, timeSeconds));
            shapes.circle(
                AmbientMoteField.x(index, timeSeconds),
                AmbientMoteField.y(index, timeSeconds),
                AmbientMoteField.size(index),
                6
            );
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void draw(Matrix4 projection, ParticleSystem particles) {
        if (particles.particles().isEmpty()) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Particle particle : particles.particles()) {
            if (particle.type.isRing()) continue;
            float life = particle.lifeRatio();
            setColor(particle.type, life);
            if (particle.type == ParticleType.TREE_LEAF) {
                float flutter = (float) Math.sin(life * 12f + particle.x * 0.05f);
                shapes.ellipse(
                    particle.x - particle.size * 0.5f,
                    particle.y - particle.size * 0.3f,
                    particle.size,
                    particle.size * (0.45f + 0.25f * flutter),
                    flutter * 40f
                );
            } else {
                shapes.circle(particle.x, particle.y, particle.size * moteScale(particle.type, life), 6);
            }
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(RING_THICKNESS);
        for (Particle particle : particles.particles()) {
            if (!particle.type.isRing()) continue;
            float life = particle.lifeRatio();
            setColor(particle.type, life);
            shapes.circle(particle.x, particle.y, ringRadius(particle.size, life), 28);
        }
        shapes.end();
        Gdx.gl.glLineWidth(1f);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Rings grow from a small seed to their full radius as life drains. */
    static float ringRadius(float fullRadius, float lifeRatio) {
        float progress = 1f - Math.max(0f, Math.min(1f, lifeRatio));
        float eased = 1f - (1f - progress) * (1f - progress);
        return Math.max(2f, fullRadius * (0.18f + 0.82f * eased));
    }

    /** Impact cores flash to full size instantly and collapse; motes shrink linearly. */
    static float moteScale(ParticleType type, float lifeRatio) {
        if (type == ParticleType.IMPACT_CORE) return (float) Math.sqrt(Math.max(0f, lifeRatio));
        return Math.max(0f, lifeRatio);
    }

    private void setColor(ParticleType type, float alpha) {
        switch (type) {
            case HIT -> shapes.setColor(0.94f, 0.70f, 0.25f, alpha);
            case IMPACT_CORE -> shapes.setColor(1.00f, 0.93f, 0.72f, alpha * 0.9f);
            case CRITICAL_RING -> shapes.setColor(0.35f, 0.92f, 0.96f, alpha * 0.85f);
            case DEATH -> shapes.setColor(0.40f, 0.31f, 0.50f, alpha * 0.85f);
            case DEATH_RING -> shapes.setColor(0.52f, 0.44f, 0.60f, alpha * 0.55f);
            case COIN -> shapes.setColor(0.84f, 0.68f, 0.30f, alpha);
            case ITEM_PICKUP -> shapes.setColor(0.45f, 0.76f, 0.40f, alpha);
            case COLLECTION_SPARKLE -> shapes.setColor(0.98f, 0.90f, 0.62f, alpha);
            case BOSS_SHOCKWAVE -> shapes.setColor(0.93f, 0.62f, 0.30f, alpha * 0.75f);
            case BOSS_DUST -> shapes.setColor(0.36f, 0.30f, 0.24f, alpha * 0.7f);
            case TREE_LEAF -> shapes.setColor(0.42f, 0.66f, 0.30f, alpha * 0.9f);
        }
    }

    @Override
    public void close() {
        shapes.dispose();
    }
}
