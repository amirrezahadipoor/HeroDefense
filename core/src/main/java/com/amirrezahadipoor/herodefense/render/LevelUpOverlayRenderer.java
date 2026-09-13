package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.LevelUpTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;

import java.util.Locale;

/** Premium mandatory five-card talent choice with explicit current and gained values. */
public final class LevelUpOverlayRenderer implements AutoCloseable {
    private static final Color GOLD = Color.valueOf("EAC66D");
    private static final Color IVORY = Color.valueOf("F3E4BC");
    private static final Color SUBTLE = Color.valueOf("AEBCAE");
    private static final Color POSITIVE = Color.valueOf("69C884");

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();

    public LevelUpOverlayRenderer() {
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
        UiFrameRenderer frames
    ) {
        beginShapes(projection);
        shapes.setColor(0.006f, 0.022f, 0.021f, 0.955f);
        shapes.rect(0f, 0f, 720f, 1280f);
        shapes.end();
        endShapes();

        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 90f, 1030f, 540f, 170f, true, false);
        for (int row = 0; row < HeroStat.values().length; row++) {
            float y = LevelUpTouchLayout.BOTTOM + row * LevelUpTouchLayout.ROW_STRIDE;
            frames.draw(
                batch, UiFrameRenderer.Kind.BUTTON,
                LevelUpTouchLayout.LEFT, y,
                LevelUpTouchLayout.RIGHT - LevelUpTouchLayout.LEFT,
                LevelUpTouchLayout.BUTTON_HEIGHT,
                true, false
            );
        }
        drawText(batch, "LEVEL " + state.heroLevel, 128f, 1151f, 1.50f, GOLD);
        drawText(batch, "CHOOSE ONE PERMANENT TALENT", 128f, 1102f, 0.78f, IVORY);
        drawText(batch, "POINTS  " + state.unspentTalentPoints, 492f, 1102f, 0.78f, POSITIVE);
        drawText(batch, "Combat is paused until every point is spent", 128f, 1063f, 0.68f, SUBTLE);

        for (int row = 0; row < HeroStat.values().length; row++) {
            HeroStat stat = HeroStat.values()[row];
            float y = LevelUpTouchLayout.BOTTOM + row * LevelUpTouchLayout.ROW_STRIDE;
            UiFrameRenderer.State cardState = frames.resolve(
                true, false,
                LevelUpTouchLayout.LEFT, y,
                LevelUpTouchLayout.RIGHT - LevelUpTouchLayout.LEFT,
                LevelUpTouchLayout.BUTTON_HEIGHT
            );
            float offset = MainMenuRenderer.pressedOffset(cardState);
            icons.draw(batch, stat.name().toLowerCase(Locale.ROOT), 108f, y + 24f + offset, 82f, cardState);
            drawText(batch, pretty(stat).toUpperCase(Locale.ROOT), 214f, y + 97f + offset, 1.00f, IVORY);
            drawText(batch, description(stat), 214f, y + 56f + offset, 0.70f, POSITIVE);
            drawText(batch, "CURRENT  " + value(state, stat), 484f, y + 74f + offset, 0.64f, GOLD);
            drawText(batch, "TAP TO ADD", 484f, y + 43f + offset, 0.56f, SUBTLE);
        }
        batch.end();
    }

    static int value(GameState state, HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> state.hero.stats.strength;
            case AGILITY -> state.hero.stats.agility;
            case LUCK -> state.hero.stats.luck;
            case DODGE -> state.hero.stats.dodge;
            case HEALTH -> state.hero.stats.health;
        };
    }

    static String description(HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> "+2 damage";
            case AGILITY -> "+0.03 attacks / second";
            case LUCK -> "+2% item-drop multiplier";
            case DODGE -> "+0.5% dodge chance";
            case HEALTH -> "+10 maximum health";
        };
    }

    private static String pretty(HeroStat stat) {
        String text = stat.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
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
