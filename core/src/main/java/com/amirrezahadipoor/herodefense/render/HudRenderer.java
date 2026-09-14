package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.gameplay.HeroProgressionSystem;
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
    static final float EXP_BAR_X = 91f;
    static final float EXP_BAR_Y = 1188f;
    static final float EXP_BAR_WIDTH = 580f;
    static final float EXP_BAR_HEIGHT = 9f;
    static final float INFO_PANEL_Y = 1065f;
    static final float INFO_PANEL_HEIGHT = 100f;

    private static final Color GOLD = Color.valueOf("EAC66D");
    private static final Color IVORY = Color.valueOf("F3E4BC");
    private static final Color SUBTLE = Color.valueOf("B8C4AF");
    private static final Color HEALTHY = Color.valueOf("48A96A");
    private static final Color WOUNDED = Color.valueOf("D39A43");
    private static final Color CRITICAL = Color.valueOf("C6534F");
    private static final Color EXP = Color.valueOf("8FD4F2");
    private static final Color EXP_FLASH = Color.valueOf("F3E4BC");

    private static final HeroProgressionSystem PROGRESSION = new HeroProgressionSystem();
    /** Real-time countdown of the level-up flash on the EXP bar. */
    private float levelFlashSeconds;
    private int lastSeenLevel = -1;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();

    public HudRenderer() {
    }

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        UiIconRenderer icons,
        UiFrameRenderer frames
    ) {
        draw(batch, projection, state, icons, frames, 0f);
    }

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        UiIconRenderer icons,
        UiFrameRenderer frames,
        float realDeltaSeconds
    ) {
        float healthRatio = healthRatio(state.hero.health, state.hero.maxHealth);
        float expRatio = experienceRatio(state);
        if (lastSeenLevel >= 0 && state.heroLevel > lastSeenLevel) levelFlashSeconds = LEVEL_FLASH_SECONDS;
        lastSeenLevel = state.heroLevel;
        levelFlashSeconds = Math.max(0f, levelFlashSeconds - Math.max(0f, realDeltaSeconds));
        UiFrameRenderer.State speedState = frames.resolve(
            true, state.simulationSpeed > 1f,
            HudTouchLayout.SPEED_X, HudTouchLayout.buttonY(),
            HudTouchLayout.BUTTON_WIDTH, HudTouchLayout.BUTTON_HEIGHT
        );
        UiFrameRenderer.State pauseState = frames.resolve(
            true, false,
            HudTouchLayout.PAUSE_X, HudTouchLayout.buttonY(),
            HudTouchLayout.BUTTON_WIDTH, HudTouchLayout.BUTTON_HEIGHT
        );
        UiFrameRenderer.State inventoryState = frames.resolve(
            true, false,
            HudTouchLayout.INVENTORY_X, HudTouchLayout.utilityButtonY(),
            HudTouchLayout.UTILITY_BUTTON_WIDTH, HudTouchLayout.UTILITY_BUTTON_HEIGHT
        );
        UiFrameRenderer.State shopState = frames.resolve(
            true, false,
            HudTouchLayout.SHOP_X, HudTouchLayout.utilityButtonY(),
            HudTouchLayout.UTILITY_BUTTON_WIDTH, HudTouchLayout.UTILITY_BUTTON_HEIGHT
        );

        float up = HudTouchLayout.topShift();
        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(
            batch, UiFrameRenderer.Kind.PANEL,
            HEALTH_PANEL_X, HEALTH_PANEL_Y + up, HEALTH_PANEL_WIDTH, HEALTH_PANEL_HEIGHT,
            true, false
        );
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 18f, INFO_PANEL_Y + up, 194f,
            INFO_PANEL_HEIGHT, true, false);
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 220f, INFO_PANEL_Y + up, 194f,
            INFO_PANEL_HEIGHT, true, false);
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            HudTouchLayout.SPEED_X, HudTouchLayout.buttonY(),
            HudTouchLayout.BUTTON_WIDTH, HudTouchLayout.BUTTON_HEIGHT,
            true, state.simulationSpeed > 1f
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            HudTouchLayout.PAUSE_X, HudTouchLayout.buttonY(),
            HudTouchLayout.BUTTON_WIDTH, HudTouchLayout.BUTTON_HEIGHT, true, false
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            HudTouchLayout.INVENTORY_X, HudTouchLayout.utilityButtonY(),
            HudTouchLayout.UTILITY_BUTTON_WIDTH, HudTouchLayout.UTILITY_BUTTON_HEIGHT,
            true, false
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            HudTouchLayout.SHOP_X, HudTouchLayout.utilityButtonY(),
            HudTouchLayout.UTILITY_BUTTON_WIDTH, HudTouchLayout.UTILITY_BUTTON_HEIGHT,
            true, false
        );
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.055f, 0.035f, 0.030f, 0.98f);
        shapes.rect(HEALTH_BAR_X, HEALTH_BAR_Y + up, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        shapes.setColor(healthColor(healthRatio));
        shapes.rect(
            HEALTH_BAR_X + 2f,
            HEALTH_BAR_Y + up + 2f,
            Math.max(0f, (HEALTH_BAR_WIDTH - 4f) * healthRatio),
            HEALTH_BAR_HEIGHT - 4f
        );
        shapes.setColor(0.90f, 0.98f, 0.82f, 0.18f);
        shapes.rect(
            HEALTH_BAR_X + 3f,
            HEALTH_BAR_Y + up + HEALTH_BAR_HEIGHT - 7f,
            Math.max(0f, (HEALTH_BAR_WIDTH - 6f) * healthRatio),
            3f
        );
        // EXP: a slim cyan bar under the health bar; flashes ivory for a moment on level-up.
        shapes.setColor(0.055f, 0.035f, 0.030f, 0.98f);
        shapes.rect(EXP_BAR_X, EXP_BAR_Y + up, EXP_BAR_WIDTH, EXP_BAR_HEIGHT);
        float flash = levelFlashSeconds / LEVEL_FLASH_SECONDS;
        shapes.setColor(
            EXP.r + (EXP_FLASH.r - EXP.r) * flash,
            EXP.g + (EXP_FLASH.g - EXP.g) * flash,
            EXP.b + (EXP_FLASH.b - EXP.b) * flash,
            1f
        );
        float expFill = flash > 0f ? Math.max(expRatio, flash) : expRatio;
        shapes.rect(
            EXP_BAR_X + 1.5f,
            EXP_BAR_Y + up + 1.5f,
            Math.max(0f, (EXP_BAR_WIDTH - 3f) * expFill),
            EXP_BAR_HEIGHT - 3f
        );
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        icons.draw(batch, "health", 28f, 1194f + up, 50f);
        drawShadowed(batch, "LV " + state.heroLevel, 102f, 1199f + up, 0.52f,
            flash > 0f ? EXP_FLASH : EXP);
        text.drawRightAligned(batch, experienceLabel(state), 580f, 1199f + up, 0.46f, SUBTLE);
        if (state.ascensionTier > 0) {
            drawShadowed(batch, "T" + state.ascensionTier, 620f, 1199f + up, 0.58f, GOLD);
        }
        drawShadowed(batch, "HEALTH", 102f, 1244f + up, 0.68f, GOLD);
        drawShadowedCentered(
            batch,
            Math.round(state.hero.health) + " / " + Math.round(state.hero.maxHealth),
            381f,
            1226f + up,
            0.84f,
            IVORY
        );

        icons.draw(batch, "wave", 29f, 1087f + up, 48f);
        drawShadowed(batch, "WAVE", 84f, 1144f + up, 0.66f, GOLD);
        drawShadowed(batch, state.waveNumber + " / " + GameState.FINAL_WAVE,
            84f, 1107f + up, 1.02f, IVORY);

        icons.draw(batch, "coin", 231f, 1087f + up, 48f);
        drawShadowed(batch, "COINS", 286f, 1144f + up, 0.66f, GOLD);
        drawShadowed(batch, "$ " + Math.max(0, state.coins), 286f, 1107f + up, 1.02f, IVORY);

        float speedOffset = MainMenuRenderer.pressedOffset(speedState);
        icons.draw(batch, "speed", 440f, 1091f + up + speedOffset, 44f, speedState);
        drawShadowed(batch, Math.round(state.simulationSpeed) + "x",
            487f, 1124f + up + speedOffset, 0.96f, IVORY);

        float pauseOffset = MainMenuRenderer.pressedOffset(pauseState);
        icons.draw(batch, "pause", 603f, 1088f + up + pauseOffset, 54f, pauseState);

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
        float offset = MainMenuRenderer.pressedOffset(state) - HudTouchLayout.bottomShift();
        icons.draw(batch, icon, x + 13f, 48f + offset, 56f, state);
        drawShadowedCentered(batch, label, x + 105f, 87f + offset, 0.74f, IVORY);
    }

    private void drawShadowedCentered(
        SpriteBatch batch, String label, float centerX, float y, float scale, Color color
    ) {
        text.drawCentered(batch, label, centerX, y, scale, color);
    }

    private void drawShadowed(
        SpriteBatch batch, String label, float x, float y, float scale, Color color
    ) {
        text.draw(batch, label, x, y, scale, color);
    }

    static final float LEVEL_FLASH_SECONDS = 0.9f;

    /** Progress toward the next level; a capped Hero shows a full bar. */
    static float experienceRatio(GameState state) {
        int required = PROGRESSION.experienceRequiredForNextLevel(state.heroLevel);
        if (required <= 0) return 1f;
        return Math.max(0f, Math.min(1f, state.heroExperience / (float) required));
    }

    static String experienceLabel(GameState state) {
        int required = PROGRESSION.experienceRequiredForNextLevel(state.heroLevel);
        return required <= 0 ? "MAX" : state.heroExperience + " / " + required + " XP";
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
        text.close();
        shapes.dispose();
    }
}
