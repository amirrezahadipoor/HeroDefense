package com.amirrezahadipoor.herodefense.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.BalanceReport;
import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.WaveSample;
import com.amirrezahadipoor.herodefense.model.GameState;
import org.junit.jupiter.api.Test;

final class BalanceSimulatorTest {
    private static final long BASELINE_SEED = 0x4845524F444546L;
    private static final float MINIMUM_AVERAGE_DAMAGE_FRACTION = 0.05f;
    private static final float MAXIMUM_AVERAGE_DAMAGE_FRACTION = 0.15f;
    private static final float MAXIMUM_SINGLE_WAVE_DAMAGE_FRACTION = 0.35f;
    private static final float MAXIMUM_CLEAR_TIME_SECONDS = 120f;

    @Test
    void balancedRunPassesAcceptanceGateAndLogsEveryWave() {
        BalanceReport report = new BalanceSimulator().run(BASELINE_SEED);

        assertTrue(report.reachedWave100(), "Balanced baseline must complete the continuous run");
        assertEquals(GameState.FINAL_WAVE, report.waves().size());
        assertEquals(1, report.waves().get(0).wave());
        assertTrue(
            report.averageDamageFraction() >= MINIMUM_AVERAGE_DAMAGE_FRACTION
                && report.averageDamageFraction() <= MAXIMUM_AVERAGE_DAMAGE_FRACTION,
            "Average gross damage fraction was " + report.averageDamageFraction()
        );
        for (WaveSample sample : report.waves()) {
            assertTrue(Float.isFinite(sample.remainingHealth()));
            assertTrue(Float.isFinite(sample.dpsToEnemyHpRatio()));
            assertTrue(Float.isFinite(sample.clearTimeSeconds()));
            assertTrue(sample.clearTimeSeconds() > 0f);
            assertTrue(
                sample.damageFraction() <= MAXIMUM_SINGLE_WAVE_DAMAGE_FRACTION,
                "Wave " + sample.wave() + " damage spike was " + sample.damageFraction()
            );
            assertTrue(
                sample.clearTimeSeconds() <= MAXIMUM_CLEAR_TIME_SECONDS,
                "Wave " + sample.wave() + " clear-time spike was " + sample.clearTimeSeconds()
            );
            assertFalse(sample.timedOut(), "Wave " + sample.wave() + " timed out");
        }
        String csv = report.toCsv();
        assertTrue(csv.startsWith("wave,start_hp,start_max_hp,remaining_hp"));
        assertEquals(report.waves().size() + 1L, csv.lines().count());
        System.out.print(csv);
    }
}
