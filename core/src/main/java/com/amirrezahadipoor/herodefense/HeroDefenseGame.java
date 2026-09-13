package com.amirrezahadipoor.herodefense;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.amirrezahadipoor.herodefense.audio.AudioCue;
import com.amirrezahadipoor.herodefense.audio.GameAudioManager;
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
import com.amirrezahadipoor.herodefense.gameplay.CombatEvent;
import com.amirrezahadipoor.herodefense.gameplay.HeroAttackUpdateResult;
import com.amirrezahadipoor.herodefense.gameplay.HeroAutoAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroDamageSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroProgressionSystem;
import com.amirrezahadipoor.herodefense.gameplay.InventoryEquipmentSystem;
import com.amirrezahadipoor.herodefense.gameplay.ItemDropSystem;
import com.amirrezahadipoor.herodefense.gameplay.KillRewardResult;
import com.amirrezahadipoor.herodefense.gameplay.KillRewardSystem;
import com.amirrezahadipoor.herodefense.gameplay.WaveCompletion;
import com.amirrezahadipoor.herodefense.gameplay.PlantingCeremony;
import com.amirrezahadipoor.herodefense.gameplay.WaveLifecycleSystem;
import com.amirrezahadipoor.herodefense.input.GameOverTouchLayout;
import com.amirrezahadipoor.herodefense.input.GdxHapticFeedback;
import com.amirrezahadipoor.herodefense.input.HapticFeedback;
import com.amirrezahadipoor.herodefense.input.HudTouchLayout;
import com.amirrezahadipoor.herodefense.input.InventoryTouchController;
import com.amirrezahadipoor.herodefense.input.LevelUpTouchLayout;
import com.amirrezahadipoor.herodefense.input.MainMenuTouchLayout;
import com.amirrezahadipoor.herodefense.input.PauseTouchController;
import com.amirrezahadipoor.herodefense.input.PauseTouchLayout;
import com.amirrezahadipoor.herodefense.input.RewardCardTouchController;
import com.amirrezahadipoor.herodefense.input.SettingsTouchController;
import com.amirrezahadipoor.herodefense.input.SettingsTouchLayout;
import com.amirrezahadipoor.herodefense.input.SimulationSpeedTouchController;
import com.amirrezahadipoor.herodefense.input.StatShopTouchLayout;
import com.amirrezahadipoor.herodefense.input.TouchInputController;
import com.amirrezahadipoor.herodefense.items.StarterLoadoutSystem;
import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.DropCollectionStage;
import com.amirrezahadipoor.herodefense.model.DropEntity;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.potions.AutoPotionSystem;
import com.amirrezahadipoor.herodefense.potions.HealthPotionSystem;
import com.amirrezahadipoor.herodefense.potions.PotionDropSystem;
import com.amirrezahadipoor.herodefense.polish.FloatingCoinTextSystem;
import com.amirrezahadipoor.herodefense.polish.FloatingDamageTextSystem;
import com.amirrezahadipoor.herodefense.polish.HitStopSystem;
import com.amirrezahadipoor.herodefense.polish.ParticleSystem;
import com.amirrezahadipoor.herodefense.polish.ScreenShakeSystem;
import com.amirrezahadipoor.herodefense.polish.TouchFeedbackSystem;
import com.amirrezahadipoor.herodefense.render.ArenaEnvironmentRenderer;
import com.amirrezahadipoor.herodefense.render.CombatEntityRenderer;
import com.amirrezahadipoor.herodefense.render.DisplayMetrics;
import com.amirrezahadipoor.herodefense.render.GameFonts;
import com.amirrezahadipoor.herodefense.render.ScreenEdges;
import com.amirrezahadipoor.herodefense.render.EquipmentSpriteRenderer;
import com.amirrezahadipoor.herodefense.render.FloatingCoinTextRenderer;
import com.amirrezahadipoor.herodefense.render.FloatingDamageTextRenderer;
import com.amirrezahadipoor.herodefense.render.GameOverOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.CeremonyHeroRenderer;
import com.amirrezahadipoor.herodefense.render.HeroSpriteRenderer;
import com.amirrezahadipoor.herodefense.render.SaplingTreeRenderer;
import com.amirrezahadipoor.herodefense.render.HudRenderer;
import com.amirrezahadipoor.herodefense.render.InventoryOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.LevelUpOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.MainMenuRenderer;
import com.amirrezahadipoor.herodefense.render.ParticleRenderer;
import com.amirrezahadipoor.herodefense.render.PauseOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.RewardCardOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.SettingsOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.StatShopOverlayRenderer;
import com.amirrezahadipoor.herodefense.render.TouchFeedbackRenderer;
import com.amirrezahadipoor.herodefense.render.UiFrameRenderer;
import com.amirrezahadipoor.herodefense.render.UiIconRenderer;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.save.LocalSaveRepository;
import com.amirrezahadipoor.herodefense.settings.GameSettings;
import com.amirrezahadipoor.herodefense.settings.LocalSettingsRepository;
import com.amirrezahadipoor.herodefense.shop.StatShopSystem;
import com.amirrezahadipoor.herodefense.skills.SkillId;
import com.amirrezahadipoor.herodefense.skills.SkillShopSystem;

