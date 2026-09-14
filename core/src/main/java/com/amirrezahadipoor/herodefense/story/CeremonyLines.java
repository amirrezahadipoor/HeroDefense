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
            case WALK_OUT -> "One root should not have to hold back all of this alone.";
            case PLANT -> "A second one, then. Grow angry, if you have to.";
            case WATER -> "I'll hold the line. That part's mine.";
            case GROW -> "The grove remembers what you have given it.";
            case WALK_BACK -> "Now — hold both.";
            default -> null;
        };
    }

    /** True only for the growth beat, which renders in the Tree's tint. */
    public static boolean isTreeVoice(PlantingCeremony.Phase phase) {
        return phase == PlantingCeremony.Phase.GROW;
    }
}
