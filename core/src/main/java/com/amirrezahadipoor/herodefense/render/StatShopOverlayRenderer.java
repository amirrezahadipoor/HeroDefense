package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.StatShopTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.shop.StatShopSystem;

import java.util.Locale;

/** Coin-only stat-shop overlay aligned exactly with touch hit targets. */
public final class StatShopOverlayRenderer implements AutoCloseable {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();

    public StatShopOverlayRenderer() {
        font.getData().setScale(1.3f);
    }

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        StatShopSystem shop,
        UiIconRenderer icons
    ) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.025f, 0.055f, 0.065f, 0.97f);
        shapes.rect(0f, 0f, 720f, 1280f);
        shapes.setColor(0.20f, 0.34f, 0.29f, 1f);
        shapes.rect(
            StatShopTouchLayout.CLOSE_X,
            StatShopTouchLayout.CLOSE_Y,
            StatShopTouchLayout.CLOSE_SIZE,
            StatShopTouchLayout.CLOSE_SIZE
        );
        for (int index = 0; index < HeroStat.values().length; index++) {
            float y = StatShopTouchLayout.ROW_TOP - StatShopTouchLayout.ROW_HEIGHT
                - index * StatShopTouchLayout.ROW_STRIDE;
            panel(y);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(Color.valueOf("E7D8B1"));
        icons.draw(batch, "close", 588f, 1138f, 64f);
        font.draw(batch, "World Tree Armory", 225f, 1215f);
        font.draw(batch, "Coins: " + state.coins, 55f, 1148f);
        for (int index = 0; index < HeroStat.values().length; index++) {
            HeroStat stat = HeroStat.values()[index];
            float y = StatShopTouchLayout.ROW_TOP - StatShopTouchLayout.ROW_HEIGHT
                - index * StatShopTouchLayout.ROW_STRIDE;
            int purchased = shop.purchasedLevels(state, stat);
            icons.draw(batch, stat.name().toLowerCase(Locale.ROOT), 68f, y + 26f, 82f);
            font.draw(batch, pretty(stat), 165f, y + 92f);
            font.draw(batch, "Upgrade " + purchased + "/" + StatShopSystem.MAX_PURCHASES_PER_STAT, 165f, y + 43f);
            if (purchased >= StatShopSystem.MAX_PURCHASES_PER_STAT) {
                font.draw(batch, "MAX", 535f, y + 68f);
            } else {
                font.draw(batch, shop.price(state, stat) + " coins", 455f, y + 68f);
            }
        }
        batch.end();
    }

    private void panel(float y) {
        shapes.setColor(0.11f, 0.20f, 0.19f, 1f);
        shapes.rect(
            StatShopTouchLayout.ROW_X,
            y,
            StatShopTouchLayout.ROW_WIDTH,
            StatShopTouchLayout.ROW_HEIGHT
        );
        shapes.setColor(0.84f, 0.68f, 0.30f, 1f);
        shapes.rect(
            StatShopTouchLayout.ROW_X,
            y + StatShopTouchLayout.ROW_HEIGHT - 5f,
            StatShopTouchLayout.ROW_WIDTH,
            5f
        );
    }

    private static String pretty(HeroStat stat) {
        String text = stat.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    @Override
    public void close() {
        font.dispose();
        shapes.dispose();
    }
}
