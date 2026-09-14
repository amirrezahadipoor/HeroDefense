package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.items.EquipmentDefinition;
import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.gameplay.DropPickupSystem;
import com.amirrezahadipoor.herodefense.gameplay.FocusSystem;
import com.amirrezahadipoor.herodefense.input.HudTouchLayout;
import com.amirrezahadipoor.herodefense.model.BossType;
import com.amirrezahadipoor.herodefense.model.DropCollectionStage;
import com.amirrezahadipoor.herodefense.model.DropEntity;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.model.Projectile;
import com.amirrezahadipoor.herodefense.potions.PotionTier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Renders every live combat actor, projectile, pickup, and enemy health bar. */
public final class CombatEntityRenderer implements AutoCloseable {
    static final float FRAME_RATE = 12f;
    static final float REGULAR_FEET_RATIO = 23f / 192f;
    static final float BOSS_FEET_RATIO = 30f / 256f;
    private static final float ATTACK_CLIP_SECONDS = 8f / FRAME_RATE;
    public static final float DROP_TARGET_X = HudTouchLayout.INVENTORY_X
        + HudTouchLayout.UTILITY_BUTTON_WIDTH * 0.5f;
    public static final float DROP_TARGET_Y = HudTouchLayout.DESIGN_UTILITY_BUTTON_Y
        + HudTouchLayout.UTILITY_BUTTON_HEIGHT * 0.5f;
    private static final float DROP_HOMING_ARC_HEIGHT = 86f;
    static final int PROJECTILE_TRAIL_STEPS = 3;
    static final int MAX_PROGRESSION_STEP = 10;
    static final int FOCUS_RING_SEGMENTS = 36;
    static final float FOCUS_RING_RADIUS = 108f;
    static final float FOCUS_RING_CENTER_Y_OFFSET = 73f;
    private static final Set<String> BOSS_ASSET_KEYS = bossAssetKeys();

    private final Map<String, EntityClips> clipsByKey = new LinkedHashMap<>();
    private final Map<String, Texture> dropTextures = new HashMap<>();
    private final RarityGlowRenderer dropGlowRenderer = new RarityGlowRenderer();
    private final Texture pixel;

