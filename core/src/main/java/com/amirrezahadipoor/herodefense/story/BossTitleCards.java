package com.amirrezahadipoor.herodefense.story;

import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.GameState;
import java.util.LinkedHashMap;
import java.util.List;

/** First-encounter boss title cards, verbatim (§2.1): one white line per identity, once ever. */
public final class BossTitleCards {
    private BossTitleCards() {
    }

    /** Verbatim title line for an identity, or null for unknown identities. */
    public static String titleFor(String bossType) {
        if (bossType == null) {
            return null;
        }
        return switch (bossType) {
            case "ANCIENT_GOLEM" -> "ANCIENT GOLEM — old guard who still stands.";
            case "THORN_MATRIARCH" -> "THORN MATRIARCH — she grew half your foes.";
            case "EMBER_WYRM" -> "EMBER WYRM — a fire that never went out.";
            case "VOID_KNIGHT" -> "VOID KNIGHT — he fell and forgot the rest.";
            default -> null;
        };
    }

    /**
     * Claims the first still-unencountered identity among {@code bosses}: records it in
     * {@code firstBossEncounters} and returns its title line, or null when every identity
     * present was already encountered (repeat appearances never reshow).
     */
    public static String claimFirstUnencountered(GameState state, List<Boss> bosses) {
        if (state == null || bosses == null) {
            return null;
        }
        if (state.firstBossEncounters == null) {
            state.firstBossEncounters = new LinkedHashMap<>();
        }
        for (Boss boss : bosses) {
            if (boss == null || boss.bossType == null) {
                continue;
            }
            String line = titleFor(boss.bossType);
            if (line == null) {
                continue;
            }
            if (Boolean.TRUE.equals(state.firstBossEncounters.get(boss.bossType))) {
                continue;
            }
            state.firstBossEncounters.put(boss.bossType, Boolean.TRUE);
            return line;
        }
        return null;
    }
}
