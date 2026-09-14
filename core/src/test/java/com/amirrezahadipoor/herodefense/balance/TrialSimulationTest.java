package com.amirrezahadipoor.herodefense.balance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.BalanceReport;
import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.WaveSample;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.trials.TrialId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Phase 22.1 scenario axis: every drafted trial pair plays full runs through the real
 * systems. Each pair runs on three seeds and the gate judges the MEDIAN run: combat is
 * chaotic, so a single seed can knife-edge (a death at 1 HP, a 0.4%-over spike) while the
 * pair is healthy. Systematic breakage moves all three seeds and still fails medians.
 */
final class TrialSimulationTest {
    private static final long SEED = 0x747269616C7331L;
    private static final long[] SEEDS = {SEED, SEED + 1, SEED + 2};
    /**
     * Empowered-run floors, recentered from 4% / 25s / 90% for the Elite era:
     * guaranteed Rare+ Elite drops grant ambient power to every empowered build
     * (measured -8 to -15% pressure, -3 to -5% clear time across both sim gates),
     * while the naked-run baseline gate keeps its 5%. Phase 26 re-derives every
     * band from the full game. 25.2b concedes one more pressured wave (88% -> 87.5%):
     * bounded rot plus shields moves pressured counts a wave or two run to run, and
     * the count metric carries no other slack.
     */
    private static final float MINIMUM_AVERAGE_DAMAGE_FRACTION = 0.035f;
    private static final float MINIMUM_AVERAGE_CLEAR_SECONDS = 24f;
    private static final float MINIMUM_PRESSURED_WAVE_FRACTION = 0.875f;
    private static final float MAXIMUM_SINGLE_WAVE_DAMAGE_FRACTION = 0.35f;
    private static final float MAXIMUM_CLEAR_SECONDS = 120f;

    private record PerRun(
        float averageDamage,
        float averageClearTime,
        long pressuredWaves,
        float maximumDamage,
        float maximumClear,
        int spikeWave
    ) {
    }

    @Test
    void noTrialPairBreaksTheDifficultyGate() {
        System.out.println(
            "trial_a,trial_b,finishes,median_damage_fraction,median_clear_seconds,"
                + "median_pressured_waves,median_maximum_damage,median_maximum_clear"
        );
        BalanceReport baseline = new BalanceSimulator().run(SEED);
        System.out.println(
            "BASELINE,,," + averageDamage(baseline.waves()) + ","
                + averageClearTime(baseline.waves())
        );
        List<String> failures = new ArrayList<>();
        TrialId[] trials = TrialId.values();
        for (int first = 0; first < trials.length; first++) {
            for (int second = first + 1; second < trials.length; second++) {
                verifyPair(trials[first], trials[second], failures);
            }
        }
        assertTrue(failures.isEmpty(), "Breaking trial pairs:\\n" + String.join("\\n", failures));
    }

    private static void verifyPair(TrialId first, TrialId second, List<String> failures) {
        String scenario = first + " + " + second;
        List<PerRun> finished = new ArrayList<>();
        for (long seed : SEEDS) {
            BalanceReport report = new BalanceSimulator().runWithTrials(seed, first, second);
            if (!report.reachedFinalWave() || report.waves().size() != GameState.FINAL_WAVE) {
                continue;
            }
            finished.add(summarize(report.waves()));
        }
        if (finished.size() < 2) {
            failures.add(scenario + " finished only " + finished.size() + "/3 seeds");
            return;
        }
        PerRun median = medians(finished);
        System.out.println(
            first + "," + second + "," + finished.size() + "/3," + median.averageDamage()
                + "," + median.averageClearTime() + "," + median.pressuredWaves() + ","
                + median.maximumDamage() + "," + median.maximumClear()
        );
        check(failures, median.averageDamage() >= MINIMUM_AVERAGE_DAMAGE_FRACTION,
            scenario + " trivialized median incoming pressure: " + median.averageDamage());
        check(failures, median.averageClearTime() >= MINIMUM_AVERAGE_CLEAR_SECONDS,
            scenario + " trivialized median clear time: " + median.averageClearTime());
        check(failures,
            median.pressuredWaves()
                >= Math.ceil(GameState.FINAL_WAVE * MINIMUM_PRESSURED_WAVE_FRACTION),
            scenario + " left too few pressured waves: " + median.pressuredWaves());
        check(failures, median.maximumDamage() <= MAXIMUM_SINGLE_WAVE_DAMAGE_FRACTION,
            scenario + " caused a damage spike: " + median.maximumDamage() + " at wave "
                + median.spikeWave());
        check(failures, median.maximumClear() <= MAXIMUM_CLEAR_SECONDS,
            scenario + " caused a clear-time spike: " + median.maximumClear());
    }

    private static PerRun summarize(List<WaveSample> waves) {
        long pressuredWaves = waves.stream()
            .filter(sample -> sample.damageFraction() >= 0.01f)
            .count();
        float maximumDamage = waves.stream()
            .map(WaveSample::damageFraction)
            .max(Float::compare)
            .orElse(0f);
        float maximumClear = waves.stream()
            .map(WaveSample::clearTimeSeconds)
            .max(Float::compare)
            .orElse(0f);
        int spikeWave = waves.stream()
            .max(Comparator.comparing(WaveSample::damageFraction))
            .map(WaveSample::wave)
            .orElse(-1);
        return new PerRun(
            averageDamage(waves), averageClearTime(waves), pressuredWaves,
            maximumDamage, maximumClear, spikeWave
        );
    }

    private static PerRun medians(List<PerRun> runs) {
        List<Float> damage = new ArrayList<>();
        List<Float> clear = new ArrayList<>();
        List<Long> pressured = new ArrayList<>();
        List<Float> maxDamage = new ArrayList<>();
        List<Float> maxClear = new ArrayList<>();
        for (PerRun run : runs) {
            damage.add(run.averageDamage());
            clear.add(run.averageClearTime());
            pressured.add(run.pressuredWaves());
            maxDamage.add(run.maximumDamage());
            maxClear.add(run.maximumClear());
        }
        damage.sort(Float::compare);
        clear.sort(Float::compare);
        pressured.sort(Long::compare);
        maxDamage.sort(Float::compare);
        maxClear.sort(Float::compare);
        int middle = runs.size() / 2;
        int spikeWave = runs.stream()
            .min(Comparator.comparing(run -> Math.abs(run.maximumDamage() - maxDamage.get(middle))))
            .map(PerRun::spikeWave)
            .orElse(-1);
        return new PerRun(
            damage.get(middle), clear.get(middle), pressured.get(middle),
            maxDamage.get(middle), maxClear.get(middle), spikeWave
        );
    }

    private static void check(List<String> failures, boolean condition, String message) {
        if (!condition) {
            failures.add(message);
        }
    }

    private static float averageDamage(List<WaveSample> waves) {
        float total = 0f;
        for (WaveSample wave : waves) total += wave.damageFraction();
        return total / waves.size();
    }

    private static float averageClearTime(List<WaveSample> waves) {
        float total = 0f;
        for (WaveSample wave : waves) total += wave.clearTimeSeconds();
        return total / waves.size();
    }
}
