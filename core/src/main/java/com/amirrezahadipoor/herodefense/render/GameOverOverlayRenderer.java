package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.GameOverTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Premium defeat/victory result surface with a readable run ledger and touch restart. */
public final class GameOverOverlayRenderer implements AutoCloseable {
    static final float DESTRUCTION_REVEAL_DELAY_SECONDS = 0.82f;
    static final float REVEAL_FADE_SECONDS = 0.28f;

    private static final Color GOLD = Color.valueOf("EAC66D");
    private static final Color IVORY = Color.valueOf("F3E4BC");
    private static final Color SUBTLE = Color.valueOf("AEBCAE");
    private static final Color VICTORY = Color.valueOf("69C884");
    private static final Color DEFEAT = Color.valueOf("DF6A65");

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    public GameOverOverlayRenderer() {
        font.getRegion().getTexture().setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );
    }

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        UiIconRenderer icons,
        float presentationSeconds,
        UiFrameRenderer frames
    ) {
        float reveal = revealProgress(presentationSeconds, state.runComplete);
        if (reveal <= 0f) return;
        beginShapes(projection);
        if (state.runComplete) {
            shapes.setColor(0.005f, 0.027f, 0.021f, 0.965f * reveal);
        } else {
            shapes.setColor(0.035f, 0.012f, 0.014f, 0.965f * reveal);
        }
        shapes.rect(0f, 0f, 720f, 1280f);
        shapes.end();
        endShapes();

        batch.setProjectionMatrix(projection);
        batch.setColor(1f, 1f, 1f, reveal);
        batch.begin();
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 70f, 950f, 580f, 220f, true,
            state.runComplete);
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 90f, 450f, 540f, 455f, true, false);
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            GameOverTouchLayout.RESTART_X, GameOverTouchLayout.RESTART_Y,
            GameOverTouchLayout.RESTART_WIDTH, GameOverTouchLayout.RESTART_HEIGHT,
            isInteractive(presentationSeconds, state.runComplete), false
        );

        Color outcome = withAlpha(state.runComplete ? VICTORY : DEFEAT, reveal);
        Color gold = withAlpha(GOLD, reveal);
        Color ivory = withAlpha(IVORY, reveal);
        Color subtle = withAlpha(SUBTLE, reveal);
        drawCentered(batch, state.runComplete ? "VICTORY" : "DEFEAT", 360f, 1124f, 0.82f, outcome);
        drawCentered(batch, outcomeTitle(state.runComplete), 360f, 1064f, 1.62f, gold);
        drawCentered(
            batch,
            state.runComplete ? "100 WAVES ENDURED" : "THE DEFENSE HAS ENDED",
            360f,
            1008f,
            0.74f,
            subtle
        );

        drawText(batch, "RUN LEDGER", 130f, 856f, 0.76f, gold);
        summaryRow(batch, "WAVE REACHED", Integer.toString(state.waveNumber), 798f, ivory, subtle);
        summaryRow(batch, "HERO LEVEL", Integer.toString(state.heroLevel), 732f, ivory, subtle);
        summaryRow(batch, "ENEMIES DEFEATED", Integer.toString(state.totalKills), 666f, ivory, subtle);
        summaryRow(batch, "KILL COINS EARNED", "$ " + state.totalKillCoinsEarned, 600f, ivory, subtle);
        summaryRow(batch, "BOSSES DEFEATED", Integer.toString(state.defeatedBosses), 534f, ivory, subtle);
        drawText(
            batch,
            state.runComplete ? "The sanctuary stands." : "Grow stronger. Defend again.",
            130f,
            486f,
            0.70f,
            outcome
        );

        UiFrameRenderer.State restartState = frames.resolve(
            isInteractive(presentationSeconds, state.runComplete), false,
            GameOverTouchLayout.RESTART_X, GameOverTouchLayout.RESTART_Y,
            GameOverTouchLayout.RESTART_WIDTH, GameOverTouchLayout.RESTART_HEIGHT
        );
        float offset = MainMenuRenderer.pressedOffset(restartState);
        icons.draw(batch, "restart", 150f, 244f + offset, 94f, restartState);
        drawText(batch, "START A NEW DEFENSE", 276f, 311f + offset, 1.08f, ivory);
        drawText(batch, "Restart at Wave 1", 276f, 267f + offset, 0.68f, subtle);
        batch.end();
        batch.setColor(Color.WHITE);
    }

    static String outcomeTitle(boolean runComplete) {
        return runComplete ? "WORLD TREE SAVED" : "WORLD TREE FALLEN";
    }

    private void summaryRow(
        SpriteBatch batch,
        String label,
        String value,
        float y,
        Color valueColor,
        Color labelColor
    ) {
        drawText(batch, label, 130f, y, 0.68f, labelColor);
        font.getData().setScale(0.92f);
        layout.setText(font, value);
        drawText(batch, value, 590f - layout.width, y, 0.92f, valueColor);
    }

    private void drawCentered(
        SpriteBatch batch, String text, float centerX, float y, float scale, Color color
    ) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        drawText(batch, text, centerX - layout.width * 0.5f, y, scale, color);
    }

    private void drawText(
        SpriteBatch batch, String text, float x, float y, float scale, Color color
    ) {
        font.getData().setScale(scale);
        font.setColor(0.003f, 0.010f, 0.009f, color.a);
        font.draw(batch, text, x + 1.5f, y - 2f);
        font.setColor(color);
        font.draw(batch, text, x, y);
    }

    private static Color withAlpha(Color color, float alpha) {
        Color result = new Color(color);
        result.a *= alpha;
        return result;
    }

    static float revealProgress(float presentationSeconds, boolean runComplete) {
        if (runComplete) return 1f;
        if (!Float.isFinite(presentationSeconds)
            || presentationSeconds <= DESTRUCTION_REVEAL_DELAY_SECONDS) {
            return 0f;
        }
        return Math.min(
            1f,
            (presentationSeconds - DESTRUCTION_REVEAL_DELAY_SECONDS) / REVEAL_FADE_SECONDS
        );
    }

    public static boolean isInteractive(float presentationSeconds, boolean runComplete) {
        return revealProgress(presentationSeconds, runComplete) >= 0.95f;
    }

    private void beginShapes(Matrix4 projection) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void endShapes() {
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void close() {
        font.dispose();
        shapes.dispose();
    }
}
