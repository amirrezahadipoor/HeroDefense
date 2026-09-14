package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.EliteAffix;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Locks one distinct glowing outline per Elite affix; regulars never glow. */
final class ElitePresentationTest {
    @Test
    void everyAffixMapsToItsOwnGlowingOutline() {
        Set<VisualRarity> seen = new HashSet<>();
        for (EliteAffix affix : EliteAffix.values()) {
            VisualRarity rarity = CombatEntityRenderer.eliteGlow(affix.id());
            assertTrue(rarity.isGlowing(), affix.id());
            assertTrue(seen.add(rarity), affix.id());
        }
    }

    @Test
    void regularsAndUnknownAffixesNeverGlow() {
        assertEquals(VisualRarity.COMMON, CombatEntityRenderer.eliteGlow(null));
        assertEquals(VisualRarity.COMMON, CombatEntityRenderer.eliteGlow("unknown_affix"));
        assertTrue(!VisualRarity.COMMON.isGlowing());
    }
}
