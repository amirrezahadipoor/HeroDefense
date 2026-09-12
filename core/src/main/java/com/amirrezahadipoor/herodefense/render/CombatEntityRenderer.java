package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.items.EquipmentDefinition;
import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.BossType;
import com.amirrezahadipoor.herodefense.model.DropEntity;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
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
    private static final Set<String> BOSS_ASSET_KEYS = bossAssetKeys();

    private final Map<String, EntityClips> clipsByKey = new LinkedHashMap<>();
    private final Map<String, Texture> dropTextures = new HashMap<>();
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
        for (Projectile projectile : state.projectiles) {
            if (projectile == null || !projectile.active) continue;
            if (projectile.critical) {
                batch.setColor(0.35f, 0.92f, 0.96f, 1f);
            } else {
                batch.setColor(0.93f, 0.71f, 0.25f, 1f);
            }
            float angle = MathUtils.atan2(projectile.velocityY, projectile.velocityX)
                * MathUtils.radiansToDegrees;
            batch.draw(
                pixel,
                projectile.x - 11f,
                projectile.y - 3f,
                11f,
                3f,
                22f,
                6f,
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
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawDrops(SpriteBatch batch, GameState state, float runTimeSeconds) {
        for (DropEntity drop : state.drops) {
            if (drop == null || !drop.active) continue;
            String path = dropTexturePath(drop);
            Texture texture = dropTextures.computeIfAbsent(path, CombatEntityRenderer::loadTexture);
            float bob = MathUtils.sin(runTimeSeconds * 5f + drop.id * 0.31f) * 5f;
            batch.setColor(1f, 1f, 1f, 0.96f);
            batch.draw(texture, drop.x - 28f, drop.y + bob, 56f, 56f);
        }
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
