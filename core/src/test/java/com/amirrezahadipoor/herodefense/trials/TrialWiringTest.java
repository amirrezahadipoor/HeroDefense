package com.amirrezahadipoor.herodefense.trials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.BossFactory;
import com.amirrezahadipoor.herodefense.gameplay.ContinuousWaveRun;
import com.amirrezahadipoor.herodefense.gameplay.EnemyFactory;
import com.amirrezahadipoor.herodefense.gameplay.EnemyWaveSpawner;
import com.amirrezahadipoor.herodefense.gameplay.HeroAutoAttackSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroDamageSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroProgressionSystem;
import com.amirrezahadipoor.herodefense.gameplay.HeroStatCalculator;
import com.amirrezahadipoor.herodefense.gameplay.ItemDropSystem;
import com.amirrezahadipoor.herodefense.gameplay.KillRewardSystem;
import com.amirrezahadipoor.herodefense.gameplay.WaveLifecycleSystem;
import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.Boss;
import com.amirrezahadipoor.herodefense.model.BossType;
import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.EnemyType;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.model.ItemTier;
import com.amirrezahadipoor.herodefense.potions.PotionDropSystem;
import com.amirrezahadipoor.herodefense.shop.StatShopSystem;
import com.amirrezahadipoor.herodefense.skills.SkillId;
import com.amirrezahadipoor.herodefense.skills.SkillShopSystem;
import org.junit.jupiter.api.Test;

/** End-to-end proof that every trial reaches its gameplay lever. */
final class TrialWiringTest {
    private static GameState runWith(long seed, TrialId... trials) {
        GameState state = GameState.newRun(seed);
        for (TrialId trial : trials) {
            state.activeTrials.add(trial.name());
        }
        return state;
    }

    @Test
    void swiftHollowQuickensRegularEnemies() {
        EnemyFactory factory = new EnemyFactory();
        Enemy plain = factory.createForWave(runWith(1L), EnemyType.ROOTLING, 0f, 0f, 0, 1);
        Enemy swift = factory.createForWave(
            runWith(1L, TrialId.SWIFT_HOLLOW), EnemyType.ROOTLING, 0f, 0f, 0, 1
        );
        assertEquals(plain.movementSpeed * 1.25f, swift.movementSpeed, 0.001f);
    }

    @Test
    void stoneSkinAndHollowCallingToughenAndSharpenRegulars() {
        EnemyFactory factory = new EnemyFactory();
        Enemy plain = factory.createForWave(runWith(2L), EnemyType.FUNGAL_BRUTE, 0f, 0f, 0, 10);
        Enemy tried = factory.createForWave(
            runWith(2L, TrialId.STONE_SKIN, TrialId.HOLLOW_CALLING),
            EnemyType.FUNGAL_BRUTE, 0f, 0f, 0, 10
        );
        assertEquals(plain.maxHealth * 1.2f, tried.maxHealth, 0.01f);
        assertEquals(plain.health * 1.2f, tried.health, 0.01f);
        assertEquals(plain.damage * 1.2f, tried.damage, 0.01f);
    }

    @Test
    void bossTrialsFattenBossHealthAndDamage() {
        BossFactory factory = new BossFactory();
        Boss plain = factory.create(runWith(3L), BossType.ANCIENT_GOLEM, 0f, 0f, 1, 0);
        Boss bounty = factory.create(
            runWith(3L, TrialId.BOSS_BOUNTY), BossType.ANCIENT_GOLEM, 0f, 0f, 1, 0
        );
        Boss crowned = factory.create(
            runWith(3L, TrialId.HEAVY_CROWNS), BossType.ANCIENT_GOLEM, 0f, 0f, 1, 0
        );
        assertEquals(plain.maxHealth * 1.3f, bounty.maxHealth, 0.01f);
        assertEquals(plain.damage * 1.3f, crowned.damage, 0.01f);
    }

