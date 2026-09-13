package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.PauseTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;

import java.util.Locale;

/** Premium pause surface: dimmed live arena, run context, and three framed touch actions. */
public final class PauseOverlayRenderer implements AutoCloseable {
    static final float TITLE_PANEL_X = 60f;
    static final float TITLE_PANEL_Y = 1096f;
    static final float TITLE_PANEL_WIDTH = 600f;
    static final float TITLE_PANEL_HEIGHT = 128f;
    static final float CONTEXT_PANEL_X = 60f;
    static final float CONTEXT_PANEL_Y = 250f;
    static final float CONTEXT_PANEL_WIDTH = 600f;
    static final float CONTEXT_PANEL_HEIGHT = 108f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        UiIconRenderer icons,
        UiFrameRenderer frames
    ) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.030f, 0.075f, 0.066f, 0.74f);
        shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
        shapes.setColor(0.07f, 0.19f, 0.16f, 0.55f);
        shapes.rect(0f, 1096f, 720f, ScreenEdges.top() - 1096f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        UiFrameRenderer.State resumeState = frames.resolve(
            true, false, PauseTouchLayout.BUTTON_X, PauseTouchLayout.RESUME_Y,
            PauseTouchLayout.BUTTON_WIDTH, PauseTouchLayout.RESUME_HEIGHT
        );
        UiFrameRenderer.State inventoryState = frames.resolve(
            true, false, PauseTouchLayout.BUTTON_X, PauseTouchLayout.INVENTORY_Y,
            PauseTouchLayout.BUTTON_WIDTH, PauseTouchLayout.SECONDARY_HEIGHT
        );
        UiFrameRenderer.State shopState = frames.resolve(
            true, false, PauseTouchLayout.BUTTON_X, PauseTouchLayout.SHOP_Y,
            PauseTouchLayout.BUTTON_WIDTH, PauseTouchLayout.SECONDARY_HEIGHT
        );

        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(
            batch, UiFrameRenderer.Kind.PANEL,
            TITLE_PANEL_X, TITLE_PANEL_Y, TITLE_PANEL_WIDTH, TITLE_PANEL_HEIGHT, true, false
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            PauseTouchLayout.BUTTON_X, PauseTouchLayout.SHOP_Y,
            PauseTouchLayout.BUTTON_WIDTH, PauseTouchLayout.SECONDARY_HEIGHT, true, false
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            PauseTouchLayout.BUTTON_X, PauseTouchLayout.INVENTORY_Y,
            PauseTouchLayout.BUTTON_WIDTH, PauseTouchLayout.SECONDARY_HEIGHT, true, false
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            PauseTouchLayout.BUTTON_X, PauseTouchLayout.RESUME_Y,
            PauseTouchLayout.BUTTON_WIDTH, PauseTouchLayout.RESUME_HEIGHT, true, true
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.PANEL,
            CONTEXT_PANEL_X, CONTEXT_PANEL_Y, CONTEXT_PANEL_WIDTH, CONTEXT_PANEL_HEIGHT,
            true, false
        );

        icons.draw(batch, "pause", 84f, 1122f, 76f);
        text.draw(batch, "COMBAT PAUSED", 180f, 1196f, 1.36f, OverlayText.GOLD);
        text.draw(batch, "The arena holds still until you return", 180f, 1150f, 0.74f,
            OverlayText.SUBTLE);

        drawAction(
            batch, icons, "shop", "STAT SHOP", "Spend earned coins on permanent upgrades",
            PauseTouchLayout.SHOP_Y, PauseTouchLayout.SECONDARY_HEIGHT, shopState
        );
        drawAction(
            batch, icons, "inventory", "INVENTORY", "Equip, compare, and sell gear",
            PauseTouchLayout.INVENTORY_Y, PauseTouchLayout.SECONDARY_HEIGHT, inventoryState
        );
        float resumeOffset = MainMenuRenderer.pressedOffset(resumeState);
        icons.draw(batch, "continue", 132f, PauseTouchLayout.RESUME_Y + 62f + resumeOffset, 116f,
            resumeState);
        text.draw(batch, "RESUME", 274f, PauseTouchLayout.RESUME_Y + 158f + resumeOffset, 1.62f,
            OverlayText.GOLD);
        text.draw(batch, "Return to the battle", 274f,
            PauseTouchLayout.RESUME_Y + 104f + resumeOffset, 0.80f, OverlayText.IVORY);

        icons.draw(batch, "wave", 84f, 272f, 62f);
        text.draw(batch, waveLabel(state), 160f, 328f, 0.66f, OverlayText.GOLD);
        text.draw(batch, "WAVE " + state.waveNumber + " / " + GameState.FINAL_WAVE, 160f, 294f,
            0.96f, OverlayText.IVORY);
        icons.draw(batch, "coin", 392f, 272f, 62f);
        text.draw(batch, "COINS", 468f, 328f, 0.66f, OverlayText.GOLD);
        text.draw(batch, MainMenuRenderer.coinTotalLabel(state.coins), 468f, 294f, 0.96f,
            OverlayText.IVORY);
        batch.end();
    }

    private void drawAction(
        SpriteBatch batch,
        UiIconRenderer icons,
        String icon,
        String title,
        String subtitle,
        float y,
        float height,
        UiFrameRenderer.State state
    ) {
        float offset = MainMenuRenderer.pressedOffset(state);
        icons.draw(batch, icon, 132f, y + (height - 84f) * 0.5f + offset, 84f, state);
        text.draw(batch, title, 246f, y + height - 40f + offset, 1.16f, OverlayText.IVORY);
        text.draw(batch, subtitle, 246f, y + 44f + offset, 0.72f, OverlayText.SUBTLE);
    }

    static String waveLabel(GameState state) {
        if (state == null) return "CURRENT WAVE";
        if (state.waveNumber % 5 == 0) return "BOSS WAVE";
        return state.heroLevel > 0
            ? ("HERO LEVEL " + state.heroLevel).toUpperCase(Locale.ROOT)
            : "CURRENT WAVE";
    }

    @Override
    public void close() {
        text.close();
        shapes.dispose();
    }
}
