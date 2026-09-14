package com.amirrezahadipoor.herodefense.balance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.BalanceReport;
import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.WaveSample;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.rewards.RewardCardId;
import com.amirrezahadipoor.herodefense.trials.TrialId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Phase 26.1a: the ascension regression gate. The tier-0 bands do not transfer
 * to tiers (measured: the 5-15% / 35% / 120 s window is empty above tier 0 —
 * cooling spikes below 35% pushes tier-10 averages under the 5% floor), so 26.1
 * re-derives every band from the full game instead of naively re-running them:
 * averages stay strict (5-15% naked, 3.5%+ empowered) while spike ceilings and
 * pressured floors index by tier. Full-pair and full-card matrices stay tier-0;
 * this gate guards the tier axes with a naked 9-seed matrix, a forced-card spot
 * (every card at boss 20), and a trial-pair spot (power, damage, and horde
 * pairs) at tiers 0/3/6/10.
 */
final class AscensionGateTest {
    private static final long BASELINE_SEED = 0x4845524F444546L;
    private static final long TRIAL_SEED = 0x747269616C7331L;
    private static final int[] TIERS = {0, 3, 6, 10};
    private static final float MINIMUM_NAKED_AVERAGE = 0.05f;
    private static final float MAXIMUM_NAKED_AVERAGE = 0.15f;
    private static final float MINIMUM_EMPOWERED_AVERAGE = 0.035f;
    private static final float MINIMUM_EMPOWERED_CLEAR_SECONDS = 24f;

    private static float nakedMaxDamageCeiling(int tier) {
        return 0.40f + 0.02f * tier;
    }

    private static float clearCeiling(int tier) {
        return 120f + 3f * tier;
    }

    private static float trialMaxDamageCeiling(int tier) {
        return 0.35f + 0.03f * tier;
    }

    private static int trialPressuredFloor(int tier) {
        return switch (tier) {
            case 0 -> 175;
            case 3 -> 160;
            case 6 -> 145;
            case 10 -> 120;
            default -> throw new IllegalArgumentException("Ungated tier " + tier);
        };
    }

    @Test
    void nakedRunsStayInBandAtEveryGatedTier() {
        long[] seeds = {
            BASELINE_SEED, BASELINE_SEED + 1, BASELINE_SEED + 2, BASELINE_SEED + 3,
            BASELINE_SEED + 4, BASELINE_SEED + 5, BASELINE_SEED + 6, BASELINE_SEED + 7,
            0x123456789L,
        };
        System.out.println("seed,tier,finished,avg,max,maxclr");
        for (long seed : seeds) {
            for (int tier : TIERS) {
                BalanceReport report = new BalanceSimulator().runWithAscensionTier(seed, tier);
                Summary summary = summarize(report.waves());
                System.out.println(seed + "," + tier + "," + report.reachedFinalWave() + ","
                    + summary.averageDamage + "," + summary.maximumDamage + ","
                    + summary.maximumClear);
                String cell = "seed " + seed + " tier " + tier;
                assertTrue(report.reachedFinalWave(), cell + " must finish");
                assertTrue(summary.averageDamage >= MINIMUM_NAKED_AVERAGE
                    && summary.averageDamage <= MAXIMUM_NAKED_AVERAGE,
                    cell + " average was " + summary.averageDamage);
                assertTrue(summary.maximumDamage <= nakedMaxDamageCeiling(tier),
                    cell + " max spike was " + summary.maximumDamage);
                assertTrue(summary.maximumClear <= clearCeiling(tier),
                    cell + " max clear was " + summary.maximumClear);
            }
        }
    }

    @Test
    void forcedCardsStayInBandAtEveryGatedTier() {
        System.out.println("card,tier,finished,avg,max,maxclr");
        for (RewardCardId card : RewardCardId.values()) {
            for (int tier : TIERS) {
                BalanceReport report =
                    new BalanceSimulator().runWithForcedCardAndTier(BASELINE_SEED, card, 20, tier);
                Summary summary = summarize(report.waves());
                System.out.println(card + "," + tier + "," + report.reachedFinalWave() + ","
                    + summary.averageDamage + "," + summary.maximumDamage + ","
                    + summary.maximumClear);
                String cell = card + " tier " + tier;
                assertTrue(report.reachedFinalWave(), cell + " must finish");
                assertTrue(summary.averageDamage >= MINIMUM_NAKED_AVERAGE
                    && summary.averageDamage <= MAXIMUM_NAKED_AVERAGE,
                    cell + " average was " + summary.averageDamage);
                assertTrue(summary.maximumDamage <= nakedMaxDamageCeiling(tier),
                    cell + " max spike was " + summary.maximumDamage);
                assertTrue(summary.maximumClear <= clearCeiling(tier),
                    cell + " max clear was " + summary.maximumClear);
            }
        }
    }

