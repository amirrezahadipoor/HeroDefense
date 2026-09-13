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
import com.amirrezahadipoor.herodefense.input.HudTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Premium segmented portrait HUD that preserves a clear view of the active arena. */
public final class HudRenderer implements AutoCloseable {
    static final float HEALTH_PANEL_X = 18f;
    static final float HEALTH_PANEL_Y = 1180f;
    static final float HEALTH_PANEL_WIDTH = 684f;
    static final float HEALTH_PANEL_HEIGHT = 78f;
    static final float HEALTH_BAR_X = 91f;
    static final float HEALTH_BAR_Y = 1203f;
    static final float HEALTH_BAR_WIDTH = 580f;
    static final float HEALTH_BAR_HEIGHT = 25f;
    static final float INFO_PANEL_Y = 1065f;
    static final float INFO_PANEL_HEIGHT = 100f;

    private static final Color GOLD = Color.valueOf("EAC66D");
    private static final Color IVORY = Color.valueOf("F3E4BC");
    private static final Color SUBTLE = Color.valueOf("B8C4AF");
    private static final Color HEALTHY = Color.valueOf("48A96A");
    private static final Color WOUNDED = Color.valueOf("D39A43");
    private static final Color CRITICAL = Color.valueOf("C6534F");

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    public HudRenderer() {
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
        float healthRatio = healthRatio(state.hero.health, state.hero.maxHealth);
        UiFrameRenderer.State speedState = frames.resolve(
            true, state.simulationSpeed > 1f,
            HudTouchLayout.SPEED_X, HudTouchLayout.BUTTON_Y,
            HudTouchLayout.BUTTON_WIDTH, HudTouchLayout.BUTTON_HEIGHT
        );
        UiFrameRenderer.State pauseState = frames.resolve(
            true, false,
            HudTouchLayout.PAUSE_X, HudTouchLayout.BUTTON_Y,
            HudTouchLayout.BUTTON_WIDTH, HudTouchLayout.BUTTON_HEIGHT
        );
        UiFrameRenderer.State inventoryState = frames.resolve(
            true, false,
            HudTouchLayout.INVENTORY_X, HudTouchLayout.UTILITY_BUTTON_Y,
            HudTouchLayout.UTILITY_BUTTON_WIDTH, HudTouchLayout.UTILITY_BUTTON_HEIGHT
        );
        UiFrameRenderer.State shopState = frames.resolve(
            true, false,
            HudTouchLayout.SHOP_X, HudTouchLayout.UTILITY_BUTTON_Y,
            HudTouchLayout.UTILITY_BUTTON_WIDTH, HudTouchLayout.UTILITY_BUTTON_HEIGHT
        );

        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(
            batch, UiFrameRenderer.Kind.PANEL,
            HEALTH_PANEL_X, HEALTH_PANEL_Y, HEALTH_PANEL_WIDTH, HEALTH_PANEL_HEIGHT,
            true, false
        );
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 18f, INFO_PANEL_Y, 194f,
            INFO_PANEL_HEIGHT, true, false);
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 220f, INFO_PANEL_Y, 194f,
            INFO_PANEL_HEIGHT, true, false);
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            HudTouchLayout.SPEED_X, HudTouchLayout.BUTTON_Y,
            HudTouchLayout.BUTTON_WIDTH, HudTouchLayout.BUTTON_HEIGHT,
            true, state.simulationSpeed > 1f
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            HudTouchLayout.PAUSE_X, HudTouchLayout.BUTTON_Y,
            HudTouchLayout.BUTTON_WIDTH, HudTouchLayout.BUTTON_HEIGHT, true, false
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            HudTouchLayout.INVENTORY_X, HudTouchLayout.UTILITY_BUTTON_Y,
            HudTouchLayout.UTILITY_BUTTON_WIDTH, HudTouchLayout.UTILITY_BUTTON_HEIGHT,
            true, false
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            HudTouchLayout.SHOP_X, HudTouchLayout.UTILITY_BUTTON_Y,
            HudTouchLayout.UTILITY_BUTTON_WIDTH, HudTouchLayout.UTILITY_BUTTON_HEIGHT,
            true, false
        );
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.055f, 0.035f, 0.030f, 0.98f);
        shapes.rect(HEALTH_BAR_X, HEALTH_BAR_Y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        shapes.setColor(healthColor(healthRatio));
        shapes.rect(
            HEALTH_BAR_X + 2f,
            HEALTH_BAR_Y + 2f,
            Math.max(0f, (HEALTH_BAR_WIDTH - 4f) * healthRatio),
            HEALTH_BAR_HEIGHT - 4f
        );
        shapes.setColor(0.90f, 0.98f, 0.82f, 0.18f);
        shapes.rect(
            HEALTH_BAR_X + 3f,
            HEALTH_BAR_Y + HEALTH_BAR_HEIGHT - 7f,
            Math.max(0f, (HEALTH_BAR_WIDTH - 6f) * healthRatio),
            3f
        );
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        icons.draw(batch, "health", 28f, 1194f, 50f);
        drawShadowed(batch, "HEALTH", 102f, 1244f, 0.68f, GOLD);
        drawShadowedCentered(
            batch,
            Math.round(state.hero.health) + " / " + Math.round(state.hero.maxHealth),
            381f,
            1226f,
            0.84f,
            IVORY
        );

        icons.draw(batch, "wave", 29f, 1087f, 48f);
        drawShadowed(batch, "WAVE", 84f, 1144f, 0.66f, GOLD);
        drawShadowed(batch, state.waveNumber + " / " + GameState.FINAL_WAVE,
            84f, 1107f, 1.02f, IVORY);

        icons.draw(batch, "coin", 231f, 1087f, 48f);
        drawShadowed(batch, "COINS", 286f, 1144f, 0.66f, GOLD);
        drawShadowed(batch, "$ " + Math.max(0, state.coins), 286f, 1107f, 1.02f, IVORY);

        float speedOffset = MainMenuRenderer.pressedOffset(speedState);
        icons.draw(batch, "speed", 440f, 1091f + speedOffset, 44f, speedState);
        drawShadowed(batch, Math.round(state.simulationSpeed) + "x",
            487f, 1124f + speedOffset, 0.96f, IVORY);

        float pauseOffset = MainMenuRenderer.pressedOffset(pauseState);
        icons.draw(batch, "pause", 603f, 1088f + pauseOffset, 54f, pauseState);

        drawUtilityAction(
            batch, icons, "inventory", "INVENTORY",
            HudTouchLayout.INVENTORY_X, inventoryState
        );
        drawUtilityAction(
            batch, icons, "shop", "SHOP",
            HudTouchLayout.SHOP_X, shopState
        );
        batch.end();
    }

    private void drawUtilityAction(
        SpriteBatch batch,
        UiIconRenderer icons,
        String icon,
        String label,
        float x,
        UiFrameRenderer.State state
    ) {
        float offset = MainMenuRenderer.pressedOffset(state);
        icons.draw(batch, icon, x + 13f, 48f + offset, 56f, state);
        drawShadowedCentered(batch, label, x + 105f, 87f + offset, 0.74f, IVORY);
    }

    private void drawShadowedCentered(
        SpriteBatch batch, String text, float centerX, float baselineY, float scale, Color color
    ) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        drawShadowed(batch, text, centerX - layout.width * 0.5f, baselineY, scale, color);
    }

    private void drawShadowed(
        SpriteBatch batch, String text, float x, float y, float scale, Color color
    ) {
        font.getData().setScale(scale);
        font.setColor(0.005f, 0.012f, 0.010f, 0.92f);
        font.draw(batch, text, x + 2f, y - 2f);
        font.setColor(color);
        font.draw(batch, text, x, y);
    }

    static float healthRatio(float health, float maxHealth) {
        if (maxHealth <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, health / maxHealth));
    }

    static Color healthColor(float ratio) {
        if (ratio < 0.25f) return CRITICAL;
        if (ratio < 0.50f) return WOUNDED;
        return HEALTHY;
    }

    static float occupiedTopArea() {
        return HEALTH_PANEL_WIDTH * HEALTH_PANEL_HEIGHT
            + 194f * INFO_PANEL_HEIGHT * 2f
            + HudTouchLayout.BUTTON_WIDTH * HudTouchLayout.BUTTON_HEIGHT * 2f;
    }

    @Override
    public void close() {
        font.dispose();
        shapes.dispose();
    }
}
