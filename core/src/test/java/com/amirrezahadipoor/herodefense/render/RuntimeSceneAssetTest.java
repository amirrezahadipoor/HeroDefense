package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.BossType;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RuntimeSceneAssetTest {
    private static final Path GENERATED = Path.of("../android/assets/generated");

    @Test
    void everyEnemyAndBossRuntimeKeyHasACompleteReviewedAtlas() throws IOException {
        for (EnemyType type : EnemyType.values()) {
            Enemy enemy = new Enemy(1L, type.name(), 0f, 0f);
            assertEquals(type.assetKey(), CombatEntityRenderer.assetKey(enemy, false));
            assertTrue(CombatEntityRenderer.regularDrawSize(type) > 0f);
            assertCompleteCombatAtlas(type.assetKey());
        }
        for (BossType type : BossType.values()) {
            Boss boss = new Boss(1L, type.name(), 0f, 0f, 1);
            assertEquals(type.assetKey(), CombatEntityRenderer.assetKey(boss, true));
            assertCompleteCombatAtlas(type.assetKey());
        }
    }

    @Test
    void arenaEnvironmentAndWorldTreeFramesArePackaged() {
        for (int variant = 0; variant < 3; variant++) {
            assertTrue(Files.isRegularFile(
                GENERATED.resolve("environment/ground_tile_" + variant + ".png")
            ));
            assertTrue(Files.isRegularFile(
                GENERATED.resolve("environment/crystal_prop_" + variant + ".png")
            ));
        }
        assertTrue(Files.isRegularFile(GENERATED.resolve("sprites/world_tree_healthy.atlas")));
        assertTrue(Files.isRegularFile(GENERATED.resolve("sprites/world_tree_damaged.atlas")));
    }

    private static void assertCompleteCombatAtlas(String key) throws IOException {
        Path atlas = GENERATED.resolve("sprites/" + key + ".atlas");
        assertTrue(Files.isRegularFile(atlas), atlas.toString());
        assertTrue(Files.isRegularFile(GENERATED.resolve("sprites/" + key + ".png")));
        String text = Files.readString(atlas);
        assertEquals(6, regionCount(text, key + "_idle"));
        assertEquals(8, regionCount(text, key + "_attack"));
        assertEquals(4, regionCount(text, key + "_hit"));
        assertEquals(10, regionCount(text, key + "_death"));
    }

    private static long regionCount(String atlas, String region) {
        return atlas.lines().filter(region::equals).count();
    }
}
