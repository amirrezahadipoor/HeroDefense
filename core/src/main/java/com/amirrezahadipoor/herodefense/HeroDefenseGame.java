package com.amirrezahadipoor.herodefense;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.save.LocalSaveRepository;

/** Android-only libGDX game loop and top-level state coordinator. */
public final class HeroDefenseGame extends ApplicationAdapter {
    private static final float MAX_FRAME_DELTA = 1f / 15f;

    private GameFlowController flow;
    private LocalSaveRepository saves;
    private GameState gameState;
    private float simulationSeconds;

    @Override
    public void create() {
        flow = new GameFlowController();
        saves = new LocalSaveRepository(Gdx.app.getPreferences(LocalSaveRepository.PREFERENCES_NAME));
        gameState = saves.load().orElseGet(() -> GameState.newRun(System.currentTimeMillis()));
    }

    @Override
    public void render() {
        float deltaSeconds = Math.min(Gdx.graphics.getDeltaTime(), MAX_FRAME_DELTA);
        switch (flow.state()) {
            case PLAYING -> updatePlaying(deltaSeconds);
            case MENU, PAUSED, LEVEL_UP, CARD_CHOICE, SHOP, GAME_OVER -> {
                // Overlay/menu states deliberately freeze the combat simulation.
            }
        }
        drawCurrentState();
    }

    public GameScreenState screenState() {
        return flow.state();
    }

    public void transitionTo(GameScreenState state) {
        flow.transitionTo(state);
    }

    public GameState gameState() {
        return gameState;
    }

    @Override
    public void pause() {
        saveNow();
    }

    @Override
    public void dispose() {
        saveNow();
    }

    private void saveNow() {
        if (saves != null && gameState != null) {
            saves.save(gameState);
        }
    }

    private void updatePlaying(float deltaSeconds) {
        simulationSeconds += deltaSeconds * gameState.simulationSpeed;
    }

    private void drawCurrentState() {
        float tint = switch (flow.state()) {
            case MENU -> 0.07f;
            case PLAYING -> 0.11f;
            case PAUSED -> 0.055f;
            case LEVEL_UP -> 0.13f;
            case CARD_CHOICE -> 0.15f;
            case SHOP -> 0.10f;
            case GAME_OVER -> 0.035f;
        };
        Gdx.gl.glClearColor(tint * 0.5f, tint, tint * 0.72f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }
}
