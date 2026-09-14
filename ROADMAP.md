# Hero Defense — Development Roadmap

A single-hero action-defense game for Android. The Hero (an Elf) stands fixed at the center of the arena, defending the World Tree behind him through 100 continuous waves. Game language is English.

> **Progress rule:** Complete, verify, commit, and push each checklist item separately. Never batch completed items into one push.
>
> **Prior-roadmap closure (2026-09-13):** At the owner's direction, Phases 0–15 are closed. A checked item normally means verified completion; where a physical/manual action or previously excluded release deliverable was not actually performed, the item is checked as **owner-closed/waived** and says so explicitly rather than claiming false verification.

## Core Specs (Quick Reference)

| Item | Value |
|---|---|
| Language / Engine | Java + **libGDX** |
| Build target | **Android only**, built/tested via Android emulator or connected device; touch input only |
| Distribution | Cafe Bazaar APK |
| CI | GitHub Actions, not the persistent workspace |
| Workspace limit | Under 128 MB; only push-able source and assets |
| Business model | Fully free; no IAP; coins are in-game currency only |
| Run structure | One continuous 100-wave run, not stage-based |
| Bosses | Every fifth wave (20 encounters), with distinct boss designs |
| Reward cards | Exactly three cards after every boss; choose one |
| Hero level cap | 100 |
| Save system | Local only |
| Visual pipeline | Offline Blender + Python (`bpy`), rigged low-poly 3D rendered to 2D sprites |
| Audio | Free online audio resources with verified licenses |
| Minimum device | Mid-range Android and newer |
| Localization | English now; Persian planned later |
| Input | Touch only (`tap` / `drag`); no keyboard or mouse-only controls |

## Phase 0 — Repository & Tooling Setup

- [x] Create a new, separate repository (fully separate from the earlier Tower Defense project).
- [x] Set up the libGDX project skeleton with **two modules only**: `core` and `android`; no desktop or browser module.
- [x] Configure Gradle so wrapper/dependency downloads happen in `/tmp` or a cache outside the committed folder.
- [x] Write a precise `.gitignore`: no build output, APKs, Gradle caches, SDK files, or Blender install files enter Git.
- [x] Store the GitHub token as a GitHub Actions secret (`RELEASE_GITHUB_TOKEN`); never hardcode it in source.
- [x] Add `.github/workflows/build-android.yml`: build on every push, run in a headless Android emulator with simulated touch, and upload the APK artifact. Final signing can come later.
- [x] Add an optional workflow that runs `core` unit tests on every push.
- [x] Add this `ROADMAP.md` at the repository root.
- [x] Verify and push the initial project skeleton and workflows.

## Phase 1 — Core Architecture

- [x] Implement the libGDX `ApplicationAdapter` game loop with states: `MENU`, `PLAYING`, `PAUSED`, `LEVEL_UP`, `CARD_CHOICE`, `SHOP`, `GAME_OVER`.
- [x] Design a simple entity structure using plain classes for `Hero`, `Enemy`, `Boss`, `Projectile`, `Item`, and `DropEntity`.
- [x] Implement central `GameState`: wave, living enemies, Hero, coins, inventory, level, and unspent talent points.
- [x] Implement local save/load using Preferences + JSON, serializing/deserializing the whole `GameState` with a backup save.
- [x] Implement `FitViewport` around a fixed 720×1280 portrait reference resolution.
- [x] Implement all input with libGDX touch/pointer APIs and touch drag callbacks. Add no keyboard bindings.

## Phase 2 — Offline Blender + Python Asset Pipeline

The source of truth is procedural Python. Real 3D rigs and bones provide coherent motion and equipment attachment, while the Android game ships only 2D sprites.

- [x] Install/run checksum-pinned Blender 4.2 LTS only in a disposable cache/CI environment; never commit Blender itself.
- [x] Write the style guide first: polygon budgets, toon color bands, outlines, fixed camera, and fixed lighting.
- [x] Implement a headless `bpy` pipeline (`blender --background --python ...`) that:
  - creates low-poly toon models for the Hero, enemies, and bosses;
  - creates separate mesh/material variants for weapons, helmets, armor, boots, and rings;
  - rigs each character with a real Armature;
  - authors Idle, Attack, Hit, and Death clips via bone animation;
  - renders every animation frame from the fixed camera to transparent PNG;
  - packs frames into sprite sheets / texture atlases for libGDX.
- [x] Run and review a pilot batch for the Hero and one enemy before scaling the roster.
- [x] Model, rig/animate as needed, and render the World Tree in healthy and damaged states.
- [x] Model and render 40 equipment items as attachable Hero-rig variants, plus six potion icons.
- [x] Model/render ground tiles and 3D props; keep appropriate flat UI assets 2D/vector.
- [x] Implement Rare/Legendary glow as a runtime code/shader effect, not baked into sprites.
- [x] Commit generation scripts; commit `.blend` files only if small, otherwise regenerate them.
- [x] Push each completed and reviewed rendered-asset batch separately.

## Phase 3 — Hero Implementation

- [x] Keep the Hero fixed at the center of the arena.
- [x] Implement five base stats: Strength → damage, Agility → attack speed, Luck → drop chance, Dodge → evasion, Health → max HP.
- [x] Implement auto-attack against the nearest/first enemy in range; derive interval from Agility.
- [x] Roll Dodge against every incoming hit before applying damage.
- [x] Drive Blender-rendered Idle/Attack/Hit/Death frames from real gameplay state.
- [x] Implement XP and leveling to level 100; grant one touch-allocated talent point each level.

## Phase 4 — Enemies & Continuous Waves

