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

/** Hash-binds the accepted touch-emulator VFX review. */
final class PremiumVfxEvidenceTest {
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path DIRECTORY = REPOSITORY.resolve("docs/art_reviews/vfx_premium_v2");
    private static final Path AUDIT = DIRECTORY.resolve("surface_audit.json");
    private static final String AUDIT_SHA256 =
        "df66db673cc8b70989ede52d9824fa24459312427ff1065c1c7df43b9018227c";
    private static final Set<String> FILES = Set.of(
        "boss_entrance_emulator.png",
        "impact_death_emulator.png",
        "projectile_trail_emulator.png",
        "tree_collapse_emulator.png",
        "vfx_contact_sheet.png"
    );

    @Test
    void acceptedVfxEvidenceRemainsExactAndReviewBound() throws IOException {
        assertEquals(AUDIT_SHA256, sha256(AUDIT));
        String review = Files.readString(REPOSITORY.resolve(
            "docs/art_reviews/VFX_PREMIUM_V2_REVIEW.md"
        ));
        assertTrue(review.contains("**Decision:** ACCEPTED"));
        assertTrue(review.contains(AUDIT_SHA256));
        assertTrue(review.contains("`34767961263` / `103752232553`"));

        JsonValue audit = new JsonReader().parse(Files.readString(AUDIT));
        assertEquals("vfx-premium-v2", audit.getString("batch"));
        assertEquals("accepted", audit.getString("decision"));
        assertEquals(34767961263L, audit.getLong("workflowRun"));
        assertEquals(10321425053L, audit.getLong("artifact"));
        assertTrue(audit.get("device").getBoolean("touchOnly"));
        JsonValue coverage = audit.get("coverage");
        assertEquals(10, coverage.get("effects").size);
        assertEquals(7, coverage.get("capturedOnDevice").size);
        assertEquals(3, coverage.get("boundByUnitTestOnly").size);
        assertEquals(256, coverage.get("budget").getInt("particleCap"));

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
