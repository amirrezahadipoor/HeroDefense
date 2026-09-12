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
import com.amirrezahadipoor.herodefense.gameplay.DropPickupSystem;
import com.amirrezahadipoor.herodefense.gameplay.EnemyFactory;
import com.amirrezahadipoor.herodefense.gameplay.EnemyMeleeAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.EnemyMovementSystem;
import com.amirrezahadipoor.herodefense.gameplay.EnemyWaveSpawner;
import com.amirrezahadipoor.herodefense.gameplay.HeroAnimationController;
import com.amirrezahadipoor.herodefense.gameplay.HeroAutoAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroDamageSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroProgressionSystem;
import com.amirrezahadipoor.herodefense.gameplay.InventoryEquipmentSystem;
import com.amirrezahadipoor.herodefense.gameplay.ItemDropSystem;
import com.amirrezahadipoor.herodefense.gameplay.KillRewardResult;
import com.amirrezahadipoor.herodefense.gameplay.KillRewardSystem;
import com.amirrezahadipoor.herodefense.gameplay.WaveCompletion;
import com.amirrezahadipoor.herodefense.gameplay.WaveLifecycleSystem;
import com.amirrezahadipoor.herodefense.input.InventoryTouchController;
import com.amirrezahadipoor.herodefense.input.LevelUpTouchLayout;
import com.amirrezahadipoor.herodefense.input.MainMenuTouchLayout;
import com.amirrezahadipoor.herodefense.input.PauseTouchController;
import com.amirrezahadipoor.herodefense.input.PauseTouchLayout;
import com.amirrezahadipoor.herodefense.input.RewardCardTouchLayout;
import com.amirrezahadipoor.herodefense.input.SettingsTouchController;
import com.amirrezahadipoor.herodefense.input.SettingsTouchLayout;
import com.amirrezahadipoor.herodefense.input.SimulationSpeedTouchController;
import com.amirrezahadipoor.herodefense.input.StatShopTouchLayout;
import com.amirrezahadipoor.herodefense.input.TouchInputController;
import com.amirrezahadipoor.herodefense.items.StarterLoadoutSystem;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.potions.AutoPotionSystem;
import com.amirrezahadipoor.herodefense.potions.HealthPotionSystem;
import com.amirrezahadipoor.herodefense.potions.PotionDropSystem;
import com.amirrezahadipoor.herodefense.render.EquipmentSpriteRenderer;
import com.amirrezahadipoor.herodefense.render.HeroSpriteRenderer;
import com.amirrezahadipoor.herodefense.render.HudRenderer;
import com.amirrezahadipoor.herodefense.render.InventoryOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.LevelUpOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.MainMenuRenderer;
import com.amirrezahadipoor.herodefense.render.RewardCardOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.SettingsOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.StatShopOverlayRenderer;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.save.LocalSaveRepository;
import com.amirrezahadipoor.herodefense.settings.GameSettings;
import com.amirrezahadipoor.herodefense.settings.LocalSettingsRepository;
import com.amirrezahadipoor.herodefense.shop.StatShopSystem;

import java.util.Optional;

/** Android-only libGDX game loop and top-level state coordinator. */
public final class HeroDefenseGame extends ApplicationAdapter {
    private static final float MAX_FRAME_DELTA = 1f / 15f;

    private GameFlowController flow;
    private AutoPotionSystem autoPotionSystem;
    private BossRewardCardSystem bossRewardCardSystem;
    private EquipmentSpriteRenderer equipmentSpriteRenderer;
    private BossSpecialAttackSystem bossSpecialAttackSystem;
    private DropPickupSystem dropPickupSystem;
    private EnemyMeleeAttackSystem enemyMeleeAttackSystem;
    private EnemyMovementSystem enemyMovementSystem;
    private WaveLifecycleSystem waveLifecycleSystem;
    private HeroAnimationController heroAnimationController;
    private HeroAutoAttackSystem heroAutoAttackSystem;
    private HeroProgressionSystem heroProgressionSystem;
    private HeroSpriteRenderer heroSpriteRenderer;
    private HudRenderer hudRenderer;
    private InventoryTouchController inventoryTouchController;
    private InventoryOverlayRenderer inventoryOverlayRenderer;
    private ItemDropSystem itemDropSystem;
    private KillRewardSystem killRewardSystem;
    private LevelUpOverlayRenderer levelUpOverlayRenderer;
    private MainMenuRenderer mainMenuRenderer;
    private PauseTouchController pauseTouchController;
    private PotionDropSystem potionDropSystem;
    private RewardCardOverlayRenderer rewardCardOverlayRenderer;
    private SettingsOverlayRenderer settingsOverlayRenderer;
    private SettingsTouchController settingsTouchController;
    private SimulationSpeedTouchController simulationSpeedTouchController;
    private StatShopOverlayRenderer statShopOverlayRenderer;
    private StatShopSystem statShopSystem;
    private SpriteBatch spriteBatch;
    private LocalSaveRepository saves;
    private LocalSettingsRepository settingsRepository;
    private GameSettings settings;
    private GameState gameState;
    private boolean continueAvailable;
    private OrthographicCamera camera;
    private Viewport viewport;
    private float simulationSeconds;

