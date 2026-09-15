package com.amirrezahadipoor.herodefense.polish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded deterministic particle pool for core combat/economy feedback. */
public final class ParticleSystem {
    public static final int MAX_PARTICLES = 256;
    private final List<Particle> particles = new ArrayList<>();
    private int emissionSequence;

    /** One impact core plus at most six short motes; criticals add a cool expanding ring. */
    public void emitHit(float x, float y, boolean critical) {
        add(ParticleType.IMPACT_CORE, x, y, 0f, 0f, critical ? 0.16f : 0.12f, critical ? 16f : 11f);
        int motes = critical ? Math.round(VfxBudget.NORMAL_HIT_MAX_MOTES * VfxBudget.CRITICAL_MULTIPLIER)
            : VfxBudget.NORMAL_HIT_MAX_MOTES;
        emitBurst(ParticleType.HIT, x, y, motes, critical ? 120f : 80f, 0.22f, critical ? 6f : 4.5f);
        if (critical) {
            add(ParticleType.CRITICAL_RING, x, y, 0f, 0f, 0.24f, 44f);
            add(ParticleType.CRITICAL_RING, x, y, 0f, 0f, 0.34f, 70f);
            // Four radial gold sparks read as a "star" burst at phone scale.
            for (int index = 0; index < VfxBudget.CRITICAL_SPARKS; index++) {
                float angle = (float) (Math.PI * 0.25f + index * Math.PI * 0.5f);
                add(ParticleType.CRITICAL_SPARK, x, y,
                    (float) Math.cos(angle) * 210f, (float) Math.sin(angle) * 210f, 0.20f, 5f);
            }
        }
    }

