package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.potions.PotionTier;
import com.amirrezahadipoor.herodefense.rewards.RewardCardId;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
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

/** Guards complete premium potion tiers and all eight reward-card icon semantics. */
final class PremiumUiSupplementAssetContractTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path GENERATED = REPOSITORY.resolve("android/assets/generated");
    private static final Path MANIFEST = GENERATED.resolve("asset_manifest.json");
    private static final String REVIEW_DOCUMENT =
        "docs/art_reviews/UI_ASSETS_PREMIUM_V2_REVIEW.md";
    private static final Path REVIEW_DIRECTORY =
        REPOSITORY.resolve("docs/art_reviews/ui_icon_supplement_premium_v2");
    private static final Path AUDIT = REVIEW_DIRECTORY.resolve("ui_supplement_audit.json");
    private static final String AUDIT_SHA256 =
        "c8f0a8f0950a03e2d6e9ca394668fa1f0df80377720fcf8803e9041f83ca6239";
    private static final String SOURCE_MANIFEST_SHA256 =
        "143a6f992d664f6be134a920398e7ce6d61090ecba68e2961689a39131f8fe02";
    private static final Set<String> POTIONS = Set.of(
        "health_potion_1", "health_potion_2", "health_potion_3",
        "health_potion_4", "health_potion_5", "health_potion_6"
    );
    private static final Set<String> NEW_REWARD_ICONS =
        Set.of("ui_general_power", "ui_lifesteal");

    @Test
    void supplementalAuditAndFiveOpenedSheetsRemainHashBound() throws IOException {
        assertEquals(AUDIT_SHA256, sha256(AUDIT));
        String review = Files.readString(REPOSITORY.resolve(REVIEW_DOCUMENT));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(AUDIT_SHA256));
        assertTrue(review.contains(SOURCE_MANIFEST_SHA256));

        JsonValue audit = json(AUDIT);
        assertEquals("ui-icon-supplement-premium-v2", audit.getString("batch"));
        assertEquals(SOURCE_MANIFEST_SHA256, audit.getString("candidateManifestSha256"));
        JsonValue summary = audit.get("summary");
        assertEquals(8, summary.getInt("assetCount"));
        assertEquals(6, summary.getInt("potionCount"));
        assertEquals(2, summary.getInt("newRewardIconCount"));
        assertEquals(8, summary.getInt("coveredRewardCardCount"));
        assertEquals(294_912L, summary.getLong("decodedBytes"));
        assertTrue(summary.getInt("minimumAlphaMargin") >= 4);
        assertEquals(5, audit.getInt("reviewSheetCount"));
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
    void everyPotionAndNewRewardSemanticRetainsAcceptedPixelsAndConstruction()
        throws IOException {
        Set<String> expected = new HashSet<>(POTIONS);
        expected.addAll(NEW_REWARD_ICONS);
        Map<String, JsonValue> catalog = byKey(json(MANIFEST).get("assets"));
        Map<String, JsonValue> audited = byKey(json(AUDIT).get("assets"));
        assertEquals(expected, audited.keySet());

        for (String key : expected) {
            JsonValue asset = catalog.get(key);
            JsonValue record = audited.get(key);
            assertTrue(asset != null, key);
            assertEquals("icons", asset.getString("family"), key);
            assertEquals("premium-v2", asset.getString("visualQuality"), key);
            assertEquals(96, asset.getInt("frameSize"), key);
            assertEquals(2, asset.getInt("renderSupersample"), key);
            assertEquals(16, asset.getInt("renderSamples"), key);
            assertEquals(REVIEW_DOCUMENT, asset.getString("reviewDocument"), key);
            JsonValue accepted = asset.get("categoryReview");
            assertEquals("accepted", accepted.getString("status"), key);
            assertEquals("potion-and-reward-card-icon-supplement",
                accepted.getString("scope"), key);
            assertEquals(AUDIT_SHA256, accepted.getString("auditSha256"), key);
            assertEquals(SOURCE_MANIFEST_SHA256,
                accepted.getString("sourceManifestSha256"), key);

            Path imagePath = GENERATED.resolve("icons/" + key + ".png");
            assertEquals(record.getString("sheetSha256"), sha256(imagePath), key);
            assertTransparentMargins(imagePath, 4);
            if (POTIONS.contains(key)) {
                int tier = Integer.parseInt(key.substring(key.length() - 1));
                assertEquals(tier, asset.getInt("tier"), key);
                assertEquals("heartwood-elixir", asset.getString("potionFamily"), key);
                assertEquals("health-potion-premium-v2",
                    asset.getString("modelRevision"), key);
                assertTrue(asset.getBoolean("heal_icon"), key);
                assertTrue(!asset.getString("tierConstruction").isBlank(), key);
            } else {
                assertEquals("heartwood-control-medallion",
                    asset.getString("iconFamily"), key);
                assertEquals("ui-control-icon-premium-v2",
                    asset.getString("modelRevision"), key);
                assertEquals(key.substring(3), asset.getString("uiIcon"), key);
                assertTrue(asset.getBoolean("touchOnlyUI"), key);
            }
            assertTrue(asset.getInt("triangles") > 0
                && asset.getInt("triangles") <= 1_200, key);
        }
        assertEquals("docs/art_reviews/PREMIUM_V2_PILOT_REVIEW.md",
            catalog.get("health_potion_6").getString("pilotReviewDocument"));
    }

    @Test
    void runtimeCoversAllPotionPathsAndAllEightRewardCards() throws IOException {
        Set<String> expectedPotionPaths = new HashSet<>();
        for (int tier = 1; tier <= 6; tier++) {
            expectedPotionPaths.add("generated/icons/health_potion_" + tier + ".png");
        }
        Set<String> actualPotionPaths = new HashSet<>();
        for (PotionTier tier : PotionTier.values()) actualPotionPaths.add(tier.iconPath());
        assertEquals(expectedPotionPaths, actualPotionPaths);

        Map<RewardCardId, String> expectedRewardKeys = Map.of(
            RewardCardId.STRENGTH, "strength",
            RewardCardId.AGILITY, "agility",
            RewardCardId.LUCK, "luck",
            RewardCardId.DODGE, "dodge",
            RewardCardId.HEALTH, "health",
            RewardCardId.GENERAL_POWER, "general_power",
            RewardCardId.COIN_INCOME, "coin",
            RewardCardId.LIFESTEAL, "lifesteal"
        );
        assertEquals(RewardCardId.values().length, expectedRewardKeys.size());
        for (Map.Entry<RewardCardId, String> entry : expectedRewardKeys.entrySet()) {
            assertEquals(entry.getValue(), entry.getKey().iconKey(), entry.getKey().name());
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                "icons/ui_" + entry.getValue() + ".png"
            )), entry.getKey().name());
        }
        String renderer = Files.readString(REPOSITORY.resolve(
            "core/src/main/java/com/amirrezahadipoor/herodefense/render/RewardCardOverlayRenderer.java"
        ));
        assertTrue(renderer.contains("icons.draw(batch, card.iconKey()"));
    }

    private static Map<String, JsonValue> byKey(JsonValue values) {
        Map<String, JsonValue> result = new HashMap<>();
        for (JsonValue value = values.child; value != null; value = value.next) {
            String key = value.getString("key");
            if (POTIONS.contains(key) || NEW_REWARD_ICONS.contains(key)) {
                result.put(key, value);
            }
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
