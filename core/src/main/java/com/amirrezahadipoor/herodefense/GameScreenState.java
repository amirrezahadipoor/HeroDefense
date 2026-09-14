package com.amirrezahadipoor.herodefense;

/** Every top-level screen/simulation state in the Android game. */
public enum GameScreenState {
    MENU,
    SETTINGS,
    PLAYING,
    PAUSED,
    LEVEL_UP,
    CARD_CHOICE,
    /** Non-interactive cinematics (new-run opening, Wave 100 planting); combat frozen, a tap only skips ahead. */
    CINEMATIC,
    INVENTORY,
    SHOP,
    ROOT_NETWORK,
    CODEX,
    GAME_OVER
}
