package com.amirrezahadipoor.herodefense.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Complete serializable state for one continuous Hero Defense run. */
public final class GameState {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int FINAL_WAVE = 100;
    public static final float ARENA_CENTER_X = 360f;
    public static final float ARENA_CENTER_Y = 600f;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public long runSeed;
    public int waveNumber = 1;
    public int coins;
    public int heroLevel = 1;
    public int heroExperience;
    public int unspentTalentPoints;
    public int defeatedBosses;
    public float worldTreeHealth = 1000f;
    public float worldTreeMaxHealth = 1000f;
    public float simulationSpeed = 1f;
    public boolean runComplete;
    public long nextEntityId = 2L;

    public Hero hero = new Hero(1L, ARENA_CENTER_X, ARENA_CENTER_Y);
    public List<Enemy> aliveEnemies = new ArrayList<>();
    public List<Boss> aliveBosses = new ArrayList<>();
    public List<Projectile> projectiles = new ArrayList<>();
    public List<DropEntity> drops = new ArrayList<>();
    public List<Item> inventory = new ArrayList<>();
    public Map<String, Item> equippedItems = new LinkedHashMap<>();
    public Map<String, Float> permanentEffects = new LinkedHashMap<>();
    public Map<Integer, String> chosenRewardCards = new LinkedHashMap<>();
    public List<Integer> healthPotions = new ArrayList<>();

    public GameState() {
        ensurePotionSlots();
    }

    public static GameState newRun(long seed) {
        GameState state = new GameState();
        state.runSeed = seed;
        state.validateAndRepair();
        return state;
    }

    public long allocateEntityId() {
        return nextEntityId++;
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
        heroLevel = Math.max(1, Math.min(100, heroLevel));
        coins = Math.max(0, coins);
        heroExperience = Math.max(0, heroExperience);
        unspentTalentPoints = Math.max(0, unspentTalentPoints);
        simulationSpeed = simulationSpeed == 2f || simulationSpeed == 3f ? simulationSpeed : 1f;
        if (hero == null) {
            hero = new Hero(1L, ARENA_CENTER_X, ARENA_CENTER_Y);
        }
        hero.keepAt(ARENA_CENTER_X, ARENA_CENTER_Y);
        hero.maxHealth = Math.max(1f, hero.maxHealth);
        hero.health = Math.max(0f, Math.min(hero.maxHealth, hero.health));
        if (aliveEnemies == null) aliveEnemies = new ArrayList<>();
        if (aliveBosses == null) aliveBosses = new ArrayList<>();
        if (projectiles == null) projectiles = new ArrayList<>();
        if (drops == null) drops = new ArrayList<>();
        if (inventory == null) inventory = new ArrayList<>();
        if (equippedItems == null) equippedItems = new LinkedHashMap<>();
        if (permanentEffects == null) permanentEffects = new LinkedHashMap<>();
        if (chosenRewardCards == null) chosenRewardCards = new LinkedHashMap<>();
        ensurePotionSlots();
        nextEntityId = Math.max(2L, nextEntityId);
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
