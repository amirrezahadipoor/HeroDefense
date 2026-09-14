package com.amirrezahadipoor.herodefense.model;

import com.amirrezahadipoor.herodefense.WorldLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Complete serializable state for one continuous Hero Defense run. */
public final class GameState {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final int FINAL_WAVE = 200;
    /** Clearing this wave (and its boss reward) triggers the planting ceremony. */
    public static final int PLANTING_WAVE = 100;
    /** Mirrors {@code HeroProgressionSystem.LEVEL_CAP}; kept here so save repair has no gameplay dependency. */
    public static final int MAX_HERO_LEVEL = 200;
    /** Mirrors {@code ItemForgeSystem.MAX_UPGRADE}. */
    public static final int MAX_ITEM_UPGRADE = 5;
    /** Seconds the monsters spend tearing down the trees after the Hero falls. */
    public static final float TREE_SIEGE_SECONDS = 3.2f;
    public static final float ARENA_CENTER_X = WorldLayout.HERO_CENTER_X;
    public static final float ARENA_CENTER_Y = WorldLayout.HERO_CENTER_Y;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public long runSeed;
    /** Persisted xorshift state keeps combat rolls deterministic across save/load. */
    public long combatRandomState;
    public int waveNumber = 1;
    public int coins;
    public int heroLevel = 1;
    public int heroExperience;
    public int unspentTalentPoints;
    public int defeatedBosses;
    public int totalKills;
    public int totalKillCoinsEarned;
    public float worldTreeHealth = 1000f;
    public float worldTreeMaxHealth = 1000f;
    public float simulationSpeed = 1f;
    public boolean waveActive;
    public boolean starterLoadoutGranted;
    public boolean awaitingBossReward;
    public int pendingRewardBossNumber;
    public boolean runComplete;
    /** Set when Wave 100 is cleared; cleared once the ceremony has played (or been skipped). */
    public boolean ceremonyPending;
    /** True once the second Heartwood stands; it is a monument, never a second loss condition. */
    public boolean secondTreePlanted;
    /** Counts down after the Hero dies while monsters destroy the trees; 0 = trees are gone. */
    public float treeSiegeRemainingSeconds;
    public long nextEntityId = 2L;

    // --- Ascension (Phase 20) ---
    public int ascensionTier;
    public int heartwood;
    public int peakWaveReached = 1;
    public boolean heroDiedThisRun;
    public int potionsUsedThisRun;
    public float fastestWaveClearSeconds = Float.MAX_VALUE;
    public float longestPauseSeconds;
    public int shopStatsBoughtThisRun;
    public float focus;
    public float focusMax = 100f;

    // Meta-progression
    public Map<String, Boolean> rootNodesPurchased = new LinkedHashMap<>();
    public Map<String, Boolean> codexUnlocked = new LinkedHashMap<>();
    public Map<String, Integer> eliteKillCounts = new LinkedHashMap<>();
    public Map<String, Boolean> firstBossKills = new LinkedHashMap<>();
    /** Idle-whisper ids already shown once; the pool never repeats (§8). */
    public Map<String, Boolean> usedWhisperIds = new LinkedHashMap<>();
    public Map<String, String> skillEvolutions = new LinkedHashMap<>();
    public List<String> activeTrials = new ArrayList<>();
    public Map<String, Boolean> trialUnlocked = new LinkedHashMap<>();

    // Run stats for secret codex entries
    public boolean bareHandedEligible = true;
    public boolean noPotionRun = true;
    public int totalRunsCompleted;
    public int totalAscensionsCompleted;
    /** Times any run has advanced into wave 200; never reset (feeds codex entry 30). */
    public int wave200ReachedCount;
    /** Simulated combat seconds in the current wave (feeds the Fastest Fall secret). */
    public float waveElapsedSeconds;

    public Hero hero = new Hero(1L, ARENA_CENTER_X, ARENA_CENTER_Y);
    public List<Enemy> aliveEnemies = new ArrayList<>();
    public List<Boss> aliveBosses = new ArrayList<>();
    public List<Projectile> projectiles = new ArrayList<>();
    public List<DropEntity> drops = new ArrayList<>();
    public List<Item> inventory = new ArrayList<>();
    public Map<String, Item> equippedItems = new LinkedHashMap<>();
    public Map<String, Float> permanentEffects = new LinkedHashMap<>();
    public Map<String, Integer> shopUpgradeLevels = new LinkedHashMap<>();
    /** Purchased skill levels keyed by {@code SkillId.saveKey()}; absent means level 0. */
    public Map<String, Integer> skillLevels = new LinkedHashMap<>();
    /** Boss number encoded as a string key for stable JSON object-key round trips. */
    public Map<String, String> chosenRewardCards = new LinkedHashMap<>();
    public List<String> pendingRewardCards = new ArrayList<>();
    public List<Integer> healthPotions = new ArrayList<>();