- [x] Implement several regular enemy types, all melee.
- [x] Spawn enemies from three directions and have them converge on the Hero.
- [x] While the Hero lives, enemies attack the Hero; when the Hero dies, destroy the World Tree and enter Game Over.
- [x] Advance seamlessly through waves 1–100 without loading screens.
- [x] Apply the wave-number difficulty formula defined in Phase 14.
- [x] Advance automatically after the current wave is fully cleared.

## Phase 5 — Boss System

- [x] Create at least four distinct boss designs, not recolors/rescales.
- [x] Rotate bosses at Waves 5, 10, 15, …, 100.
- [x] Give bosses substantially higher HP/damage using Phase 14 multipliers.
- [x] Give every boss at least one distinct animation or attack behavior.

## Phase 6 — Post-Boss Reward Cards

- [x] Pause and display exactly three random reward cards after every boss kill.
- [x] Build a pool including base stats, general power, coin income, lifesteal, and extensible effects.
- [x] Apply a card immediately when tapped and persist its effect in `GameState`.
- [x] Scale cards with a defined power budget from Phase 14.

## Phase 7 — Inventory, Equipment & Items

- [x] Implement six equipment slots: Weapon, Helmet, Armor, Boots, Ring 1, Ring 2.
- [x] Define 40 items over four tiers: Common, Uncommon, Rare, Legendary.
- [x] Give each item a name, slot, tier, stat bonuses, and icon.
- [x] Connect equipped items to rendered mesh/material sprite variants.
- [x] Show runtime Rare/Legendary glow when equipped.
- [x] Add touch inventory UI for viewing, equipping/unequipping, and selling non-potion items.
- [x] Implement low item-drop chances modified by Luck using Phase 14 rates.

## Phase 8 — Health Potions

- [x] Implement six potion tiers with Phase 14 heal amounts.
- [x] Auto-use the weakest available potion below a tunable HP threshold (default 35%).
- [x] Implement low-chance, wave-weighted potion drops.

## Phase 9 — Economy & Shop

- [x] Award coins for enemy and boss kills.
- [x] Sell unwanted non-potion items for coins.
- [x] Add a touch-only in-game shop for direct stat upgrades; no real-money purchases.
- [x] Tune shop pricing in Phase 14.

## Phase 10 — UI / UX / HUD

- [x] Build a clean phone HUD: HP, wave, coins, Pause, and Speed controls with generous tap targets.
- [x] Pause/resume all simulation from a tap target.
- [x] Cycle 1×/2×/3× simulation speed from a tap target.
- [x] Add touch-only Main Menu: new game, continue, settings.
- [x] Add touch-only Level-Up stat selection.
- [x] Add touch-only post-boss three-card selection.
- [x] Add touch/drag Inventory and Equipment screens.
- [x] Add touch-only Shop screen.
- [x] Add Game Over summary and tap-to-restart at Wave 1.
- [x] Author/render required UI icons under the Phase 2 style guide.

## Phase 11 — Audio

- [x] Find free background music and effects for hit, death, item drop, level up, and boss entrance.
- [x] Verify and record each audio file's license at download time; prefer CC0/no attribution.
- [x] Implement libGDX `Music` and `Sound` playback.

## Phase 12 — Polish & Game Feel

- [x] Add light screen shake on Hero hits and boss kills.
- [x] Add brief critical-hit hit-stop.
- [x] Add particles for hits, deaths, coins, and item pickups.
- [x] Add clear visual/haptic feedback for taps and card selection.

## Phase 13 — Testing

- [x] Run Android builds in a headless emulator, driving interaction only through simulated touch events.
- [x] Automate a touch smoke test: menu, waves, inventory, and reward card.
- [x] Owner-closed/waived: manually verify on at least one real mid-range Android touchscreen before release. This was not physically performed and remains a recorded release risk.

## Phase 14 — Comprehensive Balancing

### 14.1 Enemy Growth

- [x] Define regular enemy HP with a gentle exponential, starting from `EnemyHP(w) = 20 × (1 + 0.045)^w`, then tune.
- [x] Define enemy damage growth and cap it to prevent one-shots against reasonably built Heroes.
- [x] Start boss tuning at `BossHP(w) = EnemyHP(w) × 15` and `BossDamage(w) = EnemyDamage(w) × 3`.

### 14.2 Hero Growth

- [x] Define exact gains per stat point (starting examples: Health +10 HP, Strength +2 damage).
- [x] Define equipment tier power (starting targets: Common +5%, Uncommon +12%, Rare +25%, Legendary +45%).

### 14.3 Simulation-Based Testing

- [x] Add a renderer-independent 100-wave simulation/test logging HP remaining, DPS-to-HP ratio, and clear time per wave.
- [x] Define a pass criterion: balanced allocation reaches Wave 100 while losing about 5%–15% max HP per wave on average; remove spikes.
- [x] Re-run simulation after every coefficient change and before manual playtests.

### 14.4 Drops & Economy

- [x] Use starting per-kill item rates: Common 6%, Uncommon 3%, Rare 0.8%, Legendary 0.15%; each Luck point multiplies rates by 1.02.
- [x] Use approximately 8% potion-drop chance, weighted toward wave-appropriate tiers.
- [x] Use potion heals of 15%, 25%, 40%, 60%, 80%, and 100% max HP.
- [x] Set sell/shop prices so one boss reward is roughly one meaningful contemporary upgrade.

### 14.5 Reward Cards

- [x] Define a boss-index-scaled card power budget with similar relative impact from Boss 1 to Boss 20.
- [x] Verify in simulation that no single card trivializes the remaining run.

### 14.6 Manual Checkpoints

- [x] Owner-closed/waived: playtest Waves 1, 5, 25, 50, 75, and 100 and record felt difficulty. Automated balance evidence exists, but subjective play feel was not manually verified.
- [x] Owner-closed/waived: after coefficient changes, re-run simulation and then repeat manual playtests. The mandatory simulation reruns passed; the subjective repeat was not performed.

