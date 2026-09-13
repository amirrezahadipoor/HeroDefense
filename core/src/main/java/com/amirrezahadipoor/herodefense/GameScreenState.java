package com.amirrezahadipoor.herodefense;

/** Every top-level screen/simulation state in the Android game. */
public enum GameScreenState {
    MENU,
    SETTINGS,
    PLAYING,
    PAUSED,
    LEVEL_UP,
    CARD_CHOICE,
    /** Non-interactive Wave 100 planting ceremony; combat frozen, a tap only skips ahead. */
    CINEMATIC,
    INVENTORY,
    SHOP,
    GAME_OVER
}
