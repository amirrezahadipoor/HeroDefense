package com.amirrezahadipoor.herodefense.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.espresso.action.Tap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

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

            long touchCount = game.handledTouchUpCount();
            tapWorld(360f, 760f); // New Game
            await("new-game touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            float[] correction = touchCorrection(game, 360f, 760f);
            await("wave starts", () -> game.screenState() == GameScreenState.PLAYING);
            assertEquals(1, game.gameState().waveNumber);
            assertTrue(game.gameState().waveActive);
            assertTrue(game.gameState().livingEnemyCount() > 0);

            tapWorld(
                600f + correction[0],
                1115f + correction[1]
            ); // Pause HUD target, calibrated from the preceding real touch.
            await("paused", () -> game.screenState() == GameScreenState.PAUSED);
            tapWorld(360f + correction[0], 830f + correction[1]); // Inventory
            await("inventory opens", game::inventoryOpen);

            swipeWorld(
                360f + correction[0],
                580f + correction[1],
                360f + correction[0],
                730f + correction[1]
            ); // Drag-only inventory gesture
            assertTrue(game.inventoryOpen());
            tapWorld(620f + correction[0], 1160f + correction[1]); // Close
            await("inventory closes", () -> !game.inventoryOpen());
            tapWorld(360f + correction[0], 600f + correction[1]); // Resume
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

            long touchCount = game.handledTouchUpCount();
            tapWorld(360f, 570f); // Continue
            await("continue touch dispatch", () -> game.handledTouchUpCount() > touchCount);
            float[] correction = touchCorrection(game, 360f, 570f);
            await("reward cards", () -> game.screenState() == GameScreenState.CARD_CHOICE);
            assertEquals(3, game.gameState().pendingRewardCards.size());

            tapWorld(360f + correction[0], 890f + correction[1]); // First card
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

    private static void tapWorld(float worldX, float worldY) {
        onView(isAssignableFrom(GLSurfaceView.class)).perform(
            new GeneralClickAction(Tap.SINGLE, worldPoint(worldX, worldY), Press.FINGER)
        );
    }

    private static void swipeWorld(float fromX, float fromY, float toX, float toY) {
        onView(isAssignableFrom(GLSurfaceView.class)).perform(
            new GeneralSwipeAction(
                Swipe.FAST,
                worldPoint(fromX, fromY),
                worldPoint(toX, toY),
                Press.FINGER
            )
        );
    }

    private static CoordinatesProvider worldPoint(float worldX, float worldY) {
        return view -> {
            float scale = Math.min(
                view.getWidth() / WORLD_WIDTH,
                view.getHeight() / WORLD_HEIGHT
            );
            float viewportWidth = WORLD_WIDTH * scale;
            float viewportHeight = WORLD_HEIGHT * scale;
            float left = (view.getWidth() - viewportWidth) * 0.5f;
            float top = (view.getHeight() - viewportHeight) * 0.5f;
            return new float[] {
                left + worldX * scale,
                top + (WORLD_HEIGHT - worldY) * scale
            };
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
