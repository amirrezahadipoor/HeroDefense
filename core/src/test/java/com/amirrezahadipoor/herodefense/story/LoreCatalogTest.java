package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class LoreCatalogTest {
    @Test
    void definesExactlyThirtySequentiallyNumberedUniqueEntries() {
        assertEquals(30, LoreCatalog.all().size());
        Set<String> ids = new HashSet<>();
        int expected = 1;
        for (LoreEntry entry : LoreCatalog.all()) {
            assertEquals(expected, entry.number());
            assertEquals(String.format("codex_%02d", expected), entry.id());
            ids.add(entry.id());
            assertFalse(entry.title().isBlank());
            assertFalse(entry.body().isBlank());
            assertNotNull(entry.trigger());
            assertFalse(entry.triggerParam().isBlank());
            expected++;
        }
        assertEquals(30, ids.size());
        assertNotNull(LoreCatalog.byId("codex_01"));
        assertNotNull(LoreCatalog.byId("codex_30"));
    }

    @Test
    void triggerGroupsMatchTheRoadmapSplit() {
        Map<LoreTrigger, Integer> counts = new EnumMap<>(LoreTrigger.class);
        for (LoreEntry entry : LoreCatalog.all()) {
            counts.put(entry.trigger(), counts.getOrDefault(entry.trigger(), 0) + 1);
        }
        assertEquals(8, counts.get(LoreTrigger.WAVE_MILESTONE));
        assertEquals(4, counts.get(LoreTrigger.BOSS_FIRST_KILL));
        assertEquals(3, counts.get(LoreTrigger.ELITE_KILL));
        assertEquals(5, counts.get(LoreTrigger.ASCENSION));
        assertEquals(10, counts.get(LoreTrigger.SECRET));
    }

    @Test
    void waveBossEliteAndAscensionParamsAreExact() {
        assertEquals("1", LoreCatalog.byId("codex_01").triggerParam());
        assertEquals("100", LoreCatalog.byId("codex_08").triggerParam());
        assertEquals("ANCIENT_GOLEM", LoreCatalog.byId("codex_09").triggerParam());
        assertEquals("VOID_KNIGHT", LoreCatalog.byId("codex_12").triggerParam());
        assertEquals("blightburst", LoreCatalog.byId("codex_13").triggerParam());
        assertEquals("rootward_ward", LoreCatalog.byId("codex_14").triggerParam());
        assertEquals("weeping_rot", LoreCatalog.byId("codex_15").triggerParam());
        assertEquals("10", LoreCatalog.byId("codex_20").triggerParam());
    }

    @Test
    void bodiesMatchTheStoryDocumentVerbatim() {
        assertEquals(
            "Others stood here before you. I do not recall most of their names. I recall all of their last stands.",
            LoreCatalog.byId("codex_01").body());
        assertEquals(
            "This is the part no song is written about. Not the falling, not the standing. Just the holding. Hold anyway.",
            LoreCatalog.byId("codex_07").body());
        assertEquals(
            "Fire is supposed to go out. This one said no, and a no, given enough years, becomes a shape. The Wyrm is that no, wearing scales.",
            LoreCatalog.byId("codex_11").body());
        assertEquals(
            "The first time was survival. I suspect you already know what the second time was. Say it to yourself, if not to me.",
            LoreCatalog.byId("codex_30").body());
        for (LoreEntry entry : LoreCatalog.all()) {
            assertTrue(entry.body().length() >= 30, entry.id());
        }
    }
}