    public GameState() {
        ensurePotionSlots();
    }

    public static GameState newRun(long seed) {
        GameState state = new GameState();
        state.runSeed = seed;
        state.combatRandomState = initialRandomState(seed);
        state.validateAndRepair();
        return state;
    }

    public long allocateEntityId() {
        return nextEntityId++;
    }

    /** Returns a deterministic uniform combat roll in [0, 1) and advances saved state. */
    public float nextCombatRandomFloat() {
        long value = combatRandomState;
        if (value == 0L) {
            value = initialRandomState(runSeed);
        }
        value ^= value << 13;
        value ^= value >>> 7;
        value ^= value << 17;
        combatRandomState = value;
        return (value >>> 40) / 16_777_216f;
    }

    public void destroyWorldTree() {
        worldTreeHealth = 0f;
    }

    /** Enforces the stationary-defender rule every simulation tick. */
    public void anchorHeroAtArenaCenter() {
        if (hero != null) {
            hero.keepAt(ARENA_CENTER_X, ARENA_CENTER_Y);
        }
    }

    public int livingEnemyCount() {
        int count = 0;
        for (Enemy enemy : aliveEnemies) {
            if (enemy != null && enemy.alive && !enemy.silentWatcher) {
                count++;
            }
        }
        for (Boss boss : aliveBosses) {
            if (boss != null && boss.alive) {
                count++;
            }
        }
        return count;
    }

