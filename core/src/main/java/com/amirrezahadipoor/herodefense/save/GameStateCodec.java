package com.amirrezahadipoor.herodefense.save;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Stable JSON boundary for local run saves and test fixtures. */
public final class GameStateCodec {
    private final Json json;

    public GameStateCodec() {
        json = new Json(JsonWriter.OutputType.json);
        json.setIgnoreUnknownFields(true);
        json.setUsePrototypes(false);
        json.setTypeName("_type");
    }

    public String encode(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return json.prettyPrint(state);
    }

    public GameState decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException("save JSON cannot be blank");
        }
        GameState state = json.fromJson(GameState.class, encoded);
        if (state == null) {
            throw new IllegalArgumentException("save JSON did not contain GameState");
        }
        state.validateAndRepair();
        return state;
    }
}