import java.util.Optional;

/** Android-only libGDX game loop and top-level state coordinator. */
public final class HeroDefenseGame extends ApplicationAdapter {
    private static final float MAX_FRAME_DELTA = 1f / 15f;

    private GameFlowController flow;
    private GameAudioManager audioManager;
    private ArenaEnvironmentRenderer arenaEnvironmentRenderer;
    private AutoPotionSystem autoPotionSystem;
    private BossRewardCardSystem bossRewardCardSystem;
    private EquipmentSpriteRenderer equipmentSpriteRenderer;
    private FloatingCoinTextRenderer floatingCoinTextRenderer;
    private FloatingCoinTextSystem floatingCoinTextSystem;
    private FloatingDamageTextSystem floatingDamageTextSystem;
    private FloatingDamageTextRenderer floatingDamageTextRenderer;
    private BossSpecialAttackSystem bossSpecialAttackSystem;
    private CombatEntityRenderer combatEntityRenderer;
    private DropPickupSystem dropPickupSystem;
    private EnemyMeleeAttackSystem enemyMeleeAttackSystem;
    private EnemyMovementSystem enemyMovementSystem;
    private WaveLifecycleSystem waveLifecycleSystem;
    private HeroAnimationController heroAnimationController;
    private HeroAutoAttackSystem heroAutoAttackSystem;
    private GameOverOverlayRenderer gameOverOverlayRenderer;
    private HeroProgressionSystem heroProgressionSystem;
    private HeroSpriteRenderer heroSpriteRenderer;
    private CeremonyHeroRenderer ceremonyHeroRenderer;
    private SaplingTreeRenderer saplingTreeRenderer;
    private final PlantingCeremony plantingCeremony = new PlantingCeremony();
    private static final float WATER_DROP_INTERVAL_SECONDS = 0.07f;
    private float waterDropAccumulator;
    private HapticFeedback hapticFeedback;
    private HitStopSystem hitStopSystem;
    private HudRenderer hudRenderer;
    private InventoryTouchController inventoryTouchController;
    private InventoryOverlayRenderer inventoryOverlayRenderer;
    private ItemDropSystem itemDropSystem;
    private KillRewardSystem killRewardSystem;
    private LevelUpOverlayRenderer levelUpOverlayRenderer;
    private MainMenuRenderer mainMenuRenderer;
    private ParticleRenderer particleRenderer;
    private ParticleSystem particleSystem;
    private PauseOverlayRenderer pauseOverlayRenderer;
    private PauseTouchController pauseTouchController;
    private PotionDropSystem potionDropSystem;
    private RewardCardOverlayRenderer rewardCardOverlayRenderer;
    private RewardCardTouchController rewardCardTouchController;
    private SettingsOverlayRenderer settingsOverlayRenderer;
    private ScreenShakeSystem screenShakeSystem;
    private SettingsTouchController settingsTouchController;
    private SimulationSpeedTouchController simulationSpeedTouchController;
    private StatShopOverlayRenderer statShopOverlayRenderer;
    private StatShopSystem statShopSystem;
    private SkillShopSystem skillShopSystem;
    private StatShopTouchLayout.Tab shopTab = StatShopTouchLayout.Tab.STATS;
    private TouchFeedbackRenderer touchFeedbackRenderer;
    private TouchFeedbackSystem touchFeedbackSystem;
    private UiFrameRenderer uiFrameRenderer;
    private UiIconRenderer uiIconRenderer;
    private SpriteBatch spriteBatch;
    private LocalSaveRepository saves;
    private LocalSettingsRepository settingsRepository;
    private GameSettings settings;
    private GameState gameState;
    private boolean continueAvailable;
    private volatile boolean readyForTouch;
    private volatile long handledTouchUpCount;
    private volatile float lastTouchWorldX = Float.NaN;
    private volatile float lastTouchWorldY = Float.NaN;
    private OrthographicCamera camera;
    private DisplayMetrics displayMetrics;
    private Viewport viewport;
    private float simulationSeconds;
    private float ambientSeconds;
    private float gameOverPresentationSeconds;

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
        rewardCardTouchController = new RewardCardTouchController(bossRewardCardSystem);
        waveLifecycleSystem = new WaveLifecycleSystem(
            enemyWaveSpawner,
            new BossWaveSpawner(new BossFactory()),
            bossRewardCardSystem,
            new ContinuousWaveRun()
        );
        heroAnimationController = new HeroAnimationController();
        heroAutoAttackSystem = new HeroAutoAttackSystem();
        heroProgressionSystem = new HeroProgressionSystem();
        hapticFeedback = new GdxHapticFeedback();
        hitStopSystem = new HitStopSystem();
        killRewardSystem = new KillRewardSystem(heroProgressionSystem);
        inventoryTouchController = new InventoryTouchController(new InventoryEquipmentSystem());
        itemDropSystem = new ItemDropSystem();
        floatingCoinTextSystem = new FloatingCoinTextSystem();
        floatingDamageTextSystem = new FloatingDamageTextSystem();
        particleSystem = new ParticleSystem();
        pauseTouchController = new PauseTouchController();
        potionDropSystem = new PotionDropSystem();
        screenShakeSystem = new ScreenShakeSystem();
        settingsTouchController = new SettingsTouchController();
        simulationSpeedTouchController = new SimulationSpeedTouchController();
        statShopSystem = new StatShopSystem();
        skillShopSystem = new SkillShopSystem();
        touchFeedbackSystem = new TouchFeedbackSystem();
        saves = new LocalSaveRepository(Gdx.app.getPreferences(LocalSaveRepository.PREFERENCES_NAME));
        settingsRepository = new LocalSettingsRepository(
            Gdx.app.getPreferences(LocalSettingsRepository.PREFERENCES_NAME)
        );
        settings = settingsRepository.load();
        audioManager = new GameAudioManager(settings);
        Optional<GameState> loadedRun = saves.load();
        gameState = loadedRun.orElseGet(() -> GameState.newRun(System.currentTimeMillis()));
        continueAvailable = loadedRun.isPresent() && canContinue(gameState);
        new StarterLoadoutSystem().provisionOnce(gameState);
        camera = new OrthographicCamera();
        // Width is pinned to 720; tall panels reveal more arena instead of black bars.
        viewport = new ExtendViewport(
            WorldLayout.REFERENCE_WIDTH,
            DisplayMetrics.MIN_WORLD_HEIGHT,
            WorldLayout.REFERENCE_WIDTH,
            DisplayMetrics.MAX_WORLD_HEIGHT,
            camera
        );
        applyDisplayMetrics(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        spriteBatch = new SpriteBatch();
        arenaEnvironmentRenderer = new ArenaEnvironmentRenderer();
        combatEntityRenderer = new CombatEntityRenderer();
        floatingCoinTextRenderer = new FloatingCoinTextRenderer();
        floatingDamageTextRenderer = new FloatingDamageTextRenderer();
        gameOverOverlayRenderer = new GameOverOverlayRenderer();
        heroSpriteRenderer = new HeroSpriteRenderer();
        ceremonyHeroRenderer = new CeremonyHeroRenderer();
        saplingTreeRenderer = new SaplingTreeRenderer();
        hudRenderer = new HudRenderer();
        equipmentSpriteRenderer = new EquipmentSpriteRenderer();
        inventoryOverlayRenderer = new InventoryOverlayRenderer();
        levelUpOverlayRenderer = new LevelUpOverlayRenderer();
        mainMenuRenderer = new MainMenuRenderer();
        particleRenderer = new ParticleRenderer();
        pauseOverlayRenderer = new PauseOverlayRenderer();
        rewardCardOverlayRenderer = new RewardCardOverlayRenderer();
        settingsOverlayRenderer = new SettingsOverlayRenderer();
        statShopOverlayRenderer = new StatShopOverlayRenderer();
        touchFeedbackRenderer = new TouchFeedbackRenderer();
        uiFrameRenderer = new UiFrameRenderer();
        uiIconRenderer = new UiIconRenderer();
        installTouchInput();
        readyForTouch = true;
    }

