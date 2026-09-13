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
import java.util.HexFormat;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Hash-binds the accepted real-size emulator review for the primary surfaces. */
final class PremiumMainMenuHudEvidenceTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path DIRECTORY = REPOSITORY.resolve(
        "docs/art_reviews/main_menu_hud_premium_v2"
    );
    private static final Path AUDIT = DIRECTORY.resolve("surface_audit.json");
    private static final String AUDIT_SHA256 =
        "8fb923814763fc593550740e3165ce677232c86c6da23da10777d3a13139597f";
    private static final Set<String> FILES = Set.of(
        "main_menu_emulator.png",
        "live_hud_emulator.png",
        "main_menu_hud_contact_sheet.png"
    );

    @Test
    void acceptedPixel3aEvidenceRemainsExactAndReviewBound() throws IOException {
        assertEquals(AUDIT_SHA256, sha256(AUDIT));
        String review = Files.readString(REPOSITORY.resolve(
            "docs/art_reviews/MAIN_MENU_HUD_PREMIUM_V2_REVIEW.md"
        ));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(AUDIT_SHA256));
        assertTrue(review.contains("`34752508440` / `103711298622`"));

        JsonValue audit = new JsonReader().parse(Files.readString(AUDIT));
        assertEquals("main-menu-live-hud-premium-v2", audit.getString("batch"));
        assertEquals("accepted", audit.getString("decision"));
        assertEquals(34752508440L, audit.getLong("workflowRun"));
        assertEquals(10316422349L, audit.getLong("artifact"));
        assertTrue(audit.get("device").getBoolean("touchOnly"));
        assertEquals(21.48f,
            audit.get("metrics").getFloat("topHudOcclusionReductionPercent"));

        JsonValue records = audit.get("files");
        Set<String> actual = new java.util.HashSet<>();
        for (JsonValue record = records.child; record != null; record = record.next) {
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