    @Override
    public void create() {
        flow = new GameFlowController();
        autoPotionSystem = new AutoPotionSystem(new HealthPotionSystem());
        HeroDamageSystem heroDamageSystem = new HeroDamageSystem();
        bossSpecialAttackSystem = new BossSpecialAttackSystem(heroDamageSystem);
        dropPickupSystem = new DropPickupSystem();
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
        killRewardSystem = new KillRewardSystem(heroProgressionSystem);
        inventoryTouchController = new InventoryTouchController(new InventoryEquipmentSystem());
        itemDropSystem = new ItemDropSystem();
        pauseTouchController = new PauseTouchController();
        potionDropSystem = new PotionDropSystem();
        settingsTouchController = new SettingsTouchController();
        simulationSpeedTouchController = new SimulationSpeedTouchController();
        statShopSystem = new StatShopSystem();
        saves = new LocalSaveRepository(Gdx.app.getPreferences(LocalSaveRepository.PREFERENCES_NAME));
        settingsRepository = new LocalSettingsRepository(
            Gdx.app.getPreferences(LocalSettingsRepository.PREFERENCES_NAME)
        );
        settings = settingsRepository.load();
        Optional<GameState> loadedRun = saves.load();
        gameState = loadedRun.orElseGet(() -> GameState.newRun(System.currentTimeMillis()));
        continueAvailable = loadedRun.isPresent() && canContinue(gameState);
        new StarterLoadoutSystem().provisionOnce(gameState);
        camera = new OrthographicCamera();
        viewport = new FitViewport(WorldLayout.REFERENCE_WIDTH, WorldLayout.REFERENCE_HEIGHT, camera);
        viewport.apply(true);
        spriteBatch = new SpriteBatch();
        heroSpriteRenderer = new HeroSpriteRenderer();
        hudRenderer = new HudRenderer();
        equipmentSpriteRenderer = new EquipmentSpriteRenderer();
        inventoryOverlayRenderer = new InventoryOverlayRenderer();
        levelUpOverlayRenderer = new LevelUpOverlayRenderer();
        mainMenuRenderer = new MainMenuRenderer();
        rewardCardOverlayRenderer = new RewardCardOverlayRenderer();
        settingsOverlayRenderer = new SettingsOverlayRenderer();
        statShopOverlayRenderer = new StatShopOverlayRenderer();
        installTouchInput();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        float deltaSeconds = Math.min(Gdx.graphics.getDeltaTime(), MAX_FRAME_DELTA);
        if (flow.simulationRunning()) {
            updatePlaying(deltaSeconds);
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
        if (hudRenderer != null) {
            hudRenderer.close();
        }
        if (equipmentSpriteRenderer != null) {
            equipmentSpriteRenderer.close();
        }
        if (inventoryOverlayRenderer != null) {
            inventoryOverlayRenderer.close();
        }
        if (levelUpOverlayRenderer != null) {
            levelUpOverlayRenderer.close();
        }
        if (mainMenuRenderer != null) {
            mainMenuRenderer.close();
        }
        if (rewardCardOverlayRenderer != null) {
            rewardCardOverlayRenderer.close();
        }
        if (settingsOverlayRenderer != null) {
            settingsOverlayRenderer.close();
        }
        if (statShopOverlayRenderer != null) {
            statShopOverlayRenderer.close();
        }
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
    }

    private void saveNow() {
        if (saves != null && gameState != null) {
            saves.save(gameState);
            continueAvailable = canContinue(gameState);
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
                if (flow.state() == GameScreenState.SETTINGS) {
                    SettingsTouchLayout.Action action = settingsTouchController.tap(
                        settings, worldX, worldY
                    );
                    if (action == SettingsTouchLayout.Action.CLOSE) {
                        flow.transitionTo(GameScreenState.MENU);
                    } else if (action != SettingsTouchLayout.Action.NONE) {
                        settingsRepository.save(settings);
                    }
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
                if (flow.state() == GameScreenState.SHOP) {
                    if (StatShopTouchLayout.closeAt(worldX, worldY)) {
                        flow.returnFromOverlay();
                        saveNow();
                    } else {
                        HeroStat stat = StatShopTouchLayout.statAt(worldX, worldY);
                        if (statShopSystem.purchase(gameState, stat)) saveNow();
                    }
                    return true;
                }
                if (flow.state() == GameScreenState.MENU) {
                    MainMenuTouchLayout.Action action = MainMenuTouchLayout.actionAt(
                        worldX, worldY, continueAvailable
                    );
                    if (action == MainMenuTouchLayout.Action.NEW_GAME) {
                        startNewRun();
                    } else if (action == MainMenuTouchLayout.Action.CONTINUE) {
                        continueRun();
                    } else if (action == MainMenuTouchLayout.Action.SETTINGS) {
                        flow.transitionTo(GameScreenState.SETTINGS);
                    }
                    return true;
                }
                if (flow.state() == GameScreenState.PLAYING
                    && simulationSpeedTouchController.tap(gameState, worldX, worldY)) {
                    saveNow();
                    return true;
                }
                if (flow.state() == GameScreenState.PLAYING
                    && pauseTouchController.tap(flow, worldX, worldY)) {
                    saveNow();
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
                    && PauseTouchLayout.shopAt(worldX, worldY)) {
                    flow.transitionTo(GameScreenState.SHOP);
                    return true;
                }
                if (flow.state() == GameScreenState.PAUSED
                    && PauseTouchLayout.inventoryAt(worldX, worldY)) {
                    inventoryTouchController.open();
                    return true;
                }
                if (flow.state() == GameScreenState.PAUSED) {
                    pauseTouchController.tap(flow, worldX, worldY);
                }
                return true;
            }
        }));
    }

    private void startNewRun() {
        saves.clear();
        gameState = GameState.newRun(System.currentTimeMillis());
        new StarterLoadoutSystem().provisionOnce(gameState);
        simulationSeconds = 0f;
        waveLifecycleSystem.startCurrentWave(gameState);
        flow.transitionTo(GameScreenState.PLAYING);
        saveNow();
    }

    private void continueRun() {
        if (!continueAvailable || !canContinue(gameState)) return;
        flow.transitionTo(GameScreenState.PLAYING);
        if (gameState.awaitingBossReward) {
            flow.transitionTo(GameScreenState.CARD_CHOICE);
        } else if (gameState.unspentTalentPoints > 0) {
            flow.transitionTo(GameScreenState.LEVEL_UP);
        } else if (!gameState.waveActive) {
            waveLifecycleSystem.startCurrentWave(gameState);
        }
    }

    private static boolean canContinue(GameState state) {
        return state != null && state.hero != null && state.hero.alive && !state.runComplete;
    }

    private void updatePlaying(float deltaSeconds) {
        float simulationDelta = deltaSeconds * gameState.simulationSpeed;
        gameState.anchorHeroAtArenaCenter();
        heroAnimationController.update(gameState.hero, simulationDelta);
        enemyMovementSystem.update(gameState, simulationDelta);
        heroAutoAttackSystem.update(gameState, simulationDelta);
        bossSpecialAttackSystem.update(gameState, simulationDelta);
        boolean gameOver = enemyMeleeAttackSystem.update(gameState, simulationDelta);
        if (!gameOver) {
            autoPotionSystem.update(gameState);
        }
        itemDropSystem.processDefeatedEnemies(gameState);
        potionDropSystem.processDefeatedEnemies(gameState);
        KillRewardResult killRewards = killRewardSystem.processDefeatedEnemies(gameState);
        dropPickupSystem.update(gameState, simulationDelta);
        if (gameOver) {
            flow.transitionTo(GameScreenState.GAME_OVER);
            saveNow();
        } else if (killRewards.levelsGained() > 0) {
            flow.transitionTo(GameScreenState.LEVEL_UP);
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
            case SETTINGS -> 0.065f;
            case PLAYING -> 0.11f;
            case PAUSED -> 0.055f;
            case LEVEL_UP -> 0.13f;
            case CARD_CHOICE -> 0.15f;
            case SHOP -> 0.10f;
            case GAME_OVER -> 0.035f;
        };
        Gdx.gl.glClearColor(tint * 0.5f, tint, tint * 0.72f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (flow.state() != GameScreenState.MENU && flow.state() != GameScreenState.SETTINGS) {
            camera.update();
            spriteBatch.setProjectionMatrix(camera.combined);
            spriteBatch.begin();
            int heroFrame = heroAnimationController.frameIndex(gameState.hero);
            heroSpriteRenderer.draw(spriteBatch, gameState.hero, heroFrame);
            equipmentSpriteRenderer.draw(spriteBatch, gameState, heroFrame, simulationSeconds);
            spriteBatch.end();
        }
        if (flow.state() == GameScreenState.PLAYING) {
            hudRenderer.draw(spriteBatch, camera.combined, gameState);
        }
        if (flow.state() == GameScreenState.MENU) {
            mainMenuRenderer.draw(spriteBatch, camera.combined, continueAvailable);
        } else if (flow.state() == GameScreenState.SETTINGS) {
            settingsOverlayRenderer.draw(spriteBatch, camera.combined, settings);
        } else if (flow.state() == GameScreenState.LEVEL_UP) {
            levelUpOverlayRenderer.draw(spriteBatch, camera.combined, gameState);
        } else if (flow.state() == GameScreenState.CARD_CHOICE) {
            rewardCardOverlayRenderer.draw(spriteBatch, camera.combined, gameState);
        } else if (flow.state() == GameScreenState.SHOP) {
            statShopOverlayRenderer.draw(spriteBatch, camera.combined, gameState, statShopSystem);
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
