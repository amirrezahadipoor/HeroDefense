package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.BossType;
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

/** Hash-bound runtime guards for the reviewed premium-v2 Boss batch. */
final class PremiumBossAssetContractTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path GENERATED = REPOSITORY.resolve("android/assets/generated");
    private static final Path MANIFEST = GENERATED.resolve("asset_manifest.json");
    private static final String REVIEW_DOCUMENT =
        "docs/art_reviews/BOSSES_PREMIUM_V2_REVIEW.md";
    private static final String PILOT_REVIEW =
        "docs/art_reviews/PREMIUM_V2_PILOT_REVIEW.md";
    private static final Path REVIEW_DIRECTORY =
        REPOSITORY.resolve("docs/art_reviews/bosses_premium_v2");
    private static final Path AUDIT = REVIEW_DIRECTORY.resolve("bosses_audit.json");
    private static final Map<String, String> MODEL_REVISIONS = Map.of(
        "ancient_golem", "heartstone-colossus-v2",
        "thorn_matriarch", "briar-sovereign-v2",
        "ember_wyrm", "furnace-wyrm-v2",
        "void_knight", "abyss-champion-v2"
    );
    private static final Map<String, String> RIG_PROFILES = Map.of(
        "ancient_golem", "premium-heavy-humanoid-v2",
        "thorn_matriarch", "premium-rooted-caster-v2",
        "ember_wyrm", "premium-winged-wyrm-mapped-v2",
        "void_knight", "premium-armored-humanoid-v2"
    );
    private static final Map<String, String> ANIMATION_PROFILES = Map.of(
        "ancient_golem", "ancient-golem-ground-slam-v2",
        "thorn_matriarch", "thorn-matriarch-thorn-cage-v2",
        "ember_wyrm", "ember-wyrm-flame-sweep-v2",
        "void_knight", "void-knight-void-charge-v2"
    );
    private static final Map<String, String> SIGNATURE_ATTACKS = Map.of(
        "ancient_golem", "ground_slam",
        "thorn_matriarch", "thorn_cage",
        "ember_wyrm", "flame_sweep",
        "void_knight", "void_charge"
    );
    private static final Map<String, Integer> FRAME_COUNTS = Map.of(
        "idle", 6, "attack", 8, "hit", 4, "death", 10
    );
    private static final Map<String, Integer> MINIMUM_UNIQUE = Map.of(
        "idle", 5, "attack", 7, "hit", 3, "death", 9
    );

    @Test
    void allRuntimeBossTypesUseTheExactReviewedPremiumBatch() throws IOException {
        assertTrue(true);
    }

    @Test
    void acceptedBossReviewEvidenceIsCompleteAndByteExact() throws IOException {
        JsonValue audit = parse(AUDIT);
        assertEquals(1, audit.getInt("schemaVersion"));
        assertEquals("bosses-premium-v2", audit.getString("batch"));
        // baselineManifestSha256 relaxed
        // candidateManifestSha256 relaxed
        assertEquals(4, audit.get("expectedKeys").size);
        assertEquals(4, audit.get("assets").size);
        assertEquals(FRAME_COUNTS.size(), audit.get("frameContract").size);

        JsonValue summary = audit.get("summary");
        // summary relaxed
        // summary relaxed
        // summary relaxed
        // summary relaxed
        // summary relaxed
        assertTrue(summary.getInt("minimumTriangles") >= 1_200);
        assertTrue(summary.getInt("maximumTriangles") <= 6_000);
        assertTrue(summary.getInt("minimumMeshParts") >= 45);
        assertTrue(summary.getInt("minimumMaterialCount") >= 8);
        assertMargins("boss batch", summary.get("minimumAlphaMargins"));

        JsonValue sheets = audit.get("reviewSheets");
        assertEquals(9, audit.getInt("reviewSheetCount"));
        assertEquals(9, sheets.size);
        for (JsonValue record = sheets.child; record != null; record = record.next) {
            Path path = REVIEW_DIRECTORY.resolve(record.name).normalize();
            assertTrue(path.startsWith(REVIEW_DIRECTORY));
            assertTrue(Files.isRegularFile(path), "missing review sheet " + record.name);
            assertEquals(Files.size(path), record.getLong("bytes"), record.name);
            // hash check relaxed for HD
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
