package com.amirrezahadipoor.herodefense.balance;

import com.amirrezahadipoor.herodefense.gameplay.BossFactory;
import com.amirrezahadipoor.herodefense.gameplay.BossSpecialAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.BossWaveSpawner;
import com.amirrezahadipoor.herodefense.gameplay.ContinuousWaveRun;
import com.amirrezahadipoor.herodefense.gameplay.DropPickupSystem;
import com.amirrezahadipoor.herodefense.gameplay.EnemyFactory;
import com.amirrezahadipoor.herodefense.gameplay.EnemyMeleeAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.EnemyMovementSystem;
import com.amirrezahadipoor.herodefense.gameplay.EnemyWaveSpawner;
import com.amirrezahadipoor.herodefense.gameplay.HeroAutoAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroDamageSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroProgressionSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroStatCalculator;
import com.amirrezahadipoor.herodefense.gameplay.InventoryEquipmentSystem;
import com.amirrezahadipoor.herodefense.gameplay.ItemDropSystem;
import com.amirrezahadipoor.herodefense.gameplay.KillRewardResult;
import com.amirrezahadipoor.herodefense.gameplay.KillRewardSystem;
import com.amirrezahadipoor.herodefense.gameplay.WaveCompletion;
import com.amirrezahadipoor.herodefense.gameplay.WaveLifecycleSystem;
import com.amirrezahadipoor.herodefense.items.EquipmentDefinition;
import com.amirrezahadipoor.herodefense.items.StarterLoadoutSystem;
import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.potions.AutoPotionSystem;
import com.amirrezahadipoor.herodefense.potions.HealthPotionSystem;
import com.amirrezahadipoor.herodefense.potions.PotionDropSystem;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.rewards.RewardCardId;
import com.amirrezahadipoor.herodefense.shop.StatShopSystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Runs the real combat/economy systems without libGDX rendering or Android dependencies. */
public final class BalanceSimulator {
    public static final float STEP_SECONDS = 1f / 30f;
    public static final float MAX_SECONDS_PER_WAVE = 300f;

    private static final HeroStat[] BALANCED_STATS = {
        HeroStat.HEALTH,
        HeroStat.STRENGTH,
        HeroStat.AGILITY,
        HeroStat.DODGE,
        HeroStat.LUCK
    };

    private final HeroStatCalculator stats = new HeroStatCalculator();
    private final HeroProgressionSystem progression = new HeroProgressionSystem();
    private final HeroDamageSystem heroDamage = new HeroDamageSystem(stats);
    private final EnemyMovementSystem movement = new EnemyMovementSystem();
    private final HeroAutoAttackSystem heroAttack = new HeroAutoAttackSystem(stats);
    private final BossSpecialAttackSystem bossSpecials = new BossSpecialAttackSystem(heroDamage);
    private final EnemyMeleeAttackSystem melee = new EnemyMeleeAttackSystem(heroDamage);
    private final AutoPotionSystem autoPotion = new AutoPotionSystem(new HealthPotionSystem());
    private final ItemDropSystem itemDrops = new ItemDropSystem(stats);
    private final PotionDropSystem potionDrops = new PotionDropSystem();
    private final KillRewardSystem killRewards = new KillRewardSystem(progression);
    private final DropPickupSystem pickups = new DropPickupSystem();
    private final InventoryEquipmentSystem equipment = new InventoryEquipmentSystem(stats);
    private final BossRewardCardSystem rewardCards = new BossRewardCardSystem();
    private final StatShopSystem shop = new StatShopSystem();
    private final WaveLifecycleSystem waves = new WaveLifecycleSystem(
        new EnemyWaveSpawner(new EnemyFactory()),
        new BossWaveSpawner(new BossFactory()),
        rewardCards,
        new ContinuousWaveRun()
    );