    public CombatEntityRenderer() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
    }

    public void drawActors(SpriteBatch batch, GameState state, float runTimeSeconds) {
        for (Enemy enemy : state.aliveEnemies) {
            if (enemy != null) drawEnemy(batch, enemy, false, runTimeSeconds);
        }
        for (Boss boss : state.aliveBosses) {
            if (boss != null) drawEnemy(batch, boss, true, runTimeSeconds);
        }
        disposeInactiveBossAtlases(state);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void drawEffects(SpriteBatch batch, GameState state, float runTimeSeconds) {
        drawFocusRing(batch, state);
        drawProjectiles(batch, state);
        drawDrops(batch, state, runTimeSeconds);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawEnemy(
        SpriteBatch batch,
        Enemy enemy,
        boolean boss,
        float runTimeSeconds
    ) {
        String key = assetKey(enemy, boss);
        EntityClips clips = clipsByKey.get(key);
        if (clips == null) clips = load(key);
        Array<TextureAtlas.AtlasRegion> frames = selectedFrames(clips, enemy);
        int frameIndex = frameIndex(enemy, frames.size, runTimeSeconds);
        float size = boss ? 240f : regularDrawSize(enemy.type());
        float feetRatio = boss ? BOSS_FEET_RATIO : REGULAR_FEET_RATIO;
        float x = enemy.x - size * 0.5f;
        float y = enemy.y - size * feetRatio;
        if (!enemy.alive) batch.setColor(0.62f, 0.62f, 0.70f, 0.72f);
        batch.draw(frames.get(frameIndex), x, y, size, size);
        batch.setColor(1f, 1f, 1f, 1f);
        if (enemy.alive) drawHealthBar(batch, enemy, x, y + size * 0.88f, size);
    }

    private void drawHealthBar(
        SpriteBatch batch,
        Enemy enemy,
        float frameX,
        float barY,
        float frameSize
    ) {
        float width = frameSize * 0.66f;
        float x = frameX + (frameSize - width) * 0.5f;
        float ratio = enemy.maxHealth <= 0f
            ? 0f
            : MathUtils.clamp(enemy.health / enemy.maxHealth, 0f, 1f);
        batch.setColor(0.03f, 0.045f, 0.05f, 0.92f);
        batch.draw(pixel, x - 2f, barY - 2f, width + 4f, 10f);
        batch.setColor(0.67f, 0.18f, 0.16f, 1f);
        batch.draw(pixel, x, barY, width, 6f);
        batch.setColor(0.34f, 0.76f, 0.39f, 1f);
        batch.draw(pixel, x, barY, width * ratio, 6f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawProjectiles(SpriteBatch batch, GameState state) {
        int power = progressionStep(state);
        float heat = trailHeat(power);
        float goldRed = 0.93f + (1f - 0.93f) * heat;
        float goldGreen = 0.71f + (0.95f - 0.71f) * heat;
        float goldBlue = 0.25f + (0.75f - 0.25f) * heat;
        for (Projectile projectile : state.projectiles) {
            if (projectile == null || !projectile.active) continue;
            float angle = MathUtils.atan2(projectile.velocityY, projectile.velocityX)
                * MathUtils.radiansToDegrees;
            float speed = (float) Math.sqrt(
                projectile.velocityX * projectile.velocityX
                    + projectile.velocityY * projectile.velocityY
            );
            float nx = speed <= 0f ? 1f : projectile.velocityX / speed;
            float ny = speed <= 0f ? 0f : projectile.velocityY / speed;
            for (int step = 1; step <= PROJECTILE_TRAIL_STEPS; step++) {
                float back = step * 9f;
                float alpha = projectileTrailAlpha(step, power);
                if (projectile.critical) {
                    batch.setColor(0.35f, 0.92f, 0.96f, alpha);
                } else if (projectile.secondary) {
                    batch.setColor(0.62f, 0.86f, 0.58f, alpha * 0.9f);
                } else {
                    batch.setColor(goldRed, goldGreen, goldBlue, alpha);
                }
                float size = (projectile.secondary ? 6f : 5f) - step + power * 0.25f;
                batch.draw(
                    pixel,
                    projectile.x - nx * back - size * 0.5f,
                    projectile.y - ny * back - size * 0.5f,
                    size,
                    size
                );
            }
            if (projectile.critical) {
                batch.setColor(0.35f, 0.92f, 0.96f, 1f);
            } else if (projectile.secondary) {
                batch.setColor(0.72f, 0.90f, 0.62f, 1f);
            } else {
                batch.setColor(0.93f, 0.71f, 0.25f, 1f);
            }
            float length = projectile.critical ? 26f : 22f;
            float thickness = projectile.critical ? 7f : 6f;
            batch.draw(
                pixel,
                projectile.x - length * 0.5f,
                projectile.y - thickness * 0.5f,
                length * 0.5f,
                thickness * 0.5f,
                length,
                thickness,
                1f,
                1f,
                angle,
                0,
                0,
                1,
                1,
                false,
                false
            );
            batch.setColor(1f, 1f, 1f, 0.85f);
            batch.draw(pixel, projectile.x + nx * 6f - 2f, projectile.y + ny * 6f - 2f, 4f, 4f);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /** Lit ring segments for a 0..1 Focus ratio; degenerate ratios light none. */
    static int focusRingLitSegments(float ratio) {
        if (!Float.isFinite(ratio) || ratio <= 0f) return 0;
        if (ratio >= 1f) return FOCUS_RING_SEGMENTS;
        return Math.round(ratio * FOCUS_RING_SEGMENTS);
    }

    /**
     * Focus meter as a pixel ring around the Hero: a dim full track, a gold
     * lit arc for the charge, burning white once the Ultimate is ready.
     */
    private void drawFocusRing(SpriteBatch batch, GameState state) {
        if (state == null || state.hero == null || !state.hero.alive) return;
        float ratio = FocusSystem.ratio(state);
        int lit = focusRingLitSegments(ratio);
        boolean full = lit >= FOCUS_RING_SEGMENTS;
        float centerX = state.hero.x;
        float centerY = state.hero.y + FOCUS_RING_CENTER_Y_OFFSET;
        for (int index = 0; index < FOCUS_RING_SEGMENTS; index++) {
            double angle = index * Math.PI * 2.0 / FOCUS_RING_SEGMENTS - Math.PI * 0.5;
            float x = centerX + (float) Math.cos(angle) * FOCUS_RING_RADIUS;
            float y = centerY + (float) Math.sin(angle) * FOCUS_RING_RADIUS;
            if (index < lit) {
                if (full) {
                    batch.setColor(1f, 0.98f, 0.90f, 0.95f);
                } else {
                    batch.setColor(0.93f, 0.76f, 0.32f, 0.9f);
                }
                float size = full ? 7f : 6f;
                batch.draw(pixel, x - size * 0.5f, y - size * 0.5f, size, size);
            } else {
                batch.setColor(0.30f, 0.26f, 0.20f, 0.55f);
                batch.draw(pixel, x - 2f, y - 2f, 4f, 4f);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    static float projectileTrailAlpha(int step) {
        return projectileTrailAlpha(step, 0);
    }

    static float projectileTrailAlpha(int step, int powerStep) {
        float boost =
            Math.max(0, Math.min(MAX_PROGRESSION_STEP, powerStep)) * 0.03f;
        return Math.min(0.85f, Math.max(0f, 0.55f - step * 0.15f) + boost);
    }

    /** 0..1 heat of a normal arrow's trail gold as raw progression climbs. */
    static float trailHeat(int powerStep) {
        return Math.max(0, Math.min(MAX_PROGRESSION_STEP, powerStep))
            / (float) MAX_PROGRESSION_STEP;
    }

    /**
     * Raw progression in 0..10: the worn bow's Anvil forge level plus the
     * Ascension tier. Drives arrow-trail and bow-glow escalation.
     */
    static int progressionStep(GameState state) {
        if (state == null) return 0;
        int forge = 0;
        if (state.equippedItems != null) {
            Item bow = state.equippedItems.get(EquipmentSlot.WEAPON.name());
            if (bow != null) {
                forge = Math.max(
                    0, Math.min(GameState.MAX_ITEM_UPGRADE, bow.upgradeLevel)
                );
            }
        }
        return Math.max(
            0, Math.min(MAX_PROGRESSION_STEP, forge + Math.max(0, state.ascensionTier))
        );
    }

    private void drawDrops(SpriteBatch batch, GameState state, float runTimeSeconds) {
        for (DropEntity drop : state.drops) {
            if (drop == null || !drop.active) continue;
            String path = dropTexturePath(drop);
            Texture texture = dropTextures.computeIfAbsent(path, CombatEntityRenderer::loadTexture);
            float progress = dropHomingProgress(drop);
            float size = 56f * (1f - progress * 0.42f);
            float alpha = 0.96f * (1f - progress * 0.24f);
            float x = dropDrawX(drop);
            float y = dropDrawY(drop, runTimeSeconds);
            VisualRarity rarity = dropRarity(drop);
            drawRarityTrail(batch, drop, rarity, runTimeSeconds);
            batch.setColor(1f, 1f, 1f, alpha);
            if (rarity.isGlowing()) {
                dropGlowRenderer.draw(
                    batch,
                    new TextureRegion(texture),
                    x - size * 0.5f,
                    y - size * 0.5f,
                    size,
                    size,
                    rarity,
                    runTimeSeconds
                );
            } else {
                batch.draw(texture, x - size * 0.5f, y - size * 0.5f, size, size);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawRarityTrail(
        SpriteBatch batch,
        DropEntity drop,
        VisualRarity rarity,
        float runTimeSeconds
    ) {
        if (!rarity.isGlowing() || drop.collectionStage != DropCollectionStage.HOMING) return;
        float current = dropHomingProgress(drop);
        float pulse = 0.82f + 0.18f * MathUtils.sin(runTimeSeconds * 8f + drop.id);
        for (int step = 1; step <= 3; step++) {
            float sample = Math.max(0f, current - step * 0.11f);
            float x = dropDrawX(drop, sample);
            float y = dropDrawY(drop, runTimeSeconds, sample);
            float size = 16f - step * 3f;
            float alpha = (0.34f - step * 0.07f) * pulse;
            batch.setColor(rarity.red(), rarity.green(), rarity.blue(), alpha);
            batch.draw(pixel, x - size * 0.5f, y - size * 0.5f, size, size);
        }
    }

    static VisualRarity dropRarity(DropEntity drop) {
        if (drop == null || !"ITEM".equals(drop.dropType)) return VisualRarity.COMMON;
        EquipmentDefinition item = EquipmentCatalog.byId(drop.itemId);
        return item == null
            ? VisualRarity.COMMON
            : VisualRarity.fromTier(item.tier().name());
    }

    static float dropHomingProgress(DropEntity drop) {
        if (drop == null || drop.collectionStage != DropCollectionStage.HOMING) return 0f;
        return MathUtils.clamp(
            drop.homingElapsedSeconds / DropPickupSystem.HOMING_DURATION_SECONDS,
            0f,
            1f
        );
    }

    static float dropDrawX(DropEntity drop) {
        return dropDrawX(drop, dropHomingProgress(drop));
    }

    private static float dropDrawX(DropEntity drop, float progress) {
        return MathUtils.lerp(drop.x, DROP_TARGET_X, smoothStep(progress));
    }

    static float dropDrawY(DropEntity drop, float runTimeSeconds) {
        return dropDrawY(drop, runTimeSeconds, dropHomingProgress(drop));
    }

    private static float dropDrawY(
        DropEntity drop,
        float runTimeSeconds,
        float progress
    ) {
        float eased = smoothStep(progress);
        float bob = MathUtils.sin(runTimeSeconds * 5f + drop.id * 0.31f)
            * 5f
            * (1f - progress);
        float arc = MathUtils.sin(MathUtils.PI * progress) * DROP_HOMING_ARC_HEIGHT;
        return MathUtils.lerp(drop.y + 28f, DROP_TARGET_Y, eased) + bob + arc;
    }

    private static float smoothStep(float value) {
        return value * value * (3f - 2f * value);
    }

    static String assetKey(Enemy enemy, boolean boss) {
        if (boss && enemy instanceof Boss typedBoss) {
            return typedBoss.bossDefinition().assetKey();
        }
        return enemy.type().assetKey();
    }

    static float regularDrawSize(EnemyType type) {
        return switch (type) {
            case ROOTLING -> 148f;
            case STONEKIN -> 166f;
            case GLOOM_WOLF -> 158f;
            case FUNGAL_BRUTE -> 178f;
        };
    }

    private static boolean attacking(Enemy enemy) {
        if (!enemy.alive) return false;
        if (enemy instanceof Boss boss && boss.specialAnimationSeconds > 0f) return true;
        float interval = Math.max(0.1f, enemy.attackIntervalSeconds);
        return enemy.attackCooldownSeconds > Math.max(0f, interval - ATTACK_CLIP_SECONDS);
    }

    private static Array<TextureAtlas.AtlasRegion> selectedFrames(
        EntityClips clips,
        Enemy enemy
    ) {
        if (!enemy.alive) return clips.death;
        return attacking(enemy) ? clips.attack : clips.idle;
    }

    private static int frameIndex(Enemy enemy, int frameCount, float runTimeSeconds) {
        if (!enemy.alive) return frameCount - 1;
        if (attacking(enemy)) {
            float interval = Math.max(0.1f, enemy.attackIntervalSeconds);
            float elapsed = Math.max(0f, interval - enemy.attackCooldownSeconds);
            return Math.min(frameCount - 1, Math.max(0, (int) (elapsed * FRAME_RATE)));
        }
        return Math.floorMod((int) (runTimeSeconds * FRAME_RATE + enemy.id), frameCount);
    }

    private EntityClips load(String key) {
        TextureAtlas atlas = new TextureAtlas(
            Gdx.files.internal("generated/sprites/" + key + ".atlas")
        );
        EntityClips clips = new EntityClips(
            atlas,
            require(atlas, key + "_idle", 6),
            require(atlas, key + "_attack", 8),
            require(atlas, key + "_death", 10)
        );
        clipsByKey.put(key, clips);
        return clips;
    }

    private void disposeInactiveBossAtlases(GameState state) {
        Set<String> activeBossKeys = new HashSet<>();
        for (Boss boss : state.aliveBosses) {
            if (boss != null) activeBossKeys.add(boss.bossDefinition().assetKey());
        }
        Iterator<Map.Entry<String, EntityClips>> iterator = clipsByKey.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, EntityClips> entry = iterator.next();
            if (BOSS_ASSET_KEYS.contains(entry.getKey())
                && !activeBossKeys.contains(entry.getKey())) {
                entry.getValue().atlas.dispose();
                iterator.remove();
            }
        }
    }

    private static Array<TextureAtlas.AtlasRegion> require(
        TextureAtlas atlas,
        String name,
        int expected
    ) {
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(name);
        if (regions.size != expected) {
            atlas.dispose();
            throw new IllegalStateException(
                "Expected " + expected + " frames for " + name + ", found " + regions.size
            );
        }
        return regions;
    }

    private static String dropTexturePath(DropEntity drop) {
        if ("POTION".equals(drop.dropType)) {
            try {
                return PotionTier.valueOf(drop.itemId).iconPath();
            } catch (IllegalArgumentException | NullPointerException ignored) {
                return PotionTier.TIER_1.iconPath();
            }
        }
        if ("ITEM".equals(drop.dropType)) {
            EquipmentDefinition item = EquipmentCatalog.byId(drop.itemId);
            if (item != null) return item.iconPath();
            return "generated/icons/ui_inventory.png";
        }
        return "generated/icons/ui_coin.png";
    }

    private static Texture loadTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private static Set<String> bossAssetKeys() {
        Set<String> keys = new HashSet<>();
        for (BossType type : BossType.values()) keys.add(type.assetKey());
        return Set.copyOf(keys);
    }

    @Override
    public void close() {
        for (EntityClips clips : clipsByKey.values()) clips.atlas.dispose();
        clipsByKey.clear();
        for (Texture texture : dropTextures.values()) texture.dispose();
        dropTextures.clear();
        dropGlowRenderer.close();
        pixel.dispose();
    }

    private static final class EntityClips {
        private final TextureAtlas atlas;
        private final Array<TextureAtlas.AtlasRegion> idle;
        private final Array<TextureAtlas.AtlasRegion> attack;
        private final Array<TextureAtlas.AtlasRegion> death;

        private EntityClips(
            TextureAtlas atlas,
            Array<TextureAtlas.AtlasRegion> idle,
            Array<TextureAtlas.AtlasRegion> attack,
            Array<TextureAtlas.AtlasRegion> death
        ) {
            this.atlas = atlas;
            this.idle = idle;
            this.attack = attack;
            this.death = death;
        }
    }
}