    @Test
    void ironTideAddsThreeEnemiesToTheWave() {
        WaveLifecycleSystem lifecycle = new WaveLifecycleSystem(
            new EnemyWaveSpawner(new EnemyFactory()), new ContinuousWaveRun()
        );
        GameState plain = runWith(4L);
        GameState tide = runWith(4L, TrialId.IRON_TIDE);
        lifecycle.startCurrentWave(plain);
        lifecycle.startCurrentWave(tide);
        assertEquals(4, plain.aliveEnemies.size());
        assertEquals(7, tide.aliveEnemies.size());
    }

    @Test
    void famishedEarthCutsCoinsButAddsDodge() {
        KillRewardSystem rewards = new KillRewardSystem(new HeroProgressionSystem());
        GameState plain = defeatedRootling(5L);
        GameState famished = defeatedRootling(5L);
        famished.activeTrials.add(TrialId.FAMISHED_EARTH.name());
        int baseCoins = rewards.processDefeatedEnemies(plain).coins();
        int cutCoins = rewards.processDefeatedEnemies(famished).coins();
        assertEquals(Math.round(baseCoins * 0.7f), cutCoins);

        HeroStatCalculator stats = new HeroStatCalculator();
        assertEquals(0.10f, stats.dodgeChance(famished), 0.0001f);
        assertEquals(0f, stats.dodgeChance(plain), 0f);
    }

    @Test
    void dryVeinsBlocksEveryPotionButGrantsABonusTalentPoint() {
        PotionDropSystem potions = new PotionDropSystem();
        GameState dry = runWith(6L, TrialId.DRY_VEINS);
        for (int i = 0; i < 40; i++) {
            dry.aliveEnemies.add(deadRootling(dry));
        }
        assertEquals(0, potions.processDefeatedEnemies(dry));
        assertTrue(dry.drops.isEmpty());

        HeroProgressionSystem progression = new HeroProgressionSystem();
        GameState plain = runWith(6L);
        int firstLevelXp = progression.experienceRequiredForNextLevel(plain.heroLevel);
        assertEquals(1, progression.grantExperience(plain, firstLevelXp));
        assertEquals(1, plain.unspentTalentPoints);
        GameState veined = runWith(6L, TrialId.DRY_VEINS);
        assertEquals(1, progression.grantExperience(veined, firstLevelXp));
        assertEquals(1, veined.unspentTalentPoints);
        int secondLevelXp = progression.experienceRequiredForNextLevel(veined.heroLevel);
        assertEquals(1, progression.grantExperience(veined, secondLevelXp));
        assertEquals(2, veined.unspentTalentPoints);
        int thirdLevelXp = progression.experienceRequiredForNextLevel(veined.heroLevel);
        assertEquals(1, progression.grantExperience(veined, thirdLevelXp));
        assertEquals(4, veined.unspentTalentPoints);
    }

    @Test
    void thinBloodAndHollowCallingReshapeHeroHealth() {
        HeroStatCalculator stats = new HeroStatCalculator();
        assertEquals(100f, stats.maxHealth(runWith(7L)), 0f);
        assertEquals(80f, stats.maxHealth(runWith(7L, TrialId.THIN_BLOOD)), 0.001f);
        assertEquals(115f, stats.maxHealth(runWith(7L, TrialId.HOLLOW_CALLING)), 0.001f);
    }

    @Test
    void glassArrowsTradeDamageForAttackSpeed() {
        HeroStatCalculator stats = new HeroStatCalculator();
        GameState plain = runWith(8L);
        GameState glass = runWith(8L, TrialId.GLASS_ARROWS);
        assertEquals(stats.damage(plain) * 0.8f, stats.damage(glass), 0.001f);
        assertEquals(
            stats.attackIntervalSeconds(plain) / 1.25f,
            stats.attackIntervalSeconds(glass),
            0.0001f
        );
    }

