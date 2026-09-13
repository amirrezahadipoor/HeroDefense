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
        draw(projection, particles, 0f);
    }

    public void draw(Matrix4 projection, ParticleSystem particles, float timeSeconds) {
        if (particles.particles().isEmpty()) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Particle particle : particles.particles()) {
            if (particle.type.isRing() || particle.type.isBeam()) continue;
            float life = particle.lifeRatio();
            setColor(particle.type, life);
            if (particle.type == ParticleType.STUN_SPARK) {
                drawStunSpark(particle, timeSeconds);
            } else if (particle.type == ParticleType.CRITICAL_SPARK) {
                float length = particle.size * 3.2f * life;
                shapes.rectLine(particle.x, particle.y,
                    particle.x - particle.velocityX * 0.03f, particle.y - particle.velocityY * 0.03f,
                    Math.max(1.5f, length * 0.25f));
            } else if (particle.type == ParticleType.TREE_LEAF) {
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
        for (Particle particle : particles.particles()) {
            if (particle.type.isBeam()) drawBeam(particle, timeSeconds);
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

    /** Jagged polyline with a bright core over a wider, dimmer glow; jitter is time-seeded. */
    private void drawBeam(Particle beam, float timeSeconds) {
        float life = beam.lifeRatio();
        float dx = beam.endX - beam.x;
        float dy = beam.endY - beam.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1f) return;
        float nx = -dy / length;
        float ny = dx / length;
        int segments = Math.max(3, Math.min(BEAM_MAX_SEGMENTS, Math.round(length / 26f)));
        float jitterPhase = beam.seed + timeSeconds * 37f;
        float previousX = beam.x;
        float previousY = beam.y;
        for (int index = 1; index <= segments; index++) {
            float t = index / (float) segments;
            float amplitude = index == segments ? 0f : beamJitter(jitterPhase, index) * 9f;
            float pointX = beam.x + dx * t + nx * amplitude;
            float pointY = beam.y + dy * t + ny * amplitude;
            shapes.setColor(0.35f, 0.72f, 0.96f, life * 0.35f);
            shapes.rectLine(previousX, previousY, pointX, pointY, beam.size * 2.6f);
            shapes.setColor(0.86f, 0.97f, 1.00f, life);
            shapes.rectLine(previousX, previousY, pointX, pointY, beam.size);
            previousX = pointX;
            previousY = pointY;
        }
    }

    static final int BEAM_MAX_SEGMENTS = 9;

    /** Deterministic pseudo-noise in [-1, 1] from the phase and vertex index. */
    static float beamJitter(float phase, int index) {
        return (float) Math.sin(phase * 1.7f + index * 2.39996f) * (index % 2 == 0 ? 1f : -1f);
    }

    /** Four-point star orbiting 22 px above the stun anchor. */
    private void drawStunSpark(Particle spark, float timeSeconds) {
        float angle = spark.endX + timeSeconds * 5.2f;
        float radius = 20f;
        float cx = spark.x + (float) Math.cos(angle) * radius;
        float cy = spark.y + 22f + (float) Math.sin(angle) * radius * 0.42f;
        float r = spark.size;
        shapes.triangle(cx - r, cy, cx, cy + r * 1.6f, cx + r, cy);
        shapes.triangle(cx - r, cy, cx, cy - r * 1.6f, cx + r, cy);
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
            case CRITICAL_SPARK -> shapes.setColor(0.98f, 0.86f, 0.45f, alpha);
            case CHAIN_BEAM -> shapes.setColor(0.86f, 0.97f, 1.00f, alpha);
            case CHAIN_FLASH -> shapes.setColor(0.80f, 0.96f, 1.00f, alpha * 0.9f);
            case STUN_SPARK -> shapes.setColor(0.86f, 0.76f, 0.98f, Math.min(1f, alpha * 3f));
            case WATER_DROP -> shapes.setColor(0.56f, 0.80f, 0.96f, Math.min(1f, alpha * 1.6f));
        }
    }

    @Override
    public void close() {
        shapes.dispose();
    }
}
