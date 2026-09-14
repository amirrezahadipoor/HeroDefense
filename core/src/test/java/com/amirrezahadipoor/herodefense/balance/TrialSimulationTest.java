package com.amirrezahadipoor.herodefense.balance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.BalanceReport;
import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.WaveSample;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.trials.TrialId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Phase 22.1 scenario axis: every drafted trial pair plays a full run through the real
 * systems. No pair may kill the run, trivialize pressure, or spike damage past the gate.
 */
final class TrialSimulationTest {
    private static final long SEED = 0x747269616C7331L;
    private static final float MINIMUM_AVERAGE_DAMAGE_FRACTION = 0.05f;
    private static final float MINIMUM_AVERAGE_CLEAR_SECONDS = 25f;
    private static final float MINIMUM_PRESSURED_WAVE_FRACTION = 0.90f;
    private static final float MAXIMUM_SINGLE_WAVE_DAMAGE_FRACTION = 0.35f;
    private static final float MAXIMUM_CLEAR_SECONDS = 120f;

    @Test
    void noTrialPairBreaksTheDifficultyGate() {
        System.out.println(
            "trial_a,trial_b,average_damage_fraction,average_clear_seconds,pressured_waves,"
                + "maximum_damage_fraction,maximum_clear_seconds"
        );
        BalanceReport baseline = new BalanceSimulator().run(SEED);
        System.out.println(
            "BASELINE,," + averageDamage(baseline.waves()) + ","
                + averageClearTime(baseline.waves())
        );
        List<String> failures = new java.util.ArrayList<>();
        TrialId[] trials = TrialId.values();
        for (int first = 0; first < trials.length; first++) {
            for (int second = first + 1; second < trials.length; second++) {
                verifyPair(trials[first], trials[second], failures);
            }
        }
        assertTrue(failures.isEmpty(), "Breaking trial pairs:\n" + String.join("\n", failures));
    }

    private static void verifyPair(TrialId first, TrialId second, List<String> failures) {
        BalanceReport report = new BalanceSimulator().runWithTrials(SEED, first, second);
        String scenario = first + " + " + second;
        if (!report.reachedFinalWave()) {
            failures.add(scenario + " did not finish the run");
            return;
        }
        List<WaveSample> waves = report.waves();
        if (waves.size() != GameState.FINAL_WAVE) {
            failures.add(scenario + " sampled " + waves.size() + " waves");
            return;
        }

        float averageDamage = averageDamage(waves);
        float averageClearTime = averageClearTime(waves);
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
            .max(java.util.Comparator.comparing(WaveSample::damageFraction))
            .map(WaveSample::wave)
            .orElse(-1);
        System.out.println(
            first + "," + second + "," + averageDamage + "," + averageClearTime
                + "," + pressuredWaves + "," + maximumDamage + "," + maximumClear
        );
        check(failures, averageDamage >= MINIMUM_AVERAGE_DAMAGE_FRACTION,
            scenario + " trivialized average incoming pressure: " + averageDamage);
        check(failures, averageClearTime >= MINIMUM_AVERAGE_CLEAR_SECONDS,
            scenario + " trivialized average clear time: " + averageClearTime);
        check(failures,
            pressuredWaves >= Math.ceil(waves.size() * MINIMUM_PRESSURED_WAVE_FRACTION),
            scenario + " left too few pressured waves: " + pressuredWaves);
        check(failures, maximumDamage <= MAXIMUM_SINGLE_WAVE_DAMAGE_FRACTION,
            scenario + " caused a damage spike: " + maximumDamage + " at wave " + spikeWave);
        check(failures, maximumClear <= MAXIMUM_CLEAR_SECONDS,
            scenario + " caused a clear-time spike: " + maximumClear);
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