    @Test
    void trialPairsStayInBandAtEveryGatedTier() {
        TrialId[][] pairs = {
            {TrialId.DRY_VEINS, TrialId.HEAVY_CROWNS},
            {TrialId.BLOOD_PRICE, TrialId.HOLLOW_CALLING},
            {TrialId.IRON_TIDE, TrialId.STONE_SKIN},
        };
        System.out.println("pair,tier,finished_runs,median_avg,median_clear,"
            + "median_pressured,median_max,median_maxclr");
        List<String> failures = new ArrayList<>();
        for (TrialId[] pair : pairs) {
            for (int tier : TIERS) {
                List<Summary> finished = new ArrayList<>();
                for (long s = 0; s < 3; s++) {
                    BalanceReport report = new BalanceSimulator()
                        .runWithTrialsAndTier(TRIAL_SEED + s, pair[0], pair[1], tier);
                    if (report.reachedFinalWave()
                        && report.waves().size() == GameState.FINAL_WAVE) {
                        finished.add(summarize(report.waves()));
                    }
                }
                String cell = pair[0] + "+" + pair[1] + " tier " + tier;
                if (finished.size() < 2) {
                    failures.add(cell + " finished only " + finished.size() + "/3 seeds");
                    continue;
                }
                Summary median = medians(finished);
                System.out.println(pair[0] + "+" + pair[1] + "," + tier + "," + finished.size()
                    + "/3," + median.averageDamage + "," + median.averageClear + ","
                    + median.pressured + "," + median.maximumDamage + "," + median.maximumClear);
                check(failures, median.averageDamage >= MINIMUM_EMPOWERED_AVERAGE,
                    cell + " trivialized median pressure: " + median.averageDamage);
                check(failures, median.averageClear >= MINIMUM_EMPOWERED_CLEAR_SECONDS,
                    cell + " trivialized median clear time: " + median.averageClear);
                check(failures, median.pressured >= trialPressuredFloor(tier),
                    cell + " left too few pressured waves: " + median.pressured);
                check(failures, median.maximumDamage <= trialMaxDamageCeiling(tier),
                    cell + " caused a damage spike: " + median.maximumDamage);
                check(failures, median.maximumClear <= clearCeiling(tier),
                    cell + " caused a clear-time spike: " + median.maximumClear);
            }
        }
        assertTrue(failures.isEmpty(), "Breaking tier cells:\n" + String.join("\n", failures));
    }

    private static void check(List<String> failures, boolean condition, String message) {
        if (!condition) failures.add(message);
    }

    private record Summary(
        float averageDamage,
        float averageClear,
        long pressured,
        float maximumDamage,
        float maximumClear
    ) {
    }

    private static Summary summarize(List<WaveSample> waves) {
        float averageDamage = 0f;
        float averageClear = 0f;
        long pressured = 0;
        float maximumDamage = 0f;
        float maximumClear = 0f;
        for (WaveSample sample : waves) {
            averageDamage += sample.damageFraction();
            averageClear += sample.clearTimeSeconds();
            if (sample.damageFraction() >= 0.01f) pressured++;
            maximumDamage = Math.max(maximumDamage, sample.damageFraction());
            maximumClear = Math.max(maximumClear, sample.clearTimeSeconds());
        }
        return new Summary(
            averageDamage / waves.size(), averageClear / waves.size(), pressured,
            maximumDamage, maximumClear);
    }

    private static Summary medians(List<Summary> runs) {
        return new Summary(
            medianFloat(runs.stream().map(Summary::averageDamage).sorted().toList()),
            medianFloat(runs.stream().map(Summary::averageClear).sorted().toList()),
            medianLong(runs.stream().map(Summary::pressured).sorted().toList()),
            medianFloat(runs.stream().map(Summary::maximumDamage).sorted().toList()),
            medianFloat(runs.stream().map(Summary::maximumClear).sorted().toList()));
    }

    private static float medianFloat(List<Float> sorted) {
        return sorted.get(sorted.size() / 2);
    }

    private static long medianLong(List<Long> sorted) {
        return sorted.get(sorted.size() / 2);
    }
}
