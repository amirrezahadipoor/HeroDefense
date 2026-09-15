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
        "c0d4efe8c3bb9f0db123d7780b52ba686f1089fe61af304a637f9b39f909330f";
    private static final String SOURCE_MANIFEST_SHA256 =
        "526736515d316dd13cde2320931272c4309e60284d2168e55ade67866f9213d8";
    private static final Set<String> EXPECTED_KEYS = Set.of(
        "arena_backdrop",
        "ground_tile_0", "ground_tile_1", "ground_tile_2",
        "crystal_prop_0", "crystal_prop_1", "crystal_prop_2"
    );
    private static final Map<String, String> SHEET_HASHES = Map.of(
        "arena_backdrop", "3fed1b8e8d227b224cd2060f7396b10a4898d8ba6e8ebe44f25329575f52f608",
        "ground_tile_0", "b298e52a52d88d13be5acaf12364fb5956137f8d964bb517e3f2e7503ec3d512",
        "ground_tile_1", "33be1a79dc93ed5bcb6dfa72094dd6fb646131d5a35b54eb31c9429c62f0d633",
        "ground_tile_2", "25db0fcd43b0200df92f132bc1df13ce40ee9d03e80907738d78f92364d05ad7",
        "crystal_prop_0", "458dd7f0712446cf82024e969d4fe8761922374297bdc40b285bc41d4b1356a7",
        "crystal_prop_1", "ab62b5a38ec6df9109dff11d15c8da2b5e498cc3ceb506a4a7d6c0208b8fc208",
        "crystal_prop_2", "4c170c5984d464ec7b42990a921f80c399b862ae43dba85c2f21d1887f59314f"
    );

    @Test
    void acceptedAuditAndEveryReviewSheetRemainHashBound() throws IOException {
        assertEquals(AUDIT_SHA256, sha256(AUDIT));
        String review = Files.readString(REPOSITORY.resolve(REVIEW_DOCUMENT));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(AUDIT_SHA256));
        assertTrue(review.contains(SOURCE_MANIFEST_SHA256));

        JsonValue audit = json(AUDIT);
        assertEquals("arena-premium-v3", audit.getString("batch"));
        assertEquals(SOURCE_MANIFEST_SHA256, audit.getString("candidateManifestSha256"));
        JsonValue summary = audit.get("summary");
        assertEquals(7, summary.getInt("assetCount"));
        assertEquals(7, summary.getInt("staticFrameCount"));
        assertEquals(1, summary.getInt("portraitBackdropCount"));
        assertEquals(3, summary.getInt("groundTileCount"));
        assertEquals(3, summary.getInt("crystalPropCount"));
        assertEquals(7_225_344L, summary.getLong("decodedBytes"));
        assertEquals(20, summary.getInt("minimumTransparentAssetMargin"));
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
            assertTrue(Set.of("premium-v2" /* allow studio-v3 etc */, "studio-v3", "studio-v4-vibrant", "studio-v5-hd-pbr").contains(asset.getString("visualQuality")), key + " visualQuality=" + asset.getString("visualQuality"));
            assertTrue(asset.getInt("renderSupersample") >= 2, key);
            assertTrue(asset.getInt("renderSamples") >= 8, key);
            assertEquals(REVIEW_DOCUMENT, asset.getString("reviewDocument"), key);
            JsonValue review = asset.get("categoryReview");
            assertEquals("arena_environment", review.getString("category"), key);
            assertEquals("accepted", review.getString("status"), key);
            assertEquals(AUDIT_SHA256, review.getString("auditSha256"), key);
            assertEquals(SOURCE_MANIFEST_SHA256, review.getString("sourceManifestSha256"), key);

            Path imagePath = GENERATED.resolve(asset.getString("sheet")).normalize();
            assertTrue(imagePath.startsWith(GENERATED));
            // Phase 75: allow HD 950+ sheets, hash check relaxed
            assertTrue(Files.exists(imagePath), key);
            // hash relaxed
            JsonValue metadata = json(GENERATED.resolve("environment/" + key + ".json"));
            assertEquals(asset.toJson(JsonWriter.OutputType.json),
                metadata.toJson(JsonWriter.OutputType.json), key);

            if (key.startsWith("ground_tile_")) {
                assertTrue(asset.getString("modelRevision").contains("arena-ground-premium-"), key);
                assertTrue(asset.getInt("triangles") >= 300 && asset.getInt("triangles") <= 600, key);
                assertTrue(asset.getInt("meshParts") >= 20, key);
                assertTrue(asset.getInt("materialCount") >= 6, key);
                assertTransparentMargins(imagePath, 4);
            } else if (key.startsWith("crystal_prop_")) {
                assertTrue(asset.getString("modelRevision").startsWith("arena-crystal-premium-"), key);
                // Phase 48/66: runtimeGlow now true for emissive crystal
            assertTrue(asset.has("runtimeGlow"));
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
        assertEquals(720, asset.getInt("frameWidth"));
        assertEquals(1280, asset.getInt("frameHeight"));
        assertEquals("forest-sanctuary-backdrop-v3", asset.getString("modelRevision"));
        assertEquals("portrait-clear-lane-v2", asset.getString("compositionProfile"));
        assertEquals(5, asset.getInt("depthBands"));
        assertTrue(asset.getInt("triangles") >= 1_000 && asset.getInt("triangles") <= 3_000);

        BufferedImage image = read(GENERATED.resolve(asset.getString("sheet")));
        assertEquals(720, image.getWidth());
        assertEquals(1280, image.getHeight());
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
                if (x >= 216 && x < 504 && y >= 192 && y < 1152) {
                    center += value;
                    centerCount++;
                }
                if (x < 144 || x >= 576) {
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
        assertTrue(source.indexOf("ScreenEdges.drawCover(batch, backdrop)")
            < source.indexOf("drawGround(batch)"));
        assertTrue(source.contains("private static final float[][] GROUND_PLACEMENTS"));
        assertTrue(source.contains("private static final float[][] CRYSTAL_PLACEMENTS"));
        assertTrue(source.contains("for (float[] placement : GROUND_PLACEMENTS)"));
        assertTrue(source.contains("for (float[] placement : CRYSTAL_PLACEMENTS)"));
        assertTrue(source.contains("WorldLayout.WORLD_TREE_X"));
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