## Phase 15 — Release Preparation

- [x] Finalize CI to build, sign, and output a Cafe Bazaar-ready APK.
- [x] Owner-closed for the prior roadmap: prepare store icon, screenshots, and English description matching the visual style. The generated icon is complete; screenshots and description were explicitly waived and were not produced.
- [x] Perform final repository/APK size checks (workspace 33 MB excluding Git; signed artifact archive 24,123,865 bytes; CI enforces APK below 100 MB).
- [x] Owner-closed for the prior roadmap: push final changes and create a Git release tag. Changes were pushed; the tag was explicitly waived and was not created.

## Phase 16 — Premium Visual & Live-Gameplay UX Upgrade

The goal is a substantially more polished, eye-catching commercial-mobile presentation without gratuitous clutter. Preserve silhouette readability, touch clarity, deterministic gameplay, save compatibility, and mid-range Android performance. **Audio is frozen and out of scope for this phase.**

### 16.1 Navigation Reliability

- [x] Reproduce, root-cause, and fix the intermittent Resume touch failure; nested `PAUSED → SHOP` had overwritten Pause's return destination. Unit regression coverage and the expanded Android touch-emulator journey pass.
- [x] Add always-visible, generous Inventory and Shop touch targets to the live gameplay HUD so neither requires opening Pause first.
- [x] Automatically pause all combat simulation while Inventory or Shop is open, and restore the exact prior `PLAYING` or `PAUSED` state when closing either screen.
- [x] Show complete item details inside Inventory: name, rarity, slot, every stat bonus, equipped state, and a clear comparison against the currently equipped item.

### 16.2 Reward and Pickup Presentation

- [x] Split item/potion drops into persisted `GROUND` and `HOMING` stages without changing their deterministic reward outcome.
- [x] Animate each ground drop along a smooth raised arc into the live Inventory HUD destination, shrinking cleanly before it disappears.
- [x] Give Rare and Legendary ground drops clearly readable but restrained rarity-colored shader glow and color-matched homing trails.
- [x] Show a high-contrast golden floating `$ +N` coin number above the Hero whenever a kill awards coins.
- [x] Keep the current total coin balance clearly visible with a currency icon and `$` label on the live gameplay HUD and primary menu surface.

### 16.3 Premium Art Pipeline Foundation

- [x] Upgrade the visual style guide to a premium-v2 quality bar covering shape language, material separation, animation polish, VFX restraint, UI composition, and actual-phone readability.
- [x] Upgrade the atlas packer to enforce multi-page atlases no larger than 2048×2048, with automated frame/pivot/alpha and GPU-memory-budget checks.
- [x] Produce and review a premium-v2 pilot containing the Hero, Rootling, Ancient Golem, five-piece Verdant Covenant set, tier-6 potion drop, crystal prop, and Inventory control; the accepted before/after contact sheets and render contract are recorded in `docs/art_reviews/PREMIUM_V2_PILOT_REVIEW.md`.

### 16.4 Premium Character and World Batches

- [x] Upgrade and review the complete Hero model, 25-bone rig deformation, Idle/Attack/Hit/Death animation, silhouette, materials, and native-size sprite output; final acceptance is recorded in `docs/art_reviews/HERO_PREMIUM_V2_REVIEW.md`.
- [x] Upgrade and review all 40 equipment attachment animation atlases while preserving socket alignment with every Hero frame; the accepted 1,120-frame audit and review sheets are recorded in `docs/art_reviews/EQUIPMENT_PREMIUM_V2_REVIEW.md`.
- [x] Upgrade and review all four regular enemy models, rigs, animations, materials, silhouettes, and atlases; the accepted 112-frame audit and nine review sheets are recorded in `docs/art_reviews/ENEMIES_PREMIUM_V2_REVIEW.md`.
- [x] Upgrade and review all four Boss models, rigs, signature animations, materials, silhouettes, and atlases; the accepted 112-frame audit and nine review sheets are recorded in `docs/art_reviews/BOSSES_PREMIUM_V2_REVIEW.md`.
- [x] Upgrade and review the healthy/damaged World Tree art and destruction presentation; the accepted 22-frame audit and six review sheets are recorded in `docs/art_reviews/WORLD_TREE_PREMIUM_V2_REVIEW.md`.
- [x] Upgrade and review all arena ground tiles, crystal props, background composition, and depth treatment. (Accepted evidence: [`ARENA_PREMIUM_V2_REVIEW.md`](docs/art_reviews/ARENA_PREMIUM_V2_REVIEW.md))

### 16.5 Premium Items, UI, and Effects

- [x] Upgrade and review all equipment, potion, drop, currency, navigation, stat, speed, pause, inventory, shop, and reward-card icons.
- [x] Upgrade the Main Menu and live HUD visual hierarchy, panels, typography treatment, buttons, and touch feedback without reducing gameplay visibility.
- [x] Upgrade the Inventory and Equipment presentation, including item cards, comparison states, selection, equip/unequip, sell feedback, and rarity treatment.
- [x] Upgrade the Shop presentation, including stat cards, price/affordability states, purchase feedback, and paused-game context.
- [x] Upgrade Pause, Settings, Level-Up, Reward Card, Game Over, and Victory surfaces to the same coherent premium-v2 standard. (Accepted evidence: [`FLOW_SURFACES_PREMIUM_V2_REVIEW.md`](docs/art_reviews/FLOW_SURFACES_PREMIUM_V2_REVIEW.md))
- [x] Upgrade projectiles, impacts, critical hits, enemy deaths, Boss entrances/deaths, item collection, coins, World Tree damage, and ambient arena VFX with restrained visual layering.

