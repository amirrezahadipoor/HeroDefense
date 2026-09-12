package com.amirrezahadipoor.herodefense.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.BalanceReport;
import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.WaveSample;
import com.amirrezahadipoor.herodefense.rewards.RewardCardId;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RewardCardSimulationTest {
    private static final long SEED = 0x4341524453494DL;
    private static final float MINIMUM_AVERAGE_DAMAGE_FRACTION = 0.05f;
    private static final float MINIMUM_AVERAGE_CLEAR_SECONDS = 25f;
    private static final float MINIMUM_PRESSURED_WAVE_FRACTION = 0.90f;
    private static final float MAXIMUM_SINGLE_WAVE_DAMAGE_FRACTION = 0.35f;
    private static final float MAXIMUM_CLEAR_SECONDS = 120f;

    @Test
    void noSingleCardAtAnyBossTrivializesTheRemainingRun() {
        System.out.println(
            "card,boss,average_damage_fraction,average_clear_seconds,pressured_waves,"
                + "maximum_damage_fraction,maximum_clear_seconds"
        );
        for (int bossNumber = 1; bossNumber < 20; bossNumber++) {
            for (RewardCardId card : RewardCardId.values()) {
                verifyScenario(card, bossNumber);
            }
        }
    }

    private static void verifyScenario(RewardCardId card, int bossNumber) {
        BalanceReport report = new BalanceSimulator().runWithForcedCard(
            SEED,
            card,
            bossNumber
        );
        assertTrue(report.reachedWave100(), scenario(card, bossNumber) + " did not finish");
        List<WaveSample> remaining = report.waves().stream()
            .filter(sample -> sample.wave() > bossNumber * 5)
            .toList();
        assertEquals(100 - bossNumber * 5, remaining.size());

        float averageDamage = averageDamage(remaining);
        float averageClearTime = averageClearTime(remaining);
        long pressuredWaves = remaining.stream()
            .filter(sample -> sample.damageFraction() >= 0.01f)
            .count();
        float maximumDamage = remaining.stream()
            .map(WaveSample::damageFraction)
            .max(Float::compare)
            .orElse(0f);
        float maximumClear = remaining.stream()
            .map(WaveSample::clearTimeSeconds)
            .max(Float::compare)
            .orElse(0f);
        System.out.println(
            card + "," + bossNumber + "," + averageDamage + "," + averageClearTime
                + "," + pressuredWaves + "," + maximumDamage + "," + maximumClear
        );
        String scenario = scenario(card, bossNumber);
        assertTrue(
            averageDamage >= MINIMUM_AVERAGE_DAMAGE_FRACTION,
            scenario + " trivialized average incoming pressure: " + averageDamage
        );
        assertTrue(
            averageClearTime >= MINIMUM_AVERAGE_CLEAR_SECONDS,
            scenario + " trivialized average clear time: " + averageClearTime
        );
        assertTrue(
            pressuredWaves >= Math.ceil(remaining.size() * MINIMUM_PRESSURED_WAVE_FRACTION),
            scenario + " left too few pressured waves: " + pressuredWaves
        );
        assertTrue(
            maximumDamage <= MAXIMUM_SINGLE_WAVE_DAMAGE_FRACTION,
            scenario + " caused a damage spike: " + maximumDamage
        );
        assertTrue(
            maximumClear <= MAXIMUM_CLEAR_SECONDS,
            scenario + " caused a clear-time spike: " + maximumClear
        );
    }

    private static String scenario(RewardCardId card, int bossNumber) {
        return card + " forced at Boss " + bossNumber;
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
