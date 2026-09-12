package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.settings.GameSettings;

/** Main-menu settings overlay with large sound and music toggles. */
public final class SettingsOverlayRenderer implements AutoCloseable {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();

    public SettingsOverlayRenderer() {
        font.getData().setScale(1.5f);
    }

    public void draw(
        SpriteBatch batch, Matrix4 projection, GameSettings settings, UiIconRenderer icons
    ) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.025f, 0.055f, 0.065f, 0.99f);
        shapes.rect(0f, 0f, 720f, 1280f);
        panel(100f, 700f, 520f, 150f);
        panel(100f, 500f, 520f, 150f);
        panel(570f, 1120f, 100f, 100f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(Color.valueOf("F3E4BC"));
        icons.draw(batch, "settings", 215f, 990f, 82f);
        icons.draw(batch, "close", 588f, 1138f, 64f);
        font.draw(batch, "Settings", 315f, 1060f);
        font.draw(batch, "Sound effects", 140f, 792f);
        font.draw(batch, settings.soundEnabled ? "ON" : "OFF", 515f, 792f);
        font.draw(batch, "Music", 140f, 592f);
        font.draw(batch, settings.musicEnabled ? "ON" : "OFF", 515f, 592f);
        batch.end();
    }

    private void panel(float x, float y, float width, float height) {
        shapes.setColor(0.10f, 0.20f, 0.19f, 1f);
        shapes.rect(x, y, width, height);
        shapes.setColor(0.84f, 0.68f, 0.30f, 1f);
        shapes.rect(x, y + height - 5f, width, 5f);
    }

    @Override
    public void close() {
        font.dispose();
        shapes.dispose();
    }
}
