package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Guards the exact accepted icon/state artifact and its touch-driven runtime use. */
final class PremiumUiAssetContractTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path GENERATED = REPOSITORY.resolve("android/assets/generated");
    private static final Path MANIFEST = GENERATED.resolve("asset_manifest.json");
    private static final String REVIEW_DOCUMENT =
        "docs/art_reviews/UI_ASSETS_PREMIUM_V2_REVIEW.md";
    private static final Path REVIEW_DIRECTORY =
        REPOSITORY.resolve("docs/art_reviews/ui_assets_premium_v2");
    private static final Path AUDIT = REVIEW_DIRECTORY.resolve("ui_audit.json");
    private static final String AUDIT_SHA256 =
        "d058a4403e947e67addcf270cbe7ef98b561d9329e36682ba51435c1b137fc32";
    private static final String SOURCE_MANIFEST_SHA256 =
        "981a2a9079bcafb5caa97cfedeca487bcb5f45bce9259f6e1bd7af3688936dce";
    private static final Set<String> ICONS = Set.of(
        "ui_health", "ui_wave", "ui_coin", "ui_pause", "ui_speed",
        "ui_inventory", "ui_shop", "ui_settings", "ui_restart",
        "ui_new_game", "ui_continue", "ui_close", "ui_strength",
        "ui_agility", "ui_luck", "ui_dodge"
    );
    private static final Set<String> KINDS = Set.of("button", "panel", "slot");
    private static final Set<String> STATES = Set.of("normal", "pressed", "selected", "disabled");

    @Test
    void auditAndAllSixOpenedReviewSheetsRemainHashBound() throws IOException {
        assertEquals(AUDIT_SHA256, sha256(AUDIT));
        String review = Files.readString(REPOSITORY.resolve(REVIEW_DOCUMENT));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(AUDIT_SHA256));
        assertTrue(review.contains(SOURCE_MANIFEST_SHA256));

        JsonValue audit = json(AUDIT);
        assertEquals("ui-assets-premium-v2", audit.getString("batch"));
        assertEquals(SOURCE_MANIFEST_SHA256, audit.getString("candidateManifestSha256"));
        JsonValue summary = audit.get("summary");
        assertEquals(28, summary.getInt("assetCount"));
        assertEquals(16, summary.getInt("iconCount"));
        assertEquals(12, summary.getInt("skinCount"));
        assertEquals(3, summary.getInt("skinFamilyCount"));
        assertEquals(4, summary.getInt("stateCountPerSkin"));
        assertEquals(1_032_192L, summary.getLong("decodedBytes"));
        assertEquals(5, summary.getInt("minimumAlphaMargin"));
        assertEquals(6, audit.getInt("reviewSheetCount"));
        for (JsonValue sheet = audit.get("reviewSheets").child;
             sheet != null; sheet = sheet.next) {
            Path path = REVIEW_DIRECTORY.resolve(sheet.name).normalize();
            assertTrue(path.startsWith(REVIEW_DIRECTORY));
            assertTrue(Files.isRegularFile(path), sheet.name);
            assertEquals(sheet.getLong("bytes"), Files.size(path), sheet.name);
            assertEquals(sheet.getString("sha256"), sha256(path), sheet.name);
        }
    }

    @Test
    void allIconsAndEverySkinStateRetainAcceptedPixelsAndMetadata() throws IOException {
        Map<String, JsonValue> catalog = byKey(json(MANIFEST).get("assets"));
        Map<String, JsonValue> audited = byKey(json(AUDIT).get("assets"));
        Set<String> expectedFrames = new HashSet<>();
        for (String kind : KINDS) {
            for (String state : STATES) expectedFrames.add("ui_frame_" + kind + "_" + state);
        }
        Set<String> expected = new HashSet<>(ICONS);
        expected.addAll(expectedFrames);
        assertEquals(expected, audited.keySet());

        for (String key : expected) {
            JsonValue asset = catalog.get(key);
            JsonValue record = audited.get(key);
            assertTrue(asset != null, key);
            assertTrue(Set.of("premium-v2" /* allow studio-v3 etc */, "studio-v3", "studio-v4-vibrant", "studio-v5-hd-pbr").contains(asset.getString("visualQuality")), key + " visualQuality=" + asset.getString("visualQuality"));
            assertEquals(96, asset.getInt("frameSize"), key);
            assertEquals(96, asset.getInt("frameWidth"), key);
            assertEquals(96, asset.getInt("frameHeight"), key);
            assertTrue(asset.getInt("renderSupersample") >= 2, key);
            assertTrue(asset.getInt("renderSamples") >= 8, key);
            assertTrue(asset.getBoolean("touchOnlyUI"), key);
            assertEquals(REVIEW_DOCUMENT, asset.getString("reviewDocument"), key);
            JsonValue accepted = asset.get("categoryReview");
            assertEquals("ui_assets", accepted.getString("category"), key);
            assertEquals("accepted", accepted.getString("status"), key);
            assertEquals(AUDIT_SHA256, accepted.getString("auditSha256"), key);
            assertEquals(SOURCE_MANIFEST_SHA256,
                accepted.getString("sourceManifestSha256"), key);

            String family = ICONS.contains(key) ? "icons" : "ui";
            Path imagePath = GENERATED.resolve(family + "/" + key + ".png");
            Path metadataPath = GENERATED.resolve(family + "/" + key + ".json");
            assertEquals(record.getString("sheetSha256"), sha256(imagePath), key);
            assertEquals(asset.toJson(JsonWriter.OutputType.json),
                json(metadataPath).toJson(JsonWriter.OutputType.json), key);
            assertTransparentMargins(imagePath, 4);

            if (ICONS.contains(key)) {
                assertEquals("icons", asset.getString("family"), key);
                assertEquals("heartwood-control-medallion", asset.getString("iconFamily"), key);
                assertEquals("ui-control-icon-premium-v2", asset.getString("modelRevision"), key);
                assertEquals(key.substring(3), asset.getString("uiIcon"), key);
                assertTrue(asset.getInt("triangles") >= 300
                    && asset.getInt("triangles") <= 1_200, key);
            } else {
                assertEquals("ui", asset.getString("family"), key);
                assertEquals("forest-glass-nine-patch-v2", asset.getString("modelRevision"), key);
                assertEquals(24, asset.get("ninePatchInsets").getInt("left"), key);
                assertEquals(24, asset.get("ninePatchInsets").getInt("right"), key);
                assertEquals(24, asset.get("ninePatchInsets").getInt("top"), key);
                assertEquals(24, asset.get("ninePatchInsets").getInt("bottom"), key);
                assertTrue(KINDS.contains(asset.getString("uiSkin")), key);
                assertTrue(STATES.contains(asset.getString("uiState")), key);
                assertTrue(!asset.getString("stateConstruction").isBlank(), key);
                assertTrue(asset.getInt("triangles") >= 150
                    && asset.getInt("triangles") <= 600, key);
            }
        }
    }

    @Test
    void runtimeUsesNinePatchStatesAndRealTouchLifecycle() throws IOException {
        String frames = Files.readString(REPOSITORY.resolve(
            "core/src/main/java/com/amirrezahadipoor/herodefense/render/UiFrameRenderer.java"
        ));
        for (String token : Set.of(
            "NinePatch", "NORMAL", "PRESSED", "SELECTED", "DISABLED",
            "pressActive", "movePress", "release", "generated/ui/", "ui_frame_"
        )) assertTrue(frames.contains(token), token);
        assertTrue(frames.contains("new NinePatch(texture, INSET, INSET, INSET, INSET)"));

        String game = Files.readString(REPOSITORY.resolve(
            "core/src/main/java/com/amirrezahadipoor/herodefense/HeroDefenseGame.java"
        ));
        assertTrue(game.contains("uiFrameRenderer.press(worldX, worldY)"));
        assertTrue(game.contains("uiFrameRenderer.movePress(worldX, worldY)"));
        assertTrue(game.contains("uiFrameRenderer.release()"));

        String inventory = Files.readString(REPOSITORY.resolve(
            "core/src/main/java/com/amirrezahadipoor/herodefense/render/InventoryOverlayRenderer.java"
        ));
        assertTrue(inventory.contains("itemIndex == controller.selectedIndex()"));
        assertTrue(inventory.contains("boolean hasSelection"));
        assertTrue(inventory.contains("UiFrameRenderer.Kind.SLOT"));

        String menu = Files.readString(REPOSITORY.resolve(
            "core/src/main/java/com/amirrezahadipoor/herodefense/render/MainMenuRenderer.java"
        ));
        assertTrue(menu.contains("continueAvailable, false"));
        assertTrue(menu.contains("UiFrameRenderer.Kind.BUTTON"));
    }

    private static Map<String, JsonValue> byKey(JsonValue values) {
        Map<String, JsonValue> result = new HashMap<>();
        for (JsonValue value = values.child; value != null; value = value.next) {
            String key = value.getString("key");
            if (ICONS.contains(key) || key.startsWith("ui_frame_")) result.put(key, value);
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