    @Test
    void misersPactRaisesPricesButFillsThePurse() {
        StatShopSystem statShop = new StatShopSystem();
        GameState plain = runWith(9L);
        GameState miser = runWith(9L, TrialId.MISERS_PACT);
        int baseStat = statShop.price(plain, HeroStat.STRENGTH);
        assertEquals((int) Math.round(baseStat * 1.3 / 5.0) * 5,
            statShop.price(miser, HeroStat.STRENGTH));

        SkillShopSystem skillShop = new SkillShopSystem();
        int baseSkill = skillShop.price(plain, SkillId.MULTI_SHOT);
        assertEquals((int) Math.round(baseSkill * 1.3 / 5.0) * 5,
            skillShop.price(miser, SkillId.MULTI_SHOT));

        KillRewardSystem rewards = new KillRewardSystem(new HeroProgressionSystem());
        GameState plainKill = defeatedRootling(9L);
        GameState miserKill = defeatedRootling(9L);
        miserKill.activeTrials.add(TrialId.MISERS_PACT.name());
        int baseCoins = rewards.processDefeatedEnemies(plainKill).coins();
        int miserCoins = rewards.processDefeatedEnemies(miserKill).coins();
        assertEquals(Math.round(baseCoins * 1.3f), miserCoins);
    }

    @Test
    void bloodPriceDeepensWoundsButPaysLifesteal() {
        HeroDamageSystem damage = new HeroDamageSystem();
        GameState blood = runWith(10L, TrialId.BLOOD_PRICE);
        blood.hero.health = 100f;
        damage.applyIncomingHit(blood, 10f);
        assertEquals(88.5f, blood.hero.health, 0.001f);

        HeroAutoAttackSystem attacks = new HeroAutoAttackSystem();
        GameState hunter = runWith(11L, TrialId.BLOOD_PRICE);
        hunter.hero.health = 50f;
        Enemy target = new EnemyFactory().create(
            hunter, EnemyType.ROOTLING, hunter.hero.x + 90f, hunter.hero.y, 0
        );
        hunter.aliveEnemies.add(target);
        attacks.update(hunter, 0f);
        attacks.update(hunter, 0.2f);
        assertTrue(hunter.hero.health > 50f);
    }

    @Test
    void heavyCrownsGuaranteesARarePlusBossDrop() {
        ItemDropSystem drops = new ItemDropSystem();
        GameState crowned = runWith(12L, TrialId.HEAVY_CROWNS);
        Boss boss = new BossFactory().create(crowned, BossType.ANCIENT_GOLEM, 0f, 0f, 1, 0);
        boss.receiveDamage(Float.MAX_VALUE);
        crowned.aliveBosses.add(boss);
        assertEquals(1, drops.processDefeatedEnemies(crowned));
        assertEquals(1, crowned.drops.size());
        ItemTier tier = EquipmentCatalog.byId(crowned.drops.get(0).itemId).tier();
        assertTrue(tier.ordinal() >= ItemTier.RARE.ordinal());
    }

    @Test
    void bossBountySweetensTheHeartwoodHarvest() {
        GameState plain = runWith(13L);
        plain.peakWaveReached = 20;
        GameState bounty = runWith(13L, TrialId.BOSS_BOUNTY);
        bounty.peakWaveReached = 20;
        int base = plain.ascendAndAwardHeartwood();
        int sweetened = bounty.ascendAndAwardHeartwood();
        assertEquals(Math.round(base * 1.3f), sweetened);
    }

    private static GameState defeatedRootling(long seed) {
        GameState state = runWith(seed);
        state.waveNumber = 1;
        state.aliveEnemies.add(deadRootling(state));
        return state;
    }

    private static Enemy deadRootling(GameState state) {
        Enemy enemy = new EnemyFactory().create(state, EnemyType.ROOTLING, 0f, 0f, 0);
        enemy.receiveDamage(Float.MAX_VALUE);
        return enemy;
    }
}
