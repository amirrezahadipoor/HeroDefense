package com.amirrezahadipoor.herodefense.story;

import com.amirrezahadipoor.herodefense.gameplay.PlantingCeremony;

/**
 * Wave 100 planting-ceremony lines, verbatim (§2.3), synced one-to-one to the ceremony
 * phases: walk → plant → water → growth → return. The growth line speaks in the Tree's
 * leaf-green tint; the rest are the Hero's white.
 */
public final class CeremonyLines {
    private CeremonyLines() {
    }

    /** The spoken line for a ceremony phase, or null outside the five beats. */
    public static String lineFor(PlantingCeremony.Phase phase) {
        if (phase == null) {
            return null;
        }
        return switch (phase) {
            case WALK_OUT -> "One root should not hold this alone.";
            case PLANT -> "Then a second one. Grow angry if you must.";
            case WATER -> "I will hold the line. That is my job.";
            case GROW -> "The grove remembers your gift.";
            case WALK_BACK -> "Now hold both.";
            default -> null;
        };
    }

    /** True only for the growth beat, which renders in the Tree's tint. */
    public static boolean isTreeVoice(PlantingCeremony.Phase phase) {
        return phase == PlantingCeremony.Phase.GROW;
    }
}
