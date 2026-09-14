package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.WorldLayout;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.SpawnLane;

/** Deterministically distributes each regular wave over three arena edges. */
public final class EnemyWaveSpawner {
    public static final float EDGE_OFFSET = 40f;
    public static final int MAX_REGULAR_ENEMIES = 24;
    private static final float SIDE_JITTER = 180f;
    private static final float SOUTH_JITTER = 250f;
    /** One in fifty Rootling spawns stands silent at the tree line ("The Quiet Ones"). */
    static final int SILENT_WATCHER_ONE_IN = 50;
    /** Tree-line box where Silent Rootling watchers stand and never leave. */
    static final float TREE_LINE_MIN_X = 90f;
    static final float TREE_LINE_MAX_X = 630f;
    static final float TREE_LINE_MIN_Y = 800f;
    static final float TREE_LINE_MAX_Y = 860f;

    private final EnemyFactory factory;

    public EnemyWaveSpawner(EnemyFactory factory) {
        this.factory = factory;
    }

    public int regularCountForWave(int waveNumber) {
        return Math.min(
            MAX_REGULAR_ENEMIES,
            Math.max(3, 4 + Math.max(1, waveNumber) / 2)
        );
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
            Enemy enemy = factory.createForWave(
                state, type, x, y, lane.id(), waveNumber
            );
            if (type == EnemyType.ROOTLING && isSilentWatcher(state.runSeed, waveNumber, index)) {
                enemy.silentWatcher = true;
                enemy.x = TREE_LINE_MIN_X + watcherUnit(state.runSeed, waveNumber, index, 1L)
                    * (TREE_LINE_MAX_X - TREE_LINE_MIN_X);
                enemy.y = TREE_LINE_MIN_Y + watcherUnit(state.runSeed, waveNumber, index, 2L)
                    * (TREE_LINE_MAX_Y - TREE_LINE_MIN_Y);
            }
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

    private static boolean isSilentWatcher(long seed, int wave, int index) {
        return Math.floorMod(
            watcherMix(seed, wave, index, 0xC2B280737A5763D5L), SILENT_WATCHER_ONE_IN
        ) == 0;
    }

    private static float watcherUnit(long seed, int wave, int index, long salt) {
        return (watcherMix(seed, wave, index, 0x165667B19E3779F9L + salt) >>> 40) / 16_777_216f;
    }

    private static long watcherMix(long seed, int wave, int index, long salt) {
        long value = seed + salt * (wave * 131L + index * 17L + 1L);
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }
}