## Phase 17 — Skill Shop, Archer-Only Arsenal, and Lifesteal-Aware Rebalance

Deepen the coin economy with expensive long-horizon skills, make the Hero a pure archer, let loot be seen before it is collected, and retune enemy growth so lifesteal-fuelled builds still feel pressure.

### 17.1 Purchasable Skills

- [x] Add a `SKILLS` tab to the Shop with five coin-only skills, each upgradable ten times on a geometric price curve (`SkillId`, `SkillEffects`, `SkillShopSystem`; levels persist in `GameState.skillLevels`).
- [x] Chain Lightning: arcs a share of arrow damage to the nearest foes within 210 px, with more targets at higher levels.
- [x] Multi Shot: fires up to three extra reduced-damage arrows per volley, spread across other foes in range.
- [x] Stunning Arrows: chance per hit to freeze movement, melee, and boss specials (bosses resist 50%).
- [x] Critical Mastery: critical chance doubles and the multiplier climbs from 1.75× to 2.5× by level 10.
- [x] Eagle Range: +22 px bow reach per level over the 420 px base.
- [x] Render, review, and promote five `ui_skill_*` medallion icons through the Blender `skill-icons` batch. (Accepted evidence: [`SKILL_ICONS_PREMIUM_V2_REVIEW.md`](docs/art_reviews/SKILL_ICONS_PREMIUM_V2_REVIEW.md))

### 17.2 Arsenal, Loot Visibility, and Balance

- [x] Retire the four melee weapons and replace them with Yew Shortbow, Thornwood Bow, Verdant Recurve, and Golemsbane Warbow, borrowing reviewed same-tier bow art (`EquipmentDefinition.artId`) until dedicated art is rendered.
- [x] Drops now linger 2.6 s on the ground before homing so loot is clearly visible.
- [x] Rebalance for lifesteal and the new skills: regular HP `20 × 1.037^w`, damage `0.27 × 1.003^(w−1)`; the simulator's coin policy now buys skills, and the baseline, eight extra seeds, and all forced-card scenarios pass the gate (see `docs/BALANCE.md`).

## Phase 18 — Juice, Endless Growth, and the Second Tree

Make every new skill visibly and audibly powerful, show the Hero's growth on screen, and turn Wave 100 from an ending into the planting of a second World Tree that opens Waves 101–200 with uncapped progression.

### 18.1 Combat Feel

- [x] Floating damage numbers: normal, critical (larger, gold), chain arc (cyan), and stun ("STUN") pop-ups with deterministic positions and pooled rendering (`CombatEvent` stream from the attack system, `FloatingDamageTextSystem`, 40-label pool).
- [x] Dedicated VFX: chain-lightning arc beams between struck foes, multi-shot fan trails, stun sparks orbiting frozen enemies, and a stronger critical impact burst with a short screen shake (`CHAIN_BEAM/CHAIN_FLASH/STUN_SPARK/CRITICAL_SPARK` particle families within the VFX budget; per-arrow impact bursts).
- [x] Lift the Phase 16 audio freeze: added CC0 critical, kill, chain-lightning, stun, multi-shot, and purchase sounds (Kenney Impact/RPG/Interface packs) with per-file SHA-256 records in `docs/audio/AUDIO_LICENSES.md`; per-cue rate limiting via `AudioThrottle`, hash-bound by `AudioContractTest`.

### 18.2 Hero Progression Surfaces

- [x] Hero EXP bar in the live HUD with level badge and level-up flash (slim cyan bar under the health bar, `LV n` badge, `x / y XP` readout, ivory flash for 0.9 s on level gain).
- [x] Level-Up overlay lists stats in the Shop order (Strength, Agility, Luck, Dodge, Health) top-to-bottom; `LevelUpTouchLayout.rowBottom` inverted, smoke taps updated.

### 18.3 Endless Progression

- [x] Remove stat and skill purchase caps: stats linear through 20 then ×1.25 per level; skills base curve through 10 then ×1.45 per level; `SkillEffects.effectiveLevel` halves the gain of each further ten-level block (converges to 20 core-equivalent) with hard ceilings on every chance/count effect; shop shows `LEVEL n | ENDLESS`; save repair no longer clamps skill levels; simulator greedy loop bounded per visit.
- [x] Anvil: `ItemForgeSystem` reforges Rare/Legendary items up to +5 (Rare $150, Legendary $350, ×1.6 per step), +1 to every stat bonus per step, `+N` name suffix, sell price grows by half the spend, equipped items update max HP live; ANVIL button sits between EQUIP and SELL with cost/reason copy; `upgradeLevel` persisted on the item.
- [x] Inventory auto-sell chips for Common, Uncommon, and Rare in the inventory header; ticked tiers are sold by `DropPickupSystem` on entry with a gold `+$ n` pop-up over the Hero; persisted in device settings (`inventory.autoSell.*`), never touches equipped items or Legendaries.

### 18.4 The Second World Tree