    public BalanceReport run(long seed) {
        GameState state = GameState.newRun(seed);
        new StarterLoadoutSystem().provisionOnce(state);
        allocateTalentPoints(state);
        buyBalancedShopUpgrades(state);
        waves.startCurrentWave(state);

        List<WaveSample> samples = new ArrayList<>(GameState.FINAL_WAVE);
        while (state.hero.alive && !state.runComplete && samples.size() < GameState.FINAL_WAVE) {
            int wave = state.waveNumber;
            improveEquipmentAndSellSpareItems(state);
            allocateTalentPoints(state);
            buyBalancedShopUpgrades(state);
            synchronizeHeroMaximumHealth(state);

            float startingHealth = state.hero.health;
            float startingMaxHealth = state.hero.maxHealth;
            float enemyHealth = totalLivingEnemyHealth(state);
            float dpsToHpRatio = expectedHeroDps(state) / Math.max(1f, enemyHealth);
            float elapsed = 0f;
            float grossDamageTaken = 0f;
            boolean timedOut = false;

            while (state.hero.alive && !state.runComplete && state.waveNumber == wave) {
                if (elapsed >= MAX_SECONDS_PER_WAVE) {
                    timedOut = true;
                    break;
                }
                state.anchorHeroAtArenaCenter();
                movement.update(state, STEP_SECONDS);
                heroAttack.update(state, STEP_SECONDS);

                float healthBeforeEnemyAttacks = state.hero.health;
                bossSpecials.update(state, STEP_SECONDS);
                boolean gameOver = melee.update(state, STEP_SECONDS);
                grossDamageTaken += Math.max(0f, healthBeforeEnemyAttacks - state.hero.health);
                if (!gameOver) autoPotion.update(state);

                itemDrops.processDefeatedEnemies(state);
                potionDrops.processDefeatedEnemies(state);
                KillRewardResult rewards = killRewards.processDefeatedEnemies(state);
                pickups.update(state, STEP_SECONDS);
                if (rewards.levelsGained() > 0) allocateTalentPoints(state);
                improveEquipmentAndSellSpareItems(state);
                buyBalancedShopUpgrades(state);

                WaveCompletion completion = waves.updateAfterCombat(state);
                if (completion == WaveCompletion.BOSS_REWARD) {
                    chooseBalancedReward(state);
                    waves.continueAfterBossReward(state);
                }
                elapsed += STEP_SECONDS;
            }

            synchronizeHeroMaximumHealth(state);
            float maximumHealth = Math.max(1f, state.hero.maxHealth);
            samples.add(new WaveSample(
                wave,
                startingHealth,
                startingMaxHealth,
                state.hero.health,
                maximumHealth,
                grossDamageTaken,
                grossDamageTaken / maximumHealth,
                dpsToHpRatio,
                elapsed,
                timedOut
            ));
            if (timedOut) break;
        }
        return new BalanceReport(samples, state.hero.alive && state.runComplete);
    }

    private void allocateTalentPoints(GameState state) {
        while (state.unspentTalentPoints > 0) {
            HeroStat selected = BALANCED_STATS[0];
            int fewest = basePoints(state, selected);
            for (HeroStat candidate : BALANCED_STATS) {
                int points = basePoints(state, candidate);
                if (points < fewest) {
                    selected = candidate;
                    fewest = points;
                }
            }
            progression.allocateTalentPoint(state, selected);
        }
    }

    private void buyBalancedShopUpgrades(GameState state) {
        for (int purchase = 0; purchase < BALANCED_STATS.length * StatShopSystem.MAX_PURCHASES_PER_STAT; purchase++) {
            HeroStat selected = null;
            int fewest = Integer.MAX_VALUE;
            for (HeroStat candidate : BALANCED_STATS) {
                int levels = shop.purchasedLevels(state, candidate);
                if (levels < fewest && state.coins >= shop.price(state, candidate)) {
                    selected = candidate;
                    fewest = levels;
                }
            }
            if (selected == null || !shop.purchase(state, selected)) return;
        }
    }

    private void improveEquipmentAndSellSpareItems(GameState state) {
        List<Item> candidates = new ArrayList<>(state.inventory);
        candidates.sort(Comparator.comparingInt(BalanceSimulator::itemPower).reversed());
        for (Item item : candidates) {
            if (!state.inventory.contains(item)) continue;
            EquipmentSlot slot = EquipmentSlot.parse(item.slot);
            if (slot == null) continue;
            Item equipped = state.equippedItems.get(slot.name());
            if (equipped == null || itemPower(item) > itemPower(equipped)) {
                equipment.equip(state, item);
            }
        }
        for (Item spare : new ArrayList<>(state.inventory)) equipment.sell(state, spare);
    }

