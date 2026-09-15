# Hero Defense — Story Content

> **Voice rules are frozen in `docs/STORY_VOICE.md`:** short lines, common words, one idea per line. Hero = terse-white, Tree = leaf-green. Every line below follows those rules since Phase 31.

Full narrative text to drop straight into the systems described in Phases 20, 21, 23, and 25 of
the roadmap addendum. Two voices only, kept distinct on purpose:

- **The Hero** — terse, present-tense, spoken in the existing white battle-cry style
  (`OpeningCinematic`, mid-run beats, Ascension transitions).
- **The Tree** — slower, reflective, third-person-ish, used only in the Grove Codex. Render it in
  a different tint (leaf-green rather than white) so the player learns to tell the two apart
  without a label.

Nothing here needs new art. It is text plus the unlock/trigger conditions to wire it to.

## World Premise (one paragraph, for internal reference — not shown in-game)

Long before the first wave, something did not grow here — it fell here. Players and Codex text
never need the astronomy; they only need to feel that the corruption is not simple rot, it is
*unmaking*: stone kept moving after it should have gone still, fire kept burning after it should
have gone out, shadow kept hungering after it should have passed. The Hollow, players learn, is
that unmaking's name — and the four bosses are its four ways of touching the world: through stone
(Ancient Golem), through root and thorn (Thorn Matriarch), through fire (Ember Wyrm), and through
the shadow it left behind it when it fell (Void Knight). The World Tree is the one root the Hollow
has never finished swallowing. Every regular enemy — Rootling, Stonekin, Gloom Wolf, Fungal Brute —
is something that lived here once, still shaped like what it was.

## 1. Opening Cinematics by Ascension Tier

Tier 0 is the shipped text, included only for reference — do not rewrite it.

**Tier 0 (shipped, reference only)**
1. "Can you protect the World Tree?!"
2. "Can you?"
3. "Are you sure?!"

**Tier 1**
1. "Dark comes again."
2. "I stand again."
3. "This time I go far."

**Tier 2**
1. "The Hollow knows me now."
2. "Good. Let it fear."
3. "Roots first. Then flesh. Then Tree. Not today."

**Tier 3 and every tier after (reused as-is — do not author one set per tier)**
1. "New dawn. New fight."
2. "The Tree asks once."
3. "So do I."

## 2. Mid-Run Story Beats

Single lines, shown the same way the opening's beats are shown, but one at a time and briefly —
no new animation, just the existing white-text overlay triggered at a wave boundary. Silent,
skippable on tap, exactly like the opening.

### 2.1 First-encounter boss title cards

Shown once only, the very first time each identity is fought (not on every repeat appearance):

- **Ancient Golem**, first meeting: *"ANCIENT GOLEM — old guard who still stands."*
- **Thorn Matriarch**, first meeting: *"THORN MATRIARCH — she grew half your foes."*
- **Ember Wyrm**, first meeting: *"EMBER WYRM — a fire that never went out."*
- **Void Knight**, first meeting: *"VOID KNIGHT — he fell and forgot the rest."*

### 2.2 Between-boss reflection lines (Hero voice, one line, quiet)

- **Wave 25:** "Wolves fear something deeper than me. That should scare me more."
- **Wave 50:** "Half of what I killed, I once knew. I try not to think of it."
- **Wave 75:** "Ground past the tree line feels wrong. Not ground at all."

### 2.3 Wave 100 — the Planting Ceremony (spoken lines, synced to the existing timeline)

Matches the shipped `PlantingCeremony` beats one-to-one — walk, plant, water, growth, return.

1. *(Hero walks toward the World Tree)* "One root should not hold this alone."
2. *(planting the seed)* "Then a second one. Grow angry if you must."
3. *(watering)* "I will hold the line. That is my job."
4. *(the sapling grows — shown in the Tree's tint, not the Hero's)* "The grove remembers your gift."
5. *(Hero turns back to the anchor point)* "Now hold both."

### 2.4 Second-half reflection lines

- **Wave 125:** "Two trees now. Twice to lose. Bad trade. I would still make it."
- **Wave 150:** "It no longer sends weak first. It is done waiting."
- **Wave 175:** "What is left may be the last. Or it wants me to think so."

### 2.5 Wave 200 — Ascension transition (shown after the epilogue, before the Ascend prompt)

Tier-independent, always the same:

- "The Hollow is not gone. It is quiet while it learns to fall again."
- "Rise again. The Tree will still stand."

## 3. Boss Lore (bios for the Codex, one paragraph each)

- **Ancient Golem.** Before it was a weapon of the Hollow, it was the forest's oldest guard. A stone keeper that had not moved from its post in longer than the Tree could recall. The Hollow did not need to turn it. It only needed to make it think the fight had never ended.
- **Thorn Matriarch.** She grew half the arena's Rootlings herself, back when growing things was all she did. What she plants now still takes root. It simply does not ask, and it is not kind.
- **Ember Wyrm.** When the Hollow first touched this ground, a thing here caught fire and never fully went out. The Wyrm is what that ember became once it learned to want more fuel.
- **Void Knight.** No one here recalls what it looked like before. It does not either. It only recalls falling, and it has spent every year since trying to make another thing fall with it.

