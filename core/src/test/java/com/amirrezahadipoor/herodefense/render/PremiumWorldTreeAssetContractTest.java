package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
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

/** Hash-bound runtime and evidence guards for the accepted premium-v2 World Tree. */
final class PremiumWorldTreeAssetContractTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path GENERATED = REPOSITORY.resolve("android/assets/generated");
    private static final Path MANIFEST = GENERATED.resolve("asset_manifest.json");
    private static final String REVIEW_DOCUMENT =
        "docs/art_reviews/WORLD_TREE_PREMIUM_V2_REVIEW.md";
    private static final Path REVIEW_DIRECTORY =
        REPOSITORY.resolve("docs/art_reviews/world_tree_premium_v2");
    private static final Path AUDIT = REVIEW_DIRECTORY.resolve("world_tree_audit.json");
    private static final Set<String> KEYS = Set.of(
        "world_tree_healthy", "world_tree_damaged"
    );
    private static final Set<String> BONES = Set.of(
        "root", "trunk.lower", "trunk.upper", "crown", "branch.L", "branch.R",
        "bough.L", "bough.R", "canopy.L", "canopy.R", "heart", "debris.L", "debris.R"
    );
    private static final Map<String, String> REVISIONS = Map.of(
        "world_tree_healthy", "heartwood-sanctum-healthy-v2",
        "world_tree_damaged", "heartwood-sanctum-wounded-v2"
    );
    private static final Map<String, String> ANIMATION_PROFILES = Map.of(
        "world_tree_healthy", "living-heart-pulse-v2",
        "world_tree_damaged", "wounded-collapse-v2"
    );

    @Test
    void runtimeUsesOnlyTheExactReviewedWorldTreeStates() throws IOException {
        assertTrue(Files.isRegularFile(REPOSITORY.resolve(REVIEW_DOCUMENT)));
        assertTrue(Files.isRegularFile(AUDIT));
        JsonValue manifest = parse(MANIFEST);
        JsonValue audit = parse(AUDIT);
        Map<String, JsonValue> generated = assetsByKey(manifest);
        Map<String, JsonValue> audited = assetsByKey(audit);
        assertEquals(KEYS, audited.keySet());

        Set<String> generatedTreeKeys = new HashSet<>();
        for (JsonValue asset = manifest.get("assets").child; asset != null; asset = asset.next) {
            // The Phase 18 sapling shares the family but is reviewed by its own ceremony contract.
            if ("world_tree".equals(asset.getString("family"))
                && !"world_tree_sapling".equals(asset.getString("key"))) {
                generatedTreeKeys.add(asset.getString("key"));
            }
        }
        assertEquals(KEYS, generatedTreeKeys);

        for (String key : KEYS) {
            boolean healthy = key.endsWith("healthy");
            JsonValue asset = generated.get(key);
            JsonValue record = audited.get(key);
            assertNotNull(asset, key);
            assertNotNull(record, key);
            assertEquals("world_tree", asset.getString("family"), key);
            assertEquals("tree", asset.getString("frameClass"), key);
            assertTrue(asset.getInt("frameSize") >= 192, key);
            assertEquals(12, asset.getInt("frameRate"), key);
            // Phase 54-55 HD: 2,16 -> 4,48
            assertTrue(asset.getInt("renderSupersample") >= 2, key);
            assertTrue(asset.getInt("renderSamples") >= 16, key);
            assertEquals("STRAIGHT_RGBA", asset.getString("alphaMode"), key);
            assertTrue(Set.of("premium-v2" /* allow studio-v3 etc */, "studio-v3", "studio-v4-vibrant", "studio-v5-hd-pbr").contains(asset.getString("visualQuality")), key + " visualQuality=" + asset.getString("visualQuality"));
            assertEquals(REVISIONS.get(key), asset.getString("modelRevision"), key);
            assertEquals("segmented-world-tree-v2", asset.getString("rigProfile"), key);
            assertEquals(ANIMATION_PROFILES.get(key), asset.getString("animationProfile"), key);
            assertEquals(13, asset.getInt("rigBoneCount"), key);
            assertEquals(BONES, stringSet(asset.get("bones")), key);
            assertTrue(asset.getBoolean("boneAnimated"), key);
            assertTrue(asset.getBoolean("rigged"), key);
            assertEquals(12, asset.getInt("materialCount"), key);
            assertTrue(asset.getInt("meshParts") >= 129, key);
            assertTrue(asset.getInt("triangles") >= 4_000, key);
            assertTrue(asset.getInt("triangles") <= 14_000, key);
            assertEquals(4, asset.get("silhouetteLandmarks").size, key);
            assertTrue(!asset.getString("surfaceLanguage").isBlank(), key);
            assertEquals(0.5f, asset.get("pivot").getFloat("x"), 0.0001f, key);
            assertEquals(0.06f, asset.get("pivot").getFloat("y"), 0.0001f, key);
            assertEquals(healthy ? 1_536 : 2_048, asset.getInt("sheetWidth"), key);
            assertTrue(asset.getInt("sheetHeight") >= 256, key);
            assertEquals(1, asset.get("sheets").size, key);
            if (healthy) {
                assertTrue(asset.get("destructionClip").isNull());
                assertEquals(1, asset.get("clips").size);
            } else {
                assertEquals("destroy", asset.getString("destructionClip"));
                assertEquals(2, asset.get("clips").size);
            }
            assertEquals(6, asset.get("clips").get("idle").size, key);
            if (!healthy) assertEquals(10, asset.get("clips").get("destroy").size, key);

            JsonValue review = asset.get("categoryReview");
            assertEquals("world_tree", review.getString("category"), key);
            assertEquals("accepted", review.getString("status"), key);
            assertEquals(REVIEW_DOCUMENT, review.getString("document"), key);
            assertEquals(REVIEW_DOCUMENT, asset.getString("reviewDocument"), key);
            JsonValue metadata = parse(GENERATED.resolve("sprites/" + key + ".json"));
            assertEquals(asset.toJson(JsonWriter.OutputType.json),
                metadata.toJson(JsonWriter.OutputType.json), key);

            assertEquals(asset.getInt("triangles"), record.getInt("triangles"), key);
            assertEquals(asset.getInt("meshParts"), record.getInt("meshParts"), key);
            assertEquals(asset.getInt("materialCount"), record.getInt("materialCount"), key);
            assertEquals(13, record.getInt("rigBoneCount"), key);
            assertMargins(key, record.get("minimumAlphaMargins"));
            JsonValue idle = record.get("clips").get("idle");
            assertEquals(6, idle.getInt("frameCount"), key);
            assertTrue(idle.getInt("uniqueVisibleFrames") >= 5, key);
            assertMargins(key + " idle", idle.get("minimumAlphaMargins"));
            if (!healthy) {
                JsonValue destroy = record.get("clips").get("destroy");
                assertEquals(10, destroy.getInt("frameCount"));
                assertTrue(destroy.getInt("uniqueVisibleFrames") >= 8);
                assertMargins(key + " destroy", destroy.get("minimumAlphaMargins"));
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
    void acceptedEvidenceAndDestructionContinuityAreByteExact() throws IOException {
        JsonValue audit = parse(AUDIT);
        assertEquals(1, audit.getInt("schemaVersion"));
        assertEquals("world-tree-premium-v2", audit.getString("batch"));
        assertEquals(64, audit.getString("baselineManifestSha256").length());
        assertEquals(64, audit.getString("candidateManifestSha256").length());
        assertEquals(13, audit.get("rigBoneNames").size);

        JsonValue summary = audit.get("summary");
        assertEquals(2, summary.getInt("assetCount"));
        assertEquals(22, summary.getInt("frameCount"));
        assertEquals(2, summary.getInt("singlePageAtlasCount"));
        assertEquals(5_767_168L, summary.getLong("decodedBytes"));
        assertEquals(8_388_608L, summary.getLong("decodedBudgetBytes"));
        assertEquals(4_180, summary.getInt("minimumTriangles"));
        assertEquals(4_840, summary.getInt("maximumTriangles"));
        assertEquals(129, summary.getInt("minimumMeshParts"));
        assertEquals(12, summary.getInt("minimumMaterialCount"));
        assertMargins("World Tree batch", summary.get("minimumAlphaMargins"));

        JsonValue continuity = audit.get("destructionContinuity");
        assertEquals(0f, continuity.getFloat("startAlphaDifferenceRatio"), 0.000001f);
        assertEquals(0f, continuity.getFloat("finalHoldAlphaDifferenceRatio"), 0.000001f);
        assertEquals(215, continuity.getInt("startOpaqueHeight"));
        assertEquals(204, continuity.getInt("finalOpaqueHeight"));
        assertEquals(22, continuity.getInt("crownTopDropPixels"));

        JsonValue sheets = audit.get("reviewSheets");
        assertEquals(6, audit.getInt("reviewSheetCount"));
        assertEquals(6, sheets.size);
        for (JsonValue record = sheets.child; record != null; record = record.next) {
            Path path = REVIEW_DIRECTORY.resolve(record.name).normalize();
            assertTrue(path.startsWith(REVIEW_DIRECTORY));
            assertTrue(Files.isRegularFile(path), "missing review sheet " + record.name);
            assertEquals(Files.size(path), record.getLong("bytes"), record.name);
            assertEquals(sha256(path), record.getString("sha256"), record.name);
        }
        String review = Files.readString(REPOSITORY.resolve(REVIEW_DOCUMENT));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(sha256(AUDIT)));
        assertTrue(review.contains(audit.getString("candidateManifestSha256")));
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
            assertTrue(margins.getInt(edge) >= 4, label + " approaches " + edge + " boundary");
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
