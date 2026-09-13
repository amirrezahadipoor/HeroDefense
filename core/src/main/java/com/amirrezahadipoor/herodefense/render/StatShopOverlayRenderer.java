package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.StatShopTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.shop.StatShopSystem;

import java.util.Locale;

/** Premium coin-only stat shop with explicit affordability and paused-game context. */
public final class StatShopOverlayRenderer implements AutoCloseable {
    private static final Color GOLD = Color.valueOf("EAC66D");
    private static final Color IVORY = Color.valueOf("F3E4BC");
    private static final Color SUBTLE = Color.valueOf("AEBCAE");
    private static final Color POSITIVE = Color.valueOf("69C884");
    private static final Color NEGATIVE = Color.valueOf("DF6A65");
    private static final Color MUTED = Color.valueOf("777D76");

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();

    public StatShopOverlayRenderer() {
    }

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        StatShopSystem shop,
        UiIconRenderer icons,
        UiFrameRenderer frames,
        boolean returnsToPause
    ) {
        beginShapes(projection);
        shapes.setColor(0.040f, 0.090f, 0.080f, 0.97f);
        shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
        shapes.setColor(0.07f, 0.19f, 0.16f, 0.82f);
        shapes.rect(0f, 1160f, 720f, 120f);
        shapes.end();
        endShapes();

        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            StatShopTouchLayout.CLOSE_X, StatShopTouchLayout.CLOSE_Y,
            StatShopTouchLayout.CLOSE_SIZE, StatShopTouchLayout.CLOSE_SIZE,
            true, false
        );
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 40f, 1080f, 225f, 90f, true, false);
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 285f, 1080f, 260f, 90f, true, false);
        for (int index = 0; index < HeroStat.values().length; index++) {
            HeroStat stat = HeroStat.values()[index];
            float y = rowY(index);
            int purchased = shop.purchasedLevels(state, stat);
            boolean maxed = purchased >= StatShopSystem.MAX_PURCHASES_PER_STAT;
            int price = maxed ? 0 : shop.price(state, stat);
            boolean affordable = !maxed && state.coins >= price;
            frames.draw(
                batch, UiFrameRenderer.Kind.BUTTON,
                StatShopTouchLayout.ROW_X, y,
                StatShopTouchLayout.ROW_WIDTH, StatShopTouchLayout.ROW_HEIGHT,
                affordable, false
            );
        }
        if (shop.feedbackMessage() != null) {
            frames.draw(batch, UiFrameRenderer.Kind.PANEL, 100f, 215f, 520f, 64f, true, false);
        }
        batch.end();

        beginShapes(projection);
        for (int index = 0; index < HeroStat.values().length; index++) {
            HeroStat stat = HeroStat.values()[index];
            float y = rowY(index);
            int purchased = shop.purchasedLevels(state, stat);
            boolean maxed = purchased >= StatShopSystem.MAX_PURCHASES_PER_STAT;
            int price = maxed ? 0 : shop.price(state, stat);
            boolean affordable = !maxed && state.coins >= price;
            shapes.setColor(maxed ? GOLD : affordable ? POSITIVE : NEGATIVE);
            shapes.rect(StatShopTouchLayout.ROW_X + 6f, y + 16f, 6f,
                StatShopTouchLayout.ROW_HEIGHT - 32f);
            shapes.setColor(0.070f, 0.105f, 0.092f, 0.95f);
            shapes.rect(170f, y + 25f, 205f, 10f);
            shapes.setColor(maxed ? GOLD : POSITIVE);
            shapes.rect(
                170f,
                y + 25f,
                205f * purchased / StatShopSystem.MAX_PURCHASES_PER_STAT,
                10f
            );
        }
        shapes.end();
        endShapes();

        batch.begin();
        UiFrameRenderer.State closeState = frames.resolve(
            true, false,
            StatShopTouchLayout.CLOSE_X, StatShopTouchLayout.CLOSE_Y,
            StatShopTouchLayout.CLOSE_SIZE, StatShopTouchLayout.CLOSE_SIZE
        );
        icons.draw(batch, "close", 588f, 1138f, 64f, closeState);
        drawText(batch, "WORLD TREE ARMORY", 40f, 1232f, 1.36f, GOLD);
        drawText(batch, "Permanent upgrades bought only with earned coins", 40f, 1190f, 0.72f, SUBTLE);
        icons.draw(batch, "coin", 52f, 1097f, 58f);
        drawText(batch, "BALANCE", 119f, 1147f, 0.62f, GOLD);
        drawText(batch, "$ " + Math.max(0, state.coins), 119f, 1111f, 1.00f, IVORY);
        drawText(batch, "COMBAT PAUSED", 306f, 1147f, 0.68f, POSITIVE);
        drawText(
            batch,
            returnsToPause ? "Close returns to Pause" : "Close returns to battle",
            306f,
            1110f,
            0.68f,
            SUBTLE
        );

        for (int index = 0; index < HeroStat.values().length; index++) {
            HeroStat stat = HeroStat.values()[index];
            float y = rowY(index);
            int purchased = shop.purchasedLevels(state, stat);
            boolean maxed = purchased >= StatShopSystem.MAX_PURCHASES_PER_STAT;
            int price = maxed ? 0 : shop.price(state, stat);
            boolean affordable = !maxed && state.coins >= price;
            UiFrameRenderer.State cardState = frames.resolve(
                affordable, false,
                StatShopTouchLayout.ROW_X, y,
                StatShopTouchLayout.ROW_WIDTH, StatShopTouchLayout.ROW_HEIGHT
            );
            float offset = MainMenuRenderer.pressedOffset(cardState);
            icons.draw(batch, stat.name().toLowerCase(Locale.ROOT), 72f, y + 28f + offset, 78f, cardState);
            drawText(batch, pretty(stat).toUpperCase(Locale.ROOT), 170f, y + 105f + offset, 0.98f,
                affordable || maxed ? IVORY : MUTED);
            drawText(batch, statBenefit(stat), 170f, y + 69f + offset, 0.66f, SUBTLE);
            drawText(
                batch,
                "LEVEL " + purchased + " / " + StatShopSystem.MAX_PURCHASES_PER_STAT,
                170f,
                y + 45f + offset,
                0.58f,
                GOLD
            );
            Color affordabilityColor = maxed ? GOLD : affordable ? POSITIVE : NEGATIVE;
            drawCentered(
                batch,
                affordabilityLabel(maxed, affordable, price, state.coins),
                532f,
                y + 94f + offset,
                0.62f,
                affordabilityColor
            );
            drawCentered(
                batch,
                maxed ? "MAX" : "$ " + price,
                532f,
                y + 57f + offset,
                0.92f,
                maxed ? GOLD : affordable ? IVORY : MUTED
            );
        }

        String feedback = shop.feedbackMessage();
        if (feedback != null) {
            Color base = switch (shop.feedbackResult()) {
                case PURCHASED -> POSITIVE;
                case INSUFFICIENT_COINS -> NEGATIVE;
                case MAXED -> GOLD;
                default -> SUBTLE;
            };
            Color feedbackColor = new Color(base);
            feedbackColor.a = shop.feedbackAlpha();
            drawCentered(batch, feedback, 360f, 254f, 0.78f, feedbackColor);
        }
        batch.end();
    }

    static String affordabilityLabel(boolean maxed, boolean affordable, int price, int coins) {
        if (maxed) return "MAXED";
        if (affordable) return "AFFORDABLE";
        return "NEED $ " + Math.max(0, price - coins);
    }

    static String statBenefit(HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> "+1 base damage";
            case AGILITY -> "+1 attack-speed rating";
            case LUCK -> "+1 critical-chance rating";
            case DODGE -> "+1 dodge rating";
            case HEALTH -> "+1 vitality and max health";
        };
    }

    private static float rowY(int index) {
        return StatShopTouchLayout.ROW_TOP - StatShopTouchLayout.ROW_HEIGHT
            - index * StatShopTouchLayout.ROW_STRIDE;
    }

    private void drawCentered(
        SpriteBatch batch, String label, float centerX, float y, float scale, Color color
    ) {
        text.drawCentered(batch, label, centerX, y, scale, color);
    }

    private void drawText(
        SpriteBatch batch, String label, float x, float y, float scale, Color color
    ) {
        text.draw(batch, label, x, y, scale, color);
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

    private static String pretty(HeroStat stat) {
        String text = stat.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    @Override
    public void close() {
        text.close();
        shapes.dispose();
    }
}
