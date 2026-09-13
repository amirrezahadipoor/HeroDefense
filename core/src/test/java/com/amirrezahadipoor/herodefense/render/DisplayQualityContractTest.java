package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.input.HudTouchLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Locks the premium-v3 display contract: no letterbox, density-true text, edge-anchored HUD. */
final class DisplayQualityContractTest {
    private static final Path REPOSITORY = Path.of("..").normalize();

    @AfterEach
    void resetEdges() {
        ScreenEdges.reset();
    }

    @Test
    void sixteenByNinePanelsKeepTheExactDesignArea() {
        DisplayMetrics metrics = new DisplayMetrics(1080, 1920, 3f);
        assertEquals(1280f, metrics.worldHeight(), 0.01f);
        assertEquals(0f, metrics.bottomEdge(), 0.01f);
        assertEquals(1280f, metrics.topEdge(), 0.01f);
        assertEquals(1.5f, metrics.pixelsPerWorldUnit(), 0.001f);
    }

    @Test
    void tallPanelsRevealArenaInsteadOfBlackBars() {
        DisplayMetrics metrics = new DisplayMetrics(1080, 2220, 2.625f); // API-15 emulator
        assertEquals(1480f, metrics.worldHeight(), 0.01f);
        assertEquals(-100f, metrics.bottomEdge(), 0.01f);
        assertEquals(1380f, metrics.topEdge(), 0.01f);
        DisplayMetrics ultraWide = new DisplayMetrics(1080, 2800, 3f); // 21:9
        assertEquals(DisplayMetrics.MAX_WORLD_HEIGHT, ultraWide.worldHeight(), 0.01f);
        DisplayMetrics tablet = new DisplayMetrics(1536, 2048, 2f); // 4:3 never shrinks below design
        assertEquals(DisplayMetrics.MIN_WORLD_HEIGHT, tablet.worldHeight(), 0.01f);
    }

    @Test
    void textIsPhysicallyLegibleOnEveryDensity() {
        for (float[] panel : new float[][] {{720, 1280, 2f}, {1080, 2220, 2.625f}, {1440, 3120, 3.5f}, {480, 854, 1.5f}}) {
            DisplayMetrics metrics = new DisplayMetrics((int) panel[0], (int) panel[1], panel[2]);
            for (GameFonts.Role role : GameFonts.Role.values()) {
                float worldSize = GameFonts.worldSizeFor(role, metrics);
                float pixels = worldSize * metrics.pixelsPerWorldUnit();
                float sp = pixels / metrics.density();
                assertTrue(sp >= 10.5f, role + " renders at " + sp + "sp on " + panel[0] + "px");
                assertTrue(worldSize <= 120f, role + " too large in world units: " + worldSize);
                assertTrue(metrics.glyphPixelsForWorldUnits(worldSize) >= 16);
            }
            assertTrue(GameFonts.worldSizeFor(GameFonts.Role.DISPLAY, metrics)
                > GameFonts.worldSizeFor(GameFonts.Role.TITLE, metrics));
            assertTrue(GameFonts.worldSizeFor(GameFonts.Role.BODY, metrics)
                > GameFonts.worldSizeFor(GameFonts.Role.CAPTION, metrics));
        }
    }

    @Test
    void legacyScalesMapMonotonicallyOntoRoles() {
        assertEquals(GameFonts.Role.CAPTION, GameFonts.Role.forLegacyScale(0.66f));
        assertEquals(GameFonts.Role.LABEL, GameFonts.Role.forLegacyScale(0.84f));
        assertEquals(GameFonts.Role.BODY, GameFonts.Role.forLegacyScale(1.02f));
        assertEquals(GameFonts.Role.HEADING, GameFonts.Role.forLegacyScale(1.34f));
        assertEquals(GameFonts.Role.TITLE, GameFonts.Role.forLegacyScale(1.9f));
        assertEquals(GameFonts.Role.DISPLAY, GameFonts.Role.forLegacyScale(2.28f));
    }

    @Test
    void backdropCoversTallPanelsWithoutStretching() {
        float[] design = ScreenEdges.coverBounds(1280f);
        assertEquals(0f, design[0]); assertEquals(0f, design[1]);
        assertEquals(720f, design[2]); assertEquals(1280f, design[3]);
        float[] tall = ScreenEdges.coverBounds(1480f);
        float aspect = tall[2] / tall[3];
        assertEquals(720f / 1280f, aspect, 0.0001f);
        assertTrue(tall[1] <= -100f && tall[1] + tall[3] >= 1380f);
        assertTrue(tall[0] < 0f); // cropped equally on the sides
    }

    @Test
    void hudRowsSlideToPhysicalEdgesWithinTheCap() {
        assertEquals(0f, HudTouchLayout.topShift());
        assertEquals(1065f, HudTouchLayout.buttonY());
        ScreenEdges.update(new DisplayMetrics(1080, 2220, 2.625f));
        assertEquals(72f, HudTouchLayout.topShift(), 0.01f); // capped from 100
        assertEquals(72f, HudTouchLayout.bottomShift(), 0.01f);
        assertEquals(1137f, HudTouchLayout.buttonY(), 0.01f);
        assertEquals(-48f, HudTouchLayout.utilityButtonY(), 0.01f);
        assertTrue(HudTouchLayout.pauseAt(600f, 1180f));
        assertFalse(HudTouchLayout.pauseAt(600f, 1070f));
        assertTrue(HudTouchLayout.inventoryAt(270f, 0f));
        assertTrue(HudTouchLayout.utilityButtonY() >= ScreenEdges.bottom() + 8f);
    }

    @Test
    void noRendererOwnsABlurryDefaultBitmapFontAnymore() throws IOException {
        Path render = REPOSITORY.resolve("core/src/main/java/com/amirrezahadipoor/herodefense/render");
        try (var files = Files.list(render)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String source = Files.readString(file);
                assertFalse(source.contains("new BitmapFont()"), file.getFileName() + " uses the default font");
            }
        }
        String game = Files.readString(REPOSITORY.resolve(
            "core/src/main/java/com/amirrezahadipoor/herodefense/HeroDefenseGame.java"));
        assertTrue(game.contains("new ExtendViewport("));
        assertFalse(game.contains("FitViewport"));
        assertTrue(Files.exists(REPOSITORY.resolve("android/assets/fonts/Nunito-Bold.ttf")));
        assertTrue(Files.exists(REPOSITORY.resolve("android/assets/fonts/Nunito-ExtraBold.ttf")));
        assertTrue(Files.readString(REPOSITORY.resolve("android/assets/fonts/NUNITO-OFL.txt"))
            .contains("SIL Open Font License"));
    }
}
