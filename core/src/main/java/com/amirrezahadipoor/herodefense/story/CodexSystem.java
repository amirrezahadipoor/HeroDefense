package com.amirrezahadipoor.herodefense.story;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;

import java.util.ArrayList;
import java.util.List;

/** Unlocks Grove Codex entries from run events. Every method is idempotent and null-safe. */
public final class CodexSystem {
    public static final int MASTERY_LEVEL = 10;
    public static final float LONG_PAUSE_SECONDS = 300f;
    public static final float FASTEST_FALL_SECONDS = 10f;
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

    /** Progress secrets: 21 Bare-Handed, 26 No Potions, 29 Fastest Fall, 30 Wave 200 twice. */
    public List<String> unlockSecretsForProgress(GameState state) {
        List<String> unlocked = new ArrayList<>();
        if (state == null) return unlocked;
        if (state.waveNumber >= 50 && state.shopStatsBoughtThisRun == 0 && unlock(state, "codex_21")) {
            unlocked.add("codex_21");
        }
        if (state.waveNumber >= GameState.PLANTING_WAVE + 1 && state.noPotionRun
            && unlock(state, "codex_26")) {
            unlocked.add("codex_26");
        }
        if (state.fastestWaveClearSeconds <= FASTEST_FALL_SECONDS && unlock(state, "codex_29")) {
            unlocked.add("codex_29");
        }
        if (state.waveNumber >= GameState.FINAL_WAVE && state.wave200ReachedCount >= 2
            && unlock(state, "codex_30")) {
            unlocked.add("codex_30");
        }
        return unlocked;
    }

    /** Secret 23 Mastery: any skill first reaches level 10. */
    public List<String> unlockSecretsForSkillPurchase(GameState state) {
        List<String> unlocked = new ArrayList<>();
        if (state == null || state.skillLevels == null) return unlocked;
        for (Integer level : state.skillLevels.values()) {
            if (level != null && level >= MASTERY_LEVEL && unlock(state, "codex_23")) {
                unlocked.add("codex_23");
                break;
            }
        }
        return unlocked;
    }

    /** Secret 24 Reforged: any item first reaches +5, in inventory or equipped. */
    public List<String> unlockSecretsForForge(GameState state) {
        List<String> unlocked = new ArrayList<>();
        if (state == null) return unlocked;
        if (anyMaxForged(state) && unlock(state, "codex_24")) unlocked.add("codex_24");
        return unlocked;
    }

    /** Secret 27 The Long Pause: a 300+ second pause was resumed. */
    public List<String> unlockSecretsForPause(GameState state) {
        List<String> unlocked = new ArrayList<>();
        if (state == null) return unlocked;
        if (state.longestPauseSeconds >= LONG_PAUSE_SECONDS && unlock(state, "codex_27")) {
            unlocked.add("codex_27");
        }
        return unlocked;
    }

    private static boolean anyMaxForged(GameState state) {
        if (state.inventory != null) {
            for (Item item : state.inventory) {
                if (item != null && item.upgradeLevel >= GameState.MAX_ITEM_UPGRADE) return true;
            }
        }
        if (state.equippedItems != null) {
            for (Item item : state.equippedItems.values()) {
                if (item != null && item.upgradeLevel >= GameState.MAX_ITEM_UPGRADE) return true;
            }
        }
        return false;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException | NullPointerException bad) {
            return fallback;
        }
    }
}
