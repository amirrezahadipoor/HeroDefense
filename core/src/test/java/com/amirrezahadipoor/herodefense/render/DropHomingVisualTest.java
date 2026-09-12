package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.DropPickupSystem;
import com.amirrezahadipoor.herodefense.model.DropCollectionStage;
import com.amirrezahadipoor.herodefense.model.DropEntity;
import org.junit.jupiter.api.Test;

final class DropHomingVisualTest {
    @Test
    void groundDropArcsTowardTheLiveHudInventoryTarget() {
        DropEntity drop = new DropEntity(0L, "ITEM", 100f, 200f, 1);
        assertEquals(0f, CombatEntityRenderer.dropHomingProgress(drop));
        assertEquals(100f, CombatEntityRenderer.dropDrawX(drop));
        assertEquals(228f, CombatEntityRenderer.dropDrawY(drop, 0f));

        drop.collectionStage = DropCollectionStage.HOMING;
        drop.homingElapsedSeconds = DropPickupSystem.HOMING_DURATION_SECONDS * 0.5f;
        assertEquals(0.5f, CombatEntityRenderer.dropHomingProgress(drop), 0.0001f);
        float halfwayX = CombatEntityRenderer.dropDrawX(drop);
        float halfwayY = CombatEntityRenderer.dropDrawY(drop, 0f);
        assertTrue(halfwayX > drop.x && halfwayX < CombatEntityRenderer.DROP_TARGET_X);
        assertTrue(halfwayY > 228f, "The collection path must have a visible upward arc");

        drop.homingElapsedSeconds = DropPickupSystem.HOMING_DURATION_SECONDS;
        assertEquals(CombatEntityRenderer.DROP_TARGET_X, CombatEntityRenderer.dropDrawX(drop));
        assertEquals(
            CombatEntityRenderer.DROP_TARGET_Y,
            CombatEntityRenderer.dropDrawY(drop, 0f),
            0.001f
        );
    }
}
