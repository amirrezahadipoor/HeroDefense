package com.amirrezahadipoor.herodefense;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.amirrezahadipoor.herodefense.gameplay.BossFactory;
import com.amirrezahadipoor.herodefense.gameplay.BossSpecialAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.BossWaveSpawner;
import com.amirrezahadipoor.herodefense.gameplay.ContinuousWaveRun;
import com.amirrezahadipoor.herodefense.gameplay.EnemyFactory;
import com.amirrezahadipoor.herodefense.gameplay.EnemyMeleeAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.EnemyMovementSystem;
import com.amirrezahadipoor.herodefense.gameplay.EnemyWaveSpawner;
import com.amirrezahadipoor.herodefense.gameplay.HeroAnimationController;
import com.amirrezahadipoor.herodefense.gameplay.HeroAutoAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroDamageSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroProgressionSystem;
import com.amirrezahadipoor.herodefense.gameplay.InventoryEquipmentSystem;
import com.amirrezahadipoor.herodefense.gameplay.WaveCompletion;
import com.amirrezahadipoor.herodefense.gameplay.WaveLifecycleSystem;
import com.amirrezahadipoor.herodefense.input.InventoryTouchController;
import com.amirrezahadipoor.herodefense.input.LevelUpTouchLayout;
import com.amirrezahadipoor.herodefense.input.RewardCardTouchLayout;
import com.amirrezahadipoor.herodefense.input.TouchInputController;
import com.amirrezahadipoor.herodefense.items.StarterLoadoutSystem;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.render.EquipmentSpriteRenderer;
import com.amirrezahadipoor.herodefense.render.HeroSpriteRenderer;
import com.amirrezahadipoor.herodefense.render.InventoryOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.RewardCardOverlayRenderer;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.save.LocalSaveRepository;

/** Android-only libGDX game loop and top-level state coordinator. */
public final class HeroDefenseGame extends ApplicationAdapter {
    private static final float MAX_FRAME_DELTA = 1f / 15f;

    private GameFlowController flow;
    private BossRewardCardSystem bossRewardCardSystem;
    private EquipmentSpriteRenderer equipmentSpriteRenderer;
    private BossSpecialAttackSystem bossSpecialAttackSystem;
    private EnemyMeleeAttackSystem enemyMeleeAttackSystem;
    private EnemyMovementSystem enemyMovementSystem;
    private WaveLifecycleSystem waveLifecycleSystem;
    private HeroAnimationController heroAnimationController;
    private HeroAutoAttackSystem heroAutoAttackSystem;
    private HeroProgressionSystem heroProgressionSystem;
    private HeroSpriteRenderer heroSpriteRenderer;
    private InventoryTouchController inventoryTouchController;
    private InventoryOverlayRenderer inventoryOverlayRenderer;
    private RewardCardOverlayRenderer rewardCardOverlayRenderer;
    private SpriteBatch spriteBatch;
    private LocalSaveRepository saves;
    private GameState gameState;
    private OrthographicCamera camera;
    private Viewport viewport;
    private float simulationSeconds;