- [x] Render and review Hero Walk, Plant, and Water clips plus seed, watering can, and a sapling-to-tree growth sequence through the Blender pipeline. (`hero_ceremony` walk 8 / plant 10 / water 10 and `world_tree_sapling` grow 12 / idle 6; accepted in `docs/art_reviews/CEREMONY_PREMIUM_V2_REVIEW.md`, promoted by `tools/visual/promote_ceremony_batch.py`, hash-guarded by `PremiumCeremonyAssetContractTest`.)
- [x] Wave 100 cinematic: combat pauses, the Hero walks beside the World Tree, plants a seed, waters it, a second tree grows in place, and the Hero walks back to the anchor and resumes auto-combat; the player never controls the Hero. Touch-skippable, save-safe, deterministic. (`GameScreenState.CINEMATIC`, `gameplay/PlantingCeremony` timeline, `ceremonyPending` persisted and replayed on continue, `WaveCompletion.PLANTING_CEREMONY`, water-drop particles.)
- [x] Waves 101–200 with the second tree standing as a permanent monument; Game Over now shows the monsters destroying every planted tree instead of ending abruptly. (`FINAL_WAVE = 200`, `secondTreePlanted` idle sway via `SaplingTreeRenderer`; on Hero death survivors march on the nearest tree for `TREE_SIEGE_SECONDS` while its health drains, then both trees fall.)
- [x] Rebalance the full 1–200 run with uncapped progression; simulator gate extended to Wave 200 and `docs/BALANCE.md` updated. (Waves 1–100 unchanged; waves 101–200 continue at `HP × 1.021^(w−100)`, `damage × 1.006^(w−100)`; 9/9 seeds finish, avg 10.0%, worst wave 28.8%, 312 forced-card scenarios pass.)

## Phase 19 — Opening, Polish, and Economy Balance

Give every new run a short spoken opening, then sweep the game for bugs and rough edges, and finally rebalance the whole run around what the player actually buys: items, stat purchases, Anvil upgrades, and skills.

### 19.1 Opening Cinematic

- [x] New-run opening (English, before Wave 1): the camera zooms in on the Hero, a dark cloud rolls over the arena, and the Hero speaks in white text in three beats — "Can you protect the World Tree?!", "Can you?", "Are you sure?!" — then the camera eases back to the standard framing and Wave 1 begins. Touch-skippable, deterministic, never shown on Continue. (`gameplay/OpeningCinematic` timeline 7.8 s, camera zoom 0.58 with focus on the Hero, `render/OpeningCinematicRenderer` rolling cloud puffs + white speech; `startNewRun` enters `CINEMATIC` and spawns Wave 1 only when it ends.)
- [x] Opening ships with an on-device touch smoke flow and screen captures (`opening-line-one/three-premium-v2.png`); CI run `34788282541` green.

### 19.2 Polish and Bug Sweep

- [x] Hero level cap raised 100 → 200 with progressive XP costs past 100 (`×1.03` per late level) so waves 101–200 keep granting talent points instead of showing `MAX` for the whole second half; save repair clamps to the new cap (`HeroProgressionSystemTest`, HUD contract test).
- [x] Save repair: any run past wave 100 that is not mid-ceremony now has `secondTreePlanted = true`, so pre-18.4 saves at waves 101+ render the second tree and its siege target (`EdgeProbe` scenario → `GameStateTest`).
- [x] Save repair clamps Anvil `upgradeLevel` to 0..5 and drops null inventory/equipped entries (`GameStateTest`).
- [x] A level gained by the Hero's last shot during the tree siege no longer opens Level-Up over the defeat (guarded on `hero.alive`).
- [x] BUG: the in-game inventory tap never passed `GameSettings`, so the auto-sell chips were inert on device; Anvil forges were also not saved. Fixed, forge now saves + plays the purchase cue, and `AndroidTouchSmokeTest` toggles the COMMON chip by touch.
- [x] Continue on the save written right after New Game (wave 1 never started) replays the opening instead of dropping straight into combat (`HeroDefenseGame.untouchedFirstWave`, `OpeningReplayTest`); showcase saves in the Android smoke test moved to wave 2.
- [x] CI evidence hygiene: the API-35 emulator's Quickstep ANR dialog was overlaying every capture; `scripts/android-touch-test.sh` now sets `hide_error_dialogs` and stops the launcher before the touch run.
- [x] Systematic pass over every screen and flow (menu, HUD, combat, ceremony, opening, level-up, cards, inventory/anvil/auto-sell, shop, pause, settings, defeat/victory): flow transitions re-audited (`GameFlowController`), `EdgeProbe` save scenarios all pass, on-device captures reviewed for every overlay, stale “Ten-level combat skills” shop copy replaced now that skills are endless; every fix above carries its regression test.

### 19.3 Economy-Aware Rebalance

- [x] Rebalance the 1–200 run against the real economy: the simulator now reforges equipped Rare/Legendary items at the Anvil (cheapest step ≤ cheapest shop price) and keeps a coin ledger (`BalanceSimulator.lastLedger()`: kills ≈ 80k, sales ≈ 20k, stats 54k / skills 29k / Anvil 17k on the baseline); with that stronger player the second half was ~1–3% pressure, so waves 101–200 now grow `HP × 1.023^(w−100)`, `damage × 1.008^(w−100)` (HP w200 ≈ 7353, dmg ≈ 0.806). 9/9 and 15/15 seeds finish, avg 8.6%, worst wave 29.6%, longest clear 69 s; forced-card regression passes; `docs/BALANCE.md` gained Anvil + economy-audit sections.

## Standing Rules

- Complete → verify → update this file → commit → push for every checklist item; never batch items.
- Keep only push-able files in the workspace; SDKs, Blender, caches, and helpers belong in `/tmp` or CI.
- Verify every audio license before committing the file (audio unfrozen in Phase 18; CC0 only).
- Review every Blender-rendered batch before accepting it.
- Treat the visual style guide as non-negotiable.
- Use touch/tap/drag everywhere, including automated tests; no keyboard or mouse-only paths.

---

# Hero Defense — Roadmap Addendum (Phases 20–26)

Continues directly from `ROADMAP.md` (Phases 0–19, closed 2026-09-13). Same repository, same `Java + libGDX`, `Android only`, `touch only`, `fully offline`, `no IAP` constraints. Same progress rule: complete → verify → commit → push each checklist item separately.

