package com.amirrezahadipoor.herodefense.story;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Boss bios, verbatim from docs/STORY_CONTENT.md section 3. Each renders as the second paragraph
 * of its Codex entry 9–12 detail view, unlocked together with the entry.
 */
public final class BossLore {
    private static final Map<String, String> BIOS = bios();

    private BossLore() {
    }

    /** The bio for a boss identity, or null when unknown. */
    public static String bioFor(String bossType) {
        if (bossType == null) return null;
        return BIOS.get(bossType);
    }

    /** Detail text for an entry: Tree-voice body first, boss bio second when one applies. */
    public static String detailFor(LoreEntry entry) {
        if (entry == null) return "";
        if (entry.trigger() != LoreTrigger.BOSS_FIRST_KILL) return entry.body();
        String bio = bioFor(entry.triggerParam());
        if (bio == null) return entry.body();
        return entry.body() + "\n\n" + bio;
    }

    private static Map<String, String> bios() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("ANCIENT_GOLEM",
            "Before it was a weapon of the Hollow, it was the forest's oldest sentry — a stone shepherd that had not moved from its post in longer than the Tree could remember. The Hollow did not need to twist it. It only needed to convince it that the siege had never ended.");
        map.put("THORN_MATRIARCH",
            "She grew half the arena's Rootlings herself, back when growing things was all she did. What she plants now still takes root — it simply doesn't ask permission, and it isn't kind.");
        map.put("EMBER_WYRM",
            "When the Hollow first touched this ground, something here caught fire and never fully went out. The Wyrm is what that ember became once it learned to want more fuel.");
        map.put("VOID_KNIGHT",
            "No one here remembers what it looked like before. It doesn't either. It only remembers falling, and it has spent every year since trying to make something else fall with it.");
        return Collections.unmodifiableMap(map);
    }
}
