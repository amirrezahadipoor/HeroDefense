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
import com.amirrezahadipoor.herodefense.gameplay.ItemForgeSystem;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.potions.AutoPotionSystem;
import com.amirrezahadipoor.herodefense.potions.HealthPotionSystem;
import com.amirrezahadipoor.herodefense.potions.PotionDropSystem;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.rewards.RewardCardId;
import com.amirrezahadipoor.herodefense.shop.StatShopSystem;
import com.amirrezahadipoor.herodefense.skills.SkillId;
import com.amirrezahadipoor.herodefense.skills.SkillShopSystem;
import com.amirrezahadipoor.herodefense.trials.TrialId;

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
    private final ItemForgeSystem forge = new ItemForgeSystem(stats);
    /** Coin ledger of the last run, for economy audits (income and spend by sink). */
    private final Ledger ledger = new Ledger();
    private final BossRewardCardSystem rewardCards = new BossRewardCardSystem();
    private final StatShopSystem shop = new StatShopSystem();
    private final SkillShopSystem skillShop = new SkillShopSystem();
    private final WaveLifecycleSystem waves = new WaveLifecycleSystem(
        new EnemyWaveSpawner(new EnemyFactory()),
        new BossWaveSpawner(new BossFactory()),
        rewardCards,
        new ContinuousWaveRun()
    );

    public BalanceSimulator() {
        // Optimal play declines the Anvil's affix gamble: rerolls trade a guaranteed stat
        // step for a fresh random affix, so they are luck, not power, and stay out of
        // balance measurement.
        forge.declineAffixRerolls();
    }

    public BalanceReport run(long seed) {
        return run(seed, null, 0, 0, List.of());
    }

    public BalanceReport runWithAscensionTier(long seed, int ascensionTier) {
        return run(seed, null, 0, Math.max(0, ascensionTier), List.of());
    }

    /** Binds a single trial for the whole run; isolates one trial's pressure delta. */
    public BalanceReport runWithTrial(long seed, TrialId trial) {
        if (trial == null) {
            throw new IllegalArgumentException("A trial is required");
        }
        return run(seed, null, 0, 0, List.of(trial.name()));
    }

    /** Binds one drafted trial pair for the whole run; the Phase 22.1 scenario axis. */
    public BalanceReport runWithTrials(long seed, TrialId first, TrialId second) {
        if (first == null || second == null || first == second) {
            throw new IllegalArgumentException("Two distinct trials are required");
        }
        return run(seed, null, 0, 0, List.of(first.name(), second.name()));
    }

    /** Forces one legal card effect into a selected boss offer for comparative simulations. */
    public BalanceReport runWithForcedCard(long seed, RewardCardId card, int bossNumber) {
        int lastBoss = GameState.FINAL_WAVE / 5;
        if (card == null || bossNumber < 1 || bossNumber > lastBoss) {
            throw new IllegalArgumentException("Forced card and boss number 1.." + lastBoss + " are required");
        }
        return run(seed, card, bossNumber, 0, List.of());
    }

    public BalanceReport runWithForcedCardAndTier(long seed, RewardCardId card, int bossNumber, int ascensionTier) {
        int lastBoss = GameState.FINAL_WAVE / 5;
        if (card == null || bossNumber < 1 || bossNumber > lastBoss) {
            throw new IllegalArgumentException("Forced card and boss number 1.." + lastBoss + " are required");
        }
        return run(seed, card, bossNumber, Math.max(0, ascensionTier), List.of());
    }

    private BalanceReport run(
        long seed,
        RewardCardId forcedCard,
        int forcedBossNumber,
        int ascensionTier,
        List<String> trials
    ) {
        GameState state = GameState.newRun(seed);
        state.activeTrials.clear();
        state.activeTrials.addAll(trials);
        state.ascensionTier = ascensionTier;
        if (ascensionTier > 0) {
            applyRootBonusesForTier(state, ascensionTier);
        }
        ledger.reset();
        new StarterLoadoutSystem().provisionOnce(state);
        if (ascensionTier > 0) {
            applyRootBonusesForTier(state, ascensionTier);
        }
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
                ledger.killIncome += rewards.coins();
                int coinsBeforePickups = state.coins;
                pickups.update(state, STEP_SECONDS);
                ledger.pickupIncome += Math.max(0, state.coins - coinsBeforePickups);
                if (rewards.levelsGained() > 0) allocateTalentPoints(state);
                improveEquipmentAndSellSpareItems(state);
                buyBalancedShopUpgrades(state);

                WaveCompletion completion = waves.updateAfterCombat(state);
                if (completion == WaveCompletion.BOSS_REWARD) {
                    chooseReward(state, forcedCard, forcedBossNumber);
                    completion = waves.continueAfterBossReward(state);
                }
                if (completion == WaveCompletion.PLANTING_CEREMONY) {
                    // The ceremony is presentation only; the simulator plants instantly.
                    waves.completePlantingCeremony(state);
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

    private static void applyRootBonusesForTier(GameState state, int tier) {
        if (state == null || state.hero == null) return;
        // Simulate heartwood spending: tier 3 = first 8 nodes, tier 6 = 16, tier 10 = all
        int nodesToApply;
        if (tier >= 10) nodesToApply = 24;
        else if (tier >= 6) nodesToApply = 16;
        else if (tier >= 3) nodesToApply = 8;
        else nodesToApply = tier * 2;
        // Approximate permanent bonuses from root network without needing full catalog
        // Each tier gives +1 strength, +1 health, +25 coins, +0.2 talent point average
        state.hero.stats.strength += tier;
        state.hero.stats.health += tier * 2;
        state.hero.stats.agility += tier / 2;
        state.hero.stats.dodge += tier / 3;
        state.hero.stats.luck += tier / 3;
        state.coins += tier * 50;
        state.unspentTalentPoints += tier / 2;
        state.hero.maxHealth = state.hero.stats.maxHealth() + tier * 10f;
        state.hero.health = state.hero.maxHealth;
        state.worldTreeMaxHealth = 1000f + tier * 5f;
        state.worldTreeHealth = state.worldTreeMaxHealth;
        state.focusMax = 100f + tier * 2f;
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

    /**
     * Greedy coin policy shared by every simulated run: always buy the cheapest next stat
     * or skill level that is affordable, which approximates a thrifty player who keeps
     * both tabs of the shop moving instead of hoarding.
     */
    private void buyBalancedShopUpgrades(GameState state) {
        // Endless shop: bound the greedy loop per visit rather than by a level cap.
        int budget = 64;
        for (int purchase = 0; purchase < budget; purchase++) {
            HeroStat selectedStat = null;
            SkillId selectedSkill = null;
            int cheapest = Integer.MAX_VALUE;
            for (HeroStat candidate : BALANCED_STATS) {
                int price = shop.price(state, candidate);
                if (price < cheapest && state.coins >= price) {
                    selectedStat = candidate;
                    cheapest = price;
                }
            }
            for (SkillId candidate : SkillId.values()) {
                int price = skillShop.price(state, candidate);
                if (price < cheapest && state.coins >= price) {
                    selectedSkill = candidate;
                    selectedStat = null;
                    cheapest = price;
                }
            }
            boolean bought = selectedSkill != null
                ? skillShop.purchase(state, selectedSkill)
                : selectedStat != null && shop.purchase(state, selectedStat);
            if (!bought) return;
            if (selectedSkill != null) {
                ledger.skillSpend += cheapest;
                ledger.skillLevels++;
            } else {
                ledger.statSpend += cheapest;
                ledger.statLevels++;
            }
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
        for (Item spare : new ArrayList<>(state.inventory)) {
            int price = spare.sellPrice;
            if (equipment.sell(state, spare)) ledger.sellIncome += price;
        }
        forgeEquippedItems(state);
    }

    /**
     * Anvil policy: reforge the equipped item with the cheapest next step while it costs no
     * more than the cheapest shop purchase would; a player who has both open takes the
     * cheaper permanent gain first.
     */
    private void forgeEquippedItems(GameState state) {
        for (int step = 0; step < 8; step++) {
            Item best = null;
            int bestCost = Integer.MAX_VALUE;
            for (Item item : state.equippedItems.values()) {
                int cost = ItemForgeSystem.nextCost(item);
                if (cost > 0 && cost < bestCost && state.coins >= cost) {
                    best = item;
                    bestCost = cost;
                }
            }
            if (best == null || bestCost > cheapestShopPrice(state)) return;
            ItemForgeSystem.Result result = forge.forge(state, best);
            if (result != ItemForgeSystem.Result.FORGED
                && result != ItemForgeSystem.Result.AFFIX_REROLLED) return;
            ledger.forgeSpend += bestCost;
            if (result == ItemForgeSystem.Result.FORGED) ledger.forgeSteps++;
        }
    }

    private int cheapestShopPrice(GameState state) {
        int cheapest = Integer.MAX_VALUE;
        for (HeroStat candidate : BALANCED_STATS) cheapest = Math.min(cheapest, shop.price(state, candidate));
        for (SkillId candidate : SkillId.values()) cheapest = Math.min(cheapest, skillShop.price(state, candidate));
        return cheapest;
    }

    /** Ledger of the most recent {@link #run(long)}; the sums are coins, the counts purchases. */
    public Ledger lastLedger() {
        return ledger;
    }

    public static final class Ledger {
        public long killIncome;
        public long pickupIncome;
        public long sellIncome;
        public long statSpend;
        public long skillSpend;
        public long forgeSpend;
        public int statLevels;
        public int skillLevels;
        public int forgeSteps;

        void reset() {
            killIncome = pickupIncome = sellIncome = statSpend = skillSpend = forgeSpend = 0L;
            statLevels = skillLevels = forgeSteps = 0;
        }

        public long income() {
            return killIncome + pickupIncome + sellIncome;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                "income kills=%d pickups=%d sells=%d | spend stats=%d(%d lv) skills=%d(%d lv) forge=%d(%d steps)",
                killIncome, pickupIncome, sellIncome, statSpend, statLevels, skillSpend, skillLevels,
                forgeSpend, forgeSteps);
        }
    }

    private void chooseReward(
        GameState state,
        RewardCardId forcedCard,
        int forcedBossNumber
    ) {
        int selectedIndex;
        if (forcedCard != null && state.pendingRewardBossNumber == forcedBossNumber) {
            selectedIndex = state.pendingRewardCards.indexOf(forcedCard.name());
            if (selectedIndex < 0) {
                selectedIndex = 0;
                state.pendingRewardCards.set(selectedIndex, forcedCard.name());
            }
        } else {
            selectedIndex = 0;
            int bestScore = Integer.MIN_VALUE;
            for (int index = 0; index < state.pendingRewardCards.size(); index++) {
                RewardCardId card = RewardCardId.valueOf(state.pendingRewardCards.get(index));
                int score = rewardScore(state, card);
                if (score > bestScore) {
                    selectedIndex = index;
                    bestScore = score;
                }
            }
        }
        if (!rewardCards.chooseCard(state, selectedIndex)) {
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
            if (enemy != null && enemy.alive && !enemy.silentWatcher) result += enemy.health;
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

    /** {@code reachedFinalWave}: the Hero survived the whole 1..FINAL_WAVE run. */
    public record BalanceReport(List<WaveSample> waves, boolean reachedFinalWave) {
        public BalanceReport {
            waves = List.copyOf(waves);
        }

        /** True once the run cleared the planting wave (the tuned first half). */
        public boolean reachedWave100() {
            return reachedFinalWave || waves.size() > GameState.PLANTING_WAVE;
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
