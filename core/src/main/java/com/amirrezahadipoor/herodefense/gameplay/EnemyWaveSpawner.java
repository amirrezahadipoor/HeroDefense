package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.WorldLayout;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.SpawnLane;

/** Deterministically distributes each regular wave over three arena edges. */
public final class EnemyWaveSpawner {
    public static final float EDGE_OFFSET = 40f;
    private static final float SIDE_JITTER = 180f;
    private static final float SOUTH_JITTER = 250f;

    private final EnemyFactory factory;

    public EnemyWaveSpawner(EnemyFactory factory) {
        this.factory = factory;
    }

    public int regularCountForWave(int waveNumber) {
        return Math.max(3, 4 + Math.max(1, waveNumber) / 2);
    }

    public void spawnRegularEnemies(GameState state, int waveNumber, int count) {
        if (state == null || count <= 0) {
            return;
        }
        EnemyType[] types = EnemyType.values();
        for (int index = 0; index < count; index++) {
            SpawnLane lane = SpawnLane.fromIndex(index);
            float jitter = signedUnit(state.runSeed, waveNumber, index);
            float x;
            float y;
            switch (lane) {
                case LEFT -> {
                    x = -EDGE_OFFSET;
                    y = WorldLayout.HERO_CENTER_Y + jitter * SIDE_JITTER;
                }
                case RIGHT -> {
                    x = WorldLayout.REFERENCE_WIDTH + EDGE_OFFSET;
                    y = WorldLayout.HERO_CENTER_Y + jitter * SIDE_JITTER;
                }
                case SOUTH -> {
                    x = WorldLayout.HERO_CENTER_X + jitter * SOUTH_JITTER;
                    y = -EDGE_OFFSET;
                }
                default -> throw new IllegalStateException("Unhandled spawn lane: " + lane);
            }
            EnemyType type = types[Math.floorMod(waveNumber - 1 + index, types.length)];
            Enemy enemy = factory.create(state, type, x, y, lane.id());
            state.aliveEnemies.add(enemy);
        }
    }

    private static float signedUnit(long seed, int wave, int index) {
        long value = seed + 0x9E3779B97F4A7C15L * (wave * 31L + index + 1L);
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return ((value >>> 40) / 8_388_607.5f) - 1f;
    }
}
