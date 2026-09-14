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
            case 25 -> "The wolves run from something deeper than me. That should worry me more than it does.";
            case 50 -> "Half of what I've killed today, I might have known once. I try not to think about that.";
            case 75 -> "The ground past the tree line doesn't feel like ground anymore.";
            case 125 -> "Two trees now. Twice as much to lose. I don't recommend the trade, but I'd make it again.";
            case 150 -> "It doesn't send its strongest first anymore. It's stopped being patient.";
            case 175 -> "Whatever's left out there, it's the last of it. Or it wants me to think that.";
            default -> null;
        };
    }
}
