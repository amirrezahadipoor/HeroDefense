package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Hash-bound runtime guards for the reviewed premium-v2 regular-enemy batch. */
final class PremiumEnemyAssetContractTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path GENERATED = REPOSITORY.resolve("android/assets/generated");
    private static final Path MANIFEST = GENERATED.resolve("asset_manifest.json");
    private static final String REVIEW_DOCUMENT =
        "docs/art_reviews/ENEMIES_PREMIUM_V2_REVIEW.md";
    private static final String PILOT_REVIEW =
        "docs/art_reviews/PREMIUM_V2_PILOT_REVIEW.md";
    private static final Path REVIEW_DIRECTORY =
        REPOSITORY.resolve("docs/art_reviews/regular_enemies_premium_v2");
    private static final Path AUDIT = REVIEW_DIRECTORY.resolve("regular_enemies_audit.json");
    private static final Map<String, String> MODEL_REVISIONS = Map.of(
        "rootling", "rootling-thorn-scout-v2",
        "stonekin", "stonekin-rune-bulwark-v2",
        "gloom_wolf", "gloom-wolf-shadow-stalker-v2",
        "fungal_brute", "fungal-brute-spore-bruiser-v2"
    );
    private static final Map<String, String> RIG_PROFILES = Map.of(
        "rootling", "premium-humanoid-v2",
        "stonekin", "premium-heavy-humanoid-v2",
        "gloom_wolf", "premium-quadruped-mapped-v2",
        "fungal_brute", "premium-heavy-humanoid-v2"
    );
    private static final Map<String, String> ANIMATION_PROFILES = Map.of(
        "rootling", "rootling-skirmisher-v2",
        "stonekin", "stonekin-juggernaut-v2",
        "gloom_wolf", "gloom-wolf-pouncer-v2",
        "fungal_brute", "fungal-brute-brawler-v2"
    );
    private static final Map<String, Integer> FRAME_COUNTS = Map.of(
        "idle", 6, "attack", 8, "hit", 4, "death", 10
    );
    private static final Map<String, Integer> MINIMUM_UNIQUE = Map.of(
        "idle", 5, "attack", 7, "hit", 3, "death", 9
    );

    @Test
    void allRuntimeEnemyTypesUseTheExactReviewedPremiumBatch() throws IOException {
        assertTrue(Files.isRegularFile(REPOSITORY.resolve(REVIEW_DOCUMENT)));
        assertTrue(Files.isRegularFile(AUDIT));
        JsonValue manifest = parse(MANIFEST);
        JsonValue audit = parse(AUDIT);
        Map<String, JsonValue> generated = assetsByKey(manifest);
        Map<String, JsonValue> audited = assetsByKey(audit);

        Set<String> expectedKeys = new HashSet<>();
        for (EnemyType type : EnemyType.values()) expectedKeys.add(type.assetKey());
        assertEquals(MODEL_REVISIONS.keySet(), expectedKeys);
        assertEquals(expectedKeys, audited.keySet());
        Set<String> generatedEnemyKeys = new HashSet<>();
        for (JsonValue asset = manifest.get("assets").child; asset != null; asset = asset.next) {
            if ("enemy".equals(asset.getString("family"))) {
                generatedEnemyKeys.add(asset.getString("key"));
            }
        }
        assertEquals(expectedKeys, generatedEnemyKeys);

        Set<String> requiredBones = stringSet(manifest.get("requiredBones"));
        assertEquals(25, requiredBones.size());
        for (String key : expectedKeys) {
            JsonValue asset = generated.get(key);
            JsonValue record = audited.get(key);
            assertNotNull(asset, "missing generated enemy " + key);
            assertNotNull(record, "missing audited enemy " + key);
            assertEquals("enemy", asset.getString("family"), key);
            assertEquals(key, asset.getString("builder"), key);
            assertEquals("premium-v2", asset.getString("visualQuality"), key);
            assertEquals(MODEL_REVISIONS.get(key), asset.getString("modelRevision"), key);
            assertEquals(RIG_PROFILES.get(key), asset.getString("rigProfile"), key);
            assertEquals(ANIMATION_PROFILES.get(key), asset.getString("animationProfile"), key);
            assertEquals(2, asset.getInt("renderSupersample"), key);
            assertEquals(16, asset.getInt("renderSamples"), key);
            assertEquals(12, asset.getInt("frameRate"), key);
            assertEquals(192, asset.getInt("frameSize"), key);
            assertEquals(1_920, asset.getInt("sheetWidth"), key);
            assertEquals(768, asset.getInt("sheetHeight"), key);
            assertEquals(1, asset.get("sheets").size, key);
            assertEquals(25, asset.getInt("rigBoneCount"), key);
            assertEquals(requiredBones, stringSet(asset.get("bones")), key);
            assertTrue(asset.getBoolean("boneAnimated"), key);
            assertTrue(asset.getInt("triangles") >= 900 && asset.getInt("triangles") <= 4_000, key);
            assertTrue(asset.getInt("meshParts") >= 32, key);
            assertTrue(asset.getInt("materialCount") >= 6, key);
            assertTrue(!asset.getString("silhouette").isBlank(), key);
            assertTrue(!asset.getString("materialStory").isBlank(), key);
            assertEquals(0.5f, asset.get("pivot").getFloat("x"), 0.0001f, key);
            assertEquals(0.12f, asset.get("pivot").getFloat("y"), 0.0001f, key);

            JsonValue review = asset.get("categoryReview");
            assertEquals("regular-enemies", review.getString("category"), key);
            assertEquals("accepted", review.getString("status"), key);
            assertEquals(REVIEW_DOCUMENT, review.getString("document"), key);
            assertEquals(REVIEW_DOCUMENT, asset.getString("reviewDocument"), key);
            if ("rootling".equals(key)) {
                assertEquals(PILOT_REVIEW, asset.getString("pilotReviewDocument"));
            }

            JsonValue metadata = parse(GENERATED.resolve("sprites/" + key + ".json"));
            assertEquals(REVIEW_DOCUMENT, metadata.getString("reviewDocument"), key);
            assertEquals("accepted", metadata.get("categoryReview").getString("status"), key);
            assertEquals(asset.getString("sheet"), metadata.getString("sheet"), key);
            assertEquals(asset.getString("atlas"), metadata.getString("atlas"), key);

            assertEquals(asset.getInt("triangles"), record.getInt("triangles"), key);
            assertEquals(asset.getInt("meshParts"), record.getInt("meshParts"), key);
            assertEquals(asset.getInt("materialCount"), record.getInt("materialCount"), key);
            assertEquals(25, record.getInt("rigBoneCount"), key);
            assertMargins(key, record.get("minimumAlphaMargins"));
            for (Map.Entry<String, Integer> clip : FRAME_COUNTS.entrySet()) {
                JsonValue clipRecord = record.get("clips").get(clip.getKey());
                assertEquals(clip.getValue().intValue(), clipRecord.getInt("frameCount"), key);
                assertTrue(
                    clipRecord.getInt("uniqueVisibleFrames") >= MINIMUM_UNIQUE.get(clip.getKey()),
                    key + " " + clip.getKey()
                );
                assertMargins(key + " " + clip.getKey(), clipRecord.get("minimumAlphaMargins"));
                assertEquals(clip.getValue().intValue(), asset.get("clips").get(clip.getKey()).size, key);
            }
            assertEquals(
                sha256(resolveGenerated(asset.getString("sheet"))),
                record.getString("candidateSheetSha256"),
                key
            );
            assertEquals(
                sha256(resolveGenerated(asset.getString("atlas"))),
                record.getString("candidateAtlasSha256"),
                key
            );
            assertEquals(64, record.getString("candidateMetadataSha256").length(), key);
            assertEquals(64, record.getString("baselineSheetSha256").length(), key);
        }
    }

    @Test
    void acceptedEnemyReviewEvidenceIsCompleteAndByteExact() throws IOException {
        JsonValue audit = parse(AUDIT);
        assertEquals(1, audit.getInt("schemaVersion"));
        assertEquals("regular-enemies-premium-v2", audit.getString("batch"));
        assertEquals(64, audit.getString("baselineManifestSha256").length());
        assertEquals(64, audit.getString("candidateManifestSha256").length());
        assertEquals(4, audit.get("expectedKeys").size);
        assertEquals(4, audit.get("assets").size);
        assertEquals(FRAME_COUNTS.size(), audit.get("frameContract").size);

        JsonValue summary = audit.get("summary");
        assertEquals(4, summary.getInt("assetCount"));
        assertEquals(112, summary.getInt("frameCount"));
        assertEquals(4, summary.getInt("singlePageAtlasCount"));
        assertEquals(23_592_960L, summary.getLong("decodedBytes"));
        assertEquals(33_554_432L, summary.getLong("decodedBudgetBytes"));
        assertTrue(summary.getInt("minimumTriangles") >= 900);
        assertTrue(summary.getInt("maximumTriangles") <= 4_000);
        assertTrue(summary.getInt("minimumMeshParts") >= 32);
        assertTrue(summary.getInt("minimumMaterialCount") >= 6);
        assertMargins("enemy batch", summary.get("minimumAlphaMargins"));

        JsonValue sheets = audit.get("reviewSheets");
        assertEquals(9, audit.getInt("reviewSheetCount"));
        assertEquals(9, sheets.size);
        for (JsonValue record = sheets.child; record != null; record = record.next) {
            Path path = REVIEW_DIRECTORY.resolve(record.name).normalize();
            assertTrue(path.startsWith(REVIEW_DIRECTORY));
            assertTrue(Files.isRegularFile(path), "missing review sheet " + record.name);
            assertEquals(Files.size(path), record.getLong("bytes"), record.name);
            assertEquals(sha256(path), record.getString("sha256"), record.name);
        }
    }

    private static Map<String, JsonValue> assetsByKey(JsonValue root) {
        Map<String, JsonValue> result = new HashMap<>();
        for (JsonValue asset = root.get("assets").child; asset != null; asset = asset.next) {
            assertTrue(result.put(asset.getString("key"), asset) == null,
                "duplicate asset " + asset.getString("key"));
        }
        return result;
    }

    private static Set<String> stringSet(JsonValue values) {
        Set<String> result = new HashSet<>();
        for (JsonValue value = values.child; value != null; value = value.next) {
            result.add(value.asString());
        }
        return result;
    }

    private static void assertMargins(String label, JsonValue margins) {
        assertNotNull(margins, label + " missing margins");
        for (String edge : List.of("left", "top", "right", "bottom")) {
            assertTrue(margins.getInt(edge) >= 2, label + " approaches " + edge + " boundary");
        }
    }

    private static Path resolveGenerated(String relative) {
        Path path = GENERATED.resolve(relative).normalize();
        assertTrue(path.startsWith(GENERATED), "generated path escapes root: " + relative);
        assertTrue(Files.isRegularFile(path), "missing generated asset " + path);
        return path;
    }

    private static JsonValue parse(Path path) throws IOException {
        return new JsonReader().parse(Files.readString(path));
    }

    private static String sha256(Path path) throws IOException {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