## 4. Elite Affixes — "Whispering Wounds" (two Codex fragments each)

Each Elite carries a fragment of the Hollow's memory. Killing one drops a two-part lore thread;
the two fragments can unlock in either order.

**Blightburst** *(explodes on death)*
- Fragment I: "It does not die so much as let go. What was holding it together was never its own to keep."
- Fragment II: "The burst is not rage. It's relief."

**Rootward Ward** *(periodically shields)*
- Fragment I: "The shield is not armor. It's a root, briefly recalling what it was for."
- Fragment II: "Even changed, a thing in it still tries to protect a thing. It's just no longer sure what."

**Weeping Rot** *(leaves a damaging trail)*
- Fragment I: "The ground it crosses does not heal. Not yet. Maybe not ever."
- Fragment II: "Every trail leads back the same direction, if you follow it far enough: toward the
  Tree."

## 5. The Grove Codex — 30 Entries

Narrated in the Tree's voice throughout. Grouped by unlock trigger; the grouping is for
implementation clarity only — display them unsorted-by-category once unlocked, ordinary list order.

### 5.1 Wave-milestone entries (unlock on first reaching the wave)

1. **Wave 1 — "Before You."** *"Others stood here before you. I do not recall most of their names. I recall all of their last stands."*
2. **Wave 10 — "The Three Directions."** *"They do not come from everywhere. Only from three. I
   have never learned what holds the fourth."*
3. **Wave 20 — "Counting."** *"I used to count the seasons. Now I count waves. It is a smaller
   unit of time, and it passes no more kindly."*
4. **Wave 30 — "What Luck Finds."** *"Some of what falls from them still recalls being useful. Wear it. I would rather you have it than the ground."*
5. **Wave 40 — "The Quiet Ones."** *"Not every Rootling attacks. Some simply stand at the tree
   line and watch. I do not know if that is worse."*
6. **Wave 60 — "Old Names."** *"Rootling. Stonekin. Gloom Wolf. Fungal Brute. I gave three of those names myself, once, to living things, meaning a kinder thing."*
7. **Wave 80 — "The Long Middle."** *"This is the part no song is written about. Not the falling, not the standing. Just the holding. Hold anyway."*
8. **Wave 100 — "Two Roots."** *"I did not ask for a second trunk. I am glad of it regardless. Grief is lighter, split two ways. And so, it turns out, is standing guard."*

### 5.2 Boss-first-kill entries (unlock on first defeat of each identity)

9. **"What the Golem Guarded."** *"Before there was a Tree to defend, there was a boundary stone.
   The Golem was its keeper. I think, somewhere under the Hollow's grip, it still believes that is
   all this is: a boundary, and a keeper."*
10. **"The Matriarch's Garden."** *"She is not attacking you with monsters. She is attacking you with her children. I do not say this to trouble you. I say it because you deserve to know what you are ending, and why it still might be a mercy."*
11. **"An Ember That Refused."** *"Fire is supposed to go out. This one said no, and a no, given enough years, becomes a shape. The Wyrm is that no, wearing scales."*
12. **"The Shape of Falling."** *"I asked the Void Knight, once, in the only language I have. Stillness, and time. What it wanted. It did not answer. I do not think it recalls the question anymore. I do not think it recalls much of anything except falling, and wanting company on the way down."*

### 5.3 Elite-kill entries (unlock on first kill of an Elite carrying that affix)

13. **"On Letting Go."** *(Blightburst fragment, restated for the Codex list)* *"Some of them
    stop fighting you and start fighting the thing inside them instead. That one usually loses
    both battles at once."*
14. **"A Root's Last Job."** *(Rootward Ward)* *"I do not control what the Hollow does with what
    used to be mine. But I notice it still flinches toward protecting, even now. That is either
    hope or a very old habit. I have stopped trying to tell the difference."*
15. **"The Trail Home."** *(Weeping Rot)* *"Every rotting thing wants to return to soil eventually. I only wish this kind of soil grew a thing other than more of itself."*

### 5.4 Ascension entries (unlock on completing that Ascension tier)

16. **After Ascension 1 — "Again."** *"You came back. I did not expect that. I am not certain the Hollow expected it either. Which may be the only advantage either of us has left."*
17. **After Ascension 2 — "The Shape of a Habit."** *"Twice now. I am beginning to recognize your footsteps before I see you. That is not nothing, after this many years of forgetting faces."*
18. **After Ascension 3 — "What Doesn't Reset."** *"The waves start over. The dark starts over. You do not. Not all the way. I have watched enough Wardens to know the difference between someone starting fresh and someone simply starting again."*
19. **After Ascension 5 — "A Question I Don't Ask Often."** *"I wonder, sometimes, if the Hollow
    gets tired the way you do. I have decided I do not want to know the answer badly enough to ask
    it."*
20. **After Ascension 10 — "The Long Vigil."** *"I have had guardians who lasted a season and
    guardians who lasted a lifetime. I no longer sort them by which. I sort them by whether they
    came back. You keep coming back."*

### 5.5 Curiosity / secret entries (unusual conditions — listed with their trigger)

