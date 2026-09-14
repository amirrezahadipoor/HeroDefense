package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.BossFactory;
import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.BossType;
import com.amirrezahadipoor.herodefense.model.GameState;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BossTitleCardsTest {
    @Test
    void titlesMatchStoryContentVerbatim() {
        assertEquals(
            "ANCIENT GOLEM — the forest's oldest guardian, still standing because it has forgotten how to stop.",
            BossTitleCards.titleFor("ANCIENT_GOLEM")
        );
        assertEquals(
            "THORN MATRIARCH — she planted half of what now attacks you.",
            BossTitleCards.titleFor("THORN_MATRIARCH")
        );
        assertEquals(
            "EMBER WYRM — the Hollow's arrival left an ember. It never learned to go out.",
            BossTitleCards.titleFor("EMBER_WYRM")
        );
        assertEquals(
            "VOID KNIGHT — it does not remember what it was. Only that it fell.",
            BossTitleCards.titleFor("VOID_KNIGHT")
        );
        assertNull(BossTitleCards.titleFor("MUD_IMP"));
        assertNull(BossTitleCards.titleFor(null));
    }

    @Test
    void eachIdentityShowsOnceThenRepeatsStaySilent() {
        GameState state = GameState.newRun(18L);
        BossFactory factory = new BossFactory();
        Boss golem = factory.create(state, BossType.ANCIENT_GOLEM, 0f, 0f, 1, 0);
        Boss wyrm = factory.create(state, BossType.EMBER_WYRM, 0f, 0f, 3, 0);

        String first = BossTitleCards.claimFirstUnencountered(state, List.of(golem, wyrm));
        assertEquals(BossTitleCards.titleFor("ANCIENT_GOLEM"), first);
        assertTrue(Boolean.TRUE.equals(state.firstBossEncounters.get("ANCIENT_GOLEM")));

        String second = BossTitleCards.claimFirstUnencountered(state, List.of(golem, wyrm));
        assertEquals(BossTitleCards.titleFor("EMBER_WYRM"), second);

        assertNull(BossTitleCards.claimFirstUnencountered(state, List.of(golem, wyrm)));
        assertNull(BossTitleCards.claimFirstUnencountered(state, null));
        assertNull(BossTitleCards.claimFirstUnencountered(null, List.of(golem)));
    }
}