**Scope of this addendum.** The goal is 50+ hours of genuinely engaging play, more strategic depth, more innovation, and a stronger pull on player curiosity — reached by adding *replay depth*, not by padding wave count or writing more boilerplate. Two things are deliberately kept out of this pass: Cafe Bazaar / store-release preparation (already covered by Phases 15 and 19; nothing here changes it) and any instruction to narrate implementation in code comments — keep comments to the existing repo's habit of one short line only where behavior is non-obvious. Every new system below is designed to reuse already-rendered art, already-built UI patterns, and the already-built `BalanceSimulator` rather than requesting new Blender batches — the intent is the broadest possible change for the smallest possible new-asset footprint.

## Core Specs (Additions)

| Item | Value |
|---|---|
| Replay structure | **Ascension** (New Game+): full run reset, permanent meta-currency carries over |
| Meta-currency | **Heartwood**, earned from peak wave + ascension tier at each Ascension |
| Permanent meta-progression | **Root Network** — a one-time-purchase talent web rendered on the World Tree itself |
| Pre-run choice | **Convergence Trials** — pick 2 of 4 revealed run modifiers before every run |
| New item tier | **Mythic** — exactly 6 (one per equipment slot), unique passive instead of raw stats |
| New combat layer | **Focus meter** → tap-activated Hero Ultimate; **Skill Evolutions** at skill level 10 |
| New difficulty layer | Boss telegraphs, **Elite**-affixed enemies every 7th regular wave, per-tier Ascension scaling |
| Save schema | Bumps to version 2 (`ascensionTier`, `heartwood`, root-node state, active Trials, affixes) |

## Phase 20 — Ascension: The Root Network

The single biggest lever for total playtime: turn the existing 1–200 wave arc into the first loop of an indefinitely repeatable structure instead of a one-time finale.

### 20.1 Ascension Loop

- [x] At Game Over or after clearing Wave 200, offer an **Ascend** action: reset wave, Hero level, coins, inventory, equipped items, and skill levels to a fresh Wave 1 run, but increment a new persistent `GameState.ascensionTier` and award **Heartwood** based on peak wave reached and the ascension tier just completed.
- [x] Each ascension tier permanently raises the `DifficultyCurve` growth constants on a defined schedule (exact numbers in Phase 25.3) so a returning player faces a harder version of the same arc rather than requiring new authored content per tier.
- [x] Bump `GameStateCodec`'s schema to version 2: add `ascensionTier`, `heartwood`, and root-node ids to the save payload, with a repair path defaulting pre-Ascension saves to tier 0 — this also closes the "save format has no version field" gap noted against the shipped build.

### 20.2 The Root Network (permanent talent web)

- [x] Build a Root Network screen that renders the already-modeled World Tree full-screen (reuse `SaplingTreeRenderer`/World Tree art — no new models) with 20–30 selectable root-node overlays laid along the trunk and branches.
- [x] Each node costs Heartwood and grants a small permanent bonus applied at the start of every future run (starting Strength/Health, starting coin, an extra starting talent point, an extra inventory slot, a small Focus-fill bonus). Define values in a new `RootNetworkCatalog`, mirroring `EquipmentDefinition`'s data-table pattern.
- [x] Root nodes are one-time purchases that never reset on Ascension; reuse the existing sapling-growth frame sequence to represent lit (purchased) vs. unlit (locked) nodes, so the tree visibly fills in as the player invests — no new art batch required.
- [x] Add `RootNetworkTouchLayout`/`RootNetworkTouchController` following the existing Shop/Skill pattern; open it from the Main Menu and from the Game Over/Ascend screen.

### 20.3 Ascension-Aware Progression Feel

- [x] Show the current Ascension tier as a small badge next to the wave counter in `HudRenderer`, and on the Game Over/Victory summary.
- [x] Update the Main Menu's Continue tile to show Ascension tier + peak wave, so a five-minute session always opens on a legible sense of long-term progress.

## Phase 21 — Story Codex & Branching Epilogues

Give the world a memory. Reuses the three-beat cinematic text system already built for `OpeningCinematic`; this phase is almost entirely writing plus one new read-only screen.

### 21.1 The Grove Codex

- [ ] Add a `LoreEntry` catalog — pure text plus an unlock condition — of roughly 30 short entries (2–4 sentences each) telling the story of the World Tree, the Hero, and the four enemy archetypes (Rootling, Stonekin, Gloom Wolf, Fungal Brute) from the forest's own perspective.
- [ ] Unlock entries progressively and by different triggers: some by wave milestone, some on a boss's first kill, several only from defeating an Elite-affixed enemy (Phase 25.2), a few only after completing an Ascension — so the Codex fills in from several kinds of play, not just time.
- [ ] Add a Codex screen, reachable from the Main Menu and from Pause, listing locked entries as silhouettes and unlocked entries in full, in the same card layout style as Inventory.

### 21.2 Evolving Opening & Branching Endings

- [ ] Extend `OpeningCinematic`'s three-beat line set to vary with `ascensionTier` — Ascension 0 keeps the shipped lines; Ascension 1+ has the Hero and Tree acknowledge the repeated cycle, so a returning player is narratively addressed, not just mechanically reset to Wave 1.
- [ ] Replace the single Victory/Game Over text with 3–4 short branching epilogues chosen by run outcome (a flawless ascension with no Hero death, a clear that came down to the wire, a Game Over before Wave 50, a Game Over after Wave 150), reusing `GameOverOverlayRenderer`.
- [ ] Wire the opening-line and epilogue selection into the same deterministic save/replay path already covering the opening (`OpeningReplayTest`) so Continue never replays the wrong variant.

## Phase 22 — Convergence Trials (pre-run drafting)

A genuine strategic decision before each run that changes *how* it is played, not just how strong the Hero eventually gets — the "make it a bit more thoughtful" ask.

### 22.1 Trial Cards

