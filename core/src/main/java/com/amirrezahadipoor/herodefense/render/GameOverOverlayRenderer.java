package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.GameOverTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;

/** End-of-run summary and touch restart presentation. */
public final class GameOverOverlayRenderer implements AutoCloseable {
    static final float DESTRUCTION_REVEAL_DELAY_SECONDS = 0.82f;
    static final float REVEAL_FADE_SECONDS = 0.28f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();

    public GameOverOverlayRenderer() {
        font.getData().setScale(1.35f);
    }

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        UiIconRenderer icons,
        float presentationSeconds
    ) {
        float reveal = revealProgress(presentationSeconds, state.runComplete);
        if (reveal <= 0f) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.025f, 0.04f, 0.045f, 0.97f * reveal);
        shapes.rect(0f, 0f, 720f, 1280f);
        shapes.setColor(0.09f, 0.17f, 0.16f, reveal);
        shapes.rect(90f, 470f, 540f, 470f);
        shapes.setColor(0.15f, 0.32f, 0.27f, reveal);
        shapes.rect(
            GameOverTouchLayout.RESTART_X,
            GameOverTouchLayout.RESTART_Y,
            GameOverTouchLayout.RESTART_WIDTH,
            GameOverTouchLayout.RESTART_HEIGHT
        );
        shapes.setColor(0.84f, 0.68f, 0.30f, reveal);
        shapes.rect(
            GameOverTouchLayout.RESTART_X,
            GameOverTouchLayout.RESTART_Y + GameOverTouchLayout.RESTART_HEIGHT - 7f,
            GameOverTouchLayout.RESTART_WIDTH,
            7f
        );
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.setColor(1f, 1f, 1f, reveal);
        batch.begin();
        font.setColor(Color.valueOf(state.runComplete ? "F2D58A" : "E7D8B1"));
        font.getColor().a = reveal;
        font.getData().setScale(2f);
        font.draw(batch, state.runComplete ? "World Tree Saved" : "World Tree Fallen", 165f, 1080f);
        font.getData().setScale(1.3f);
        font.setColor(Color.valueOf("F3E4BC"));
        font.getColor().a = reveal;
        font.draw(batch, "Run Summary", 270f, 880f);
        font.draw(batch, "Wave reached", 145f, 800f);
        font.draw(batch, Integer.toString(state.waveNumber), 530f, 800f);
        font.draw(batch, "Hero level", 145f, 730f);
        font.draw(batch, Integer.toString(state.heroLevel), 530f, 730f);
        font.draw(batch, "Enemies defeated", 145f, 660f);
        font.draw(batch, Integer.toString(state.totalKills), 530f, 660f);
        font.draw(batch, "Kill coins earned", 145f, 590f);
        font.draw(batch, Integer.toString(state.totalKillCoinsEarned), 530f, 590f);
        font.draw(batch, "Bosses defeated", 145f, 520f);
        font.draw(batch, Integer.toString(state.defeatedBosses), 530f, 520f);
        font.getData().setScale(1.55f);
        icons.draw(batch, "restart", 150f, 245f, 92f);
        font.draw(batch, "Restart at Wave 1", 255f, 305f);
        font.getData().setScale(1.35f);
        batch.end();
        batch.setColor(Color.WHITE);
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

    @Override
    public void close() {
        font.dispose();
        shapes.dispose();
    }
}
