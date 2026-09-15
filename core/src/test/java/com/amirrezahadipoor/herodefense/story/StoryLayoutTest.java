package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.items.MythicEffects;
import org.junit.jupiter.api.Test;

/** Phase 31.3: verifies no rewritten line overflows its overlay at minimum density (60-char aim, hard cap 120). */
final class StoryLayoutTest {

    private static final int AIM = 60;
    private static final int HARD_CAP = 120;
    private static final int MAX_WORD = 22; // longest common word in our plain set is "Rootlings"(9) / "something" gone

    @Test
    void noStoryBodyOverflowsAtMinimumDensity() {
        // Codex
        for (LoreEntry e : LoreCatalog.all()) {
            checkBody(e.id(), e.body());
        }
        // Boss bios
        for (String id : new String[]{"ANCIENT_GOLEM","THORN_MATRIARCH","EMBER_WYRM","VOID_KNIGHT"}) {
            String bio = BossLore.bioFor(id);
            if (bio != null) checkBody("boss:" + id, bio);
        }
        // Elite fragments
        for (String affix : new String[]{"blightburst","rootward_ward","weeping_rot"}) {
            for (int k=1;k<=2;k++) {
                String f = EliteFragments.fragmentFor(affix, k);
                if (f != null) checkBody("elite:" + affix + ":" + k, f);
            }
        }
        // Mythic flavors
        for (String id : MythicEffects.ALL_IDS) {
            String f = MythicEffects.flavorLine(id);
            if (f != null) checkBody("mythic:" + id, f);
        }
    }

    private static void checkBody(String id, String body) {
        assertTrue(body != null && !body.isBlank(), id + " blank");
        // No single word should be absurdly long (would overflow even at 240dp)
        for (String w : body.split("\\s+")) {
            String clean = w.replaceAll("[^A-Za-z]", "");
            assertTrue(clean.length() <= MAX_WORD,
                id + " word too long (" + clean.length() + "): " + clean);
        }
        // Split into sentences (our overlay wraps at sentence boundaries but also word-wraps)
        String[] sents = body.split("(?<=[.!?])\\s+");
        for (String s : sents) {
            int len = s.trim().length();
            assertTrue(len <= HARD_CAP,
                id + " sentence too long (" + len + "): " + s);
            // Aim warning: we allow up to HARD_CAP but prefer AIM; test will still pass if a few are over AIM
            // The hard cap guarantees no horizontal overflow at 360dp / 14sp (overlay width ~300dp fits ~55 chars/line, so 120 wraps to 3 lines max)
        }
        // Whole body should not be excessively long for overlay (Codex detail view scrolls, but single overlay like fragments should be short)
        assertTrue(body.length() <= 300, id + " body too long: " + body.length());
    }
}
