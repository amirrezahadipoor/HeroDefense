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

/** Hash-binds the accepted touch-emulator Inventory review. */
final class PremiumInventoryEvidenceTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path DIRECTORY = REPOSITORY.resolve(
        "docs/art_reviews/inventory_equipment_premium_v2"
    );
    private static final Path AUDIT = DIRECTORY.resolve("surface_audit.json");
    private static final String AUDIT_SHA256 =
        "992c255deee1076e54857ee7ff8c9877719b8ae8b97c4477c1e51252fb7b1f75";
    private static final Set<String> FILES = Set.of(
        "inventory_details_emulator.png",
        "inventory_sell_feedback_emulator.png",
        "inventory_equipment_contact_sheet.png"
    );

    @Test
    void acceptedInventoryEvidenceRemainsExactAndReviewBound() throws IOException {
        assertEquals(AUDIT_SHA256, sha256(AUDIT));
        String review = Files.readString(REPOSITORY.resolve(
            "docs/art_reviews/INVENTORY_EQUIPMENT_PREMIUM_V2_REVIEW.md"
        ));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(AUDIT_SHA256));
        assertTrue(review.contains("`34758288876` / `103726423231`"));

        JsonValue audit = new JsonReader().parse(Files.readString(AUDIT));
        assertEquals("inventory-equipment-premium-v2", audit.getString("batch"));
        assertEquals("accepted", audit.getString("decision"));
        assertEquals(34758288876L, audit.getLong("workflowRun"));
        assertEquals(10317873133L, audit.getLong("artifact"));
        assertTrue(audit.get("device").getBoolean("touchOnly"));
        JsonValue coverage = audit.get("coverage");
        assertEquals(6, coverage.getInt("equipmentSlots"));
        assertEquals(4, coverage.getInt("visibleInventoryRows"));
        assertEquals(4, coverage.get("rarities").size);
        assertEquals(4, coverage.get("comparisonStates").size);
        assertEquals(5, coverage.get("actions").size);
        assertEquals(3, coverage.get("feedback").size);

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
