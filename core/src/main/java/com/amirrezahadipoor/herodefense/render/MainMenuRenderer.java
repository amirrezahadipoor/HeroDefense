package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.MainMenuTouchLayout;

/** Premium touch-first menu using the reviewed arena, Heartwood frames, and clear type hierarchy. */
public final class MainMenuRenderer implements AutoCloseable {
    static final float TITLE_PANEL_X = 44f;
    static final float TITLE_PANEL_Y = 912f;
    static final float TITLE_PANEL_WIDTH = 632f;
    static final float TITLE_PANEL_HEIGHT = 252f;
    static final float COIN_PANEL_X = 500f;
    static final float COIN_PANEL_Y = 1192f;
    static final float COIN_PANEL_WIDTH = 176f;
    static final float COIN_PANEL_HEIGHT = 64f;

    private static final Color GOLD = Color.valueOf("F2D58A");
    private static final Color IVORY = Color.valueOf("F3E4BC");
    private static final Color MUTED = Color.valueOf("85877E");
    private static final Color SUBTLE = Color.valueOf("B8C4AF");

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();
    private Texture backdrop;

    public MainMenuRenderer() {
    }

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        boolean continueAvailable,
        int coins,
        UiIconRenderer icons,
        UiFrameRenderer frames
    ) {
        batch.setProjectionMatrix(projection);
        batch.begin();
        batch.setColor(0.82f, 0.90f, 0.86f, 1f);
        ScreenEdges.drawCover(batch, backdrop());
        batch.setColor(Color.WHITE);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.060f, 0.130f, 0.112f, 0.40f);
        shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
        shapes.setColor(0.040f, 0.095f, 0.085f, 0.34f);
        shapes.rect(0f, ScreenEdges.bottom(), 54f, ScreenEdges.height());
        shapes.rect(666f, ScreenEdges.bottom(), 54f, ScreenEdges.height());
        shapes.setColor(0.05f, 0.15f, 0.12f, 0.45f);
        shapes.rect(0f, ScreenEdges.bottom(), 720f, 220f - ScreenEdges.bottom());
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        UiFrameRenderer.State newGameState = frames.resolve(
            true, false, MainMenuTouchLayout.BUTTON_X, 690f,
            MainMenuTouchLayout.BUTTON_WIDTH, MainMenuTouchLayout.BUTTON_HEIGHT
        );
        UiFrameRenderer.State continueState = frames.resolve(
            continueAvailable, false, MainMenuTouchLayout.BUTTON_X, 500f,
            MainMenuTouchLayout.BUTTON_WIDTH, MainMenuTouchLayout.BUTTON_HEIGHT
        );
        UiFrameRenderer.State settingsState = frames.resolve(
            true, false, MainMenuTouchLayout.BUTTON_X, 310f,
            MainMenuTouchLayout.BUTTON_WIDTH, MainMenuTouchLayout.BUTTON_HEIGHT
        );

        batch.begin();
        frames.draw(
            batch, UiFrameRenderer.Kind.PANEL,
            TITLE_PANEL_X, TITLE_PANEL_Y, TITLE_PANEL_WIDTH, TITLE_PANEL_HEIGHT,
            true, false
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.PANEL,
            COIN_PANEL_X, COIN_PANEL_Y, COIN_PANEL_WIDTH, COIN_PANEL_HEIGHT,
            true, false
        );
        drawButton(batch, frames, 690f, true);
        drawButton(batch, frames, 500f, continueAvailable);
        drawButton(batch, frames, 310f, true);

        icons.draw(batch, "coin", 508f, 1201f, 46f);
        drawShadowedCentered(batch, coinTotalLabel(coins), 611f, 1232f, 1.05f, GOLD);
        drawShadowedCentered(batch, "THE WORLD TREE AWAITS", 360f, 1120f, 0.86f, GOLD);
        drawShadowedCentered(batch, "HERO DEFENSE", 360f, 1058f, 2.28f, GOLD);
        drawShadowedCentered(
            batch, "Hold the last green sanctuary through 100 waves",
            360f, 988f, 0.92f, IVORY
        );

        drawMenuAction(
            batch, icons, "new_game", "NEW GAME", "Begin a fresh defense",
            690f, newGameState, true
        );
        drawMenuAction(
            batch, icons, "continue", "CONTINUE", "Return to the active wave",
            500f, continueState, continueAvailable
        );
        drawMenuAction(
            batch, icons, "settings", "SETTINGS", "Comfort, music, and effects",
            310f, settingsState, true
        );
        drawShadowedCentered(batch, "100 WAVES  |  ONE LAST TREE", 360f, 174f, 0.84f, SUBTLE);
        batch.end();
    }

    private void drawButton(SpriteBatch batch, UiFrameRenderer frames, float y, boolean enabled) {
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            MainMenuTouchLayout.BUTTON_X, y,
            MainMenuTouchLayout.BUTTON_WIDTH, MainMenuTouchLayout.BUTTON_HEIGHT,
            enabled, false
        );
    }

    private void drawMenuAction(
        SpriteBatch batch,
        UiIconRenderer icons,
        String icon,
        String title,
        String subtitle,
        float y,
        UiFrameRenderer.State state,
        boolean enabled
    ) {
        float offset = pressedOffset(state);
        Color primary = enabled ? IVORY : MUTED;
        Color secondary = enabled ? SUBTLE : MUTED;
        icons.draw(batch, icon, 146f, y + 27f + offset, 82f, state);
        drawShadowed(batch, title, 258f, y + 92f + offset, 1.34f, primary);
        drawShadowed(batch, subtitle, 258f, y + 49f + offset, 0.78f, secondary);
    }

    private void drawShadowedCentered(
        SpriteBatch batch, String label, float centerX, float y, float scale, Color color
    ) {
        text.drawCentered(batch, label, centerX, y, scale, color);
    }

    private void drawShadowed(
        SpriteBatch batch, String label, float x, float y, float scale, Color color
    ) {
        text.draw(batch, label, x, y, scale, color);
    }

    private Texture backdrop() {
        if (backdrop == null) {
            backdrop = new Texture(Gdx.files.internal("generated/environment/arena_backdrop.png"));
            backdrop.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        return backdrop;
    }

    static float pressedOffset(UiFrameRenderer.State state) {
        return state == UiFrameRenderer.State.PRESSED ? -4f : 0f;
    }

    static String coinTotalLabel(int coins) {
        return "$ " + Math.max(0, coins);
    }

    @Override
    public void close() {
        if (backdrop != null) backdrop.dispose();
        text.close();
        shapes.dispose();
    }
}
