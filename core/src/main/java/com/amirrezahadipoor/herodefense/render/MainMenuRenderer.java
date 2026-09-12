package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.MainMenuTouchLayout;

/** Touch-first English main menu with new game, continue, and settings. */
public final class MainMenuRenderer implements AutoCloseable {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();

    public MainMenuRenderer() {
        font.getData().setScale(1.45f);
    }

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        boolean continueAvailable,
        UiIconRenderer icons
    ) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.025f, 0.065f, 0.065f, 0.98f);
        shapes.rect(0f, 0f, 720f, 1280f);
        button(690f, true);
        button(500f, continueAvailable);
        button(310f, true);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(Color.valueOf("F2D58A"));
        font.getData().setScale(2.1f);
        font.draw(batch, "Hero Defense", 195f, 1080f);
        font.getData().setScale(1.15f);
        font.draw(batch, "Defend the World Tree through 100 waves", 120f, 990f);
        font.getData().setScale(1.45f);
        font.setColor(Color.valueOf("F3E4BC"));
        icons.draw(batch, "new_game", 150f, 720f, 84f);
        icons.draw(batch, "continue", 150f, 530f, 84f);
        icons.draw(batch, "settings", 150f, 340f, 84f);
        font.draw(batch, "New Game", 268f, 778f);
        font.setColor(continueAvailable ? Color.valueOf("F3E4BC") : Color.valueOf("77776F"));
        font.draw(batch, "Continue", 275f, 588f);
        font.setColor(Color.valueOf("F3E4BC"));
        font.draw(batch, "Settings", 276f, 398f);
        batch.end();
    }

    private void button(float y, boolean enabled) {
        shapes.setColor(enabled ? Color.valueOf("183832") : Color.valueOf("222B29"));
        shapes.rect(
            MainMenuTouchLayout.BUTTON_X,
            y,
            MainMenuTouchLayout.BUTTON_WIDTH,
            MainMenuTouchLayout.BUTTON_HEIGHT
        );
        shapes.setColor(enabled ? Color.valueOf("D6AD4C") : Color.valueOf("555B57"));
        shapes.rect(
            MainMenuTouchLayout.BUTTON_X,
            y + MainMenuTouchLayout.BUTTON_HEIGHT - 6f,
            MainMenuTouchLayout.BUTTON_WIDTH,
            6f
        );
    }

    @Override
    public void close() {
        font.dispose();
        shapes.dispose();
    }
}