    @Override
    public void create() {
        flow = new GameFlowController();
        HeroDamageSystem heroDamageSystem = new HeroDamageSystem();
        bossSpecialAttackSystem = new BossSpecialAttackSystem(heroDamageSystem);
        enemyMeleeAttackSystem = new EnemyMeleeAttackSystem(heroDamageSystem);
        enemyMovementSystem = new EnemyMovementSystem();
        EnemyWaveSpawner enemyWaveSpawner = new EnemyWaveSpawner(new EnemyFactory());
        bossRewardCardSystem = new BossRewardCardSystem();
        waveLifecycleSystem = new WaveLifecycleSystem(
            enemyWaveSpawner,
            new BossWaveSpawner(new BossFactory()),
            bossRewardCardSystem,
            new ContinuousWaveRun()
        );
        heroAnimationController = new HeroAnimationController();
        heroAutoAttackSystem = new HeroAutoAttackSystem();
        heroProgressionSystem = new HeroProgressionSystem();
        inventoryTouchController = new InventoryTouchController(new InventoryEquipmentSystem());
        saves = new LocalSaveRepository(Gdx.app.getPreferences(LocalSaveRepository.PREFERENCES_NAME));
        gameState = saves.load().orElseGet(() -> GameState.newRun(System.currentTimeMillis()));
        new StarterLoadoutSystem().provisionOnce(gameState);
        camera = new OrthographicCamera();
        viewport = new FitViewport(WorldLayout.REFERENCE_WIDTH, WorldLayout.REFERENCE_HEIGHT, camera);
        viewport.apply(true);
        spriteBatch = new SpriteBatch();
        heroSpriteRenderer = new HeroSpriteRenderer();
        equipmentSpriteRenderer = new EquipmentSpriteRenderer();
        inventoryOverlayRenderer = new InventoryOverlayRenderer();
        rewardCardOverlayRenderer = new RewardCardOverlayRenderer();
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

    /** Awards kill XP and opens the touch allocation overlay whenever a level is gained. */
    public int grantHeroExperience(int experience) {
        int levelsGained = heroProgressionSystem.grantExperience(gameState, experience);
        if (levelsGained > 0 && flow.state() == GameScreenState.PLAYING) {
            flow.transitionTo(GameScreenState.LEVEL_UP);
            saveNow();
        }
        return levelsGained;
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
        if (equipmentSpriteRenderer != null) {
            equipmentSpriteRenderer.close();
        }
        if (inventoryOverlayRenderer != null) {
            inventoryOverlayRenderer.close();
        }
        if (rewardCardOverlayRenderer != null) {
            rewardCardOverlayRenderer.close();
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
                if (flow.state() == GameScreenState.PAUSED && inventoryTouchController.isOpen()) {
                    inventoryTouchController.drag(gameState, deltaY);
                }
                return true;
            }

            @Override
            public boolean onTouchUp(float worldX, float worldY, int pointer, boolean isTap) {
                if (!isTap) {
                    return true;
                }
                if (flow.state() == GameScreenState.CARD_CHOICE) {
                    int choiceIndex = RewardCardTouchLayout.cardIndexAt(worldX, worldY);
                    if (bossRewardCardSystem.chooseCard(gameState, choiceIndex)) {
                        WaveCompletion result = waveLifecycleSystem.continueAfterBossReward(gameState);
                        flow.transitionTo(
                            result == WaveCompletion.RUN_COMPLETED
                                ? GameScreenState.GAME_OVER
                                : GameScreenState.PLAYING
                        );
                        saveNow();
                    }
                    return true;
                }
                if (flow.state() == GameScreenState.LEVEL_UP) {
                    HeroStat selectedStat = LevelUpTouchLayout.statAt(worldX, worldY);
                    if (heroProgressionSystem.allocateTalentPoint(gameState, selectedStat)) {
                        saveNow();
                        if (gameState.unspentTalentPoints == 0) {
                            flow.transitionTo(GameScreenState.PLAYING);
                        }
                    }
                    return true;
                }
                if (flow.state() == GameScreenState.MENU
                    && worldX >= 120f && worldX <= 600f
                    && worldY >= 150f && worldY <= 310f) {
                    waveLifecycleSystem.startCurrentWave(gameState);
                    flow.transitionTo(GameScreenState.PLAYING);
                    return true;
                }
                if (flow.state() == GameScreenState.PLAYING
                    && worldX >= 610f && worldY >= 1120f) {
                    flow.transitionTo(GameScreenState.PAUSED);
                    return true;
                }
                if (flow.state() == GameScreenState.PAUSED && inventoryTouchController.isOpen()) {
                    InventoryTouchController.Action action = inventoryTouchController.tap(
                        gameState, worldX, worldY
                    );
                    if (action == InventoryTouchController.Action.EQUIPPED
                        || action == InventoryTouchController.Action.UNEQUIPPED
                        || action == InventoryTouchController.Action.SOLD) {
                        saveNow();
                    }
                    return true;
                }
                if (flow.state() == GameScreenState.PAUSED
                    && worldX >= 180f && worldX <= 540f
                    && worldY >= 760f && worldY <= 900f) {
                    inventoryTouchController.open();
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
        enemyMovementSystem.update(gameState, simulationDelta);
        heroAutoAttackSystem.update(gameState, simulationDelta);
        bossSpecialAttackSystem.update(gameState, simulationDelta);
        if (enemyMeleeAttackSystem.update(gameState, simulationDelta)) {
            flow.transitionTo(GameScreenState.GAME_OVER);
            saveNow();
        } else {
            WaveCompletion waveCompletion = waveLifecycleSystem.updateAfterCombat(gameState);
            if (waveCompletion == WaveCompletion.BOSS_REWARD) {
                flow.transitionTo(GameScreenState.CARD_CHOICE);
            } else if (waveCompletion == WaveCompletion.RUN_COMPLETED) {
                flow.transitionTo(GameScreenState.GAME_OVER);
            }
            if (waveCompletion != WaveCompletion.NO_CHANGE) {
                saveNow();
            }
        }
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
            int heroFrame = heroAnimationController.frameIndex(gameState.hero);
            heroSpriteRenderer.draw(spriteBatch, gameState.hero, heroFrame);
            equipmentSpriteRenderer.draw(spriteBatch, gameState, heroFrame, simulationSeconds);
            spriteBatch.end();
        }
        if (flow.state() == GameScreenState.CARD_CHOICE) {
            rewardCardOverlayRenderer.draw(spriteBatch, camera.combined, gameState);
        } else if (flow.state() == GameScreenState.PAUSED) {
            if (inventoryTouchController.isOpen()) {
                inventoryOverlayRenderer.drawInventory(
                    spriteBatch, camera.combined, gameState, inventoryTouchController
                );
            } else {
                inventoryOverlayRenderer.drawPauseMenu(spriteBatch, camera.combined);
            }
        }
    }
}
