package com.amirrezahadipoor.herodefense.input;

import com.badlogic.gdx.Gdx;

/** Android-backed short vibration impulses; no-op behavior is delegated to libGDX. */
public final class GdxHapticFeedback implements HapticFeedback {
    @Override
    public void tap() {
        Gdx.input.vibrate(14);
    }

    @Override
    public void cardSelection() {
        Gdx.input.vibrate(34);
    }
}
