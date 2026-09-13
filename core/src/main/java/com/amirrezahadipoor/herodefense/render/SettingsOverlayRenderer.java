package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.SettingsTouchLayout;
import com.amirrezahadipoor.herodefense.settings.GameSettings;

/** Premium settings surface: two large explicit ON/OFF toggles over the reviewed arena. */
public final class SettingsOverlayRenderer implements AutoCloseable {
    static final float CLOSE_X = 570f;
    static final float CLOSE_Y = 1120f;
    static final float CLOSE_SIZE = 100f;
    static final float SOUND_ROW_Y = 700f;
    static final float MUSIC_ROW_Y = 500f;
    static final float NOTE_PANEL_X = 100f;
    static final float NOTE_PANEL_Y = 300f;
    static final float NOTE_PANEL_WIDTH = 520f;
    static final float NOTE_PANEL_HEIGHT = 120f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();
    private Texture backdrop;

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameSettings settings,
        UiIconRenderer icons,
        UiFrameRenderer frames
    ) {
        batch.setProjectionMatrix(projection);
        batch.begin();
        batch.setColor(0.70f, 0.80f, 0.76f, 1f);
        ScreenEdges.drawCover(batch, backdrop());
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.060f, 0.130f, 0.112f, 0.58f);
        shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
        shapes.setColor(0.07f, 0.19f, 0.16f, 0.62f);
        shapes.rect(0f, 1096f, 720f, ScreenEdges.top() - 1096f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        UiFrameRenderer.State closeState = frames.resolve(
            true, false, CLOSE_X, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE
        );
        UiFrameRenderer.State soundState = frames.resolve(
            true, settings.soundEnabled, SettingsTouchLayout.ROW_X, SOUND_ROW_Y,
            SettingsTouchLayout.ROW_WIDTH, SettingsTouchLayout.ROW_HEIGHT
        );
        UiFrameRenderer.State musicState = frames.resolve(
            true, settings.musicEnabled, SettingsTouchLayout.ROW_X, MUSIC_ROW_Y,
            SettingsTouchLayout.ROW_WIDTH, SettingsTouchLayout.ROW_HEIGHT
        );

        batch.begin();
        frames.draw(batch, UiFrameRenderer.Kind.BUTTON, CLOSE_X, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE,
            true, false);
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON, SettingsTouchLayout.ROW_X, SOUND_ROW_Y,
            SettingsTouchLayout.ROW_WIDTH, SettingsTouchLayout.ROW_HEIGHT, true,
            settings.soundEnabled
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON, SettingsTouchLayout.ROW_X, MUSIC_ROW_Y,
            SettingsTouchLayout.ROW_WIDTH, SettingsTouchLayout.ROW_HEIGHT, true,
            settings.musicEnabled
        );
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, NOTE_PANEL_X, NOTE_PANEL_Y,
            NOTE_PANEL_WIDTH, NOTE_PANEL_HEIGHT, true, false);

        icons.draw(batch, "settings", 60f, 1122f, 76f);
        text.draw(batch, "SETTINGS", 156f, 1196f, 1.36f, OverlayText.GOLD);
        text.draw(batch, "Comfort choices saved on this device", 156f, 1150f, 0.74f,
            OverlayText.SUBTLE);
        icons.draw(batch, "close", 588f, 1138f, 64f, closeState);

        drawToggle(batch, "SOUND EFFECTS", "Hits, drops, level-ups, and boss entrances",
            SOUND_ROW_Y, settings.soundEnabled, soundState);
        drawToggle(batch, "MUSIC", "World Tree vigil theme",
            MUSIC_ROW_Y, settings.musicEnabled, musicState);

        text.draw(batch, "TOUCH ONLY", 130f, 386f, 0.66f, OverlayText.GOLD);
        text.draw(batch, "Tap a row to switch it. Changes apply instantly.", 130f, 352f, 0.74f,
            OverlayText.IVORY);
        text.draw(batch, "Tap Close to return to the main menu.", 130f, 324f, 0.74f,
            OverlayText.SUBTLE);
        batch.end();
    }

    private void drawToggle(
        SpriteBatch batch,
        String title,
        String subtitle,
        float y,
        boolean enabled,
        UiFrameRenderer.State state
    ) {
        float offset = MainMenuRenderer.pressedOffset(state);
        text.draw(batch, title, 136f, y + 104f + offset, 1.16f, OverlayText.IVORY);
        text.draw(batch, subtitle, 136f, y + 54f + offset, 0.68f, OverlayText.SUBTLE);
        text.drawRightAligned(batch, toggleLabel(enabled), 590f, y + 96f + offset, 1.14f,
            enabled ? OverlayText.GOLD : OverlayText.MUTED);
        text.drawRightAligned(batch, enabled ? "tap to mute" : "tap to enable", 590f,
            y + 54f + offset, 0.62f, OverlayText.SUBTLE);
    }

    static String toggleLabel(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    private Texture backdrop() {
        if (backdrop == null) {
            backdrop = new Texture(Gdx.files.internal("generated/environment/arena_backdrop.png"));
            backdrop.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        return backdrop;
    }

    @Override
    public void close() {
        text.close();
        shapes.dispose();
        if (backdrop != null) {
            backdrop.dispose();
            backdrop = null;
        }
    }
}
