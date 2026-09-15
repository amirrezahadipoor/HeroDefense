package com.amirrezahadipoor.herodefense.story;

/**
 * Between-boss reflection lines, verbatim (§2.2/§2.4): one quiet Hero-voice line at the start
 * of waves 25/50/75/125/150/175, shown as a brief skippable white-text story beat.
 */
public final class ReflectionLines {
    private ReflectionLines() {
    }

    /** The reflection line for a wave start, or null on waves without one. */
    public static String lineForWave(int waveNumber) {
        return switch (waveNumber) {
            case 25 -> "Wolves fear something deeper than me. That should scare me more.";
            case 50 -> "Half of what I killed, I once knew. I try not to think of it.";
            case 75 -> "Ground past the tree line feels wrong. Not ground at all.";
            case 125 -> "Two trees now. Twice to lose. Bad trade. I would still make it.";
            case 150 -> "It no longer sends weak first. It is done waiting.";
            case 175 -> "What is left may be the last. Or it wants me to think so.";
            default -> null;
        };
    }
}
