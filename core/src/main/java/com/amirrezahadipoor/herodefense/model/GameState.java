package com.amirrezahadipoor.herodefense.model;

import com.amirrezahadipoor.herodefense.WorldLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Complete serializable state for one continuous Hero Defense run. */
public final class GameState {
    public static final int CURRENT_SCHEMA_VERSION = 1;
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
            if (enemy != null && enemy.alive) {
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
            // Any run already past the planting wave has its second tree standing.
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
