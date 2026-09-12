package com.amirrezahadipoor.herodefense.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.amirrezahadipoor.herodefense.GameScreenState;
import com.amirrezahadipoor.herodefense.HeroDefenseGame;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.save.GameStateCodec;

import org.junit.Test;
import org.junit.runner.RunWith;

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
            UiDevice device = device();
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("main menu", () -> game.screenState() == GameScreenState.MENU);

            tapWorld(device, 360f, 760f); // New Game
            await("wave starts", () -> game.screenState() == GameScreenState.PLAYING);
            assertEquals(1, game.gameState().waveNumber);
            assertTrue(game.gameState().waveActive);
            assertTrue(game.gameState().livingEnemyCount() > 0);

            tapWorld(device, 630f, 1115f); // Pause HUD target
            await("paused", () -> game.screenState() == GameScreenState.PAUSED);
            tapWorld(device, 360f, 830f); // Inventory
            await("inventory opens", game::inventoryOpen);

            swipeWorld(device, 360f, 580f, 360f, 730f); // Drag-only inventory gesture
            assertTrue(game.inventoryOpen());
            tapWorld(device, 620f, 1160f); // Inventory close
            await("inventory closes", () -> !game.inventoryOpen());
            tapWorld(device, 360f, 600f); // Resume
            await("play resumes", () -> game.screenState() == GameScreenState.PLAYING);

            assertEquals(
                "com.amirrezahadipoor.herodefense.debug",
                InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName()
            );
        }
    }

    @Test
    public void touchContinuesIntoAndSelectsExactlyOneRewardCard() {
        prepareRewardCardSave();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            UiDevice device = device();
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("saved run menu", () -> game.screenState() == GameScreenState.MENU);

            tapWorld(device, 360f, 570f); // Continue
            await("reward cards", () -> game.screenState() == GameScreenState.CARD_CHOICE);
            assertEquals(3, game.gameState().pendingRewardCards.size());

            tapWorld(device, 360f, 890f); // First card
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

    private static UiDevice device() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.waitForIdle();
        return device;
    }

    private static void tapWorld(UiDevice device, float worldX, float worldY) {
        int[] point = screenPoint(device, worldX, worldY);
        assertTrue(device.click(point[0], point[1]));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void swipeWorld(
        UiDevice device, float fromX, float fromY, float toX, float toY
    ) {
        int[] start = screenPoint(device, fromX, fromY);
        int[] end = screenPoint(device, toX, toY);
        assertTrue(device.swipe(start[0], start[1], end[0], end[1], 18));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static int[] screenPoint(UiDevice device, float worldX, float worldY) {
        int width = device.getDisplayWidth();
        int height = device.getDisplayHeight();
        float scale = Math.min(width / WORLD_WIDTH, height / WORLD_HEIGHT);
        float viewportWidth = WORLD_WIDTH * scale;
        float viewportHeight = WORLD_HEIGHT * scale;
        float left = (width - viewportWidth) * 0.5f;
        float top = (height - viewportHeight) * 0.5f;
        return new int[] {
            Math.round(left + worldX * scale),
            Math.round(top + (WORLD_HEIGHT - worldY) * scale)
        };
    }

    private static void await(String label, BooleanSupplier condition) {
        long deadline = SystemClock.uptimeMillis() + 5_000L;
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            SystemClock.sleep(50L);
        }
        throw new AssertionError("Timed out waiting for " + label);
    }

    private static void clearRunSave() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getSharedPreferences(SAVE_NAME, Context.MODE_PRIVATE).edit().clear().commit());
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