    /** Repairs safe defaults after loading an older or partially written save. */
    public void validateAndRepair() {
        schemaVersion = CURRENT_SCHEMA_VERSION;
        waveNumber = Math.max(1, Math.min(FINAL_WAVE, waveNumber));
        heroLevel = Math.max(1, Math.min(MAX_HERO_LEVEL, heroLevel));
        coins = Math.max(0, coins);
        heroExperience = Math.max(0, heroExperience);
        unspentTalentPoints = Math.max(0, unspentTalentPoints);
        totalKills = Math.max(0, totalKills);
        totalKillCoinsEarned = Math.max(0, totalKillCoinsEarned);
        simulationSpeed = simulationSpeed == 2f || simulationSpeed == 3f ? simulationSpeed : 1f;
        if (combatRandomState == 0L) {
            combatRandomState = initialRandomState(runSeed);
        }
        if (hero == null) {
            hero = new Hero(1L, ARENA_CENTER_X, ARENA_CENTER_Y);
        }
        hero.keepAt(ARENA_CENTER_X, ARENA_CENTER_Y);
        hero.validateAndRepair();
        worldTreeMaxHealth = Math.max(1f, worldTreeMaxHealth);
        worldTreeHealth = Math.max(0f, Math.min(worldTreeMaxHealth, worldTreeHealth));
        treeSiegeRemainingSeconds = Float.isFinite(treeSiegeRemainingSeconds)
            ? Math.max(0f, Math.min(TREE_SIEGE_SECONDS, treeSiegeRemainingSeconds)) : 0f;
        if (!hero.alive && treeSiegeRemainingSeconds <= 0f) {
            destroyWorldTree();
        }
        if (hero.alive) treeSiegeRemainingSeconds = 0f;
        if (waveNumber <= PLANTING_WAVE) {
            ceremonyPending = false;
            secondTreePlanted = false;
        } else if (!ceremonyPending) {
            secondTreePlanted = true;
        }
        if (ceremonyPending) waveActive = false;
        if (aliveEnemies == null) aliveEnemies = new ArrayList<>();
        if (aliveBosses == null) aliveBosses = new ArrayList<>();
        if (livingEnemyCount() > 0) waveActive = true;
        if (runComplete) waveActive = false;
        if (projectiles == null) projectiles = new ArrayList<>();
        if (drops == null) drops = new ArrayList<>();
        if (inventory == null) inventory = new ArrayList<>();
        if (equippedItems == null) equippedItems = new LinkedHashMap<>();
        inventory.removeIf(item -> item == null);
        equippedItems.values().removeIf(item -> item == null);
        for (Item item : inventory) item.upgradeLevel = Math.max(0, Math.min(MAX_ITEM_UPGRADE, item.upgradeLevel));
        for (Item item : equippedItems.values()) item.upgradeLevel = Math.max(0, Math.min(MAX_ITEM_UPGRADE, item.upgradeLevel));
        synchronizeEquipmentHealth();
        if (permanentEffects == null) permanentEffects = new LinkedHashMap<>();
        if (shopUpgradeLevels == null) shopUpgradeLevels = new LinkedHashMap<>();
        shopUpgradeLevels.replaceAll((key, value) -> value == null ? 0 : Math.max(0, value));
        if (skillLevels == null) skillLevels = new LinkedHashMap<>();
        skillLevels.replaceAll((key, value) -> value == null ? 0 : Math.max(0, value));
        if (chosenRewardCards == null) chosenRewardCards = new LinkedHashMap<>();
        if (pendingRewardCards == null) pendingRewardCards = new ArrayList<>();
        if (pendingRewardCards.size() != 3) {
            pendingRewardCards.clear();
            awaitingBossReward = false;
            pendingRewardBossNumber = 0;
        }
        if (awaitingBossReward) waveActive = false;
        ensurePotionSlots();
        nextEntityId = Math.max(2L, nextEntityId);

        // --- Ascension fields (Phase 20) ---
        ascensionTier = Math.max(0, ascensionTier);
        heartwood = Math.max(0, heartwood);
        peakWaveReached = Math.max(1, Math.min(FINAL_WAVE, peakWaveReached));
        if (waveNumber > peakWaveReached) peakWaveReached = waveNumber;
        potionsUsedThisRun = Math.max(0, potionsUsedThisRun);
        fastestWaveClearSeconds = Float.isFinite(fastestWaveClearSeconds) && fastestWaveClearSeconds > 0f
            ? fastestWaveClearSeconds : Float.MAX_VALUE;
        longestPauseSeconds = Float.isFinite(longestPauseSeconds) ? Math.max(0f, longestPauseSeconds) : 0f;
        shopStatsBoughtThisRun = Math.max(0, shopStatsBoughtThisRun);
        focus = Float.isFinite(focus) ? Math.max(0f, Math.min(focusMax, focus)) : 0f;
        focusMax = Float.isFinite(focusMax) && focusMax > 0f ? focusMax : 100f;
        totalRunsCompleted = Math.max(0, totalRunsCompleted);
        totalAscensionsCompleted = Math.max(0, totalAscensionsCompleted);
        wave200ReachedCount = Math.max(0, wave200ReachedCount);
        waveElapsedSeconds = Float.isFinite(waveElapsedSeconds) ? Math.max(0f, waveElapsedSeconds) : 0f;
        if (rootNodesPurchased == null) rootNodesPurchased = new LinkedHashMap<>();
        if (codexUnlocked == null) codexUnlocked = new LinkedHashMap<>();
        if (eliteKillCounts == null) eliteKillCounts = new LinkedHashMap<>();
        if (firstBossKills == null) firstBossKills = new LinkedHashMap<>();
        if (usedWhisperIds == null) usedWhisperIds = new LinkedHashMap<>();
        if (skillEvolutions == null) skillEvolutions = new LinkedHashMap<>();
        if (activeTrials == null) activeTrials = new ArrayList<>();
        if (trialUnlocked == null) trialUnlocked = new LinkedHashMap<>();
        rootNodesPurchased.values().removeIf(v -> v == null);
        codexUnlocked.values().removeIf(v -> v == null);
        firstBossKills.values().removeIf(v -> v == null);
        usedWhisperIds.values().removeIf(v -> v == null);
        skillEvolutions.values().removeIf(v -> v == null);
        trialUnlocked.values().removeIf(v -> v == null);
        activeTrials.removeIf(t -> t == null);
        eliteKillCounts.replaceAll((k, v) -> v == null ? 0 : Math.max(0, v));
    }

    public static int calculateHeartwoodReward(int peakWave, int ascensionTier, boolean flawless) {
        int base = peakWave / 5;
        if (peakWave >= FINAL_WAVE) base += 50;
        base += ascensionTier * 10;
        if (flawless) base += 20;
        return Math.max(0, base);
    }

    public void recordWaveReached(int wave) {
        if (wave > peakWaveReached) peakWaveReached = wave;
        if (wave > waveNumber) waveNumber = wave;
    }

