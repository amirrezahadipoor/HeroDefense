package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.settings.GameSettings;

/** Premium Main Menu settings surface with explicit large touch toggles. */
public final class SettingsOverlayRenderer implements AutoCloseable {
    private static final Color GOLD = Color.valueOf("EAC66D");
    private static final Color IVORY = Color.valueOf("F3E4BC");
    private static final Color SUBTLE = Color.valueOf("AEBCAE");
    private static final Color POSITIVE = Color.valueOf("69C884");
    private static final Color MUTED = Color.valueOf("777D76");

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();

    public SettingsOverlayRenderer() {
        font.getRegion().getTexture().setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );
    }

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameSettings settings,
        UiIconRenderer icons,
        UiFrameRenderer frames
    ) {
        beginShapes(projection);
        shapes.setColor(0.006f, 0.022f, 0.021f, 0.985f);
        shapes.rect(0f, 0f, 720f, 1280f);
        shapes.setColor(0.04f, 0.13f, 0.11f, 0.82f);
        shapes.rect(0f, 1160f, 720f, 120f);
        shapes.end();
        endShapes();

        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(batch, UiFrameRenderer.Kind.BUTTON, 570f, 1120f, 100f, 100f, true, false);
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 100f, 900f, 520f, 190f, true, false);
        frames.draw(batch, UiFrameRenderer.Kind.BUTTON, 100f, 700f, 520f, 150f,
            true, settings.soundEnabled);
        frames.draw(batch, UiFrameRenderer.Kind.BUTTON, 100f, 500f, 520f, 150f,
            true, settings.musicEnabled);

        UiFrameRenderer.State closeState = frames.resolve(true, false, 570f, 1120f, 100f, 100f);
        UiFrameRenderer.State soundState = frames.resolve(true, settings.soundEnabled,
            100f, 700f, 520f, 150f);
        UiFrameRenderer.State musicState = frames.resolve(true, settings.musicEnabled,
            100f, 500f, 520f, 150f);
        icons.draw(batch, "close", 588f, 1138f, 64f, closeState);
        icons.draw(batch, "settings", 132f, 942f, 104f);
        drawText(batch, "SETTINGS", 262f, 1034f, 1.55f, GOLD);
        drawText(batch, "Comfort controls", 262f, 986f, 0.78f, IVORY);
        drawText(batch, "Changes save immediately", 262f, 948f, 0.68f, SUBTLE);
        drawToggle(batch, "settings", "SOUND EFFECTS", "Combat and touch cues",
            settings.soundEnabled, 700f, soundState, icons);
        drawToggle(batch, "wave", "MUSIC", "Background soundtrack",
            settings.musicEnabled, 500f, musicState, icons);
        drawText(batch, "Tap either row to switch it on or off", 160f, 426f, 0.72f, SUBTLE);
        batch.end();
    }

    private void drawToggle(
        SpriteBatch batch,
        String icon,
        String title,
        String detail,
        boolean enabled,
        float y,
        UiFrameRenderer.State state,
        UiIconRenderer icons
    ) {
        float offset = MainMenuRenderer.pressedOffset(state);
        icons.draw(batch, icon, 128f, y + 29f + offset, 88f, state);
        drawText(batch, title, 242f, y + 102f + offset, 1.02f, IVORY);
        drawText(batch, detail, 242f, y + 57f + offset, 0.72f, SUBTLE);
        drawText(batch, enabled ? "ON" : "OFF", 526f, y + 84f + offset, 1.02f,
            enabled ? POSITIVE : MUTED);
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
