package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.LevelUpTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;

import java.util.Locale;

/** Five large touch rows for spending every pending level-up talent point. */
public final class LevelUpOverlayRenderer implements AutoCloseable {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();

    public LevelUpOverlayRenderer() {
        font.getData().setScale(1.25f);
    }

    public void draw(SpriteBatch batch, Matrix4 projection, GameState state) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.025f, 0.055f, 0.065f, 0.97f);
        shapes.rect(0f, 0f, 720f, 1280f);
        for (int row = 0; row < HeroStat.values().length; row++) {
            float y = LevelUpTouchLayout.BOTTOM + row * LevelUpTouchLayout.ROW_STRIDE;
            shapes.setColor(0.10f, 0.20f, 0.19f, 1f);
            shapes.rect(
                LevelUpTouchLayout.LEFT,
                y,
                LevelUpTouchLayout.RIGHT - LevelUpTouchLayout.LEFT,
                LevelUpTouchLayout.BUTTON_HEIGHT
            );
            shapes.setColor(0.84f, 0.68f, 0.30f, 1f);
            shapes.rect(
                LevelUpTouchLayout.LEFT,
                y + LevelUpTouchLayout.BUTTON_HEIGHT - 5f,
                LevelUpTouchLayout.RIGHT - LevelUpTouchLayout.LEFT,
                5f
            );
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(Color.valueOf("F2D58A"));
        font.getData().setScale(1.75f);
        font.draw(batch, "Level " + state.heroLevel, 275f, 1160f);
        font.getData().setScale(1.15f);
        font.setColor(Color.valueOf("F3E4BC"));
        font.draw(batch, "Choose a stat · Points " + state.unspentTalentPoints, 210f, 1090f);
        for (int row = 0; row < HeroStat.values().length; row++) {
            HeroStat stat = HeroStat.values()[row];
            float y = LevelUpTouchLayout.BOTTOM + row * LevelUpTouchLayout.ROW_STRIDE;
            font.getData().setScale(1.28f);
            font.draw(batch, pretty(stat), 120f, y + 88f);
            font.getData().setScale(1f);
            font.draw(batch, description(stat), 120f, y + 42f);
            font.draw(batch, "Current " + value(state, stat), 475f, y + 66f);
        }
        font.getData().setScale(1.25f);
        batch.end();
    }

    private static int value(GameState state, HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> state.hero.stats.strength;
            case AGILITY -> state.hero.stats.agility;
            case LUCK -> state.hero.stats.luck;
            case DODGE -> state.hero.stats.dodge;
            case HEALTH -> state.hero.stats.health;
        };
    }

    private static String description(HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> "+2 damage";
            case AGILITY -> "+0.03 attacks/second";
            case LUCK -> "+2% item-drop multiplier";
            case DODGE -> "+0.5% dodge chance";
            case HEALTH -> "+10 maximum HP";
        };
    }

    private static String pretty(HeroStat stat) {
        String text = stat.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    @Override
    public void close() {
        font.dispose();
        shapes.dispose();
    }
}