    @Override
    public void resize(int width, int height) {
        applyDisplayMetrics(width, height);
    }

    private void applyDisplayMetrics(int width, int height) {
        displayMetrics = new DisplayMetrics(width, height, Gdx.graphics.getDensity());
        viewport.update(width, height, false);
        // Keep the 1280-unit design area centred; overflow is split above and below it.
        camera.position.set(
            WorldLayout.REFERENCE_WIDTH * 0.5f,
            WorldLayout.REFERENCE_HEIGHT * 0.5f,
            0f
        );
        camera.update();
        GameFonts.shared().rebuild(displayMetrics);
        ScreenEdges.update(displayMetrics);
    }

    /** Current panel mapping, exposed for instrumentation tests. */
    public DisplayMetrics displayMetrics() {
        return displayMetrics;
    }

    @Override
    public void render() {
        audioManager.update(settings);
        float deltaSeconds = Math.min(Gdx.graphics.getDeltaTime(), MAX_FRAME_DELTA);
        audioManager.tick(deltaSeconds);
        touchFeedbackSystem.update(deltaSeconds);
        inventoryTouchController.update(deltaSeconds);
        statShopSystem.update(deltaSeconds);
        skillShopSystem.update(deltaSeconds);
        if (flow.simulationRunning()) {
            float gameplayDelta = hitStopSystem.consume(deltaSeconds);
            if (gameplayDelta > 0f) updatePlaying(gameplayDelta);
        } else if (flow.state() == GameScreenState.CINEMATIC) {
            updateCinematic(deltaSeconds);
        }
        ambientSeconds += deltaSeconds;
        if (flow.state() == GameScreenState.GAME_OVER) {
            // Combat has stopped; let the final death, shockwave, and leaf motes settle.
            screenShakeSystem.update(deltaSeconds);
            particleSystem.update(deltaSeconds);
        }
        if (flow.state() == GameScreenState.GAME_OVER && !gameState.runComplete) {
            gameOverPresentationSeconds = Math.min(
                10f,
                gameOverPresentationSeconds + deltaSeconds
            );
        } else {
            gameOverPresentationSeconds = 0f;
        }
        drawCurrentState(deltaSeconds);
    }

