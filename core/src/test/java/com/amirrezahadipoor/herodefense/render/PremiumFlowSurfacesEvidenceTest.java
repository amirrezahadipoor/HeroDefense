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

/** Hash-binds the accepted touch-emulator flow-surfaces review. */
final class PremiumFlowSurfacesEvidenceTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path DIRECTORY = REPOSITORY.resolve("docs/art_reviews/flow_surfaces_premium_v2");
    private static final Path AUDIT = DIRECTORY.resolve("surface_audit.json");
    private static final String AUDIT_SHA256 =
        "02347d3aae5f1f68219f839e61f5fa261d9f9e443e033d71c319da600e6bff83";
    private static final Set<String> FILES = Set.of(
        "pause_emulator.png",
        "settings_emulator.png",
        "level_up_emulator.png",
        "reward_cards_emulator.png",
        "victory_emulator.png",
        "defeat_emulator.png",
        "flow_surfaces_contact_sheet.png"
    );

    @Test
    void acceptedFlowSurfaceEvidenceRemainsExactAndReviewBound() throws IOException {
        assertEquals(AUDIT_SHA256, sha256(AUDIT));
        String review = Files.readString(REPOSITORY.resolve(
            "docs/art_reviews/FLOW_SURFACES_PREMIUM_V2_REVIEW.md"
        ));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(AUDIT_SHA256));
        assertTrue(review.contains("`34766141882` / `103747331693`"));

        JsonValue audit = new JsonReader().parse(Files.readString(AUDIT));
        assertEquals("flow-surfaces-premium-v2", audit.getString("batch"));
        assertEquals("accepted", audit.getString("decision"));
        assertEquals(34766141882L, audit.getLong("workflowRun"));
        assertEquals(10320901806L, audit.getLong("artifact"));
        assertTrue(audit.get("device").getBoolean("touchOnly"));
        JsonValue coverage = audit.get("coverage");
        assertEquals(6, coverage.get("surfaces").size);
        assertEquals(2, coverage.get("levelUpPreview").size);
        assertEquals(2, coverage.get("settingsStates").size);
        assertEquals(3, coverage.get("rewardContext").size);
        assertEquals(2, coverage.get("endOfRunOutcomes").size);
        assertEquals("OverlayText", coverage.getString("sharedTypography"));

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
