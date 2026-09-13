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

## Standing Rules

- Complete → verify → update this file → commit → push for every checklist item; never batch items.
- Keep only push-able files in the workspace; SDKs, Blender, caches, and helpers belong in `/tmp` or CI.
- Verify every audio license before committing the file.
- Review every Blender-rendered batch before accepting it.
- Treat the visual style guide as non-negotiable.
- Use touch/tap/drag everywhere, including automated tests; no keyboard or mouse-only paths.
