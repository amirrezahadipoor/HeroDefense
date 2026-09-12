package com.amirrezahadipoor.herodefense;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

/** Android-only libGDX application entry point. */
public final class HeroDefenseGame extends ApplicationAdapter {
    @Override
    public void render() {
        Gdx.gl.glClearColor(0.035f, 0.055f, 0.075f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }
}
