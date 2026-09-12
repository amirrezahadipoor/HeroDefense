package com.amirrezahadipoor.herodefense.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class StarterLoadoutSystemTest {
    @Test
    void provisionsExactlyOneVisibleStarterBowForANewRun() {
        GameState state = GameState.newRun(80L);
        StarterLoadoutSystem starter = new StarterLoadoutSystem();

        assertTrue(starter.provisionOnce(state));
        assertEquals("ashwood_bow", state.equippedItems.get(EquipmentSlot.WEAPON.name()).id);
        assertFalse(starter.provisionOnce(state));
        assertEquals(1, state.equippedItems.size());
    }
}
