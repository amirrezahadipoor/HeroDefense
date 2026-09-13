package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/**
 * Rasterises the licensed Nunito faces at the panel's real pixel size once per resize.
 *
 * <p>Every text site asks for a role (title, heading, body, label) instead of a raw scale, so
 * legibility is decided in one place and stays physically consistent across densities.
 */
public final class GameFonts implements AutoCloseable {
    public enum Role {
        DISPLAY(32f, true),
        TITLE(22f, true),
        HEADING(17f, true),
        BODY(14f, false),
        LABEL(12f, true),
        CAPTION(11f, false);

        /** Physical size in Android sp; converted to world units per device so text never shrinks. */
        public final float sp;
        public final boolean heavy;

        Role(float sp, boolean heavy) {
            this.sp = sp;
            this.heavy = heavy;
        }

        /** Maps the legacy free-form scale multiplier onto the nearest typographic role. */
        public static Role forLegacyScale(float scale) {
            if (scale <= 0.72f) return CAPTION;
            if (scale <= 0.92f) return LABEL;
            if (scale <= 1.12f) return BODY;
            if (scale <= 1.55f) return HEADING;
            if (scale <= 2.05f) return TITLE;
            return DISPLAY;
        }
    }

    /** Guard rails in world units so low-density tablets and giant-font phones both stay sane. */
    static final float MIN_WORLD_SIZE = 16f;
    static final float MAX_WORLD_SIZE_FACTOR = 2.6f;
    static final String BOLD_PATH = "fonts/Nunito-Bold.ttf";
    static final String EXTRA_BOLD_PATH = "fonts/Nunito-ExtraBold.ttf";
    static final String CHARACTERS =
        FreeTypeFontGenerator.DEFAULT_CHARS + "\u2013\u2014\u2019\u2022\u2026\u00d7\u00b7";

    private static GameFonts shared;

    /** The libGDX application whose GL context owns the glyph textures. */
    private final Application owner = Gdx.app;
    private final FreeTypeFontGenerator bold;
    private final FreeTypeFontGenerator extraBold;
    private final BitmapFont[] fonts = new BitmapFont[Role.values().length];
    private DisplayMetrics metrics;

    private GameFonts() {
        FreeTypeFontGenerator.setMaxTextureSize(2048);
        bold = new FreeTypeFontGenerator(Gdx.files.internal(BOLD_PATH));
        extraBold = new FreeTypeFontGenerator(Gdx.files.internal(EXTRA_BOLD_PATH));
        rebuild(new DisplayMetrics(
            Gdx.graphics.getBackBufferWidth(),
            Gdx.graphics.getBackBufferHeight(),
            Gdx.graphics.getDensity()
        ));
    }

    /**
     * One glyph atlas set per libGDX application; renderers share it and never own a font.
     *
     * <p>Glyph textures live in the GL context of the application that rasterised them. When a
     * new {@link Application} starts in the same process (activity recreation, instrumentation
     * launching several activities back to back) the old atlases are dead, so a fresh set is
     * rasterised instead of drawing invisible text from the stale context.
     */
    public static GameFonts shared() {
        if (shared != null && shared.owner != Gdx.app) {
            shared.close();
        }
        if (shared == null) shared = new GameFonts();
        return shared;
    }

    public static boolean hasShared() {
        return shared != null;
    }

    /** Re-rasterises when the back buffer changes so glyphs stay pixel-exact after rotation/resize. */
    public void rebuild(DisplayMetrics newMetrics) {
        if (metrics != null
            && metrics.screenWidth() == newMetrics.screenWidth()
            && metrics.screenHeight() == newMetrics.screenHeight()) {
            return;
        }
        metrics = newMetrics;
        for (Role role : Role.values()) {
            BitmapFont previous = fonts[role.ordinal()];
            if (previous != null) previous.dispose();
            fonts[role.ordinal()] = generate(role);
        }
    }

    public BitmapFont font(Role role) {
        return fonts[role.ordinal()];
    }

    public DisplayMetrics metrics() {
        return metrics;
    }

    /** World-unit glyph size for a role on the current panel; pure so tests can lock the contract. */
    static float worldSizeFor(Role role, DisplayMetrics metrics) {
        float natural = metrics.worldUnitsForDp(role.sp);
        float floor = MIN_WORLD_SIZE * role.sp / Role.CAPTION.sp;
        float ceiling = floor * MAX_WORLD_SIZE_FACTOR;
        return Math.max(floor, Math.min(ceiling, natural));
    }

    private BitmapFont generate(Role role) {
        float worldSize = worldSizeFor(role, metrics);
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = metrics.glyphPixelsForWorldUnits(worldSize);
        parameter.characters = CHARACTERS;
        parameter.color = Color.WHITE;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        parameter.hinting = FreeTypeFontGenerator.Hinting.AutoMedium;
        parameter.kerning = true;
        parameter.incremental = false;
        BitmapFont font = (role.heavy ? extraBold : bold).generateFont(parameter);
        // Glyphs are rasterised in pixels; scale back so one glyph unit equals one world unit.
        font.getData().setScale(1f / metrics.pixelsPerWorldUnit());
        font.setUseIntegerPositions(false);
        return font;
    }

    @Override
    public void close() {
        for (BitmapFont font : fonts) if (font != null) font.dispose();
        bold.dispose();
        extraBold.dispose();
        if (shared == this) shared = null;
    }

    /** Releases the shared set only if this application still owns it; a newer app keeps its own. */
    public static void closeSharedFor(Application application) {
        if (shared != null && shared.owner == application) shared.close();
    }
}