    public boolean readyForTouch() {
        return readyForTouch;
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

    /** Read-only test visibility; inventory actions themselves still require touch. */
    public boolean inventoryOpen() {
        return inventoryTouchController != null && inventoryTouchController.isOpen();
    }

    /** Read-only test visibility; inventory selection still changes only through touch. */
    public int inventorySelectedIndex() {
        return inventoryTouchController == null ? -1 : inventoryTouchController.selectedIndex();
    }

    /** Read-only test visibility; action feedback still originates only from touch. */
    public String inventoryFeedbackMessage() {
        return inventoryTouchController == null ? null : inventoryTouchController.feedbackMessage();
    }

    /** Read-only test visibility; Shop feedback still originates only from touch. */
    public String shopFeedbackMessage() {
        if (shopTab == StatShopTouchLayout.Tab.SKILLS && skillShopSystem != null) {
            return skillShopSystem.feedbackMessage();
        }
        return statShopSystem == null ? null : statShopSystem.feedbackMessage();
    }

    /** Read-only test visibility of the active shop tab; changed only by touch. */
    public StatShopTouchLayout.Tab shopTab() {
        return shopTab;
    }

    /** Read-only test visibility used to confirm device touches reached libGDX coordinates. */
    public long handledTouchUpCount() {
        return handledTouchUpCount;
    }

    public float lastTouchWorldX() {
        return lastTouchWorldX;
    }

    public float lastTouchWorldY() {
        return lastTouchWorldY;
    }

    /** Awards kill XP and opens the touch allocation overlay whenever a level is gained. */
    public int grantHeroExperience(int experience) {
        int levelsGained = heroProgressionSystem.grantExperience(gameState, experience);
        if (levelsGained > 0) audioManager.play(AudioCue.LEVEL_UP);
        if (levelsGained > 0 && flow.state() == GameScreenState.PLAYING) {
            flow.transitionTo(GameScreenState.LEVEL_UP);
            saveNow();
        }
        return levelsGained;
    }

    @Override
    public void pause() {
        if (audioManager != null) audioManager.pauseForBackground();
        saveNow();
    }

    @Override
    public void resume() {
        if (audioManager != null) audioManager.resumeFromBackground();
    }

    @Override
    public void dispose() {
        saveNow();
        GameFonts.closeSharedFor(Gdx.app);
        if (audioManager != null) {
            audioManager.close();
        }
        if (arenaEnvironmentRenderer != null) {
            arenaEnvironmentRenderer.close();
        }
        if (combatEntityRenderer != null) {
            combatEntityRenderer.close();
        }
        if (floatingDamageTextRenderer != null) {
            floatingDamageTextRenderer.close();
            floatingDamageTextRenderer = null;
        }
        if (floatingCoinTextRenderer != null) {
            floatingCoinTextRenderer.close();
        }
        if (gameOverOverlayRenderer != null) {
            gameOverOverlayRenderer.close();
        }
        if (heroSpriteRenderer != null) {
            heroSpriteRenderer.close();
        }
        if (ceremonyHeroRenderer != null) {
            ceremonyHeroRenderer.close();
        }
        if (saplingTreeRenderer != null) {
            saplingTreeRenderer.close();
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
        if (particleRenderer != null) {
            particleRenderer.close();
        }
        if (pauseOverlayRenderer != null) {
            pauseOverlayRenderer.close();
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
        if (touchFeedbackRenderer != null) {
            touchFeedbackRenderer.close();
        }
        if (uiFrameRenderer != null) {
            uiFrameRenderer.close();
        }
        if (uiIconRenderer != null) {
            uiIconRenderer.close();
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
                uiFrameRenderer.press(worldX, worldY);
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
                uiFrameRenderer.movePress(worldX, worldY);
                if (flow.state() == GameScreenState.INVENTORY
                    && inventoryTouchController.isOpen()) {
                    inventoryTouchController.drag(gameState, deltaY);
                }
                return true;
            }

            @Override
            public boolean onTouchUp(float worldX, float worldY, int pointer, boolean isTap) {
                uiFrameRenderer.release();
                lastTouchWorldX = worldX;
                lastTouchWorldY = worldY;
                handledTouchUpCount++;
                if (!isTap) {
                    return true;
                }
                boolean cardChoiceTap = flow.state() == GameScreenState.CARD_CHOICE;
                if (!cardChoiceTap) {
                    touchFeedbackSystem.triggerTap(worldX, worldY);
                    hapticFeedback.tap();
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
                if (flow.state() == GameScreenState.GAME_OVER) {
                    if (GameOverOverlayRenderer.isInteractive(
                        gameOverPresentationSeconds,
                        gameState.runComplete
                    ) && GameOverTouchLayout.restartAt(worldX, worldY)) {
                        startNewRun();
                    }
                    return true;
                }
                if (flow.state() == GameScreenState.CARD_CHOICE) {
                    if (rewardCardTouchController.tap(gameState, worldX, worldY)) {
                        touchFeedbackSystem.triggerCardSelection(worldX, worldY);
                        hapticFeedback.cardSelection();
                        WaveCompletion result = waveLifecycleSystem.continueAfterBossReward(gameState);
                        if (result == WaveCompletion.RUN_COMPLETED) {
                            flow.transitionTo(GameScreenState.GAME_OVER);
                        } else if (result == WaveCompletion.PLANTING_CEREMONY) {
                            beginPlantingCeremony();
                        } else {
                            flow.transitionTo(GameScreenState.PLAYING);
                        }
                        saveNow();
                    } else {
                        touchFeedbackSystem.triggerTap(worldX, worldY);
                        hapticFeedback.tap();
                    }
                    return true;
                }
                if (flow.state() == GameScreenState.CINEMATIC) {
                    plantingCeremony.skip();
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
                    StatShopTouchLayout.Tab tab = StatShopTouchLayout.tabAt(worldX, worldY);
                    if (StatShopTouchLayout.closeAt(worldX, worldY)) {
                        flow.returnFromOverlay();
                        saveNow();
                    } else if (tab != null) {
                        shopTab = tab;
                    } else if (shopTab == StatShopTouchLayout.Tab.SKILLS) {
                        SkillId skill = StatShopTouchLayout.skillAt(worldX, worldY);
                        if (skillShopSystem.purchase(gameState, skill)) {
                            audioManager.play(AudioCue.PURCHASE);
                            saveNow();
                        }
                    } else {
                        HeroStat stat = StatShopTouchLayout.statAt(worldX, worldY);
                        if (statShopSystem.purchase(gameState, stat)) {
                            audioManager.play(AudioCue.PURCHASE);
                            saveNow();
                        }
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
                    && HudTouchLayout.inventoryAt(worldX, worldY)) {
                    flow.transitionTo(GameScreenState.INVENTORY);
                    inventoryTouchController.open();
                    saveNow();
                    return true;
                }
                if (flow.state() == GameScreenState.PLAYING
                    && HudTouchLayout.shopAt(worldX, worldY)) {
                    flow.transitionTo(GameScreenState.SHOP);
                    saveNow();
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
                if (flow.state() == GameScreenState.INVENTORY
                    && inventoryTouchController.isOpen()) {
                    InventoryTouchController.Action action = inventoryTouchController.tap(
                        gameState, worldX, worldY
                    );
                    if (action == InventoryTouchController.Action.CLOSED) {
                        flow.returnFromOverlay();
                        saveNow();
                    } else if (action == InventoryTouchController.Action.EQUIPPED
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
                    flow.transitionTo(GameScreenState.INVENTORY);
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
        gameOverPresentationSeconds = 0f;
        hitStopSystem.clear();
        particleSystem.clear();
        floatingCoinTextSystem.clear();
        floatingDamageTextSystem.clear();
        waveLifecycleSystem.startCurrentWave(gameState);
        flow.transitionTo(GameScreenState.PLAYING);
        saveNow();
    }

    private void continueRun() {
        if (!continueAvailable || !canContinue(gameState)) return;
        flow.transitionTo(GameScreenState.PLAYING);
        if (gameState.awaitingBossReward) {
            flow.transitionTo(GameScreenState.CARD_CHOICE);
        } else if (gameState.ceremonyPending) {
            // A save closed mid-ceremony replays it from the start; it is deterministic.
            beginPlantingCeremony();
        } else if (gameState.unspentTalentPoints > 0) {
            flow.transitionTo(GameScreenState.LEVEL_UP);
        } else if (!gameState.waveActive) {
            int bossesBefore = livingBossCount(gameState);
            waveLifecycleSystem.startCurrentWave(gameState);
            if (livingBossCount(gameState) > bossesBefore) {
                audioManager.play(AudioCue.BOSS_ENTRANCE);
                presentBossEntrance(gameState);
            }
        }
    }

    private void emitDefeatParticles(GameState state) {
        for (Enemy enemy : state.aliveEnemies) emitDefeatParticles(enemy, false);
        for (Boss boss : state.aliveBosses) emitDefeatParticles(boss, true);
    }

    private void emitDefeatParticles(Enemy enemy, boolean boss) {
        if (enemy == null || enemy.alive || enemy.defeatParticlesEmitted) return;
        enemy.defeatParticlesEmitted = true;
        if (boss) {
            particleSystem.emitBossDeath(enemy.x, enemy.y + 40f);
        } else {
            particleSystem.emitDeath(enemy.x, enemy.y + 30f);
        }
        particleSystem.emitCoins(enemy.x, enemy.y + 50f);
    }

    private void presentBossEntrance(GameState state) {
        for (Boss boss : state.aliveBosses) {
            if (boss == null || !boss.alive || boss.entrancePresented) continue;
            boss.entrancePresented = true;
            particleSystem.emitBossEntrance(boss.x, boss.y + 10f);
        }
        screenShakeSystem.triggerBossEntrance();
    }

    /** Sparkles where a homing drop lands on the Inventory control, before the drop is removed. */
    private void emitCollectionSparkles(GameState state, float deltaSeconds) {
        for (DropEntity drop : state.drops) {
            if (drop == null || !drop.active
                || drop.collectionStage != DropCollectionStage.HOMING) {
                continue;
            }
            if (drop.homingElapsedSeconds + deltaSeconds >= DropPickupSystem.HOMING_DURATION_SECONDS
                && ("ITEM".equals(drop.dropType) || "POTION".equals(drop.dropType))) {
                particleSystem.emitCollectionSparkle(
                    CombatEntityRenderer.DROP_TARGET_X, CombatEntityRenderer.DROP_TARGET_Y + 30f
                );
            }
        }
    }

    private void emitPendingPickupParticles(GameState state, float deltaSeconds) {
        for (DropEntity drop : state.drops) {
            if (drop == null || !drop.active || drop.collectionEffectEmitted) continue;
            boolean enteringHoming = drop.collectionStage == DropCollectionStage.HOMING
                || drop.pickupDelaySeconds <= deltaSeconds;
            if (enteringHoming
                && ("ITEM".equals(drop.dropType) || "POTION".equals(drop.dropType))) {
                drop.collectionEffectEmitted = true;
                particleSystem.emitItemPickup(drop.x, drop.y + 25f);
            }
        }
    }

    private static float totalEnemyHealth(GameState state) {
        float total = 0f;
        for (Enemy enemy : state.aliveEnemies) {
            if (enemy != null) total += Math.max(0f, enemy.health);
        }
        for (Boss boss : state.aliveBosses) {
            if (boss != null) total += Math.max(0f, boss.health);
        }
        return total;
    }

    private static int livingBossCount(GameState state) {
        int count = 0;
        for (Boss boss : state.aliveBosses) {
            if (boss != null && boss.alive) count++;
        }
        return count;
    }

    private void beginPlantingCeremony() {
        flow.transitionTo(GameScreenState.CINEMATIC);
        hitStopSystem.clear();
        plantingCeremony.begin();
        gameState.anchorHeroAtArenaCenter();
    }

    /** Presentation-only ceremony tick; the wave-101 hand-off happens once it completes. */
    private void updateCinematic(float deltaSeconds) {
        screenShakeSystem.update(deltaSeconds);
        particleSystem.update(deltaSeconds);
        floatingCoinTextSystem.update(deltaSeconds);
        floatingDamageTextSystem.update(deltaSeconds);
        boolean finished = plantingCeremony.update(deltaSeconds);
        if (plantingCeremony.pouring()) {
            waterDropAccumulator += deltaSeconds;
            while (waterDropAccumulator >= WATER_DROP_INTERVAL_SECONDS) {
                waterDropAccumulator -= WATER_DROP_INTERVAL_SECONDS;
                particleSystem.emitWaterDrops(
                    plantingCeremony.heroX() + 52f, plantingCeremony.heroY() + 58f
                );
            }
        } else {
            waterDropAccumulator = 0f;
        }
        if (finished) {
            int bossesBefore = livingBossCount(gameState);
            waveLifecycleSystem.completePlantingCeremony(gameState);
            flow.transitionTo(GameScreenState.PLAYING);
            if (livingBossCount(gameState) > bossesBefore) {
                audioManager.play(AudioCue.BOSS_ENTRANCE);
                presentBossEntrance(gameState);
            }
            saveNow();
        }
    }

    private static boolean canContinue(GameState state) {
        return state != null && state.hero != null && state.hero.alive && !state.runComplete;
    }

    private void updatePlaying(float deltaSeconds) {
        float simulationDelta = deltaSeconds * gameState.simulationSpeed;
        screenShakeSystem.update(simulationDelta);
        particleSystem.update(simulationDelta);
        floatingCoinTextSystem.update(simulationDelta);
        floatingDamageTextSystem.update(simulationDelta);
        gameState.anchorHeroAtArenaCenter();
        heroAnimationController.update(gameState.hero, simulationDelta);
        enemyMovementSystem.update(gameState, simulationDelta);
        int livingBeforeAttack = gameState.livingEnemyCount();
        int bossesBeforeAttack = livingBossCount(gameState);
        float enemyHealthBeforeAttack = totalEnemyHealth(gameState);
        HeroAttackUpdateResult attackEvents = heroAutoAttackSystem.update(gameState, simulationDelta);
        floatingDamageTextSystem.emitAll(attackEvents.events());
        // Every arrow in a Multi Shot volley bursts where it lands (events carry y + 40 for text).
        for (CombatEvent event : attackEvents.events()) {
            switch (event.kind()) {
                case HIT -> particleSystem.emitHit(event.x(), event.y() - 40f, false);
                case CRITICAL_HIT -> particleSystem.emitHit(event.x(), event.y() - 40f, true);
                case CHAIN_ARC -> particleSystem.emitChainArc(
                    event.fromX(), event.fromY(), event.x(), event.y());
                case STUN -> particleSystem.emitStunSparks(event.x(), event.y() - 30f, event.amount());
                default -> { }
            }
        }
        if (attackEvents.criticalHits() > 0) {
            hitStopSystem.triggerCriticalHit();
            screenShakeSystem.triggerCriticalHit();
            audioManager.play(AudioCue.CRITICAL);
        }
        if (attackEvents.chainArcs() > 0) audioManager.play(AudioCue.CHAIN_LIGHTNING);
        if (attackEvents.stuns() > 0) audioManager.play(AudioCue.STUN);
        if (attackEvents.shots() > 1) audioManager.play(AudioCue.MULTI_SHOT);
        if (totalEnemyHealth(gameState) < enemyHealthBeforeAttack - 0.001f) {
            audioManager.play(AudioCue.HIT);
        }
        if (gameState.livingEnemyCount() < livingBeforeAttack) {
            audioManager.play(AudioCue.DEATH);
            audioManager.play(AudioCue.KILL);
        }
        if (livingBossCount(gameState) < bossesBeforeAttack) {
            screenShakeSystem.triggerBossKill();
        }
        float heroHealthBeforeAttack = gameState.hero.health;
        bossSpecialAttackSystem.update(gameState, simulationDelta);
        boolean gameOver = enemyMeleeAttackSystem.update(gameState, simulationDelta);
        if (gameState.hero.health < heroHealthBeforeAttack - 0.001f) {
            screenShakeSystem.triggerHeroHit();
            particleSystem.emitHit(gameState.hero.x, gameState.hero.y + 45f, false);
            audioManager.play(gameState.hero.alive ? AudioCue.HIT : AudioCue.DEATH);
        }
        emitDefeatParticles(gameState);
        if (!gameOver && gameState.hero.alive) {
            autoPotionSystem.update(gameState);
        }
        int itemDrops = itemDropSystem.processDefeatedEnemies(gameState);
        if (itemDrops > 0) audioManager.play(AudioCue.ITEM_DROP);
        potionDropSystem.processDefeatedEnemies(gameState);
        KillRewardResult killRewards = killRewardSystem.processDefeatedEnemies(gameState);
        if (killRewards.coins() > 0) {
            floatingCoinTextSystem.emit(
                gameState.hero.x,
                gameState.hero.y + 145f,
                killRewards.coins()
            );
        }
        if (killRewards.levelsGained() > 0) audioManager.play(AudioCue.LEVEL_UP);
        emitPendingPickupParticles(gameState, simulationDelta);
        emitCollectionSparkles(gameState, simulationDelta);
        dropPickupSystem.update(gameState, simulationDelta, settings);
        if (dropPickupSystem.lastAutoSoldItems() > 0) {
            floatingDamageTextSystem.emitCoins(
                dropPickupSystem.lastAutoSoldCoins(),
                gameState.hero.x, gameState.hero.y + 96f
            );
            audioManager.play(AudioCue.ITEM_DROP);
        }
        if (gameOver) {
            particleSystem.emitTreeDestruction(WorldLayout.WORLD_TREE_X, WorldLayout.WORLD_TREE_Y);
            if (gameState.secondTreePlanted) {
                particleSystem.emitTreeDestruction(WorldLayout.SECOND_TREE_X, WorldLayout.SECOND_TREE_Y);
            }
            screenShakeSystem.triggerTreeFall();
            flow.transitionTo(GameScreenState.GAME_OVER);
            saveNow();
        } else if (killRewards.levelsGained() > 0) {
            flow.transitionTo(GameScreenState.LEVEL_UP);
            saveNow();
        } else {
            int bossesBeforeWaveAdvance = livingBossCount(gameState);
            WaveCompletion waveCompletion = waveLifecycleSystem.updateAfterCombat(gameState);
            if (livingBossCount(gameState) > bossesBeforeWaveAdvance) {
                audioManager.play(AudioCue.BOSS_ENTRANCE);
                presentBossEntrance(gameState);
            }
            if (waveCompletion == WaveCompletion.BOSS_REWARD) {
                flow.transitionTo(GameScreenState.CARD_CHOICE);
            } else if (waveCompletion == WaveCompletion.PLANTING_CEREMONY) {
                beginPlantingCeremony();
            } else if (waveCompletion == WaveCompletion.RUN_COMPLETED) {
                flow.transitionTo(GameScreenState.GAME_OVER);
            }
            if (waveCompletion != WaveCompletion.NO_CHANGE) {
                saveNow();
            }
        }
        simulationSeconds += simulationDelta;
    }

    private void drawCurrentState(float presentationDeltaSeconds) {
        float tint = switch (flow.state()) {
            case MENU -> 0.14f;
            case SETTINGS -> 0.13f;
            case PLAYING -> 0.20f;
            case PAUSED -> 0.11f;
            case LEVEL_UP -> 0.22f;
            case CARD_CHOICE -> 0.24f;
            case CINEMATIC -> 0.21f;
            case INVENTORY -> 0.18f;
            case SHOP -> 0.18f;
            case GAME_OVER -> 0.08f;
        };
        Gdx.gl.glClearColor(tint * 0.55f, tint, tint * 0.78f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (flow.state() != GameScreenState.MENU && flow.state() != GameScreenState.SETTINGS) {
            float baseCameraX = WorldLayout.REFERENCE_WIDTH * 0.5f;
            float baseCameraY = WorldLayout.REFERENCE_HEIGHT * 0.5f;
            camera.position.set(
                baseCameraX + screenShakeSystem.offsetX(),
                baseCameraY + screenShakeSystem.offsetY(),
                camera.position.z
            );
            camera.update();
            spriteBatch.setProjectionMatrix(camera.combined);
            spriteBatch.begin();
            arenaEnvironmentRenderer.draw(
                spriteBatch,
                gameState,
                simulationSeconds,
                presentationDeltaSeconds
            );
            spriteBatch.end();
            particleRenderer.drawAmbient(camera.combined, ambientSeconds);
            spriteBatch.begin();
            boolean cinematic = flow.state() == GameScreenState.CINEMATIC;
            if (cinematic) {
                if (plantingCeremony.saplingVisible()) {
                    saplingTreeRenderer.drawGrowing(spriteBatch, plantingCeremony);
                }
            } else if (gameState.secondTreePlanted) {
                saplingTreeRenderer.drawIdle(spriteBatch, ambientSeconds);
            }
            combatEntityRenderer.drawActors(spriteBatch, gameState, simulationSeconds);
            if (cinematic) {
                ceremonyHeroRenderer.draw(spriteBatch, plantingCeremony);
            } else {
                int heroFrame = heroAnimationController.frameIndex(gameState.hero);
                heroSpriteRenderer.draw(spriteBatch, gameState.hero, heroFrame);
                equipmentSpriteRenderer.draw(spriteBatch, gameState, heroFrame, simulationSeconds);
            }
            combatEntityRenderer.drawEffects(spriteBatch, gameState, simulationSeconds);
            spriteBatch.end();
            particleRenderer.draw(camera.combined, particleSystem, simulationSeconds);
            floatingDamageTextRenderer.draw(
                spriteBatch, camera.combined, floatingDamageTextSystem
            );
            floatingCoinTextRenderer.draw(
                spriteBatch, camera.combined, floatingCoinTextSystem
            );
            camera.position.set(baseCameraX, baseCameraY, camera.position.z);
            camera.update();
        }
        if (flow.state() == GameScreenState.PLAYING || flow.state() == GameScreenState.CINEMATIC) {
            hudRenderer.draw(
                spriteBatch, camera.combined, gameState, uiIconRenderer, uiFrameRenderer,
                presentationDeltaSeconds
            );
        }
        if (flow.state() == GameScreenState.MENU) {
            mainMenuRenderer.draw(
                spriteBatch,
                camera.combined,
                continueAvailable,
                gameState.coins,
                uiIconRenderer,
                uiFrameRenderer
            );
        } else if (flow.state() == GameScreenState.SETTINGS) {
            settingsOverlayRenderer.draw(
                spriteBatch, camera.combined, settings, uiIconRenderer, uiFrameRenderer
            );
        } else if (flow.state() == GameScreenState.LEVEL_UP) {
            levelUpOverlayRenderer.draw(
                spriteBatch, camera.combined, gameState, uiIconRenderer, uiFrameRenderer
            );
        } else if (flow.state() == GameScreenState.GAME_OVER) {
            gameOverOverlayRenderer.draw(
                spriteBatch,
                camera.combined,
                gameState,
                uiIconRenderer,
                uiFrameRenderer,
                gameOverPresentationSeconds
            );
        } else if (flow.state() == GameScreenState.CARD_CHOICE) {
            rewardCardOverlayRenderer.draw(
                spriteBatch, camera.combined, gameState, uiIconRenderer, uiFrameRenderer
            );
        } else if (flow.state() == GameScreenState.SHOP) {
            statShopOverlayRenderer.draw(
                spriteBatch,
                camera.combined,
                gameState,
                statShopSystem,
                skillShopSystem,
                shopTab,
                uiIconRenderer,
                uiFrameRenderer,
                flow.returnState() == GameScreenState.PAUSED
            );
        } else if (flow.state() == GameScreenState.INVENTORY) {
            inventoryOverlayRenderer.drawInventory(
                spriteBatch,
                camera.combined,
                gameState,
                inventoryTouchController,
                uiIconRenderer,
                uiFrameRenderer,
                settings
            );
        } else if (flow.state() == GameScreenState.PAUSED) {
            pauseOverlayRenderer.draw(
                spriteBatch, camera.combined, gameState, uiIconRenderer, uiFrameRenderer
            );
        }
        touchFeedbackRenderer.draw(camera.combined, touchFeedbackSystem);
    }
}