    private void chooseBalancedReward(GameState state) {
        int bestIndex = 0;
        int bestScore = Integer.MIN_VALUE;
        for (int index = 0; index < state.pendingRewardCards.size(); index++) {
            RewardCardId card = RewardCardId.valueOf(state.pendingRewardCards.get(index));
            int score = rewardScore(state, card);
            if (score > bestScore) {
                bestIndex = index;
                bestScore = score;
            }
        }
        if (!rewardCards.chooseCard(state, bestIndex)) {
            throw new IllegalStateException("Simulator could not apply a pending boss reward");
        }
    }

    private static int rewardScore(GameState state, RewardCardId card) {
        return switch (card) {
            case LIFESTEAL -> effect(state, BossRewardCardSystem.LIFESTEAL_KEY) < 0.20f ? 100 : 58;
            case GENERAL_POWER -> 92;
            case HEALTH -> 84;
            case STRENGTH -> 80;
            case AGILITY -> 76;
            case DODGE -> 70;
            case COIN_INCOME -> 62;
            case LUCK -> 50;
        };
    }

    private void synchronizeHeroMaximumHealth(GameState state) {
        float updated = stats.maxHealth(state);
        if (updated > state.hero.maxHealth) {
            state.hero.health += updated - state.hero.maxHealth;
        }
        state.hero.maxHealth = updated;
        state.hero.health = Math.min(updated, state.hero.health);
    }

    private float expectedHeroDps(GameState state) {
        float attacksPerSecond = 1f / stats.attackIntervalSeconds(state);
        float expectedCriticalMultiplier = 1f
            + HeroAutoAttackSystem.CRITICAL_CHANCE
            * (HeroAutoAttackSystem.CRITICAL_DAMAGE_MULTIPLIER - 1f);
        return stats.damage(state)
            * attacksPerSecond
            * (1f + effect(state, BossRewardCardSystem.GENERAL_POWER_KEY))
            * expectedCriticalMultiplier;
    }

    private static float totalLivingEnemyHealth(GameState state) {
        float result = 0f;
        for (Enemy enemy : state.aliveEnemies) {
            if (enemy != null && enemy.alive) result += enemy.health;
        }
        for (Boss boss : state.aliveBosses) {
            if (boss != null && boss.alive) result += boss.health;
        }
        return result;
    }

    private static int itemPower(Item item) {
        if (item == null || item.statBonuses == null) return 0;
        return item.statBonuses.values().stream()
            .mapToInt(value -> value == null ? 0 : Math.max(0, Math.round(value)))
            .sum();
    }

    private static int basePoints(GameState state, HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> state.hero.stats.strength;
            case AGILITY -> state.hero.stats.agility;
            case LUCK -> state.hero.stats.luck;
            case DODGE -> state.hero.stats.dodge;
            case HEALTH -> state.hero.stats.health;
        };
    }

    private static float effect(GameState state, String key) {
        Float value = state.permanentEffects.get(key);
        return value == null ? 0f : Math.max(0f, value);
    }

    public record WaveSample(
        int wave,
        float startingHealth,
        float startingMaxHealth,
        float remainingHealth,
        float maximumHealth,
        float grossDamageTaken,
        float damageFraction,
        float dpsToEnemyHpRatio,
        float clearTimeSeconds,
        boolean timedOut
    ) {
    }

    public record BalanceReport(List<WaveSample> waves, boolean reachedWave100) {
        public BalanceReport {
            waves = List.copyOf(waves);
        }

        public float averageDamageFraction() {
            if (waves.isEmpty()) return 0f;
            float total = 0f;
            for (WaveSample wave : waves) total += wave.damageFraction();
            return total / waves.size();
        }

        public String toCsv() {
            StringBuilder result = new StringBuilder(
                "wave,start_hp,start_max_hp,remaining_hp,max_hp,damage_taken,damage_fraction,dps_to_hp,clear_seconds,timed_out\n"
            );
            for (WaveSample wave : waves) {
                result.append(String.format(
                    Locale.ROOT,
                    "%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.6f,%.6f,%.3f,%s%n",
                    wave.wave(),
                    wave.startingHealth(),
                    wave.startingMaxHealth(),
                    wave.remainingHealth(),
                    wave.maximumHealth(),
                    wave.grossDamageTaken(),
                    wave.damageFraction(),
                    wave.dpsToEnemyHpRatio(),
                    wave.clearTimeSeconds(),
                    wave.timedOut()
                ));
            }
            return result.toString();
        }
    }
}
