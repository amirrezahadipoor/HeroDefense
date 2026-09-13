package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Hash-binds the accepted arena artifact, review evidence, and live composition contract. */
final class PremiumArenaAssetContractTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path GENERATED = REPOSITORY.resolve("android/assets/generated");
    private static final Path MANIFEST = GENERATED.resolve("asset_manifest.json");
    private static final String REVIEW_DOCUMENT =
        "docs/art_reviews/ARENA_PREMIUM_V2_REVIEW.md";
    private static final Path REVIEW_DIRECTORY =
        REPOSITORY.resolve("docs/art_reviews/arena_premium_v2");
    private static final Path AUDIT = REVIEW_DIRECTORY.resolve("arena_audit.json");
    private static final String AUDIT_SHA256 =
        "68ee03cba63c1a64f0f4cccd63aa98c28cc3c80eff5cf46afc5b6c8f12277320";
    private static final String SOURCE_MANIFEST_SHA256 =
        "98f19dd23591722cb8027ed7756beedcd69bbe94851bbd715faa547689f23801";
    private static final Set<String> EXPECTED_KEYS = Set.of(
        "arena_backdrop",
        "ground_tile_0", "ground_tile_1", "ground_tile_2",
        "crystal_prop_0", "crystal_prop_1", "crystal_prop_2"
    );
    private static final Map<String, String> SHEET_HASHES = Map.of(
        "arena_backdrop", "9cd32149c5ec5c16de0d5744d94dcdb4c9f73a10ac1030a063df78cf1e12cc86",
        "ground_tile_0", "727f587265e3d6251c1039da46c78af0e5851609fcc98662a560f90656e334c0",
        "ground_tile_1", "40a40973baea503feb3b08412345d5e75eb5d2f5d29b98c61ea720be2b69fbe7",
        "ground_tile_2", "de668bbcd8940cdaa66f7bbb6c2ac5dc648721113985ce2fd8cb0fc778b07b4b",
        "crystal_prop_0", "4b37a9885f2bb0a16ab8f82006b9aa8f93a787f27ef257e8686401f8a8db0bb2",
        "crystal_prop_1", "d70c35e11dd34b06de70e08c456ff275656bf5b6a3b924095eb5c7ee0ec57e20",
        "crystal_prop_2", "0c2411184ff4d3a8cbf8866ee65087c0ece866608cc8e7623322d8ac187a029c"
    );

    @Test
    void acceptedAuditAndEveryReviewSheetRemainHashBound() throws IOException {
        assertEquals(AUDIT_SHA256, sha256(AUDIT));
        String review = Files.readString(REPOSITORY.resolve(REVIEW_DOCUMENT));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(AUDIT_SHA256));
        assertTrue(review.contains(SOURCE_MANIFEST_SHA256));

        JsonValue audit = json(AUDIT);
        assertEquals("arena-premium-v2", audit.getString("batch"));
        assertEquals(SOURCE_MANIFEST_SHA256, audit.getString("candidateManifestSha256"));
        JsonValue summary = audit.get("summary");
        assertEquals(7, summary.getInt("assetCount"));
        assertEquals(7, summary.getInt("staticFrameCount"));
        assertEquals(1, summary.getInt("portraitBackdropCount"));
        assertEquals(3, summary.getInt("groundTileCount"));
        assertEquals(3, summary.getInt("crystalPropCount"));
        assertEquals(1_806_336L, summary.getLong("decodedBytes"));
        assertEquals(9, summary.getInt("minimumTransparentAssetMargin"));
        assertEquals(6, audit.getInt("reviewSheetCount"));
        for (JsonValue sheet = audit.get("reviewSheets").child;
             sheet != null; sheet = sheet.next) {
            Path path = REVIEW_DIRECTORY.resolve(sheet.name).normalize();
            assertTrue(path.startsWith(REVIEW_DIRECTORY));
            assertTrue(Files.isRegularFile(path), sheet.name);
            assertEquals(sheet.getString("sha256"), sha256(path), sheet.name);
            assertEquals(sheet.getLong("bytes"), Files.size(path), sheet.name);
        }
    }

    @Test
    void allSevenRuntimeAssetsRetainAcceptedPixelsAndPremiumMetadata() throws IOException {
        JsonValue manifest = json(MANIFEST);
        Map<String, JsonValue> byKey = assetsByKey(manifest);
        Map<String, JsonValue> audited = assetsByKey(json(AUDIT));
        assertEquals(EXPECTED_KEYS, audited.keySet());

        for (String key : EXPECTED_KEYS) {
            JsonValue asset = byKey.get(key);
            assertTrue(asset != null, key);
            assertEquals("environment", asset.getString("family"), key);
            assertEquals("premium-v2", asset.getString("visualQuality"), key);
            assertEquals(2, asset.getInt("renderSupersample"), key);
            assertEquals(16, asset.getInt("renderSamples"), key);
            assertEquals(REVIEW_DOCUMENT, asset.getString("reviewDocument"), key);
            JsonValue review = asset.get("categoryReview");
            assertEquals("arena_environment", review.getString("category"), key);
            assertEquals("accepted", review.getString("status"), key);
            assertEquals(AUDIT_SHA256, review.getString("auditSha256"), key);
            assertEquals(SOURCE_MANIFEST_SHA256, review.getString("sourceManifestSha256"), key);

            Path imagePath = GENERATED.resolve(asset.getString("sheet")).normalize();
            assertTrue(imagePath.startsWith(GENERATED));
            assertEquals(SHEET_HASHES.get(key), sha256(imagePath), key);
            assertEquals(SHEET_HASHES.get(key), audited.get(key).getString("sheetSha256"), key);
            JsonValue metadata = json(GENERATED.resolve("environment/" + key + ".json"));
            assertEquals(asset.toJson(JsonWriter.OutputType.json),
                metadata.toJson(JsonWriter.OutputType.json), key);

            if (key.startsWith("ground_tile_")) {
                assertEquals("arena-ground-premium-v2", asset.getString("modelRevision"), key);
                assertTrue(asset.getInt("triangles") >= 300 && asset.getInt("triangles") <= 600, key);
                assertTrue(asset.getInt("meshParts") >= 20, key);
                assertTrue(asset.getInt("materialCount") >= 6, key);
                assertTransparentMargins(imagePath, 4);
            } else if (key.startsWith("crystal_prop_")) {
                assertEquals("arena-crystal-premium-v2", asset.getString("modelRevision"), key);
                assertFalse(asset.getBoolean("runtimeGlow"), key);
                assertTrue(asset.getInt("triangles") >= 700 && asset.getInt("triangles") <= 2_200, key);
                assertTrue(asset.getInt("meshParts") >= 30, key);
                assertTrue(asset.getInt("materialCount") >= 8, key);
                assertTransparentMargins(imagePath, 4);
            }
        }
    }

    @Test
    void portraitBackdropIsFullBleedDarkAndCenterWeighted() throws IOException {
        JsonValue asset = assetsByKey(json(MANIFEST)).get("arena_backdrop");
        assertEquals("arena", asset.getString("frameClass"));
        assertEquals(360, asset.getInt("frameWidth"));
        assertEquals(640, asset.getInt("frameHeight"));
        assertEquals("forest-sanctuary-backdrop-v2", asset.getString("modelRevision"));
        assertEquals("portrait-clear-lane-v2", asset.getString("compositionProfile"));
        assertEquals(5, asset.getInt("depthBands"));
        assertTrue(asset.getInt("triangles") >= 1_000 && asset.getInt("triangles") <= 3_000);

        BufferedImage image = read(GENERATED.resolve(asset.getString("sheet")));
        assertEquals(360, image.getWidth());
        assertEquals(640, image.getHeight());
        long total = 0;
        long center = 0;
        long edges = 0;
        int centerCount = 0;
        int edgeCount = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                assertEquals(255, argb >>> 24, "backdrop alpha at " + x + "," + y);
                int value = luminance(argb);
                total += value;
                if (x >= 108 && x < 252 && y >= 96 && y < 576) {
                    center += value;
                    centerCount++;
                }
                if (x < 72 || x >= 288) {
                    edges += value;
                    edgeCount++;
                }
            }
        }
        double mean = total / (double) (image.getWidth() * image.getHeight());
        double centerMean = center / (double) centerCount;
        double edgeMean = edges / (double) edgeCount;
        assertTrue(mean >= 20.0 && mean <= 105.0, "restrained backdrop mean " + mean);
        assertTrue(centerMean >= edgeMean + 1.0,
            "center lane " + centerMean + " must read above edges " + edgeMean);
    }

    @Test
    void liveRendererUsesBackdropThenSparseDepthScaledPeripheralLandmarks() throws IOException {
        String source = Files.readString(REPOSITORY.resolve(
            "core/src/main/java/com/amirrezahadipoor/herodefense/render/ArenaEnvironmentRenderer.java"
        ));
        assertTrue(source.contains("generated/environment/arena_backdrop.png"));
        assertTrue(source.indexOf("batch.draw(\n            backdrop")
            < source.indexOf("drawGround(batch)"));
        assertTrue(source.contains("private static final float[][] GROUND_PLACEMENTS"));
        assertTrue(source.contains("private static final float[][] CRYSTAL_PLACEMENTS"));
        assertTrue(source.contains("for (float[] placement : GROUND_PLACEMENTS)"));
        assertTrue(source.contains("for (float[] placement : CRYSTAL_PLACEMENTS)"));
        assertTrue(source.contains("WorldLayout.REFERENCE_WIDTH"));
        assertTrue(source.contains("WorldLayout.REFERENCE_HEIGHT"));
        assertFalse(source.contains("for (int row = 0; row < 8; row++)"));
    }

    private static Map<String, JsonValue> assetsByKey(JsonValue document) {
        JsonValue assets = document.get("assets");
        Map<String, JsonValue> result = new HashMap<>();
        for (JsonValue asset = assets.child; asset != null; asset = asset.next) {
            String key = asset.getString("key");
            if (EXPECTED_KEYS.contains(key)) result.put(key, asset);
        }
        return result;
    }

    private static void assertTransparentMargins(Path path, int minimum) {
        BufferedImage image = read(path);
        int left = image.getWidth();
        int top = image.getHeight();
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 0) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        assertTrue(right >= left, path + " empty");
        assertTrue(left >= minimum, path + " left margin");
        assertTrue(top >= minimum, path + " top margin");
        assertTrue(image.getWidth() - right - 1 >= minimum, path + " right margin");
        assertTrue(image.getHeight() - bottom - 1 >= minimum, path + " bottom margin");
    }

    private static int luminance(int argb) {
        int red = (argb >>> 16) & 0xff;
        int green = (argb >>> 8) & 0xff;
        int blue = argb & 0xff;
        return (299 * red + 587 * green + 114 * blue) / 1_000;
    }

    private static JsonValue json(Path path) throws IOException {
        return new JsonReader().parse(Files.readString(path));
    }

    private static BufferedImage read(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) throw new IllegalStateException("Unreadable image " + path);
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }

    private static String sha256(Path path) throws IOException {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