- [ ] Before every new run — New Game and every Ascension — show 4 Trial cards and let the player pick exactly 2, reusing `RewardCardOverlayRenderer`'s existing card-choice presentation.
- [ ] Define roughly 12 Trials as paired risk/reward modifiers active for that run only, for example: enemies move faster in exchange for more coin income; no potions drop in exchange for extra talent points; bosses hit harder in exchange for a guaranteed Rare+ card every boss; Elites appear twice as often in exchange for bonus Heartwood at Ascension.
- [ ] Persist the two active Trials in `GameState` for the run's duration and show them as small, permanent icons on the live HUD, so their effect is never a mid-run surprise.
- [ ] Feed the active Trial pair into `BalanceSimulator` as an additional scenario axis (Phase 26.1) so no combination of Trials breaks the difficulty gate.

### 22.2 Curiosity Hooks

- [ ] Keep two Trials locked until specific Codex or Ascension conditions are met, so the drafting pool itself is something to discover, not a static menu seen in full on day one.

## Phase 23 — Itemization Depth: Affixes, Sets, and the Mythic Tier

Give players build decisions worth thinking about without redrawing the 40-item roster.

### 23.1 Affixes

- [ ] Roll one random minor affix (from roughly 15 possibilities — extra crit chance, extra lifesteal, extra coin-on-kill, and similar) onto every Rare and Legendary drop, stored as `Item.affixId` alongside its existing tier bonus. Common and Uncommon stay affix-free so early loot decisions stay simple.
- [ ] Show the affix line distinctly in `InventoryItemDetails`, below the tier's base stat bonuses.
- [ ] Extend `ItemForgeSystem` so an Anvil reforge has a small, forge-level-scaling chance to reroll an item's affix instead of adding a stat step — a second late-game coin sink with its own gambling hook.

### 23.2 Set Items

- [ ] Group 8 of the existing 40 items into two 4-piece sets (reusing existing art, e.g. the Verdant Covenant pieces already in the catalog) that grant a bonus at 2 and 4 equipped pieces — for example +5% attack speed at 2, an extra Chain Lightning target at 4 — via a new `EquipmentSetBonus` table keyed off a new `EquipmentDefinition.setId`.
- [ ] Surface active/partial set status in Inventory ("2/4 Verdant Covenant equipped") so the incentive to hunt down the rest of a set is visible while playing, not just in a wiki.

### 23.3 Mythic Tier & Escalating Presentation

- [ ] Add a fifth tier, Mythic, above Legendary: exactly one per equipment slot (6 total), each carrying a build-defining unique passive instead of raw stats — Chain Lightning also applies Stun, auto-potions also grant a few seconds of bonus lifesteal, a critical hit refunds part of the shot's cooldown, and similar.
- [ ] Make the Mythic drop rate near-zero from regular kills, but guaranteed once per Ascension tier on that tier's Wave 200 clear — the first Mythic becomes a memorable milestone rather than another slot-machine spin.
- [ ] Render Mythic items by reusing the existing Legendary mesh/material variants with one new, distinct particle-glow tier in `RarityGlowRenderer`/`VisualRarity` — a shader/color change, not a new Blender batch.
- [ ] Let the Hero's arrow trail and bow glow escalate visually with Anvil forge level and Ascension tier (color/intensity ramps already available to the existing glow and trail renderers), so raw progression is readable on screen without any new geometry.

## Phase 24 — Active Play: Focus Meter and the Ultimate Ability

Answers the "auto-attack only, no agency" gap directly with one meaningful tap-timed decision per fight, without breaking the fixed-Hero, no-dodge-input design the game is built around.

### 24.1 Focus Meter

- [ ] Add a `Focus` resource that fills from landed hits, shown as a ring around the Hero using the same HUD-bar rendering approach already built for the EXP bar.
- [ ] At full Focus, show a glowing tap target; tapping it unleashes the Hero's Ultimate — a screen-wide effect assembled from existing VFX systems (chain-beam fan, an enlarged critical burst, a stronger screen shake at a higher, rate-limited budget) — and drains Focus to zero.
- [ ] Scale Ultimate strength and Focus-fill rate with Hero level and any equipped Mythic passives, so building toward a strong Ultimate is itself a stat-allocation decision, not a fixed script.

### 24.2 Skill Evolutions

- [ ] At `SkillId.CORE_LEVELS` (level 10), let the player choose one of two Evolutions per skill instead of continuing the flat endless curve — Chain Lightning evolves into either "Storm Chain" (always hits 3 targets, chance to stun) or "Vampiric Chain" (arcs heal the Hero for a share of the damage dealt), and similarly for the other four skills.
- [ ] Make each Evolution a one-time coin-gated choice per skill per run, resetting on Ascension along with the rest of the skill shop, so there is a real build fork rather than one optimal endless-purchase order.

## Phase 25 — Difficulty Overhaul: Telegraphs, Elites, and Endless Ascension Scaling

Directly answers the flat, "easy once you open the shop" curve: boss hits are currently applied before their animation finishes, the middle third of the run barely escalates, and there is nothing beyond Wave 200 to get harder against.

### 25.1 Boss Telegraphs

- [ ] Move `BossSpecialAttackSystem`'s damage application from the start of the attack to the end of `specialAnimationSeconds`, and draw a readable, boss-color-matched ground warning for that whole window, so potion timing and positioning near a special actually matter.

### 25.2 Elite Affixes

- [ ] Every 7th non-boss wave, mark 1–2 spawned enemies as Elite: a larger silhouette scale, a distinct outline color (reusing `RarityGlowRenderer`), one random affix from a small pool (explodes on death, periodically shields, leaves a damaging trail), and roughly 3× HP / 1.5× damage relative to a regular enemy that wave.
- [ ] Guarantee at least a Rare-tier drop from every Elite kill, and make Elites the primary source of the Phase 21.1 lore entries that are gated behind them — tying the hardest optional fights directly to the story hook.

