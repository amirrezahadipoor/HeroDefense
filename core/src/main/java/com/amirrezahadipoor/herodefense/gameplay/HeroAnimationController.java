package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.model.Hero;
import com.amirrezahadipoor.herodefense.model.HeroAnimationState;

/** Advances real combat-selected clips using the rendered 12 FPS frame contract. */
public final class HeroAnimationController {
    public static final float RENDERED_FRAMES_PER_SECOND = 12f;
    public static final int IDLE_FRAMES = 6;
    public static final int ATTACK_FRAMES = 8;
    public static final int HIT_FRAMES = 4;
    public static final int DEATH_FRAMES = 10;

    public void update(Hero hero, float deltaSeconds) {
        if (hero == null || deltaSeconds < 0f) {
            return;
        }
        if (!hero.alive && hero.animationState != HeroAnimationState.DEATH) {
            hero.beginDeathAnimation();
        }
        hero.animationStateSeconds += deltaSeconds;
        switch (hero.animationState) {
            case ATTACK -> {
                if (hero.animationStateSeconds >= attackDuration(hero)) {
                    hero.beginIdleAnimation();
                }
            }
            case HIT -> {
                if (hero.animationStateSeconds >= clipDuration(HIT_FRAMES)) {
                    hero.beginIdleAnimation();
                }
            }
            case DEATH -> hero.animationStateSeconds = Math.min(
                hero.animationStateSeconds, clipDuration(DEATH_FRAMES)
            );
            case IDLE -> {
                // The idle clock intentionally runs continuously and loops in frameIndex().
            }
        }
    }

    public int frameIndex(Hero hero) {
        return switch (hero.animationState) {
            case IDLE -> loopingFrame(hero.animationStateSeconds, IDLE_FRAMES, RENDERED_FRAMES_PER_SECOND);
            case ATTACK -> oneShotFrame(hero.animationStateSeconds, ATTACK_FRAMES, attackDuration(hero));
            case HIT -> oneShotFrame(hero.animationStateSeconds, HIT_FRAMES, clipDuration(HIT_FRAMES));
            case DEATH -> oneShotFrame(hero.animationStateSeconds, DEATH_FRAMES, clipDuration(DEATH_FRAMES));
        };
    }

    private static float attackDuration(Hero hero) {
        return clipDuration(ATTACK_FRAMES) / hero.stats.attacksPerSecond();
    }

    private static float clipDuration(int frameCount) {
        return frameCount / RENDERED_FRAMES_PER_SECOND;
    }

    private static int loopingFrame(float elapsed, int count, float framesPerSecond) {
        return Math.max(0, (int) (elapsed * framesPerSecond)) % count;
    }

    private static int oneShotFrame(float elapsed, int count, float duration) {
        float progress = duration <= 0f ? 1f : elapsed / duration;
        return Math.min(count - 1, Math.max(0, (int) (progress * count)));
    }
}
