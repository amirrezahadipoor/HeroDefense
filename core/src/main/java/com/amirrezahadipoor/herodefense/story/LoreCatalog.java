package com.amirrezahadipoor.herodefense.story;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 30 Grove Codex entries, verbatim from docs/STORY_CONTENT.md section 5, in the Tree's voice.
 * Trigger params: wave number, boss identity, Elite affix id, completed Ascension count, secret id.
 */
public final class LoreCatalog {
    private static final List<LoreEntry> ALL = Collections.unmodifiableList(Arrays.asList(
        entry(1, "Before You.",
            "Others stood here before you. I do not recall most of their names. I recall all of their last stands.",
            LoreTrigger.WAVE_MILESTONE, "1"),
        entry(2, "The Three Directions.",
            "They do not come from everywhere. Only from three. I have never learned what holds the fourth.",
            LoreTrigger.WAVE_MILESTONE, "10"),
        entry(3, "Counting.",
            "I used to count the seasons. Now I count waves. It is a smaller unit of time, and it passes no more kindly.",
            LoreTrigger.WAVE_MILESTONE, "20"),
        entry(4, "What Luck Finds.",
            "Some of what falls from them still recalls being useful. Wear it. I would rather you have it than the ground.",
            LoreTrigger.WAVE_MILESTONE, "30"),
        entry(5, "The Quiet Ones.",
            "Not every Rootling attacks. Some simply stand at the tree line and watch. I do not know if that is worse.",
            LoreTrigger.WAVE_MILESTONE, "40"),
        entry(6, "Old Names.",
            "Rootling. Stonekin. Gloom Wolf. Fungal Brute. I gave three of those names myself, once, to living things, meaning a kinder thing.",
            LoreTrigger.WAVE_MILESTONE, "60"),
        entry(7, "The Long Middle.",
            "This is the part no song is written about. Not the falling, not the standing. Just the holding. Hold anyway.",
            LoreTrigger.WAVE_MILESTONE, "80"),
        entry(8, "A Grove Takes Root.",
            "I did not ask for a second trunk, nor a third. I am glad of both regardless. Grief is lighter, split three ways. And so, it turns out, is standing guard.",
            LoreTrigger.WAVE_MILESTONE, "100"),
        entry(9, "What the Golem Guarded.",
            "Before there was a Tree to defend, there was a boundary stone. The Golem was its keeper. I think, somewhere under the Hollow's grip, it still believes that is all this is: a boundary, and a keeper.",
            LoreTrigger.BOSS_FIRST_KILL, "ANCIENT_GOLEM"),
        entry(10, "The Matriarch's Garden.",
            "She is not attacking you with monsters. She is attacking you with her children. I do not say this to trouble you. I say it because you deserve to know what you are ending, and why it still might be a mercy.",
            LoreTrigger.BOSS_FIRST_KILL, "THORN_MATRIARCH"),
        entry(11, "An Ember That Refused.",
            "Fire is supposed to go out. This one said no, and a no, given enough years, becomes a shape. The Wyrm is that no, wearing scales.",
            LoreTrigger.BOSS_FIRST_KILL, "EMBER_WYRM"),
        entry(12, "The Shape of Falling.",
            "I asked the Void Knight, once, in the only language I have. Stillness, and time. What it wanted. It did not answer. I do not think it recalls the question anymore. I do not think it recalls much of anything except falling, and wanting company on the way down.",
            LoreTrigger.BOSS_FIRST_KILL, "VOID_KNIGHT"),
        entry(13, "On Letting Go.",
            "Some of them stop fighting you and start fighting the thing inside them instead. That one usually loses both battles at once.",
            LoreTrigger.ELITE_KILL, "blightburst"),
        entry(14, "A Root's Last Job.",
            "I do not control what the Hollow does with what used to be mine. But I notice it still flinches toward protecting, even now. That is either hope or a very old habit. I have stopped trying to tell the difference.",
            LoreTrigger.ELITE_KILL, "rootward_ward"),
        entry(15, "The Trail Home.",
            "Every rotting thing wants to return to soil eventually. I only wish this kind of soil grew a thing other than more of itself.",
            LoreTrigger.ELITE_KILL, "weeping_rot"),
        entry(16, "Again.",
            "You came back. I did not expect that. I am not certain the Hollow expected it either. Which may be the only advantage either of us has left.",
            LoreTrigger.ASCENSION, "1"),
        entry(17, "The Shape of a Habit.",
            "Twice now. I am beginning to recognize your footsteps before I see you. That is not nothing, after this many years of forgetting faces.",
            LoreTrigger.ASCENSION, "2"),
        entry(18, "What Doesn't Reset.",
            "The waves start over. The dark starts over. You do not. Not all the way. I have watched enough Wardens to know the difference between someone starting fresh and someone simply starting again.",
            LoreTrigger.ASCENSION, "3"),
        entry(19, "A Question I Don't Ask Often.",
            "I wonder, sometimes, if the Hollow gets tired the way you do. I have decided I do not want to know the answer badly enough to ask it.",
            LoreTrigger.ASCENSION, "5"),
        entry(20, "The Long Vigil.",
            "I have had guardians who lasted a season and guardians who lasted a lifetime. I no longer sort them by which. I sort them by whether they came back. You keep coming back.",
            LoreTrigger.ASCENSION, "10"),
        entry(21, "Bare-Handed",
            "You did that with what you were given, not with what you bought. I do not know whether to call that discipline or stubbornness. Possibly they are the same root.",
            LoreTrigger.SECRET, "bare_handed"),
        entry(22, "A Full Set",
            "Matched things hold together better than mismatched ones. I could have told you that before you spent the coin learning it.",
            LoreTrigger.SECRET, "full_set"),
        entry(23, "Mastery, Spent",
            "You have done that thing ten times so precisely that it no longer looks like effort. I recall when standing here felt like that too.",
            LoreTrigger.SECRET, "mastery"),
        entry(24, "Reforged",
            "Nothing stays as it was made. You, least of all. I mean that kindly.",
            LoreTrigger.SECRET, "reforged"),
        entry(25, "Six Mythics",
            "I did not think there were six things left in this whole grove worth calling unique. I am glad to be wrong.",
            LoreTrigger.SECRET, "six_mythics"),
        entry(26, "No Potions Spent",
            "You never once needed the weakest thing I could offer you. I hope that was strength, and not simply luck standing beside you the whole way.",
            LoreTrigger.SECRET, "no_potions"),
        entry(27, "The Long Pause",
            "I do not mind if you leave and come back. I have had many years of practice at waiting. It is rather a specialty of mine, at this point.",
            LoreTrigger.SECRET, "long_pause"),
        entry(28, "Every Elite, Once",
            "You have heard every fragment I have to whisper through them now. There is more to tell. There is always more. It simply is not theirs to carry.",
            LoreTrigger.SECRET, "every_elite"),
        entry(29, "Fastest Fall",
            "That was over before the Hollow finished sending it. I do not think it noticed yet.",
            LoreTrigger.SECRET, "fastest_fall"),
        entry(30, "Two Hundred, Once More",
            "The first time was survival. I suspect you already know what the second time was. Say it to yourself, if not to me.",
            LoreTrigger.SECRET, "wave200_twice")
    ));

    private static final Map<String, LoreEntry> BY_ID = index();

    private LoreCatalog() {
    }

    public static List<LoreEntry> all() {
        return ALL;
    }

    public static LoreEntry byId(String id) {
        return BY_ID.get(id);
    }

    private static LoreEntry entry(int number, String title, String body, LoreTrigger trigger, String triggerParam) {
        return new LoreEntry(String.format("codex_%02d", number), number, title, body, trigger, triggerParam);
    }

    private static Map<String, LoreEntry> index() {
        Map<String, LoreEntry> map = new LinkedHashMap<>();
        for (LoreEntry entry : ALL) {
            if (map.put(entry.id(), entry) != null) {
                throw new IllegalStateException("Duplicate codex id: " + entry.id());
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
