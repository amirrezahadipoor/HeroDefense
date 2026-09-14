package com.amirrezahadipoor.herodefense.input;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.story.LoreCatalog;

/** Read-only Codex list controller: select entries, drag-scroll, close. */
public final class CodexTouchController {
    public enum Action {
        NONE,
        SELECTED,
        CLOSED
    }

    private static final float ROW_DRAG_THRESHOLD = 55f;

    private volatile boolean open;
    private int selectedIndex = -1;
    private int firstVisibleIndex;
    private float accumulatedDrag;

    public void open() {
        open = true;
        selectedIndex = -1;
        firstVisibleIndex = 0;
        accumulatedDrag = 0f;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        this.open = false;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public int firstVisibleIndex() {
        return firstVisibleIndex;
    }

    public Action tap(GameState state, float x, float y) {
        if (!open) return Action.NONE;
        if (CodexTouchLayout.closeAt(x, y)) {
            open = false;
            return Action.CLOSED;
        }
        int row = CodexTouchLayout.visibleRowAt(x, y);
        if (row < 0) return Action.NONE;
        int index = firstVisibleIndex + row;
        if (index < 0 || index >= LoreCatalog.all().size()) return Action.NONE;
        selectedIndex = index;
        return Action.SELECTED;
    }

    public void drag(GameState state, float deltaY) {
        if (!open || state == null) return;
        accumulatedDrag += deltaY;
        int maxFirst = Math.max(0, LoreCatalog.all().size() - CodexTouchLayout.VISIBLE_ROWS);
        while (accumulatedDrag >= ROW_DRAG_THRESHOLD) {
            firstVisibleIndex = Math.min(maxFirst, firstVisibleIndex + 1);
            accumulatedDrag -= ROW_DRAG_THRESHOLD;
        }
        while (accumulatedDrag <= -ROW_DRAG_THRESHOLD) {
            firstVisibleIndex = Math.max(0, firstVisibleIndex - 1);
            accumulatedDrag += ROW_DRAG_THRESHOLD;
        }
    }
}
