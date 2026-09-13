package com.amirrezahadipoor.herodefense.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.DropPickupSystem;
import com.amirrezahadipoor.herodefense.model.DropCollectionStage;
import com.amirrezahadipoor.herodefense.model.DropEntity;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.ItemTier;
import com.badlogic.gdx.Preferences;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class AutoSellSettingsTest {
    @Test
    void togglesCoverCommonUncommonRareOnlyAndPersist() {
        GameSettings settings = new GameSettings();
        for (ItemTier tier : ItemTier.values()) assertFalse(settings.autoSells(tier));
        assertTrue(settings.toggleAutoSell(ItemTier.COMMON));
        assertTrue(settings.toggleAutoSell(ItemTier.RARE));
        assertFalse(settings.toggleAutoSell(ItemTier.LEGENDARY));
        assertTrue(settings.autoSells(ItemTier.COMMON));
        assertFalse(settings.autoSells(ItemTier.UNCOMMON));
        assertTrue(settings.autoSells(ItemTier.RARE));
        assertFalse(settings.autoSells(ItemTier.LEGENDARY));

        MemoryPreferences preferences = new MemoryPreferences();
        LocalSettingsRepository repository = new LocalSettingsRepository(preferences);
        repository.save(settings);
        assertTrue(preferences.flushed);
        GameSettings loaded = repository.load();
        assertTrue(loaded.autoSellCommon);
        assertFalse(loaded.autoSellUncommon);
        assertTrue(loaded.autoSellRare);
        assertTrue(loaded.soundEnabled);
    }

    @Test
    void tickedTiersAreSoldOnEntryAndOthersReachTheBackpack() {
        GameState state = GameState.newRun(5L);
        GameSettings settings = new GameSettings();
        settings.autoSellCommon = true;
        state.drops.add(ripeDrop(state, "leather_cap"));      // COMMON
        state.drops.add(ripeDrop(state, "starfall_bow"));     // RARE
        DropPickupSystem pickup = new DropPickupSystem();
        int coinsBefore = state.coins;
        assertEquals(2, pickup.update(state, 1f, settings));
        assertEquals(1, state.inventory.size());
        assertEquals("starfall_bow", state.inventory.get(0).id);
        assertEquals(1, pickup.lastAutoSoldItems());
        assertEquals(12, pickup.lastAutoSoldCoins());
        assertEquals(coinsBefore + 12, state.coins);
        assertTrue(state.drops.isEmpty());

        // A later update with nothing sold reports zero, and null settings never sell.
        assertEquals(0, pickup.update(state, 1f, settings));
        assertEquals(0, pickup.lastAutoSoldItems());
        state.drops.add(ripeDrop(state, "leather_cap"));
        assertEquals(1, pickup.update(state, 1f, null));
        assertEquals(2, state.inventory.size());
    }

    private static DropEntity ripeDrop(GameState state, String itemId) {
        DropEntity drop = new DropEntity(state.allocateEntityId(), "ITEM", 360f, 640f, 1);
        drop.itemId = itemId;
        drop.pickupDelaySeconds = 0f;
        drop.collectionStage = DropCollectionStage.HOMING;
        drop.homingElapsedSeconds = DropPickupSystem.HOMING_DURATION_SECONDS;
        return drop;
    }

    /** Minimal in-memory Preferences so the repository round-trip runs without a device. */
    private static final class MemoryPreferences implements Preferences {
        private final Map<String, Object> values = new HashMap<>();
        boolean flushed;

        @Override public Preferences putBoolean(String key, boolean val) { values.put(key, val); return this; }
        @Override public Preferences putInteger(String key, int val) { values.put(key, val); return this; }
        @Override public Preferences putLong(String key, long val) { values.put(key, val); return this; }
        @Override public Preferences putFloat(String key, float val) { values.put(key, val); return this; }
        @Override public Preferences putString(String key, String val) { values.put(key, val); return this; }
        @Override public Preferences put(Map<String, ?> vals) { values.putAll(vals); return this; }
        @Override public boolean getBoolean(String key) { return getBoolean(key, false); }
        @Override public int getInteger(String key) { return getInteger(key, 0); }
        @Override public long getLong(String key) { return getLong(key, 0L); }
        @Override public float getFloat(String key) { return getFloat(key, 0f); }
        @Override public String getString(String key) { return getString(key, ""); }
        @Override public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key); return value instanceof Boolean b ? b : defValue;
        }
        @Override public int getInteger(String key, int defValue) {
            Object value = values.get(key); return value instanceof Integer i ? i : defValue;
        }
        @Override public long getLong(String key, long defValue) {
            Object value = values.get(key); return value instanceof Long l ? l : defValue;
        }
        @Override public float getFloat(String key, float defValue) {
            Object value = values.get(key); return value instanceof Float f ? f : defValue;
        }
        @Override public String getString(String key, String defValue) {
            Object value = values.get(key); return value instanceof String s ? s : defValue;
        }
        @Override public Map<String, ?> get() { return values; }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public void clear() { values.clear(); }
        @Override public void remove(String key) { values.remove(key); }
        @Override public void flush() { flushed = true; }
    }
}
