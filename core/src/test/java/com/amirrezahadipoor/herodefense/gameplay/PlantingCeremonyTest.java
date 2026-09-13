package com.amirrezahadipoor.herodefense.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class PlantingCeremonyTest {
    @Test
    void timelineWalksOutPlantsWatersGrowsAndReturnsToTheAnchor() {
        PlantingCeremony ceremony = new PlantingCeremony();
        assertEquals(PlantingCeremony.Phase.IDLE, ceremony.phase());
        ceremony.begin();

        assertEquals(PlantingCeremony.Phase.WALK_OUT, ceremony.phase());
        assertEquals(GameState.ARENA_CENTER_X, ceremony.heroX(), 0.001f);
        assertEquals("walk", ceremony.heroClip());
        assertFalse(ceremony.saplingVisible());

        advance(ceremony, PlantingCeremony.WALK_OUT_SECONDS + 0.01f);
        assertEquals(PlantingCeremony.Phase.PLANT, ceremony.phase());
        assertEquals(PlantingCeremony.STAND_X, ceremony.heroX(), 0.001f);
        assertEquals(PlantingCeremony.STAND_Y, ceremony.heroY(), 0.001f);
        assertEquals("plant", ceremony.heroClip());
        assertFalse(ceremony.saplingVisible());

        advance(ceremony, PlantingCeremony.PLANT_SECONDS * 0.6f);
        assertTrue(ceremony.saplingVisible());
        assertEquals(0, ceremony.saplingGrowFrame());

        advance(ceremony, PlantingCeremony.PLANT_SECONDS * 0.4f + 0.01f);
        assertEquals(PlantingCeremony.Phase.WATER, ceremony.phase());
        assertEquals("water", ceremony.heroClip());
        advance(ceremony, 5f / PlantingCeremony.GESTURE_FPS);
        assertTrue(ceremony.pouring());

        advance(ceremony, PlantingCeremony.WATER_SECONDS);
        assertEquals(PlantingCeremony.Phase.GROW, ceremony.phase());
        assertFalse(ceremony.pouring());
        advance(ceremony, PlantingCeremony.GROW_SECONDS - 0.02f);
        assertEquals(PlantingCeremony.GROW_FRAMES - 1, ceremony.saplingGrowFrame());

        advance(ceremony, 0.05f);
        assertEquals(PlantingCeremony.Phase.WALK_BACK, ceremony.phase());
        assertFalse(ceremony.heroFacesRight());
        assertEquals("walk", ceremony.heroClip());
        assertTrue(ceremony.saplingVisible());

        boolean finished = false;
        for (int step = 0; step < 100 && !finished; step++) finished = ceremony.update(0.05f);
        assertTrue(finished);
        assertFalse(ceremony.isActive());
        assertEquals(PlantingCeremony.Phase.DONE, ceremony.phase());
        assertEquals(GameState.ARENA_CENTER_X, ceremony.heroX(), 0.001f);
        assertEquals(GameState.ARENA_CENTER_Y, ceremony.heroY(), 0.001f);
        assertFalse(ceremony.update(0.05f));
    }

    @Test
    void skipCompletesOnTheNextUpdateAndIsDeterministic() {
        PlantingCeremony ceremony = new PlantingCeremony();
        ceremony.begin();
        ceremony.update(0.5f);
        ceremony.skip();
        assertTrue(ceremony.update(0f));
        assertEquals(PlantingCeremony.Phase.DONE, ceremony.phase());

        PlantingCeremony a = new PlantingCeremony();
        PlantingCeremony b = new PlantingCeremony();
        a.begin();
        b.begin();
        for (int step = 0; step < 40; step++) {
            a.update(0.0333f);
            b.update(0.0333f);
            assertEquals(a.heroX(), b.heroX());
            assertEquals(a.heroFrame(), b.heroFrame());
        }
    }

    @Test
    void oversizedFrameDeltasAreClampedSoTheCeremonyCannotBeSkippedByAHitch() {
        PlantingCeremony ceremony = new PlantingCeremony();
        ceremony.begin();
        assertFalse(ceremony.update(5f));
        assertTrue(ceremony.elapsedSeconds() <= 0.11f);
        assertFalse(ceremony.update(Float.NaN));
    }

    private static void advance(PlantingCeremony ceremony, float seconds) {
        float remaining = seconds;
        while (remaining > 0f) {
            float step = Math.min(0.05f, remaining);
            ceremony.update(step);
            remaining -= step;
        }
    }
}
