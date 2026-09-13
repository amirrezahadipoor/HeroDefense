package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Owns the reviewed forest-glass nine-patch skins and resolves touch-visible states. */
public final class UiFrameRenderer implements AutoCloseable {
    public enum Kind {
        BUTTON,
        PANEL,
        SLOT
    }

    public enum State {
        NORMAL,
        PRESSED,
        SELECTED,
        DISABLED
    }

    private static final int INSET = 24;
    private final Map<Kind, Map<State, NinePatch>> patches = new EnumMap<>(Kind.class);
    private boolean pressActive;
    private float pressX;
    private float pressY;

    public void press(float worldX, float worldY) {
        pressActive = true;
        pressX = worldX;
        pressY = worldY;
    }

    public void movePress(float worldX, float worldY) {
        if (!pressActive) return;
        pressX = worldX;
        pressY = worldY;
    }

    public void release() {
        pressActive = false;
    }

    public State resolve(
        boolean enabled,
        boolean selected,
        float x,
        float y,
        float width,
        float height
    ) {
        if (!enabled) return State.DISABLED;
        if (pressActive && contains(pressX, pressY, x, y, width, height)) {
            return State.PRESSED;
        }
        return selected ? State.SELECTED : State.NORMAL;
    }

    public void draw(
        SpriteBatch batch,
        Kind kind,
        float x,
        float y,
        float width,
        float height,
        boolean enabled,
        boolean selected
    ) {
        if (batch == null || width <= 0f || height <= 0f) return;
        State state = resolve(enabled, selected, x, y, width, height);
        patches.computeIfAbsent(kind, ignored -> new EnumMap<>(State.class))
            .computeIfAbsent(state, value -> load(kind, value))
            .draw(batch, x, y, width, height);
    }

    private static NinePatch load(Kind kind, State state) {
        String key = "ui_frame_"
            + kind.name().toLowerCase(Locale.ROOT)
            + "_"
            + state.name().toLowerCase(Locale.ROOT);
        Texture texture = new Texture(Gdx.files.internal("generated/ui/" + key + ".png"));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return new NinePatch(texture, INSET, INSET, INSET, INSET);
    }

    private static boolean contains(
        float pointX,
        float pointY,
        float x,
        float y,
        float width,
        float height
    ) {
        return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
    }

    @Override
    public void close() {
        for (Map<State, NinePatch> byState : patches.values()) {
            for (NinePatch patch : byState.values()) patch.getTexture().dispose();
            byState.clear();
        }
        patches.clear();
    }
}
