package com.amirrezahadipoor.herodefense;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

/** Owns legal top-level state transitions without depending on rendering APIs. */
public final class GameFlowController {
    private static final Map<GameScreenState, EnumSet<GameScreenState>> ALLOWED = buildTransitions();

    private GameScreenState state = GameScreenState.MENU;
    private GameScreenState returnState = GameScreenState.PLAYING;

    public GameScreenState state() {
        return state;
    }

    public GameScreenState returnState() {
        return returnState;
    }

    public boolean canTransitionTo(GameScreenState target) {
        Objects.requireNonNull(target, "target");
        return target == state || ALLOWED.get(state).contains(target);
    }

    public void transitionTo(GameScreenState target) {
        Objects.requireNonNull(target, "target");
        if (!canTransitionTo(target)) {
            throw new IllegalStateException("Illegal game-state transition: " + state + " -> " + target);
        }
        if (target == GameScreenState.PAUSED || target == GameScreenState.SHOP) {
            returnState = state == GameScreenState.MENU ? GameScreenState.MENU : GameScreenState.PLAYING;
        }
        state = target;
    }

    public void returnFromOverlay() {
        if (state != GameScreenState.PAUSED && state != GameScreenState.SHOP) {
            throw new IllegalStateException("Current state is not a resumable overlay: " + state);
        }
        state = returnState;
    }

    private static Map<GameScreenState, EnumSet<GameScreenState>> buildTransitions() {
        Map<GameScreenState, EnumSet<GameScreenState>> transitions = new EnumMap<>(GameScreenState.class);
        transitions.put(GameScreenState.MENU, EnumSet.of(GameScreenState.PLAYING, GameScreenState.SHOP));
        transitions.put(GameScreenState.PLAYING, EnumSet.of(
            GameScreenState.PAUSED,
            GameScreenState.LEVEL_UP,
            GameScreenState.CARD_CHOICE,
            GameScreenState.SHOP,
            GameScreenState.GAME_OVER,
            GameScreenState.MENU
        ));
        transitions.put(GameScreenState.PAUSED, EnumSet.of(GameScreenState.PLAYING, GameScreenState.MENU));
        transitions.put(GameScreenState.LEVEL_UP, EnumSet.of(GameScreenState.PLAYING, GameScreenState.GAME_OVER));
        transitions.put(GameScreenState.CARD_CHOICE, EnumSet.of(GameScreenState.PLAYING, GameScreenState.GAME_OVER));
        transitions.put(GameScreenState.SHOP, EnumSet.of(GameScreenState.MENU, GameScreenState.PLAYING));
        transitions.put(GameScreenState.GAME_OVER, EnumSet.of(GameScreenState.MENU, GameScreenState.PLAYING));
        return transitions;
    }
}
