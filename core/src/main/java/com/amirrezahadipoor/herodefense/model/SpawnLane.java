package com.amirrezahadipoor.herodefense.model;

/** The three arena edges used by regular-enemy waves. */
public enum SpawnLane {
    LEFT(0),
    RIGHT(1),
    SOUTH(2);

    private final int id;

    SpawnLane(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static SpawnLane fromIndex(int index) {
        SpawnLane[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
