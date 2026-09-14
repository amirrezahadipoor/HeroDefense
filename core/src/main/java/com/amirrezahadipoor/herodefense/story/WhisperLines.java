package com.amirrezahadipoor.herodefense.story;

import java.util.List;
import java.util.Map;

/**
 * Idle-whisper pool (§8): one-sentence Tree-voice lines, each shown at most once ever, when a
 * Resume follows a single real-time pause of at least 300 seconds. Order is fixed: the first
 * still-unused line is always next, and an exhausted pool stays silent.
 */
public final class WhisperLines {
    public static final List<String> LINES = List.of(
        "The roots kept your place while you were gone.",
        "Even the Tree dreams, little guardian, but it always wakes.",
        "I counted every breath of your absence in falling leaves.",
        "Rest is also a weapon, and you are learning to wield it.",
        "The dark between battles is where roots grow deepest.",
        "Welcome back — the grove never stopped watching the paths."
    );

    private WhisperLines() {
    }

    public static String idFor(int index) {
        return "whisper_" + (index + 1);
    }

    /** The first line whose id is absent from {@code usedIds}, or null once all are used. */
    public static String firstUnused(Map<String, Boolean> usedIds) {
        for (int index = 0; index < LINES.size(); index++) {
            if (usedIds == null || !Boolean.TRUE.equals(usedIds.get(idFor(index)))) {
                return LINES.get(index);
            }
        }
        return null;
    }

    /** Records the given pool line as used; unknown lines are ignored. */
    public static void markUsed(Map<String, Boolean> usedIds, String line) {
        if (usedIds == null || line == null) {
            return;
        }
        int index = LINES.indexOf(line);
        if (index >= 0) {
            usedIds.put(idFor(index), Boolean.TRUE);
        }
    }
}
