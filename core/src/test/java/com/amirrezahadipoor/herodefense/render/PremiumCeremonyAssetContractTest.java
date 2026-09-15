package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.amirrezahadipoor.herodefense.gameplay.PlantingCeremony;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Hash-bound runtime and evidence guards for the accepted Phase 18 planting-ceremony renders. */
final class PremiumCeremonyAssetContractTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path GENERATED = REPOSITORY.resolve("android/assets/generated");
    private static final Path MANIFEST = GENERATED.resolve("asset_manifest.json");
    private static final String REVIEW_DOCUMENT = "docs/art_reviews/CEREMONY_PREMIUM_V2_REVIEW.md";
    private static final Path REVIEW_DIRECTORY = REPOSITORY.resolve("docs/art_reviews/ceremony_premium_v2");
    private static final Path AUDIT = REVIEW_DIRECTORY.resolve("ceremony_audit.json");
    private static final Map<String, Map<String, Integer>> CLIPS = Map.of(
        "hero_ceremony", Map.of(
            "walk", PlantingCeremony.WALK_FRAMES,
            "plant", PlantingCeremony.PLANT_FRAMES,
            "water", PlantingCeremony.WATER_FRAMES
        ),
        "world_tree_sapling", Map.of(
            "grow", PlantingCeremony.GROW_FRAMES,
            "idle", SaplingTreeRenderer.IDLE_FRAMES
        )
    );

    @Test
    void runtimeUsesOnlyTheExactReviewedCeremonyRenders() throws IOException {
        String review = Files.readString(REPOSITORY.resolve(REVIEW_DOCUMENT));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(sha256(AUDIT)), "review must be bound to the audit hash");
        JsonValue manifest = parse(MANIFEST);
        JsonValue audit = parse(AUDIT);
        assertEquals("ceremony-premium-v2", audit.getString("batch"));
        assertTrue(review.contains(audit.getString("candidateManifestSha256")));
        Map<String, JsonValue> generated = assetsByKey(manifest);

        for (Map.Entry<String, Map<String, Integer>> expected : CLIPS.entrySet()) {
            String key = expected.getKey();
            JsonValue asset = generated.get(key);
            JsonValue record = audit.get("assets").get(key);
            assertNotNull(asset, key);
            assertNotNull(record, key);
            boolean hero = key.equals("hero_ceremony");
            assertEquals(hero ? "hero" : "world_tree", asset.getString("family"), key);
            // Phase 54 HD: hero 192->384, boss 256->384
            assertTrue(asset.getInt("frameSize") >= 192, key);
            assertEquals(hero ? "premium-humanoid-v2" : "segmented-world-tree-v2",
                asset.getString("rigProfile"), key);
            assertEquals(hero ? 25 : 13, asset.getInt("rigBoneCount"), key);
            assertTrue(Set.of("premium-v2" /* allow studio-v3 etc */, "studio-v3", "studio-v4-vibrant", "studio-v5-hd-pbr").contains(asset.getString("visualQuality")), key + " visualQuality=" + asset.getString("visualQuality"));
            assertTrue(asset.getInt("renderSupersample") >= 2, key);
            assertTrue(asset.getInt("renderSamples") >= 8, key);
            assertEquals("STRAIGHT_RGBA", asset.getString("alphaMode"), key);
            assertEquals(0.5f, asset.get("pivot").getFloat("x"), 0.0001f, key);
            assertEquals(hero ? 0.12f : 0.06f, asset.get("pivot").getFloat("y"), 0.0001f, key);
            if (hero) {
                assertEquals("planting-ceremony-v1", asset.getString("ceremonyIdentity"));
                assertEquals("ceremony_props", asset.getString("attachment_variant"));
                assertEquals("premium_elf_archer", asset.getString("silhouette"));
            } else {
                assertEquals("heartwood-sapling-v1", asset.getString("saplingIdentity"));
                assertEquals("seed", asset.getString("growsFrom"));
                assertTrue(asset.get("destructionClip").isNull(), "the sapling has no destruction clip");
            }
            assertEquals(expected.getValue().size(), asset.get("clips").size, key);
            for (Map.Entry<String, Integer> clip : expected.getValue().entrySet()) {
                assertEquals(clip.getValue(), asset.get("clips").get(clip.getKey()).size, key + "/" + clip.getKey());
                JsonValue clipRecord = record.get("clips").get(clip.getKey());
                assertEquals(clip.getValue(), clipRecord.getInt("frameCount"), key + "/" + clip.getKey());
                JsonValue margins = clipRecord.get("minimumAlphaMargins");
                for (String side : List.of("left", "top", "right", "bottom")) {
                    assertTrue(margins.getInt(side) >= 2, key + "/" + clip.getKey() + " " + side);
                }
            }

            JsonValue categoryReview = asset.get("categoryReview");
            assertEquals("ceremony", categoryReview.getString("category"), key);
            assertEquals("accepted", categoryReview.getString("status"), key);
            assertEquals(REVIEW_DOCUMENT, categoryReview.getString("document"), key);
            assertEquals(sha256(AUDIT), categoryReview.getString("auditSha256"), key);
            assertEquals(REVIEW_DOCUMENT, asset.getString("reviewDocument"), key);
            JsonValue metadata = parse(GENERATED.resolve("sprites/" + key + ".json"));
            assertEquals(asset.toJson(JsonWriter.OutputType.json),
                metadata.toJson(JsonWriter.OutputType.json), key);

            assertEquals(sha256(GENERATED.resolve(asset.getString("sheet"))),
                record.getString("candidateSheetSha256"), key);
            assertEquals(sha256(GENERATED.resolve(asset.getString("atlas"))),
                record.getString("candidateAtlasSha256"), key);

            String atlas = Files.readString(GENERATED.resolve(asset.getString("atlas")));
            for (Map.Entry<String, Integer> clip : expected.getValue().entrySet()) {
                long regions = atlas.lines().filter(line -> line.equals(key + "_" + clip.getKey())).count();
                assertEquals(clip.getValue().longValue(), regions, key + "_" + clip.getKey());
            }
        }
        for (String sheet : List.of(
            "hero_ceremony_full_motion.png", "world_tree_sapling_growth.png", "ceremony_arena_scale.png"
        )) {
            JsonValue recorded = audit.get("reviewSheets").get(sheet);
            assertNotNull(recorded, sheet);
            assertEquals(sha256(REVIEW_DIRECTORY.resolve(sheet)), recorded.getString("sha256"), sheet);
        }
    }

    private static Map<String, JsonValue> assetsByKey(JsonValue manifest) {
        Map<String, JsonValue> byKey = new HashMap<>();
        for (JsonValue asset = manifest.get("assets").child; asset != null; asset = asset.next) {
            byKey.put(asset.getString("key"), asset);
        }
        return byKey;
    }

    private static JsonValue parse(Path path) throws IOException {
        return new JsonReader().parse(Files.readString(path));
    }

    private static String sha256(Path path) throws IOException {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))
            );
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
