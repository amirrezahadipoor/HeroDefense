package com.amirrezahadipoor.herodefense.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.badlogic.gdx.backends.android.AndroidGraphics;
import com.amirrezahadipoor.herodefense.GameScreenState;
import com.amirrezahadipoor.herodefense.HeroDefenseGame;
import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.save.GameStateCodec;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Drives real app paths only through touchscreen coordinates; direct state access is assertion-only. */
@RunWith(AndroidJUnit4.class)
public final class AndroidTouchSmokeTest {
    private static final float WORLD_WIDTH = 720f;
    private static final float WORLD_HEIGHT = 1280f;
    private static final String SAVE_NAME = "hero-defense-local-save";

    @Test
    public void touchNavigatesMenuWavePauseInventoryDragAndResume() {
        clearRunSave();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("main menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);
            SystemClock.sleep(1_500L);
            captureScreen("main-menu-premium-v2.png");
            tapWorld(surface, 360f, 380f);
            await("premium settings", () -> game.screenState() == GameScreenState.SETTINGS);
            SystemClock.sleep(500L);
            captureScreen("settings-premium-v2.png");
            tapWorld(surface, 620f, 1_170f);
            await("settings closes", () -> game.screenState() == GameScreenState.MENU);

            long touchCount = game.handledTouchUpCount();
            tapWorld(surface, 360f, 760f); // New Game
            await("new-game touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            float[] correction = touchCorrection(game, 360f, 760f);
            await("wave starts", () -> game.screenState() == GameScreenState.PLAYING);
            assertEquals(1, game.gameState().waveNumber);
            assertTrue(game.gameState().waveActive);
            assertTrue(game.gameState().livingEnemyCount() > 0);
            SystemClock.sleep(1_500L);
            captureScreen("live-hud-premium-v2.png");

            tapWorld(surface, 450f + correction[0], 76f + correction[1]); // Direct Shop
            await("direct shop opens", () -> game.screenState() == GameScreenState.SHOP);
            tapWorld(surface, 620f + correction[0], 1_170f + correction[1]); // Close Shop
            await("direct shop returns to play", () -> game.screenState() == GameScreenState.PLAYING);

            tapWorld(surface, 270f + correction[0], 76f + correction[1]); // Direct Inventory
            await("direct inventory pauses", () ->
                game.screenState() == GameScreenState.INVENTORY && game.inventoryOpen()
            );
            tapWorld(surface, 620f + correction[0], 1_160f + correction[1]); // Close Inventory
            await("direct inventory returns to play", () ->
                game.screenState() == GameScreenState.PLAYING && !game.inventoryOpen()
            );

            tapWorld(
                surface,
                600f + correction[0],
                1115f + correction[1]
            ); // Pause HUD target, calibrated from the preceding real touch.
            await("paused", () -> game.screenState() == GameScreenState.PAUSED);
            SystemClock.sleep(500L);
            captureScreen("pause-premium-v2.png");

            tapWorld(surface, 360f + correction[0], 1_000f + correction[1]); // Stat Shop
            await("shop opens over pause", () -> game.screenState() == GameScreenState.SHOP);
            tapWorld(surface, 620f + correction[0], 1_170f + correction[1]); // Close Shop
            await("shop returns to pause", () -> game.screenState() == GameScreenState.PAUSED);
            tapWorld(surface, 360f + correction[0], 600f + correction[1]); // Resume
            await("resume after shop", () -> game.screenState() == GameScreenState.PLAYING);

            tapWorld(surface, 600f + correction[0], 1_115f + correction[1]); // Pause again
            await("paused again", () -> game.screenState() == GameScreenState.PAUSED);
            tapWorld(surface, 360f + correction[0], 830f + correction[1]); // Inventory
            await("inventory opens over pause", () ->
                game.screenState() == GameScreenState.INVENTORY && game.inventoryOpen()
            );

            swipeWorld(
                surface,
                360f + correction[0],
                580f + correction[1],
                360f + correction[0],
                730f + correction[1]
            ); // Drag-only inventory gesture
            assertTrue(game.inventoryOpen());
            tapWorld(surface, 620f + correction[0], 1160f + correction[1]); // Close
            await("inventory returns to pause", () ->
                game.screenState() == GameScreenState.PAUSED && !game.inventoryOpen()
            );
            tapWorld(surface, 360f + correction[0], 600f + correction[1]); // Resume
            await("play resumes", () -> game.screenState() == GameScreenState.PLAYING);

            assertEquals(
                "com.amirrezahadipoor.herodefense.debug",
                InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName()
            );
        }
    }

    @Test
    public void touchSelectsComparesAndSellsFromPremiumInventory() {
        prepareInventoryShowcaseSave();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("inventory showcase menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);

            long touchCount = game.handledTouchUpCount();
            tapWorld(surface, 360f, 570f); // Continue prepared run
            await("continue touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            float[] correction = touchCorrection(game, 360f, 570f);
            await("showcase run", () -> game.screenState() == GameScreenState.PLAYING);

            tapWorld(surface, 270f + correction[0], 76f + correction[1]);
            await("premium inventory", () ->
                game.screenState() == GameScreenState.INVENTORY && game.inventoryOpen()
            );
            tapWorld(surface, 200f + correction[0], 600f + correction[1]);
            await("inventory row selection", () -> game.inventorySelectedIndex() == 0);
            SystemClock.sleep(1_000L);
            captureScreen("inventory-details-premium-v2.png");

            int coinsBefore = game.gameState().coins;
            tapWorld(surface, 525f + correction[0], 135f + correction[1]);
            await("visible sell confirmation", () -> game.inventoryFeedbackMessage() != null);
            assertTrue(game.gameState().coins > coinsBefore);
            SystemClock.sleep(100L);
            captureScreen("inventory-sell-feedback-premium-v2.png");
            tapWorld(surface, 620f + correction[0], 1_160f + correction[1]);
            await("inventory showcase closes", () -> game.screenState() == GameScreenState.PLAYING);
        }
    }

    @Test
    public void touchReviewsAffordabilityAndPurchasesFromPausedShop() {
        prepareShopShowcaseSave();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("shop showcase menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);

            long touchCount = game.handledTouchUpCount();
            tapWorld(surface, 360f, 570f);
            await("continue touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            float[] correction = touchCorrection(game, 360f, 570f);
            await("shop showcase run", () -> game.screenState() == GameScreenState.PLAYING);
            tapWorld(surface, 450f + correction[0], 76f + correction[1]);
            await("premium shop", () -> game.screenState() == GameScreenState.SHOP);
            SystemClock.sleep(1_000L);
            captureScreen("shop-affordability-premium-v2.png");

            tapWorld(surface, 360f + correction[0], 990f + correction[1]);
            await("purchase confirmation", () -> game.shopFeedbackMessage() != null);
            assertEquals(25, game.gameState().coins);
            SystemClock.sleep(100L);
            captureScreen("shop-purchase-feedback-premium-v2.png");
            tapWorld(surface, 620f + correction[0], 1_170f + correction[1]);
            await("shop showcase closes", () -> game.screenState() == GameScreenState.PLAYING);
        }
    }

    @Test
    public void touchContinuesIntoPremiumLevelUpAndAllocatesOnePoint() {
        prepareLevelUpSave();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("level-up menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);
            tapWorld(surface, 360f, 570f);
            float[] correction = touchCorrection(game, 360f, 570f);
            await("premium level-up", () -> game.screenState() == GameScreenState.LEVEL_UP);
            SystemClock.sleep(1_000L);
            captureScreen("level-up-premium-v2.png");
            tapWorld(surface, 360f + correction[0], 895f + correction[1]);
            await("one talent allocated", () -> game.gameState().unspentTalentPoints == 1);
            assertEquals(GameScreenState.LEVEL_UP, game.screenState());
        }
    }

    @Test
    public void touchOpensPersistedPremiumDefeatResult() {
        prepareTerminalSave(false);
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("defeat-result menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);
            tapWorld(surface, 360f, 570f);
            await("premium defeat", () -> game.screenState() == GameScreenState.GAME_OVER);
            SystemClock.sleep(1_500L);
            captureScreen("defeat-premium-v2.png");
            assertFalse(game.gameState().hero.alive);
            assertFalse(game.gameState().runComplete);
        }
    }

    @Test
    public void touchOpensPersistedPremiumVictoryResult() {
        prepareTerminalSave(true);
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("victory-result menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);
            tapWorld(surface, 360f, 570f);
            await("premium victory", () -> game.screenState() == GameScreenState.GAME_OVER);
            SystemClock.sleep(1_000L);
            captureScreen("victory-premium-v2.png");
            assertTrue(game.gameState().runComplete);
            assertEquals(100, game.gameState().waveNumber);
        }
    }

    @Test
    public void touchContinuesIntoAndSelectsExactlyOneRewardCard() {
        prepareRewardCardSave();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("saved run menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);

            long touchCount = game.handledTouchUpCount();
            tapWorld(surface, 360f, 570f); // Continue
            await("continue touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            float[] correction = touchCorrection(game, 360f, 570f);
            await("reward cards", () -> game.screenState() == GameScreenState.CARD_CHOICE);
            assertEquals(3, game.gameState().pendingRewardCards.size());
            SystemClock.sleep(1_000L);
            captureScreen("reward-cards-premium-v2.png");

            tapWorld(surface, 360f + correction[0], 890f + correction[1]); // First card
            await("card applied", () -> game.screenState() == GameScreenState.PLAYING);
            assertEquals(1, game.gameState().chosenRewardCards.size());
            assertEquals(6, game.gameState().waveNumber);
            assertFalse(game.gameState().awaitingBossReward);
        }
    }

    private static HeroDefenseGame gameFrom(ActivityScenario<AndroidLauncher> scenario) {
        AtomicReference<HeroDefenseGame> reference = new AtomicReference<>();
        scenario.onActivity(activity -> reference.set(activity.gameForTests()));
        assertNotNull(reference.get());
        return reference.get();
    }

    private static View gameSurfaceFrom(ActivityScenario<AndroidLauncher> scenario) {
        AtomicReference<View> reference = new AtomicReference<>();
        scenario.onActivity(activity -> reference.set(
            ((AndroidGraphics) activity.getGraphics()).getView()
        ));
        assertNotNull(reference.get());
        return reference.get();
    }

    private static void tapWorld(View surface, float worldX, float worldY) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            float[] point = worldPoint(surface, worldX, worldY);
            long downTime = SystemClock.uptimeMillis();
            dispatchTouch(surface, downTime, downTime, MotionEvent.ACTION_DOWN, point[0], point[1]);
            dispatchTouch(surface, downTime, downTime + 32L, MotionEvent.ACTION_UP, point[0], point[1]);
        });
    }

    private static void swipeWorld(
        View surface, float fromX, float fromY, float toX, float toY
    ) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            float[] start = worldPoint(surface, fromX, fromY);
            float[] end = worldPoint(surface, toX, toY);
            long downTime = SystemClock.uptimeMillis();
            dispatchTouch(surface, downTime, downTime, MotionEvent.ACTION_DOWN, start[0], start[1]);
            for (int step = 1; step <= 12; step++) {
                float progress = step / 12f;
                float x = start[0] + (end[0] - start[0]) * progress;
                float y = start[1] + (end[1] - start[1]) * progress;
                dispatchTouch(
                    surface,
                    downTime,
                    downTime + step * 16L,
                    MotionEvent.ACTION_MOVE,
                    x,
                    y
                );
            }
            dispatchTouch(
                surface,
                downTime,
                downTime + 13L * 16L,
                MotionEvent.ACTION_UP,
                end[0],
                end[1]
            );
        });
    }

    private static void dispatchTouch(
        View surface, long downTime, long eventTime, int action, float x, float y
    ) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            assertTrue(surface.dispatchTouchEvent(event));
        } finally {
            event.recycle();
        }
    }

    private static float[] worldPoint(View surface, float worldX, float worldY) {
        float scale = Math.min(
            surface.getWidth() / WORLD_WIDTH,
            surface.getHeight() / WORLD_HEIGHT
        );
        float viewportWidth = WORLD_WIDTH * scale;
        float viewportHeight = WORLD_HEIGHT * scale;
        float left = (surface.getWidth() - viewportWidth) * 0.5f;
        float top = (surface.getHeight() - viewportHeight) * 0.5f;
        return new float[] {
            left + worldX * scale,
            top + (WORLD_HEIGHT - worldY) * scale
        };
    }

    private static float[] touchCorrection(
        HeroDefenseGame game, float expectedWorldX, float expectedWorldY
    ) {
        float actualX = game.lastTouchWorldX();
        float actualY = game.lastTouchWorldY();
        assertFalse(Float.isNaN(actualX));
        assertFalse(Float.isNaN(actualY));
        return new float[] {expectedWorldX - actualX, expectedWorldY - actualY};
    }

    private static void await(String label, BooleanSupplier condition) {
        long deadline = SystemClock.uptimeMillis() + 5_000L;
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            SystemClock.sleep(50L);
        }
        throw new AssertionError("Timed out waiting for " + label);
    }

    private static void captureScreen(String name) {
        Bitmap screenshot = InstrumentationRegistry.getInstrumentation()
            .getUiAutomation()
            .takeScreenshot();
        assertNotNull(screenshot);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File directory = new File(context.getExternalMediaDirs()[0], "additional_test_output");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        File destination = new File(directory, name);
        try (FileOutputStream output = new FileOutputStream(destination)) {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
        } catch (IOException exception) {
            throw new AssertionError("Could not capture " + destination, exception);
        } finally {
            screenshot.recycle();
        }
        assertTrue(destination.isFile());
        assertTrue(destination.length() > 0L);
    }

    private static void persistPreparedState(GameState state) {
        String json = new GameStateCodec().encode(state);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getSharedPreferences(SAVE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString("run.primary", json)
            .commit());
    }

    private static void clearRunSave() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getSharedPreferences(SAVE_NAME, Context.MODE_PRIVATE).edit().clear().commit());
    }

    private static void prepareInventoryShowcaseSave() {
        GameState state = GameState.newRun(881L);
        for (String id : new String[] {
            "crown_of_first_leaves",
            "crystalbark_plate",
            "verdant_glaive",
            "boots_of_three_winds",
            "sapphire_luck_ring"
        }) {
            state.inventory.add(EquipmentCatalog.byId(id).createItem());
        }
        String json = new GameStateCodec().encode(state);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getSharedPreferences(SAVE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString("run.primary", json)
            .commit());
    }

    private static void prepareShopShowcaseSave() {
        GameState state = GameState.newRun(882L);
        state.coins = 80;
        state.shopUpgradeLevels.put(HeroStat.AGILITY.name(), 2);
        state.shopUpgradeLevels.put(HeroStat.LUCK.name(), 20);
        state.shopUpgradeLevels.put(HeroStat.HEALTH.name(), 1);
        String json = new GameStateCodec().encode(state);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getSharedPreferences(SAVE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString("run.primary", json)
            .commit());
    }

    private static void prepareLevelUpSave() {
        GameState state = GameState.newRun(883L);
        state.heroLevel = 7;
        state.unspentTalentPoints = 2;
        state.hero.stats.strength = 5;
        state.hero.stats.agility = 4;
        state.hero.stats.luck = 3;
        state.hero.stats.dodge = 2;
        state.hero.stats.health = 6;
        persistPreparedState(state);
    }

    private static void prepareTerminalSave(boolean victory) {
        GameState state = GameState.newRun(victory ? 885L : 884L);
        state.runComplete = victory;
        state.waveNumber = victory ? 100 : 47;
        state.heroLevel = victory ? 28 : 14;
        state.totalKills = victory ? 1_248 : 436;
        state.totalKillCoinsEarned = victory ? 9_640 : 3_120;
        state.defeatedBosses = victory ? 20 : 9;
        state.hero.alive = victory;
        state.hero.health = victory ? state.hero.maxHealth : 0f;
        persistPreparedState(state);
    }

    private static void prepareRewardCardSave() {
        GameState state = GameState.newRun(991L);
        state.waveNumber = 5;
        new BossRewardCardSystem().prepareChoices(state, 1);
        String json = new GameStateCodec().encode(state);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getSharedPreferences(SAVE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString("run.primary", json)
            .commit());
    }
}
