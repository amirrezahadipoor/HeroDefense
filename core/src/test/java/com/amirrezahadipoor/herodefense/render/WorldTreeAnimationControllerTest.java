package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class WorldTreeAnimationControllerTest {
    @Test
    void livingTreeLoopsAndCrossesTheDamageThresholdWithoutStartingDestruction() {
        WorldTreeAnimationController controller = new WorldTreeAnimationController();
        GameState state = GameState.newRun(41L);

        WorldTreeAnimationController.Selection healthy = controller.select(state, 0.26f, 0.1f);
        assertEquals(WorldTreeAnimationController.VisualState.HEALTHY, healthy.state());
        assertEquals(3, healthy.frameIndex());
        assertFalse(WorldTreeAnimationController.isDamaged(state));

        state.worldTreeHealth = state.worldTreeMaxHealth * 0.40f;
        WorldTreeAnimationController.Selection damaged = controller.select(state, 0.50f, 0.1f);
        assertEquals(WorldTreeAnimationController.VisualState.DAMAGED, damaged.state());
        assertEquals(0, damaged.frameIndex());
        assertTrue(WorldTreeAnimationController.isDamaged(state));
    }

    @Test
    void destructionUsesPresentationTimeAndHoldsItsFinalFrame() {
        WorldTreeAnimationController controller = new WorldTreeAnimationController();
        GameState state = GameState.newRun(42L);
        state.destroyWorldTree();

        WorldTreeAnimationController.Selection first = controller.select(state, 90f, 0.1f);
        assertEquals(WorldTreeAnimationController.VisualState.DESTROYING, first.state());
        assertEquals(0, first.frameIndex());

        WorldTreeAnimationController.Selection second = controller.select(state, 90f, 0.09f);
        assertEquals(1, second.frameIndex());

        WorldTreeAnimationController.Selection current = second;
        for (int index = 0; index < 20; index++) {
            current = controller.select(state, 90f, 0.1f);
        }
        assertEquals(WorldTreeAnimationController.VisualState.DESTROYED, current.state());
        assertEquals(9, current.frameIndex());
        assertEquals(9, controller.select(state, 90f, 8f).frameIndex());
    }

    @Test
    void aNewLivingRunResetsTheOneShotBeforeAnotherDestruction() {
        WorldTreeAnimationController controller = new WorldTreeAnimationController();
        GameState fallen = GameState.newRun(43L);
        fallen.hero.alive = false;
        controller.select(fallen, 0f, 0.1f);
        controller.select(fallen, 0f, 0.1f);

        GameState living = GameState.newRun(44L);
        assertEquals(
            WorldTreeAnimationController.VisualState.HEALTHY,
            controller.select(living, 0f, 0.1f).state()
        );

        living.destroyWorldTree();
        WorldTreeAnimationController.Selection restarted = controller.select(living, 0f, 0.1f);
        assertEquals(WorldTreeAnimationController.VisualState.DESTROYING, restarted.state());
        assertEquals(0, restarted.frameIndex());
    }

    @Test
    void invalidPresentationTimesRemainSafe() {
        WorldTreeAnimationController controller = new WorldTreeAnimationController();
        GameState state = GameState.newRun(45L);
        assertEquals(0, controller.select(state, Float.NaN, Float.NaN).frameIndex());
        state.hero = null;
        assertEquals(0, controller.select(state, 0f, Float.POSITIVE_INFINITY).frameIndex());
        assertTrue(WorldTreeAnimationController.isDestroyed(state));
    }
}
