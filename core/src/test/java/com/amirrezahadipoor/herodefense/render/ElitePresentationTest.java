package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.EliteAffix;
import com.amirrezahadipoor.herodefense.model.Enemy;
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
    void raisedShieldsBurnBrighterWhileTheyHold() {
        Enemy shielded = new Enemy(1L, "ROOTLING", 0f, 0f);
        shielded.affixShieldRemainingSeconds = 1f;
        assertEquals(1.8f, CombatEntityRenderer.eliteGlowIntensity(shielded), 1e-6f);
        assertEquals(1f, CombatEntityRenderer.eliteGlowIntensity(new Enemy()), 1e-6f);
        assertEquals(1f, CombatEntityRenderer.eliteGlowIntensity(null), 1e-6f);
    }

    @Test
    void rotPatchesFadeAsTheyAge() {
        assertEquals(0.7f, CombatEntityRenderer.rotSegmentAlpha(1f), 1e-6f);
        assertEquals(0.475f, CombatEntityRenderer.rotSegmentAlpha(0.5f), 1e-6f);
        assertEquals(0.25f, CombatEntityRenderer.rotSegmentAlpha(0f), 1e-6f);
        assertEquals(0.7f, CombatEntityRenderer.rotSegmentAlpha(2f), 1e-6f);
        assertEquals(0.25f, CombatEntityRenderer.rotSegmentAlpha(-1f), 1e-6f);
    }

    @Test
    void regularsAndUnknownAffixesNeverGlow() {
        assertEquals(VisualRarity.COMMON, CombatEntityRenderer.eliteGlow(null));
        assertEquals(VisualRarity.COMMON, CombatEntityRenderer.eliteGlow("unknown_affix"));
        assertTrue(!VisualRarity.COMMON.isGlowing());
    }
}
