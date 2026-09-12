package com.amirrezahadipoor.herodefense.audio;

/** License-ledger-backed short effects bundled with the Android assets. */
public enum AudioCue {
    HIT("audio/sfx/hit.ogg", 0.34f),
    DEATH("audio/sfx/death.ogg", 0.42f),
    ITEM_DROP("audio/sfx/item_drop.ogg", 0.45f),
    LEVEL_UP("audio/sfx/level_up.ogg", 0.42f),
    BOSS_ENTRANCE("audio/sfx/boss_entrance.ogg", 0.40f);

    private final String path;
    private final float volume;

    AudioCue(String path, float volume) {
        this.path = path;
        this.volume = volume;
    }

    public String path() {
        return path;
    }

    public float volume() {
        return volume;
    }
}