    /** Jagged branching arcs with deterministic jitter, impact flash per target, and per-target pop. */
    public void emitChainArc(float fromX, float fromY, float toX, float toY) {
        Particle beam = addAndGet(ParticleType.CHAIN_BEAM, fromX, fromY, 0f, 0f, 0.22f, 3.5f);
        beam.endX = toX;
        beam.endY = toY;
        // Impact flash per target
        add(ParticleType.CHAIN_FLASH, toX, toY, 0f, 0f, 0.16f, 14f);
        emitBurst(ParticleType.HIT, toX, toY, VfxBudget.CHAIN_ARC_MAX_MOTES, 90f, 0.18f, 3.5f);
        // Deterministic branching for longer arcs: one short offshoot at midpoint
        float dx = toX - fromX;
        float dy = toY - fromY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 140f) {
            float nx = -dy / Math.max(1f, len);
            float ny = dx / Math.max(1f, len);
            float midX = (fromX + toX) * 0.5f;
            float midY = (fromY + toY) * 0.5f;
            float jitter = (emissionSequence % 2 == 0 ? 1f : -1f) * 13f;
            float bx = midX + nx * jitter;
            float by = midY + ny * jitter;
            Particle branch = addAndGet(ParticleType.CHAIN_BEAM, midX, midY, 0f, 0f, 0.16f, 2.0f);
            branch.endX = bx;
            branch.endY = by;
        }
    }

    /** Muzzle flash at the bow when Multi Shot fires: bright core plus fanned sparks. */
    public void emitMuzzleFlash(float x, float y, int shots) {
        add(ParticleType.IMPACT_CORE, x, y, 0f, 0f, 0.13f, 16f);
        add(ParticleType.CRITICAL_RING, x, y, 0f, 0f, 0.18f, 22f);
        int motes = Math.min(6, Math.max(2, shots * 2));
        emitBurst(ParticleType.HIT, x, y, motes, 95f, 0.16f, 3.2f);
    }

    /** Three stars orbiting the head for the whole stun; capped so a crowd stays readable. */
    public void emitStunSparks(float x, float y, float durationSeconds) {
        for (int index = 0; index < VfxBudget.STUN_SPARKS; index++) {
            Particle spark = addAndGet(ParticleType.STUN_SPARK, x, y, 0f, 0f,
                Math.min(1.6f, durationSeconds), 4.5f);
            spark.endX = index * (float) (Math.PI * 2.0 / VfxBudget.STUN_SPARKS);
        }
    }

    /** Collapse dust plus one soft ground ring so every kill reads at phone scale. */
    public void emitDeath(float x, float y) {
        add(ParticleType.DEATH_RING, x, y - 12f, 0f, 0f, 0.36f, 40f);
        emitBurst(ParticleType.DEATH, x, y, 10, 95f, 0.48f, 7f);
    }

    public void emitCoins(float x, float y) {
        emitBurst(ParticleType.COIN, x, y, 5, 72f, 0.62f, 6f);
    }

    public void emitItemPickup(float x, float y) {
        emitBurst(ParticleType.ITEM_PICKUP, x, y, 12, 88f, 0.70f, 7f);
    }

    /** Small upward sparkle where a homing drop lands on the Inventory control. */
    public void emitCollectionSparkle(float x, float y) {
        emitBurst(ParticleType.COLLECTION_SPARKLE, x, y, 6, 58f, 0.34f, 4f);
    }

    /** Boss entrance: one shockwave plus grounded dust, within the documented boss multiplier. */
    public void emitBossEntrance(float x, float y) {
        // Arrival lingers longer than any hit so the shockwave reads through the wave banner.
        add(ParticleType.BOSS_SHOCKWAVE, x, y, 0f, 0f, 0.75f, 150f);
        add(ParticleType.BOSS_SHOCKWAVE, x, y, 0f, 0f, 1.05f, 190f);
        emitBurst(ParticleType.BOSS_DUST, x, y, bossMotes(), 110f, 0.85f, 9f);
    }

    /**
     * Ultimate blast: one large shockwave, an enlarged double critical ring,
     * and a radial gold star, all inside the Ultimate budget multiplier.
     */
    public void emitUltimateBlast(float x, float y) {
        add(ParticleType.BOSS_SHOCKWAVE, x, y, 0f, 0f, 0.60f, 260f);
        add(ParticleType.CRITICAL_RING, x, y, 0f, 0f, 0.30f, 90f);
        add(ParticleType.CRITICAL_RING, x, y, 0f, 0f, 0.42f, 140f);
        int motes = Math.round(VfxBudget.NORMAL_HIT_MAX_MOTES * VfxBudget.ULTIMATE_MULTIPLIER);
        emitBurst(ParticleType.HIT, x, y, motes, 200f, 0.40f, 7f);
        for (int index = 0; index < VfxBudget.ULTIMATE_SPARKS; index++) {
            float angle = index * (float) (Math.PI * 2.0 / VfxBudget.ULTIMATE_SPARKS);
            add(ParticleType.CRITICAL_SPARK, x, y,
                (float) Math.cos(angle) * 260f, (float) Math.sin(angle) * 260f, 0.26f, 6f);
        }
    }

    /** Boss death: a larger shockwave layered over the standard collapse treatment. */
    public void emitBossDeath(float x, float y) {
        emitDeath(x, y);
        add(ParticleType.BOSS_SHOCKWAVE, x, y, 0f, 0f, 0.55f, 210f);
        emitBurst(ParticleType.BOSS_DUST, x, y, bossMotes(), 130f, 0.60f, 9f);
    }

    /** Falling leaves as the World Tree collapses; slow, wide, and never brighter than actors. */
    public void emitTreeDestruction(float x, float y) {
        for (int index = 0; index < 18; index++) {
            float spread = (index % 6 - 2.5f) * 34f;
            float lift = (index / 6) * 46f;
            add(
                ParticleType.TREE_LEAF,
                x + spread,
                y + 90f + lift,
                (index % 2 == 0 ? -1f : 1f) * (22f + (index % 3) * 9f),
                -18f - (index % 4) * 7f,
                1.35f + (index % 5) * 0.12f,
                7f + (index % 3) * 1.5f
            );
        }
    }

    /** A short arc of droplets from the watering-can spout toward the soil (ceremony only). */
    public void emitWaterDrops(float spoutX, float spoutY) {
        for (int index = 0; index < 3; index++) {
            add(
                ParticleType.WATER_DROP,
                spoutX + (index - 1) * 3f,
                spoutY,
                34f + index * 9f,
                -20f - index * 6f,
                0.34f + index * 0.04f,
                2.6f + index * 0.4f
            );
        }
    }

    public void update(float deltaSeconds) {
        if (deltaSeconds <= 0f) return;
        for (Particle particle : particles) {
            particle.remainingSeconds -= deltaSeconds;
            particle.x += particle.velocityX * deltaSeconds;
            particle.y += particle.velocityY * deltaSeconds;
            particle.velocityX *= Math.max(0f, 1f - drag(particle.type) * deltaSeconds);
            particle.velocityY += gravity(particle.type) * deltaSeconds;
        }
        particles.removeIf(particle -> particle.remainingSeconds <= 0f);
    }

    public List<Particle> particles() {
        return Collections.unmodifiableList(particles);
    }

    public void clear() {
        particles.clear();
    }

    static int bossMotes() {
        return Math.round(VfxBudget.NORMAL_HIT_MAX_MOTES * VfxBudget.BOSS_MULTIPLIER);
    }

    private static float drag(ParticleType type) {
        return switch (type) {
            case TREE_LEAF -> 0.4f;
            case BOSS_DUST -> 4.0f;
            case WATER_DROP -> 0.2f;
            default -> 3.2f;
        };
    }

    private static float gravity(ParticleType type) {
        return switch (type) {
            case COIN, COLLECTION_SPARKLE -> 22f;
            case TREE_LEAF -> -14f;
            case BOSS_DUST -> -30f;
            case IMPACT_CORE, CRITICAL_RING, DEATH_RING, BOSS_SHOCKWAVE,
                CHAIN_BEAM, CHAIN_FLASH, STUN_SPARK -> 0f;
            case CRITICAL_SPARK -> -20f;
            case WATER_DROP -> -420f;
            default -> -48f;
        };
    }

    private Particle addAndGet(
        ParticleType type,
        float x,
        float y,
        float velocityX,
        float velocityY,
        float lifetime,
        float size
    ) {
        add(type, x, y, velocityX, velocityY, lifetime, size);
        return particles.get(particles.size() - 1);
    }

    private void add(
        ParticleType type,
        float x,
        float y,
        float velocityX,
        float velocityY,
        float lifetime,
        float size
    ) {
        if (particles.size() >= MAX_PARTICLES) particles.remove(0);
        particles.add(new Particle(type, x, y, velocityX, velocityY, lifetime, size));
    }

    private void emitBurst(
        ParticleType type,
        float x,
        float y,
        int count,
        float speed,
        float lifetime,
        float size
    ) {
        for (int index = 0; index < count; index++) {
            float angle = (emissionSequence++ * 2.3999632f + index * 1.37f) % ((float) Math.PI * 2f);
            float variation = 0.68f + (index % 4) * 0.11f;
            add(
                type,
                x,
                y,
                (float) Math.cos(angle) * speed * variation,
                (float) Math.sin(angle) * speed * variation + speed * 0.28f,
                lifetime * (0.85f + (index % 3) * 0.08f),
                size * (0.82f + (index % 3) * 0.10f)
            );
        }
    }
}
