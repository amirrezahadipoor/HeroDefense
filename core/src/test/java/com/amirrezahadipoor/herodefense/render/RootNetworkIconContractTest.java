package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.ascension.RootNodeBonusType;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Phase 27.0: every Root Network node icon must resolve to a reviewed file. A
 * missing key used to crash the app on open (FOCUS nodes asked for
 * {@code ui_chain_lightning.png}, which never existed).
 */
final class RootNetworkIconContractTest {
    private static final Path ASSETS = Path.of("..").resolve("android/assets").normalize();

    @Test
    void everyNodeBonusIconResolvesToAReviewedFile() {
        for (RootNodeBonusType type : RootNodeBonusType.values()) {
            String key = RootNetworkOverlayRenderer.iconKeyFor(type);
            Path file = ASSETS.resolve(UiIconRenderer.assetPath(key));
            assertTrue(Files.isRegularFile(file), () -> "missing icon for " + type + ": " + file);
        }
    }
}