21. **"Bare-Handed"** *(reach Wave 50 having bought no Shop stats this run)* — *"You did that with what you were given, not with what you bought. I do not know whether to call that discipline or stubbornness. Possibly they are the same root."*
22. **"A Full Set"** *(equip a complete 4-piece set for the first time)* — *"Matched things hold
    together better than mismatched ones. I could have told you that before you spent the coin
    learning it."*
23. **"Mastery, Spent"** *(any skill reaches level 10 for the first time)* — *"You have done that thing ten times so precisely that it no longer looks like effort. I recall when standing here felt like that too."*
24. **"Reforged"** *(forge any item to its maximum Anvil level for the first time)* — *"Nothing
    stays as it was made. You, least of all. I mean that kindly."*
25. **"Six Mythics"** *(own all six Mythic items at once, any slot combination)* — *"I did not
    think there were six things left in this whole grove worth calling unique. I am glad to be
    wrong."*
26. **"No Potions Spent"** *(clear an entire Wave 1–100 stretch without consuming a single potion)*
    — *"You never once needed the weakest thing I could offer you. I hope that was strength, and
    not simply luck standing beside you the whole way."*
27. **"The Long Pause"** *(pause the game mid-wave for an unusually long time, then resume)* —
    *"I do not mind if you leave and come back. I have had many years of practice at waiting. It is rather a specialty of mine, at this point."*
28. **"Every Elite, Once"** *(kill at least one Elite of every affix type)* — *"You have heard every fragment I have to whisper through them now. There is more to tell. There is always more. It simply is not theirs to carry."*
29. **"Fastest Fall"** *(clear a single wave in record time, e.g. under a defined threshold)* —
    *"That was over before the Hollow finished sending it. I do not think it noticed yet."*
30. **"Two Hundred, Once More"** *(reach Wave 200 for the second time, any Ascension tier)* —
    *"The first time was survival. I suspect you already know what the second time was. Say it
    to yourself, if not to me."*

## 6. Branching Epilogues (shown at run end, before the Ascension transition on a win)

**A — Flawless Victory** *(Wave 200 cleared, Hero never died this run)*
1. "Two hundred waves. No step lost."
2. "The Hollow needs a new plan."
3. "Till then, the Tree and I stand."

**B — Hard-Fought Victory** *(Wave 200 cleared, Hero died and was revived by the run's normal
recovery, or finished under a defined HP/potion-usage threshold)*
1. "Two hundred waves. All were close."
2. "I do not recall it all. I recall not letting go."
3. "That is enough. It has to be."

**C — Early Fall** *(Game Over before Wave 50)*
1. "Not to the middle."
2. "The Tree falls soft and quiet this early."
3. "Next time it will be loud."

**D — Middle Fall** *(Game Over, Wave 50–149)*
1. "Close to the second root."
2. "I went farther than last time. Far is not far enough."
3. "Again."

**E — Late Fall** *(Game Over, Wave 150–199)*
1. "One tree stood when I fell. That must count."
2. "The Hollow paid past wave one hundred. It just lasted a bit more."
3. "Next time it pays for all."

## 7. Mythic Item Flavor (one per equipment slot)

- **Weapon — "Sunfall, the Last Arrow."** *Passive: Chain Lightning arcs also apply Stun.*
  Flavor: "Shot once, long ago, at a high fall. The arrow did not come back whole. Nor did what it hit."
- **Helmet — "Crown of the Hollow Eye."** *Passive: a critical hit marks its target; further hits
  on a marked enemy deal bonus damage for a few seconds.* Flavor: "Wear it and you see weak spots as the Hollow sees strength. The one true thing to aim for."
- **Armor — "Bark of the First Root."** *Passive: every tenth hit taken triggers a free heal
  without spending a potion.* Flavor: "Cut from the World Tree's bark when it could spare wood. It knows how to close a wound."
- **Boots — "Windrunner's Last Steps."** *Passive: attack speed slowly climbs the longer the Hero
  holds position in a wave, capped.* Flavor: "Made for running. He never ran again after putting them on. He no longer needed to."
- **Ring — "Verdant Oath."** *Passive: auto-potions also grant a few seconds of bonus lifesteal.*
  Flavor: "A promise in sap. What heals you lets you keep healing."
- **Ring — "Emberless Core."** *Passive: critical hits refund part of the shot's cooldown.*
  Flavor: "The ember that never went out, cooled and put to work. No longer left to spread."

## 8. A Few Extra Touches (small, optional, not required by any roadmap item)

- **Idle whisper:** if the game sits paused for an unusually long real-world stretch, the very
  next Resume can silently show one unused Codex-style Tree line before combat continues — reuses
  the same white/green text overlay, costs nothing new to build, and rewards players who step away
  and come back rather than only players mid-session.
- **Silent Rootling detail:** entry 5 above ("The Quiet Ones") is worth actually implementing as a
  rare, harmless spawn variant — a Rootling that stands still at the tree line and never attacks —
  so the lore line has something on screen to point at, not just text. Purely visual, zero balance
  impact, and the kind of detail a player notices once and then looks for every run after.
