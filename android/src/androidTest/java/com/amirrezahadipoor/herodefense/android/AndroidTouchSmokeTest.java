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

            tapWorld(surface, 450f + correction[0], utilityRowY(surface) + correction[1]); // Direct Shop
            await("direct shop opens", () -> game.screenState() == GameScreenState.SHOP);
            tapWorld(surface, 620f + correction[0], 1_170f + correction[1]); // Close Shop
            await("direct shop returns to play", () -> game.screenState() == GameScreenState.PLAYING);

            tapWorld(surface, 270f + correction[0], utilityRowY(surface) + correction[1]); // Direct Inventory
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
                statusRowY(surface) + correction[1]
            ); // Pause HUD target, calibrated from the preceding real touch.
            await("paused", () -> game.screenState() == GameScreenState.PAUSED);
            SystemClock.sleep(600L);
            captureScreen("pause-premium-v2.png");

            tapWorld(surface, 360f + correction[0], 1_000f + correction[1]); // Stat Shop
            await("shop opens over pause", () -> game.screenState() == GameScreenState.SHOP);
            tapWorld(surface, 620f + correction[0], 1_170f + correction[1]); // Close Shop
            await("shop returns to pause", () -> game.screenState() == GameScreenState.PAUSED);
            tapWorld(surface, 360f + correction[0], 600f + correction[1]); // Resume
            await("resume after shop", () -> game.screenState() == GameScreenState.PLAYING);

            tapWorld(surface, 600f + correction[0], statusRowY(surface) + correction[1]); // Pause again
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

            tapWorld(surface, 270f + correction[0], utilityRowY(surface) + correction[1]);
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
            tapWorld(surface, 450f + correction[0], utilityRowY(surface) + correction[1]);
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
            SystemClock.sleep(800L);
            captureScreen("reward-cards-premium-v2.png");

            tapWorld(surface, 360f + correction[0], 890f + correction[1]); // First card
            await("card applied", () -> game.screenState() == GameScreenState.PLAYING);
            assertEquals(1, game.gameState().chosenRewardCards.size());
            assertEquals(6, game.gameState().waveNumber);
            assertFalse(game.gameState().awaitingBossReward);
        }
    }

    @Test
    public void touchTogglesSettingsFromMainMenu() {
        clearRunSave();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("main menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);

            long touchCount = game.handledTouchUpCount();
            tapWorld(surface, 360f, 380f); // Settings
            await("settings touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            float[] correction = touchCorrection(game, 360f, 380f);
            await("settings opens", () -> game.screenState() == GameScreenState.SETTINGS);
            tapWorld(surface, 360f + correction[0], 775f + correction[1]); // Sound toggle
            SystemClock.sleep(700L);
            captureScreen("settings-premium-v2.png");
            tapWorld(surface, 360f + correction[0], 775f + correction[1]); // Restore sound
            tapWorld(surface, 620f + correction[0], 1_170f + correction[1]); // Close
            await("settings closes", () -> game.screenState() == GameScreenState.MENU);
        }
    }

    @Test
    public void touchSpendsTalentPointsFromPremiumLevelUp() {
        prepareLevelUpSave();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("level-up save menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);

            long touchCount = game.handledTouchUpCount();
            tapWorld(surface, 360f, 570f); // Continue
            await("continue touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            float[] correction = touchCorrection(game, 360f, 570f);
            await("level-up overlay", () -> game.screenState() == GameScreenState.LEVEL_UP);
            assertEquals(2, game.gameState().unspentTalentPoints);
            SystemClock.sleep(800L);
            captureScreen("level-up-premium-v2.png");

            tapWorld(surface, 360f + correction[0], 295f + correction[1]); // Strength row
            await("first point spent", () -> game.gameState().unspentTalentPoints == 1);
            assertEquals(GameScreenState.LEVEL_UP, game.screenState());
            tapWorld(surface, 360f + correction[0], 895f + correction[1]); // Health row
            await("second point resumes play", () -> game.screenState() == GameScreenState.PLAYING);
            assertEquals(1, game.gameState().hero.stats.strength);
            assertEquals(1, game.gameState().hero.stats.health);
        }
    }

    @Test
    public void touchRestartsFromPremiumVictorySummary() {
        prepareVictorySave();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("victory save menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);

            long touchCount = game.handledTouchUpCount();
            tapWorld(surface, 360f, 570f); // Continue the final reward choice
            await("continue touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            float[] correction = touchCorrection(game, 360f, 570f);
            await("final reward cards", () -> game.screenState() == GameScreenState.CARD_CHOICE);
            tapWorld(surface, 360f + correction[0], 890f + correction[1]); // Choose first card
            await("victory summary", () ->
                game.screenState() == GameScreenState.GAME_OVER && game.gameState().runComplete
            );
            SystemClock.sleep(800L);
            captureScreen("victory-premium-v2.png");

            tapWorld(surface, 360f + correction[0], 290f + correction[1]); // Defend again
            await("fresh run", () ->
                game.screenState() == GameScreenState.PLAYING && game.gameState().waveNumber == 1
            );
            assertFalse(game.gameState().runComplete);
        }
    }

    @Test
    public void touchRestartsFromPremiumDefeatSummary() {
        prepareDefeatSave();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("defeat save menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);

            long touchCount = game.handledTouchUpCount();
            tapWorld(surface, 360f, 570f); // Continue the doomed one-HP run
            await("continue touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            float[] correction = touchCorrection(game, 360f, 570f);
            await("doomed wave", () -> game.screenState() == GameScreenState.PLAYING);
            await("hero falls to the first melee hit", 20_000L, () ->
                game.screenState() == GameScreenState.GAME_OVER
            );
            assertFalse(game.gameState().runComplete);
            assertFalse(game.gameState().hero.alive);
            SystemClock.sleep(350L); // Leaves and the collapse ring are still airborne
            captureScreen("vfx-tree-collapse-premium-v2.png");
            SystemClock.sleep(1_450L); // Let the World Tree destruction reveal finish
            captureScreen("defeat-premium-v2.png");

            tapWorld(surface, 360f + correction[0], 290f + correction[1]); // Restart at Wave 1
            await("restarted run", () ->
                game.screenState() == GameScreenState.PLAYING
                    && game.gameState().hero.alive
                    && game.gameState().waveNumber == 1
            );
        }
    }

    @Test
    public void touchContinuesIntoBossEntranceAndCombatVfx() {
        prepareBossEntranceSave();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("boss save menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);

            long touchCount = game.handledTouchUpCount();
            tapWorld(surface, 360f, 570f); // Continue into the boss wave
            await("continue touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            await("boss wave", () ->
                game.screenState() == GameScreenState.PLAYING
                    && game.gameState().aliveBosses.stream().anyMatch(boss -> boss.alive)
            );
            SystemClock.sleep(420L); // Arrival shockwaves are mid-expansion
            captureScreen("vfx-boss-entrance-premium-v2.png");
            SystemClock.sleep(1_300L);
            for (int frame = 0; frame < 4; frame++) { // Burst so trails and impacts are caught in flight
                captureScreen("vfx-combat-" + frame + "-premium-v2.png");
                SystemClock.sleep(230L);
            }
            assertTrue(game.gameState().hero.alive);
        }
    }

    private static void prepareBossEntranceSave() {
        GameState state = GameState.newRun(886L);
        state.waveNumber = 5;
        state.heroLevel = 4;
        state.waveActive = false; // Continue starts the wave, so the entrance plays organically
        writeSave(state);
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

    /** Mirrors HudTouchLayout: how far each HUD row slides toward the physical edge. */
    private static float edgeShift(View surface) {
        float visibleWorldHeight = surface.getHeight() / (surface.getWidth() / WORLD_WIDTH);
        float clamped = Math.max(WORLD_HEIGHT, Math.min(1_720f, visibleWorldHeight));
        return Math.min(72f, (clamped - WORLD_HEIGHT) * 0.5f);
    }

    /** World y through the middle of the inventory/shop row on this panel. */
    private static float utilityRowY(View surface) {
        return 76f - edgeShift(surface);
    }

    /** World y through the middle of the speed/pause row on this panel. */
    private static float statusRowY(View surface) {
        return 1_115f + edgeShift(surface);
    }

    /** Mirrors the ExtendViewport: width pinned to 720, extra height split above and below. */
    private static float[] worldPoint(View surface, float worldX, float worldY) {
        float scale = surface.getWidth() / WORLD_WIDTH;
        float visibleWorldHeight = surface.getHeight() / scale;
        float bottomWorld = (WORLD_HEIGHT - visibleWorldHeight) * 0.5f;
        return new float[] {
            worldX * scale,
            (visibleWorldHeight - (worldY - bottomWorld)) * scale
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
        await(label, 5_000L, condition);
    }

    private static void await(String label, long timeoutMillis, BooleanSupplier condition) {
        long deadline = SystemClock.uptimeMillis() + timeoutMillis;
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            SystemClock.sleep(50L);
        }
        throw new AssertionError("Timed out waiting for " + label);
    }

    /** Mean luma of the captured frame must clear the premium-v3 floor; no more OLED-black UI. */
    private static final float MIN_MEAN_LUMA = 34f;

    private static void assertReadableBrightness(Bitmap screenshot, String name) {
        long total = 0L;
        int samples = 0;
        for (int y = 0; y < screenshot.getHeight(); y += 12) {
            for (int x = 0; x < screenshot.getWidth(); x += 12) {
                int pixel = screenshot.getPixel(x, y);
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                total += Math.round(0.2126f * r + 0.7152f * g + 0.0722f * b);
                samples++;
            }
        }
        float mean = samples == 0 ? 0f : (float) total / samples;
        assertTrue(name + " mean luma " + mean + " below " + MIN_MEAN_LUMA, mean >= MIN_MEAN_LUMA);
    }

    private static void captureScreen(String name) {
        Bitmap screenshot = InstrumentationRegistry.getInstrumentation()
            .getUiAutomation()
            .takeScreenshot();
        assertNotNull(screenshot);
        if (!name.startsWith("vfx-")) assertReadableBrightness(screenshot, name);
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
        state.waveNumber = 3;
        state.heroLevel = 3;
        state.unspentTalentPoints = 2;
        state.waveActive = true; // Mirrors a level-up earned on the wave's final kill
        writeSave(state);
    }

    private static void prepareVictorySave() {
        GameState state = GameState.newRun(884L);
        state.waveNumber = 100;
        state.heroLevel = 64;
        state.totalKills = 4_120;
        state.totalKillCoinsEarned = 38_500;
        state.defeatedBosses = 19;
        new BossRewardCardSystem().prepareChoices(state, 20);
        writeSave(state);
    }

    private static void prepareDefeatSave() {
        GameState state = GameState.newRun(885L);
        state.waveNumber = 37;
        state.heroLevel = 22;
        state.totalKills = 1_204;
        state.totalKillCoinsEarned = 9_310;
        state.defeatedBosses = 7;
        state.simulationSpeed = 3f;
        state.hero.health = 1f;
        writeSave(state);
    }

    private static void writeSave(GameState state) {
        String json = new GameStateCodec().encode(state);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getSharedPreferences(SAVE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString("run.primary", json)
            .commit());
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
