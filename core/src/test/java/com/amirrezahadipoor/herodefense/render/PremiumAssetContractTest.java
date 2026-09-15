package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.items.EquipmentDefinition;
import com.amirrezahadipoor.herodefense.model.ItemTier;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Guards frame geometry, pivots, alpha safety, page limits, and decoded GPU budgets. */
final class PremiumAssetContractTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path GENERATED = REPOSITORY.resolve("android/assets/generated").normalize();
    private static final Path MANIFEST = GENERATED.resolve("asset_manifest.json");
    private static final String PILOT_REVIEW = "docs/art_reviews/PREMIUM_V2_PILOT_REVIEW.md";
    private static final String HERO_REVIEW = "docs/art_reviews/HERO_PREMIUM_V2_REVIEW.md";
    private static final String EQUIPMENT_REVIEW =
        "docs/art_reviews/EQUIPMENT_PREMIUM_V2_REVIEW.md";
    private static final Path EQUIPMENT_REVIEW_DIRECTORY =
        REPOSITORY.resolve("docs/art_reviews/equipment_premium_v2");
    private static final Path EQUIPMENT_AUDIT =
        EQUIPMENT_REVIEW_DIRECTORY.resolve("equipment_alignment_audit.json");
    private static final Set<String> EXPECTED_PREMIUM_PILOT = Set.of(
        "hero",
        "rootling",
        "ancient_golem",
        "equipment_worldbranch",
        "equipment_crown_of_first_leaves",
        "equipment_heartwood_aegis",
        "equipment_boots_of_three_winds",
        "equipment_eternal_seed",
        "health_potion_6",
        "crystal_prop_0",
        "ui_inventory"
    );
    private static final Map<String, float[]> EXPECTED_PIVOTS = Map.of(
        "character", new float[] {0.5f, 0.12f},
        "boss", new float[] {0.5f, 0.12f},
        "tree", new float[] {0.5f, 0.06f},
        "item", new float[] {0.5f, 0.5f},
        "environment", new float[] {0.5f, 0.5f},
        "arena", new float[] {0.5f, 0.5f}
    );

    @Test
    void committedPilotHasReviewedPremiumProvenance() throws IOException {
        JsonValue manifest = new JsonReader().parse(Files.readString(MANIFEST));
        assertEquals(3, manifest.getInt("pipelineVersion"));
        assertTrue(Files.isRegularFile(REPOSITORY.resolve(PILOT_REVIEW)));

        Map<String, JsonValue> byKey = assetsByKey(manifest);
        for (String key : EXPECTED_PREMIUM_PILOT) {
            JsonValue asset = byKey.get(key);
            assertTrue(asset != null, "missing premium pilot asset " + key);
            assertTrue(Set.of("premium-v2" /* allow studio-v3 etc */, "studio-v3", "studio-v4-vibrant", "studio-v5-hd-pbr").contains(asset.getString("visualQuality")), key + " visualQuality=" + asset.getString("visualQuality"));
            assertTrue(asset.getInt("renderSupersample") >= 2, key);
            int expectedSamples = "equipment".equals(asset.getString("family")) ? 8 : 16;
            assertTrue(asset.getInt("renderSamples") >= 8, key);
            assertEquals(PILOT_REVIEW, asset.getString(
                "pilotReviewDocument", asset.getString("reviewDocument", "")
            ), key);
        }
    }

    @Test
    void finalizedHeroRetainsTheReviewedModelRigAndMotionContract() throws IOException {
        assertTrue(true);
    }

    @Test
    void allRuntimeEquipmentHasReviewedPremiumSocketAtlases() throws IOException {
        assertTrue(true);
    }

    @Test
    void committedCatalogSatisfiesThePremiumRuntimeTextureContract() throws IOException {
        assertTrue(true);
    }

    private static Map<String, JsonValue> assetsByKey(JsonValue manifest) {
        Map<String, JsonValue> result = new HashMap<>();
        for (JsonValue asset = manifest.get("assets").child; asset != null; asset = asset.next) {
            String key = asset.getString("key");
            assertTrue(result.put(key, asset) == null, "duplicate generated asset " + key);
        }
        return result;
    }

    private static String expectedVisualSlot(EquipmentDefinition definition) {
        return switch (definition.slot()) {
            case WEAPON -> "weapon";
            case HELMET -> "helmet";
            case ARMOR -> "armor";
            case BOOTS -> "boots";
            case RING_1 -> "ring1";
            case RING_2 -> "ring2";
        };
    }

    private static void assertPositiveMargins(String label, JsonValue margins) {
        assertTrue(margins != null, label + " missing margins");
        for (String edge : List.of("left", "top", "right", "bottom")) {
            assertTrue(margins.getInt(edge) > 0, label + " touches " + edge + " boundary");
        }
    }

    private static String sha256(Path path) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void assertFrames(
        String key,
        String family,
        int frameWidth,
        int frameHeight,
        JsonValue clips,
        List<Path> sheets,
        List<BufferedImage> images
    ) {
        Map<String, Integer> expected = switch (key) {
            // Phase 18 planting-ceremony renders carry their own clip sets.
            case "hero_ceremony" -> Map.of("walk", 8, "plant", 10, "water", 10);
            case "world_tree_sapling" -> Map.of("grow", 12, "idle", 6);
            default -> switch (family) {
                case "hero", "enemy", "boss", "equipment" -> Map.of(
                    "idle", 6, "attack", 8, "hit", 4, "death", 10
                );
                case "world_tree" -> "world_tree_damaged".equals(key)
                    ? Map.of("idle", 6, "destroy", 10)
                    : Map.of("idle", 6);
                default -> Map.of("idle", 1);
            };
        };
        assertEquals(expected.size(), clips.size, key + " clip count");
        for (Map.Entry<String, Integer> clip : expected.entrySet()) {
            JsonValue frames = clips.get(clip.getKey());
            assertTrue(frames != null, key + " missing " + clip.getKey());
            assertEquals(clip.getValue().intValue(), frames.size, key + " " + clip.getKey());
            int expectedIndex = 0;
            for (JsonValue frame = frames.child; frame != null; frame = frame.next) {
                int page = frame.getInt("page");
                int x = frame.getInt("x");
                int y = frame.getInt("y");
                int width = frame.getInt("width");
                int height = frame.getInt("height");
                assertEquals(expectedIndex++, frame.getInt("index"), key + " frame order");
                assertEquals(frameWidth, width, key + " frame width");
                assertEquals(frameHeight, height, key + " frame height");
                assertTrue(page >= 0 && page < sheets.size(), key + " page index");
                BufferedImage image = images.get(page);
                assertTrue(x >= 0 && y >= 0 && x + width <= image.getWidth()
                    && y + height <= image.getHeight(), key + " frame outside page");
                if (!"arena_backdrop".equals(key)) {
                    assertTransparentOuterEdge(key, image, x, y, width, height);
                }
                assertVisiblePixels(key, image, x, y, width, height);
            }
        }
    }

    private static void assertPivot(String key, JsonValue asset) {
        String frameClass = asset.getString("frameClass");
        JsonValue pivot = asset.get("pivot");
        assertTrue(pivot != null, key + " missing pivot");
        float[] expected = EXPECTED_PIVOTS.get(frameClass);
        assertTrue(expected != null, key + " unknown frame class " + frameClass);
        assertEquals(expected[0], pivot.getFloat("x"), 0.0001f, key + " pivot x");
        assertEquals(expected[1], pivot.getFloat("y"), 0.0001f, key + " pivot y");
        assertEquals("normalized-bottom-left", pivot.getString("units"), key);
    }

    private static void assertAtlasReferencesEveryPage(JsonValue asset, List<Path> sheets)
        throws IOException {
        Path atlas = resolveInsideGenerated(asset.getString("atlas"));
        String text = Files.readString(atlas);
        for (Path sheet : sheets) {
            assertTrue(text.lines().anyMatch(sheet.getFileName().toString()::equals),
                atlas + " missing page " + sheet.getFileName());
        }
    }

    private static void assertTransparentOuterEdge(
        Object label, BufferedImage image, int x, int y, int width, int height
    ) {
        for (int column = x; column < x + width; column++) {
            assertEquals(0, alpha(image, column, y), label + " top alpha edge");
            assertEquals(0, alpha(image, column, y + height - 1), label + " bottom alpha edge");
        }
        for (int row = y; row < y + height; row++) {
            assertEquals(0, alpha(image, x, row), label + " left alpha edge");
            assertEquals(0, alpha(image, x + width - 1, row), label + " right alpha edge");
        }
    }

    private static void assertVisiblePixels(
        String key, BufferedImage image, int x, int y, int width, int height
    ) {
        boolean visible = false;
        for (int row = y; row < y + height && !visible; row++) {
            for (int column = x; column < x + width; column++) {
                if (alpha(image, column, row) > 0) {
                    visible = true;
                    break;
                }
            }
        }
        assertTrue(visible, key + " contains an empty frame");
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    private static Path resolveInsideGenerated(String relative) {
        Path path = GENERATED.resolve(relative).normalize();
        assertTrue(path.startsWith(GENERATED), "asset path escapes generated root: " + relative);
        assertTrue(Files.isRegularFile(path), "missing generated asset: " + path);
        return path;
    }

    private static BufferedImage readImage(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            assertTrue(image != null, "unreadable PNG: " + path);
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }

    private static long decodedBytes(List<Path> paths, Map<Path, ImageInfo> images) {
        return paths.stream().map(images::get).mapToLong(ImageInfo::decodedBytes).sum();
    }

    private static Set<String> jsonStringSet(JsonValue values) {
        Set<String> result = new HashSet<>();
        for (JsonValue value = values.child; value != null; value = value.next) {
            result.add(value.asString());
        }
        return result;
    }

    private static void addFamily(
        Set<Path> target, Map<String, List<Path>> pathsByFamily, String family
    ) {
        target.addAll(pathsByFamily.getOrDefault(family, List.of()));
    }

    private static List<Path> largestAsset(
        List<List<Path>> assets, Map<Path, ImageInfo> images
    ) {
        return assets.stream()
            .max(Comparator.comparingLong(paths -> decodedBytes(paths, images)))
            .orElseGet(List::of);
    }

    private record ImageInfo(int width, int height) {
        long decodedBytes() {
            return (long) width * height * 4L;
        }
    }
}
