package com.amirrezahadipoor.herodefense;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.amirrezahadipoor.herodefense.gameplay.HeroAnimationController;
import com.amirrezahadipoor.herodefense.gameplay.HeroAutoAttackSystem;
import com.amirrezahadipoor.herodefense.input.TouchInputController;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.render.HeroSpriteRenderer;
import com.amirrezahadipoor.herodefense.save.LocalSaveRepository;

/** Android-only libGDX game loop and top-level state coordinator. */
public final class HeroDefenseGame extends ApplicationAdapter {
    private static final float MAX_FRAME_DELTA = 1f / 15f;

    private GameFlowController flow;
    private HeroAnimationController heroAnimationController;
    private HeroAutoAttackSystem heroAutoAttackSystem;
    private HeroSpriteRenderer heroSpriteRenderer;
    private SpriteBatch spriteBatch;
    private LocalSaveRepository saves;
    private GameState gameState;
    private OrthographicCamera camera;
    private Viewport viewport;
    private float simulationSeconds;

    @Override
    public void create() {
        flow = new GameFlowController();
        heroAnimationController = new HeroAnimationController();
        heroAutoAttackSystem = new HeroAutoAttackSystem();
        saves = new LocalSaveRepository(Gdx.app.getPreferences(LocalSaveRepository.PREFERENCES_NAME));
        gameState = saves.load().orElseGet(() -> GameState.newRun(System.currentTimeMillis()));
        camera = new OrthographicCamera();
        viewport = new FitViewport(WorldLayout.REFERENCE_WIDTH, WorldLayout.REFERENCE_HEIGHT, camera);
        viewport.apply(true);
        spriteBatch = new SpriteBatch();
        heroSpriteRenderer = new HeroSpriteRenderer();
        installTouchInput();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
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
        if (heroSpriteRenderer != null) {
            heroSpriteRenderer.close();
        }
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
    }

    private void saveNow() {
        if (saves != null && gameState != null) {
            saves.save(gameState);
        }
    }

    private void installTouchInput() {
        Gdx.input.setInputProcessor(new TouchInputController(viewport, new TouchInputController.Listener() {
            @Override
            public boolean onTouchDown(float worldX, float worldY, int pointer) {
                return true;
            }

            @Override
            public boolean onTouchDragged(
                float worldX,
                float worldY,
                float deltaX,
                float deltaY,
                int pointer
            ) {
                // Inventory drag behavior is attached to this callback by the UI layer.
                return true;
            }

            @Override
            public boolean onTouchUp(float worldX, float worldY, int pointer, boolean isTap) {
                if (!isTap) {
                    return true;
                }
                if (flow.state() == GameScreenState.MENU
                    && worldX >= 120f && worldX <= 600f
                    && worldY >= 150f && worldY <= 310f) {
                    flow.transitionTo(GameScreenState.PLAYING);
                    return true;
                }
                if (flow.state() == GameScreenState.PLAYING
                    && worldX >= 610f && worldY >= 1120f) {
                    flow.transitionTo(GameScreenState.PAUSED);
                    return true;
                }
                if (flow.state() == GameScreenState.PAUSED
                    && worldX >= 180f && worldX <= 540f
                    && worldY >= 480f && worldY <= 720f) {
                    flow.returnFromOverlay();
                }
                return true;
            }
        }));
    }

    private void updatePlaying(float deltaSeconds) {
        float simulationDelta = deltaSeconds * gameState.simulationSpeed;
        gameState.anchorHeroAtArenaCenter();
        heroAnimationController.update(gameState.hero, simulationDelta);
        heroAutoAttackSystem.update(gameState, simulationDelta);
        simulationSeconds += simulationDelta;
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

        if (flow.state() != GameScreenState.MENU) {
            camera.update();
            spriteBatch.setProjectionMatrix(camera.combined);
            spriteBatch.begin();
            heroSpriteRenderer.draw(
                spriteBatch,
                gameState.hero,
                heroAnimationController.frameIndex(gameState.hero)
            );
            spriteBatch.end();
        }
    }
}
