package com.amirrezahadipoor.herodefense.save;

import com.badlogic.gdx.Preferences;
import com.amirrezahadipoor.herodefense.model.GameState;

import java.util.Optional;

/** Atomic-ish local Preferences save with one known-good fallback copy. */
public final class LocalSaveRepository {
    public static final String PREFERENCES_NAME = "hero-defense-local-save";
    private static final String PRIMARY_KEY = "run.primary";
    private static final String BACKUP_KEY = "run.backup";

    private final Preferences preferences;
    private final GameStateCodec codec;

    public LocalSaveRepository(Preferences preferences) {
        this(preferences, new GameStateCodec());
    }

    LocalSaveRepository(Preferences preferences, GameStateCodec codec) {
        if (preferences == null) {
            throw new IllegalArgumentException("preferences cannot be null");
        }
        this.preferences = preferences;
        this.codec = codec;
    }

    public void save(GameState state) {
        String nextJson = codec.encode(state);
        String previousJson = preferences.getString(PRIMARY_KEY, "");
        if (!previousJson.isBlank()) {
            preferences.putString(BACKUP_KEY, previousJson);
        }
        preferences.putString(PRIMARY_KEY, nextJson);
        preferences.flush();
    }

    public Optional<GameState> load() {
        Optional<GameState> primary = decodeSafely(preferences.getString(PRIMARY_KEY, ""));
        if (primary.isPresent()) {
            return primary;
        }
        return decodeSafely(preferences.getString(BACKUP_KEY, ""));
    }

    public boolean hasSave() {
        return load().isPresent();
    }

    public void clear() {
        preferences.remove(PRIMARY_KEY);
        preferences.remove(BACKUP_KEY);
        preferences.flush();
    }

    private Optional<GameState> decodeSafely(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(codec.decode(value));
        } catch (RuntimeException malformedSave) {
            return Optional.empty();
        }
    }
}
