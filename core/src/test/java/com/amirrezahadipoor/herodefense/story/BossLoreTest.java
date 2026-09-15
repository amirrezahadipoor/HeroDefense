package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BossLoreTest {
    @Test
    void everyBossIdentityHasAVerbatimBio() {
        assertEquals(
            "Before it was a weapon of the Hollow, it was the forest's oldest guard. A stone keeper that had not moved from its post in longer than the Tree could recall. The Hollow did not need to turn it. It only needed to make it think the fight had never ended.",
            BossLore.bioFor("ANCIENT_GOLEM"));
        assertTrue(BossLore.bioFor("THORN_MATRIARCH").startsWith("She grew half the arena's Rootlings"));
        assertTrue(BossLore.bioFor("EMBER_WYRM").contains("never fully went out"));
        assertTrue(BossLore.bioFor("VOID_KNIGHT").endsWith("fall with it."));
        assertNull(BossLore.bioFor("NOPE"));
        assertNull(BossLore.bioFor(null));
    }

    @Test
    void bossEntriesComposeTreeVoiceFirstAndBioSecond() {
        for (String id : new String[] {"codex_09", "codex_10", "codex_11", "codex_12"}) {
            LoreEntry entry = LoreCatalog.byId(id);
            String detail = BossLore.detailFor(entry);
            assertTrue(detail.startsWith(entry.body()), id);
            assertTrue(detail.contains(BossLore.bioFor(entry.triggerParam())), id);
            assertTrue(detail.indexOf(entry.body()) < detail.indexOf(BossLore.bioFor(entry.triggerParam())), id);
        }
    }

    @Test
    void otherEntriesRenderTheirBodyAlone() {
        for (LoreEntry entry : LoreCatalog.all()) {
            if (entry.trigger() == LoreTrigger.BOSS_FIRST_KILL) continue;
            assertEquals(entry.body(), BossLore.detailFor(entry));
            assertFalse(BossLore.detailFor(entry).contains("\n\n"));
        }
        assertEquals("", BossLore.detailFor(null));
    }
}