    public void resetForNewRun(long newSeed) {
        // Keep meta-progression
        int keptTier = ascensionTier;
        int keptHeartwood = heartwood;
        Map<String, Boolean> keptRoots = new LinkedHashMap<>(rootNodesPurchased);
        Map<String, Boolean> keptCodex = new LinkedHashMap<>(codexUnlocked);
        Map<String, Integer> keptElite = new LinkedHashMap<>(eliteKillCounts);
        Map<String, Boolean> keptFirstBoss = new LinkedHashMap<>(firstBossKills);
        Map<String, Boolean> keptWhispers = new LinkedHashMap<>(usedWhisperIds);
        Map<String, Boolean> keptTrialsUnlocked = new LinkedHashMap<>(trialUnlocked);
        int keptRuns = totalRunsCompleted;
        int keptAscensions = totalAscensionsCompleted;

        // Full reset to fresh run
        GameState fresh = newRun(newSeed);
        fresh.ascensionTier = keptTier;
        fresh.heartwood = keptHeartwood;
        fresh.rootNodesPurchased = keptRoots;
        fresh.codexUnlocked = keptCodex;
        fresh.eliteKillCounts = keptElite;
        fresh.firstBossKills = keptFirstBoss;
        fresh.usedWhisperIds = keptWhispers;
        fresh.trialUnlocked = keptTrialsUnlocked;
        fresh.totalRunsCompleted = keptRuns;
        fresh.totalAscensionsCompleted = keptAscensions;
        fresh.peakWaveReached = 1;

        // Copy fresh into this
        this.runSeed = fresh.runSeed;
        this.combatRandomState = fresh.combatRandomState;
        this.waveNumber = 1;
        this.coins = 0;
        this.heroLevel = 1;
        this.heroExperience = 0;
        this.unspentTalentPoints = 0;
        this.defeatedBosses = 0;
        this.totalKills = 0;
        this.totalKillCoinsEarned = 0;
        this.worldTreeHealth = worldTreeMaxHealth;
        this.waveActive = false;
        this.starterLoadoutGranted = false;
        this.awaitingBossReward = false;
        this.pendingRewardBossNumber = 0;
        this.runComplete = false;
        this.ceremonyPending = false;
        this.secondTreePlanted = false;
        this.treeSiegeRemainingSeconds = 0f;
        this.heroDiedThisRun = false;
        this.potionsUsedThisRun = 0;
        this.fastestWaveClearSeconds = Float.MAX_VALUE;
        this.longestPauseSeconds = 0f;
        this.shopStatsBoughtThisRun = 0;
        this.bareHandedEligible = true;
        this.noPotionRun = true;
        this.waveElapsedSeconds = 0f;
        this.focus = 0f;
        this.hero = fresh.hero;
        this.aliveEnemies = fresh.aliveEnemies;
        this.aliveBosses = fresh.aliveBosses;
        this.projectiles = fresh.projectiles;
        this.drops = fresh.drops;
        this.inventory = fresh.inventory;
        this.equippedItems = fresh.equippedItems;
        this.permanentEffects = fresh.permanentEffects;
        this.shopUpgradeLevels = fresh.shopUpgradeLevels;
        this.skillLevels = fresh.skillLevels;
        this.chosenRewardCards = fresh.chosenRewardCards;
        this.pendingRewardCards = fresh.pendingRewardCards;
        this.healthPotions = fresh.healthPotions;
        this.activeTrials = fresh.activeTrials;
        this.skillEvolutions = fresh.skillEvolutions;
        this.nextEntityId = 2L;
        validateAndRepair();
    }

    public int ascendAndAwardHeartwood() {
        boolean flawless = !heroDiedThisRun;
        int earned = calculateHeartwoodReward(peakWaveReached, ascensionTier, flawless);
        heartwood += earned;
        ascensionTier++;
        totalAscensionsCompleted++;
        totalRunsCompleted++;
        return earned;
    }

    private static long initialRandomState(long seed) {
        long mixed = seed + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return mixed == 0L ? 0xD1B54A32D192ED03L : mixed;
    }

    private void synchronizeEquipmentHealth() {
        int bonusPoints = 0;
        for (Item item : equippedItems.values()) {
            if (item == null || item.statBonuses == null) continue;
            Float bonus = item.statBonuses.get(HeroStat.HEALTH.name());
            if (bonus != null && bonus > 0f) bonusPoints += Math.round(bonus);
        }
        hero.maxHealth = hero.stats.maxHealth() + bonusPoints * HeroStats.MAX_HEALTH_PER_POINT;
        hero.health = Math.max(0f, Math.min(hero.maxHealth, hero.health));
    }

    private void ensurePotionSlots() {
        if (healthPotions == null) {
            healthPotions = new ArrayList<>();
        }
        while (healthPotions.size() < 6) {
            healthPotions.add(0);
        }
        while (healthPotions.size() > 6) {
            healthPotions.remove(healthPotions.size() - 1);
        }
        for (int i = 0; i < healthPotions.size(); i++) {
            healthPotions.set(i, Math.max(0, healthPotions.get(i)));
        }
    }
}
