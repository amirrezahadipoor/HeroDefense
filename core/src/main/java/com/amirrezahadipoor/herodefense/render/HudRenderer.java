package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.HudTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Compact portrait HUD for health, run progress, currency, pause, and speed. */
public final class HudRenderer implements AutoCloseable {
    private static final float HP_X = 30f;
    private static final float HP_Y = 1190f;
    private static final float HP_WIDTH = 660f;
    private static final float HP_HEIGHT = 45f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();

    public HudRenderer() {
        font.getData().setScale(1.15f);
    }

    public void draw(
        SpriteBatch batch, Matrix4 projection, GameState state, UiIconRenderer icons
    ) {
        float healthRatio = state.hero.maxHealth <= 0f
            ? 0f
            : Math.max(0f, Math.min(1f, state.hero.health / state.hero.maxHealth));
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.015f, 0.03f, 0.035f, 0.90f);
        shapes.rect(16f, 1045f, 688f, 215f);
        shapes.setColor(0.12f, 0.08f, 0.07f, 1f);
        shapes.rect(HP_X, HP_Y, HP_WIDTH, HP_HEIGHT);
        shapes.setColor(0.20f, 0.66f, 0.38f, 1f);
        shapes.rect(HP_X, HP_Y, HP_WIDTH * healthRatio, HP_HEIGHT);
        button(HudTouchLayout.SPEED_X, HudTouchLayout.BUTTON_Y);
        button(HudTouchLayout.PAUSE_X, HudTouchLayout.BUTTON_Y);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(Color.valueOf("F3E4BC"));
        icons.draw(batch, "health", 35f, 1194f, 36f);
        icons.draw(batch, "wave", 35f, 1081f, 52f);
        icons.draw(batch, "coin", 215f, 1081f, 52f);
        icons.draw(batch, "speed", 438f, 1080f, 54f);
        icons.draw(batch, "pause", 603f, 1080f, 54f);
        font.draw(batch, "HP " + Math.round(state.hero.health) + "/" + Math.round(state.hero.maxHealth), 80f, 1223f);
        font.draw(batch, state.waveNumber + "/" + GameState.FINAL_WAVE, 92f, 1128f);
        font.draw(batch, Integer.toString(state.coins), 272f, 1128f);
        font.draw(batch, Math.round(state.simulationSpeed) + "x", 492f, 1128f);
        batch.end();
    }

    private void button(float x, float y) {
        shapes.setColor(0.10f, 0.20f, 0.19f, 1f);
        shapes.rect(x, y, HudTouchLayout.BUTTON_WIDTH, HudTouchLayout.BUTTON_HEIGHT);
        shapes.setColor(0.84f, 0.68f, 0.30f, 1f);
        shapes.rect(x, y + HudTouchLayout.BUTTON_HEIGHT - 5f, HudTouchLayout.BUTTON_WIDTH, 5f);
    }

    @Override
    public void close() {
        font.dispose();
        shapes.dispose();
    }
}
