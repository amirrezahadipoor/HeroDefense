package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.input.InventoryTouchLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks explicit comparison, rarity, selection, and action presentation. */
final class PremiumInventoryPresentationTest {
    @Test
    void comparisonLabelsNeverDependOnColorAlone() {
        assertEquals("NEW +4", InventoryOverlayRenderer.comparisonLabel(4f, false));
        assertEquals("UP +4", InventoryOverlayRenderer.comparisonLabel(4f, true));
        assertEquals("DOWN -2", InventoryOverlayRenderer.comparisonLabel(-2f, true));
        assertEquals("SAME", InventoryOverlayRenderer.comparisonLabel(0f, true));
    }

    @Test
    void inventoryRetainsLargeTouchActionsAndFourReadableRows() {
        assertTrue(InventoryTouchLayout.ACTION_WIDTH >= 280f);
        assertTrue(InventoryTouchLayout.ACTION_HEIGHT >= 110f);
        assertTrue(InventoryTouchLayout.LIST_ROW_HEIGHT >= 90f);
        assertEquals(4, InventoryTouchLayout.VISIBLE_ROWS);
        assertTrue(InventoryTouchLayout.CLOSE_SIZE >= 100f);
    }

    @Test
    void rendererUsesGeneratedStatesAndExplicitPremiumSemantics() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/amirrezahadipoor/herodefense/render/InventoryOverlayRenderer.java"
        ));
        for (String required : new String[] {
            "EQUIPPED LOADOUT", "BACKPACK", "ITEM DETAILS", "STAT COMPARISON",
            "UP ", "DOWN ", "SAME", "REPLACE", "SELL  $ ",
            "rarityAccent", "controller.feedbackMessage()",
            "UiFrameRenderer.Kind.SLOT", "UiFrameRenderer.Kind.BUTTON"
        }) {
            assertTrue(source.contains(required), required);
        }
    }
}
