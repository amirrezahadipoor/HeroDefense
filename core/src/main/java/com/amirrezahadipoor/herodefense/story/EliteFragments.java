package com.amirrezahadipoor.herodefense.story;

/** Verbatim `docs/STORY_CONTENT.md` §4 "Whispering Wounds" fragments, one per Elite kill. */
public final class EliteFragments {
    private EliteFragments() {
    }

    /**
     * Fragment for an Elite kill: odd kill counts show I, even counts II, so the
     * two-part thread alternates deterministically per affix.
     */
    public static String fragmentFor(String affixId, int killCount) {
        boolean first = Math.max(1, killCount) % 2 == 1;
        if ("blightburst".equals(affixId)) {
            return first
                ? "It doesn't die so much as let go. Whatever was holding it together was never its own to keep."
                : "The burst isn't rage. It's relief.";
        }
        if ("rootward_ward".equals(affixId)) {
            return first
                ? "The shield isn't armor. It's a root, briefly remembering what it was for."
                : "Even corrupted, something in it still tries to protect something. It's just no longer sure what.";
        }
        if ("weeping_rot".equals(affixId)) {
            return first
                ? "The ground it crosses doesn't heal. Not yet. Maybe not ever."
                : "Every trail leads back the same direction, if you follow it far enough: toward the Tree.";
        }
        return null;
    }
}
