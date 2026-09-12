package com.amirrezahadipoor.herodefense.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
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
            HeroDefenseGame game = gameFrom(scenario);
            await("libGDX touch input", game::readyForTouch);
            await("main menu", () -> game.screenState() == GameScreenState.MENU);
            View surface = gameSurfaceFrom(scenario);

            long touchCount = game.handledTouchUpCount();
            tapWorld(surface, 360f, 760f); // New Game
            await("new-game touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            float[] correction = touchCorrection(game, 360f, 760f);
            await("wave starts", () -> game.screenState() == GameScreenState.PLAYING);
            assertEquals(1, game.gameState().waveNumber);
            assertTrue(game.gameState().waveActive);
            assertTrue(game.gameState().livingEnemyCount() > 0);

            tapWorld(
                surface,
                600f + correction[0],
                1115f + correction[1]
            ); // Pause HUD target, calibrated from the preceding real touch.
            await("paused", () -> game.screenState() == GameScreenState.PAUSED);

            tapWorld(surface, 360f + correction[0], 1_000f + correction[1]); // Stat Shop
            await("shop opens over pause", () -> game.screenState() == GameScreenState.SHOP);
            tapWorld(surface, 620f + correction[0], 1_170f + correction[1]); // Close Shop
            await("shop returns to pause", () -> game.screenState() == GameScreenState.PAUSED);
            tapWorld(surface, 360f + correction[0], 600f + correction[1]); // Resume
            await("resume after shop", () -> game.screenState() == GameScreenState.PLAYING);

            tapWorld(surface, 600f + correction[0], 1_115f + correction[1]); // Pause again
            await("paused again", () -> game.screenState() == GameScreenState.PAUSED);
            tapWorld(surface, 360f + correction[0], 830f + correction[1]); // Inventory
            await("inventory opens", game::inventoryOpen);

            swipeWorld(
                surface,
                360f + correction[0],
                580f + correction[1],
                360f + correction[0],
                730f + correction[1]
            ); // Drag-only inventory gesture
            assertTrue(game.inventoryOpen());
            tapWorld(surface, 620f + correction[0], 1160f + correction[1]); // Close
            await("inventory closes", () -> !game.inventoryOpen());
            tapWorld(surface, 360f + correction[0], 600f + correction[1]); // Resume
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
