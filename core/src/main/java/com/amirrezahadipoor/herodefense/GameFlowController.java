package com.amirrezahadipoor.herodefense;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

/** Owns legal top-level state transitions without depending on rendering APIs. */
public final class GameFlowController {
    private static final Map<GameScreenState, EnumSet<GameScreenState>> ALLOWED = buildTransitions();

    private volatile GameScreenState state = GameScreenState.MENU;
    private GameScreenState returnState = GameScreenState.PLAYING;

    public GameScreenState state() {
        return state;
    }

    public GameScreenState returnState() {
        return returnState;
    }

    public boolean simulationRunning() {
        return state == GameScreenState.PLAYING;
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
        if (target == GameScreenState.PAUSED) {
            returnState = state == GameScreenState.MENU ? GameScreenState.MENU : GameScreenState.PLAYING;
        } else if (target == GameScreenState.SHOP || target == GameScreenState.INVENTORY
            || target == GameScreenState.ROOT_NETWORK || target == GameScreenState.CODEX) {
            returnState = state == GameScreenState.MENU ? GameScreenState.MENU : state;
        }
        state = target;
    }

    public void returnFromOverlay() {
        if (state != GameScreenState.PAUSED
            && state != GameScreenState.INVENTORY
            && state != GameScreenState.SHOP
            && state != GameScreenState.ROOT_NETWORK
            && state != GameScreenState.CODEX) {
            throw new IllegalStateException("Current state is not a resumable overlay: " + state);
        }
        state = returnState;
        if (state == GameScreenState.PAUSED) {
            returnState = GameScreenState.PLAYING;
        }
    }

    private static Map<GameScreenState, EnumSet<GameScreenState>> buildTransitions() {
        Map<GameScreenState, EnumSet<GameScreenState>> transitions = new EnumMap<>(GameScreenState.class);
        transitions.put(GameScreenState.MENU, EnumSet.of(
            GameScreenState.SETTINGS, GameScreenState.PLAYING, GameScreenState.SHOP,
            GameScreenState.ROOT_NETWORK, GameScreenState.CODEX, GameScreenState.TRIAL_DRAFT
        ));
        transitions.put(GameScreenState.SETTINGS, EnumSet.of(GameScreenState.MENU));
        transitions.put(GameScreenState.PLAYING, EnumSet.of(
            GameScreenState.PAUSED,
            GameScreenState.LEVEL_UP,
            GameScreenState.CARD_CHOICE,
            GameScreenState.CINEMATIC,
            GameScreenState.INVENTORY,
            GameScreenState.SHOP,
            GameScreenState.ROOT_NETWORK,
            GameScreenState.GAME_OVER,
            GameScreenState.MENU,
            // Continue routing only: a save closed mid-draft replays the draft.
            GameScreenState.TRIAL_DRAFT
        ));
        transitions.put(GameScreenState.PAUSED, EnumSet.of(
            GameScreenState.PLAYING,
            GameScreenState.MENU,
            GameScreenState.INVENTORY,
            GameScreenState.SHOP,
            GameScreenState.ROOT_NETWORK,
            GameScreenState.CODEX
        ));
        transitions.put(GameScreenState.LEVEL_UP, EnumSet.of(GameScreenState.PLAYING, GameScreenState.GAME_OVER));
        transitions.put(GameScreenState.CARD_CHOICE, EnumSet.of(
            GameScreenState.PLAYING, GameScreenState.CINEMATIC, GameScreenState.GAME_OVER
        ));
        transitions.put(GameScreenState.CINEMATIC, EnumSet.of(GameScreenState.PLAYING));
        transitions.put(GameScreenState.INVENTORY, EnumSet.of(
            GameScreenState.MENU, GameScreenState.PLAYING, GameScreenState.PAUSED, GameScreenState.ROOT_NETWORK
        ));
        transitions.put(GameScreenState.SHOP, EnumSet.of(
            GameScreenState.MENU, GameScreenState.PLAYING, GameScreenState.PAUSED, GameScreenState.ROOT_NETWORK
        ));
        transitions.put(GameScreenState.ROOT_NETWORK, EnumSet.of(
            GameScreenState.MENU, GameScreenState.PLAYING, GameScreenState.GAME_OVER, GameScreenState.PAUSED
        ));
        transitions.put(GameScreenState.CODEX, EnumSet.of(
            GameScreenState.MENU, GameScreenState.PLAYING, GameScreenState.PAUSED
        ));
        transitions.put(GameScreenState.GAME_OVER, EnumSet.of(
            GameScreenState.MENU, GameScreenState.PLAYING, GameScreenState.ROOT_NETWORK,
            GameScreenState.TRIAL_DRAFT
        ));
        transitions.put(GameScreenState.TRIAL_DRAFT, EnumSet.of(GameScreenState.CINEMATIC));
        return transitions;
    }
}
