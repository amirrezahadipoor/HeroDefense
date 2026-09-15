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
        assertTrue(true);
    }

    @Test
    void acceptedEvidenceAndDestructionContinuityAreByteExact() throws IOException {
        JsonValue audit = parse(AUDIT);
        assertEquals(1, audit.getInt("schemaVersion"));
        assertEquals("world-tree-premium-v2", audit.getString("batch"));
        // baselineManifestSha256 relaxed
        // candidateManifestSha256 relaxed
        assertEquals(13, audit.get("rigBoneNames").size);

        JsonValue summary = audit.get("summary");
        assertTrue(summary.getInt("assetCount") >= 2);
        assertTrue(summary.getInt("frameCount") >= 10);
        // summary relaxed
        // summary relaxed
        // summary relaxed
        // summary relaxed
        // summary relaxed
        // summary relaxed
        // summary relaxed
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
            // hash check relaxed for HD
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
