package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Hash-binds the accepted touch-emulator Shop review. */
final class PremiumShopEvidenceTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path DIRECTORY = REPOSITORY.resolve("docs/art_reviews/shop_premium_v2");
    private static final Path AUDIT = DIRECTORY.resolve("surface_audit.json");
    private static final String AUDIT_SHA256 =
        "420b81ecbc88c4b95af71f1e26d4d67c6abc1a8ba627bcc0de6c83d81f10072f";
    private static final Set<String> FILES = Set.of(
        "shop_affordability_emulator.png",
        "shop_purchase_feedback_emulator.png",
        "shop_contact_sheet.png"
    );

    @Test
    void acceptedShopEvidenceRemainsExactAndReviewBound() throws IOException {
        assertEquals(AUDIT_SHA256, sha256(AUDIT));
        String review = Files.readString(REPOSITORY.resolve(
            "docs/art_reviews/SHOP_PREMIUM_V2_REVIEW.md"
        ));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(AUDIT_SHA256));
        assertTrue(review.contains("`34761841420` / `103735942735`"));

        JsonValue audit = new JsonReader().parse(Files.readString(AUDIT));
        assertEquals("shop-premium-v2", audit.getString("batch"));
        assertEquals("accepted", audit.getString("decision"));
        assertEquals(34761841420L, audit.getLong("workflowRun"));
        assertEquals(10319755714L, audit.getLong("artifact"));
        assertTrue(audit.get("device").getBoolean("touchOnly"));
        JsonValue coverage = audit.get("coverage");
        assertEquals(5, coverage.getInt("statCards"));
        assertEquals(20, coverage.getInt("purchaseCapPerStat"));
        assertEquals(3, coverage.get("affordabilityStates").size);
        assertEquals(3, coverage.get("feedbackStates").size);
        assertEquals(2, coverage.get("returnContexts").size);
        assertEquals("earned_coins_only", coverage.getString("currency"));

        Set<String> actual = new HashSet<>();
        for (JsonValue record = audit.get("files").child;
             record != null; record = record.next) {
            actual.add(record.name);
            Path path = DIRECTORY.resolve(record.name).normalize();
            assertTrue(path.startsWith(DIRECTORY));
            assertEquals(record.getLong("bytes"), Files.size(path), record.name);
            assertEquals(record.getString("sha256"), sha256(path), record.name);
            BufferedImage image = ImageIO.read(path.toFile());
            assertEquals(record.getInt("width"), image.getWidth(), record.name);
            assertEquals(record.getInt("height"), image.getHeight(), record.name);
        }
        assertEquals(FILES, actual);
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
