package com.amirrezahadipoor.herodefense.story;

import com.amirrezahadipoor.herodefense.model.GameState;

import java.util.ArrayList;
import java.util.List;

/** Unlocks Grove Codex entries from run events. Every method is idempotent and null-safe. */
public final class CodexSystem {
    public boolean isUnlocked(GameState state, String id) {
        if (state == null || state.codexUnlocked == null || id == null) return false;
        return Boolean.TRUE.equals(state.codexUnlocked.get(id));
    }

    public int unlockedCount(GameState state) {
        if (state == null || state.codexUnlocked == null) return 0;
        int count = 0;
        for (LoreEntry entry : LoreCatalog.all()) {
            if (isUnlocked(state, entry.id())) count++;
        }
        return count;
    }

    /** Unlocks one entry by id; returns true only on the first unlock. */
    public boolean unlock(GameState state, String id) {
        if (state == null || id == null || LoreCatalog.byId(id) == null) return false;
        if (state.codexUnlocked == null) return false;
        if (Boolean.TRUE.equals(state.codexUnlocked.get(id))) return false;
        state.codexUnlocked.put(id, true);
        return true;
    }

    /** Wave milestones 1–8: every entry whose wave was reached. */
    public List<String> unlockForWaveReached(GameState state) {
        List<String> unlocked = new ArrayList<>();
        if (state == null) return unlocked;
        for (LoreEntry entry : LoreCatalog.all()) {
            if (entry.trigger() != LoreTrigger.WAVE_MILESTONE) continue;
            if (state.waveNumber >= parseInt(entry.triggerParam(), Integer.MAX_VALUE)
                && unlock(state, entry.id())) {
                unlocked.add(entry.id());
            }
        }
        return unlocked;
    }

    /** Boss-first-kill entries 9–12 for the defeated identity. */
    public List<String> unlockForBossKill(GameState state, String bossType) {
        List<String> unlocked = new ArrayList<>();
        if (state == null || bossType == null) return unlocked;
        for (LoreEntry entry : LoreCatalog.all()) {
            if (entry.trigger() == LoreTrigger.BOSS_FIRST_KILL
                && bossType.equals(entry.triggerParam())
                && unlock(state, entry.id())) {
                unlocked.add(entry.id());
            }
        }
        return unlocked;
    }

    /** Elite-kill entries 13–15 for the defeated affix (Elites arrive in Phase 25.2). */
    public List<String> unlockForEliteKill(GameState state, String affixId) {
        List<String> unlocked = new ArrayList<>();
        if (state == null || affixId == null) return unlocked;
        for (LoreEntry entry : LoreCatalog.all()) {
            if (entry.trigger() == LoreTrigger.ELITE_KILL
                && affixId.equals(entry.triggerParam())
                && unlock(state, entry.id())) {
                unlocked.add(entry.id());
            }
        }
        return unlocked;
    }

    /** Ascension entries 16–20: every entry whose completed count was reached. */
    public List<String> unlockForAscension(GameState state) {
        List<String> unlocked = new ArrayList<>();
        if (state == null) return unlocked;
        for (LoreEntry entry : LoreCatalog.all()) {
            if (entry.trigger() != LoreTrigger.ASCENSION) continue;
            if (state.ascensionTier >= parseInt(entry.triggerParam(), Integer.MAX_VALUE)
                && unlock(state, entry.id())) {
                unlocked.add(entry.id());
            }
        }
        return unlocked;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException | NullPointerException bad) {
            return fallback;
        }
    }
}
