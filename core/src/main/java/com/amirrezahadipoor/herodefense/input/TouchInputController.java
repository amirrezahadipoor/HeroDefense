package com.amirrezahadipoor.herodefense.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Converts Android pointer coordinates into fixed world coordinates and emits only
 * tap/drag callbacks. Keyboard and mouse-specific controls are intentionally absent.
 */
public final class TouchInputController extends InputAdapter {
    public interface Listener {
        boolean onTouchDown(float worldX, float worldY, int pointer);
        boolean onTouchDragged(float worldX, float worldY, float deltaX, float deltaY, int pointer);
        boolean onTouchUp(float worldX, float worldY, int pointer, boolean isTap);
    }

    private static final float TAP_SLOP_WORLD_UNITS = 28f;

    private final Viewport viewport;
    private final Listener listener;
    private final Vector3 unprojected = new Vector3();
    private final Vector2[] downPositions = new Vector2[10];
    private final Vector2[] previousPositions = new Vector2[10];
    private final boolean[] dragging = new boolean[10];

    public TouchInputController(Viewport viewport, Listener listener) {
        this.viewport = viewport;
        this.listener = listener;
        for (int index = 0; index < downPositions.length; index++) {
            downPositions[index] = new Vector2();
            previousPositions[index] = new Vector2();
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!supports(pointer)) {
            return false;
        }
        Vector3 world = toWorld(screenX, screenY);
        downPositions[pointer].set(world.x, world.y);
        previousPositions[pointer].set(world.x, world.y);
        dragging[pointer] = false;
        return listener.onTouchDown(world.x, world.y, pointer);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!supports(pointer)) {
            return false;
        }
        Vector3 world = toWorld(screenX, screenY);
        Vector2 previous = previousPositions[pointer];
        float deltaX = world.x - previous.x;
        float deltaY = world.y - previous.y;
        if (downPositions[pointer].dst2(world.x, world.y)
            > TAP_SLOP_WORLD_UNITS * TAP_SLOP_WORLD_UNITS) {
            dragging[pointer] = true;
        }
        previous.set(world.x, world.y);
        return listener.onTouchDragged(world.x, world.y, deltaX, deltaY, pointer);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!supports(pointer)) {
            return false;
        }
        Vector3 world = toWorld(screenX, screenY);
        boolean isTap = !dragging[pointer]
            && downPositions[pointer].dst2(world.x, world.y)
            <= TAP_SLOP_WORLD_UNITS * TAP_SLOP_WORLD_UNITS;
        dragging[pointer] = false;
        return listener.onTouchUp(world.x, world.y, pointer, isTap);
    }

    private Vector3 toWorld(int screenX, int screenY) {
        return viewport.unproject(unprojected.set(screenX, screenY, 0f));
    }

    private boolean supports(int pointer) {
        return pointer >= 0 && pointer < downPositions.length;
    }
}
