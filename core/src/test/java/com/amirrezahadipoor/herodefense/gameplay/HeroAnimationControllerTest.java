package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amirrezahadipoor.herodefense.model.Hero;
import com.amirrezahadipoor.herodefense.model.HeroAnimationState;
import com.amirrezahadipoor.herodefense.model.IncomingHitResult;
import org.junit.jupiter.api.Test;

final class HeroAnimationControllerTest {
    private final HeroAnimationController controller = new HeroAnimationController();

    @Test
    void actualAttackEventSelectsAttackFramesThenReturnsToIdle() {
        Hero hero = new Hero(1L, 0f, 0f);
        hero.beginAttackAnimation();
        assertEquals(HeroAnimationState.ATTACK, hero.animationState);
        assertEquals(0, controller.frameIndex(hero));

        controller.update(hero, 0.34f);
        assertEquals(4, controller.frameIndex(hero));
        controller.update(hero, 0.34f);
        assertEquals(HeroAnimationState.IDLE, hero.animationState);
    }

    @Test
    void failedDodgeSelectsHitButSuccessfulDodgeDoesNot() {
        Hero hero = new Hero(1L, 0f, 0f);
        hero.stats.dodge = 10;
        assertEquals(IncomingHitResult.DODGED, hero.receiveIncomingHit(5f, 0.01f));
        assertEquals(HeroAnimationState.IDLE, hero.animationState);

        assertEquals(IncomingHitResult.DAMAGED, hero.receiveIncomingHit(5f, 0.9f));
        assertEquals(HeroAnimationState.HIT, hero.animationState);
    }

    @Test
    void deathClipStopsOnItsFinalFrame() {
        Hero hero = new Hero(1L, 0f, 0f);
        hero.receiveIncomingHit(1_000f, 0.9f);
        controller.update(hero, 10f);

        assertEquals(HeroAnimationState.DEATH, hero.animationState);
        assertEquals(HeroAnimationController.DEATH_FRAMES - 1, controller.frameIndex(hero));
    }
}