### 25.3 Endless Ascension Scaling

- [ ] Define an explicit per-tier schedule for the growth constants as a function of `ascensionTier` (`t`), starting from a tunable form such as `ENEMY_HEALTH_GROWTH(t) = 1.037 × (1 + 0.015·t)` and `ENEMY_DAMAGE_GROWTH(t) = 1.003 × (1 + 0.008·t)` for the first half, with the same relative bump applied to the second-half constants, plus the Elite wave interval tightening by one wave every three tiers (floor of every 4th wave) — then tune against the simulator exactly as Phase 14 did for the base curve.
- [ ] Close the flat middle-third the shipped build has (waves 25–80 landing at nearly the same damage fraction as each other): add a slow third growth segment across that span so pressure rises end to end instead of only at the two endpoints, and re-verify against the existing 5–15% average / 35% single-wave gate.

## Phase 26 — Comprehensive Rebalancing & Hours Accounting

Extends the existing simulator-driven balance discipline to every new system above, and writes down the arithmetic behind the 50-hour target so it can be checked against the shipped numbers, not just claimed.

### 26.1 Simulator Extensions

- [ ] Extend `BalanceSimulator` with an `ascensionTier` parameter and an active-Trial-pair axis; re-run the existing 9-seed-plus-forced-card regression gate at ascension tiers 0, 3, 6, and 10.
- [ ] Add an Elite-affix-aware damage accounting path so Elite waves are included in the 5–15% average / 35% single-wave gross-damage ceiling rather than exempted from it.
- [ ] Add a Focus/Ultimate usage model to the simulator's policy (fire the Ultimate on cooldown) so its power budget is tuned against the same regression gate as every other system, and give the simulator a simple Evolution-choice policy (pick the higher-DPS Evolution) for the same reason.

### 26.2 Manual Balance Guidance

- [ ] Repeat the Phase 14.6-style manual checkpoints at Ascension tiers 0, 5, and 10, recording felt difficulty rather than only the automated gate's numbers.
- [ ] Record a target session model in `docs/BALANCE.md`: how long one Wave 1–200 run takes at a defined "engaged, shopping, no idle time" pace, and require every ascension tier's run to land within roughly ±20% of that time even as it gets harder — so added challenge comes from build precision, not from quietly padding wave count.

### 26.3 The Hours Table

- [ ] Add a table (`docs/BALANCE.md` or a new `docs/PROGRESSION_HOURS.md`) deriving expected total playtime from the shipped numbers: one full Wave 1–200 clear, Root Network node cost versus Heartwood income per ascension, the number of ascensions needed to exhaust the Root Network, Codex completion pace across the unlock triggers in Phase 21.1, and Mythic-item collection pace — so the 50-hour target is an equation the team can re-check after every later balance pass, not a one-time estimate.

## Standing Rules (additions)

- Every new system above must reuse existing rendered art, shaders, or UI layout patterns unless a checklist item explicitly says otherwise — no new Blender batch is authorized by this addendum.
- Keep code comments to the existing repository's habit: one short line only where behavior is genuinely non-obvious. Do not narrate implementation step-by-step in comments.
- Every new numeric system (Ascension scaling, Trials, affixes, Elites, Focus/Ultimate, skill Evolutions) must pass through `BalanceSimulator`'s regression gate before being considered done, exactly like every Phase 14–19 system before it.
- This addendum does not touch Cafe Bazaar/release packaging; Phases 15 and 19's release state is unchanged.

---

## Appendix — Story Content (Phases 20, 21, 23, 25)

Full narrative text wired to the systems above. Two voices: Hero (white, terse, present-tense) and Tree (leaf-green, reflective, Codex only).

### World Premise

Long before the first wave, something did not grow here — it fell here. The Hollow is that unmaking's name — four bosses are its four ways of touching the world: stone (Golem), root/thorn (Matriarch), fire (Wyrm), shadow (Void Knight). World Tree is the one root never swallowed.

### 1. Opening Cinematics by Ascension Tier

- Tier 0 (shipped): "Can you protect the World Tree?!" / "Can you?" / "Are you sure?!"
- Tier 1: "Again, the dark comes." / "Again, I stand." / "This time — further."
- Tier 2: "The Hollow remembers me now." / "Good. Let it be afraid." / "Roots first. Then flesh. Then the Tree. Not today."
- Tier 3+: "Another dawn. Another siege." / "The Tree does not ask twice." / "Neither do I."

### 2. Mid-Run Story Beats

- Boss first-encounter title cards (once per identity)
- Reflection lines: Wave 25, 50, 75, 125, 150, 175
- Wave 100 Planting Ceremony 5 lines synced to timeline
- Wave 200 Ascension transition 2 lines

(See full story content document for exact wording — to be wired in Phase 21.2)

### 3-8. Codex, Epilogues, Mythic Flavor

30 Codex entries, 5 epilogues (Flawless/Hard-Fought/Early/Middle/Late Fall), 6 Mythic flavor passives, Elite Whispering Wounds fragments — full text in `docs/STORY_CONTENT.md` (to be added).

## Standing Rules (final)

- Complete → verify → update this file → commit → push for every checklist item; never batch items.
- Keep only push-able files in the workspace; SDKs, Blender, caches, and helpers belong in `/tmp` or CI.
- Verify every audio license before committing the file (audio unfrozen in Phase 18; CC0 only).
- Review every Blender-rendered batch before accepting it.
- Treat the visual style guide as non-negotiable.
- Use touch/tap/drag everywhere, including automated tests; no keyboard or mouse-only paths.

