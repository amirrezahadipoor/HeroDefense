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

/** Hash-binds all six accepted touch-emulator overlay states and two review sheets. */
final class PremiumOverlayEvidenceTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path DIRECTORY = REPOSITORY.resolve(
        "docs/art_reviews/overlay_surfaces_premium_v2"
    );
    private static final Path AUDIT = DIRECTORY.resolve("surface_audit.json");
    private static final String AUDIT_SHA256 =
        "ac49b0b0ad2bffb9a1f5a15c9590531ac6460593a117f4b2a88fda721f696dc5";
    private static final Set<String> FILES = Set.of(
        "pause_emulator.png",
        "settings_emulator.png",
        "level_up_emulator.png",
        "reward_cards_emulator.png",
        "defeat_emulator.png",
        "victory_emulator.png",
        "pause_settings_level_contact_sheet.png",
        "reward_results_contact_sheet.png"
    );

    @Test
    void acceptedOverlayEvidenceRemainsExactAndReviewBound() throws IOException {
        assertEquals(AUDIT_SHA256, sha256(AUDIT));
        String review = Files.readString(REPOSITORY.resolve(
            "docs/art_reviews/OVERLAY_SURFACES_PREMIUM_V2_REVIEW.md"
        ));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(AUDIT_SHA256));
        assertTrue(review.contains("`34768821991` / `103754537381`"));

        JsonValue audit = new JsonReader().parse(Files.readString(AUDIT));
        assertEquals("overlay-surfaces-premium-v2", audit.getString("batch"));
        assertEquals("accepted", audit.getString("decision"));
        assertEquals(34768821991L, audit.getLong("workflowRun"));
        assertEquals(10320544285L, audit.getLong("artifact"));
        assertTrue(audit.get("device").getBoolean("touchOnly"));
        JsonValue coverage = audit.get("coverage");
        assertEquals(6, coverage.getInt("capturedSurfaceCount"));
        assertEquals(2, coverage.getInt("contactSheetCount"));
        assertEquals(6, coverage.get("surfaces").size);
        assertEquals(2, coverage.get("terminalMenuTouchPaths").size);
        assertEquals(3, coverage.getInt("rewardChoiceCount"));
        assertEquals(5, coverage.getInt("talentChoiceCount"));

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
