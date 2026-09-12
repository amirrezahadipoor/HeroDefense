package com.amirrezahadipoor.herodefense.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.BalanceReport;
import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.WaveSample;
import org.junit.jupiter.api.Test;

final class BalanceSimulatorTest {
    private static final long BASELINE_SEED = 0x4845524F444546L;

    @Test
    void logsRendererIndependentWaveMetrics() {
        BalanceReport report = new BalanceSimulator().run(BASELINE_SEED);

        assertFalse(report.waves().isEmpty());
        assertEquals(1, report.waves().get(0).wave());
        for (WaveSample sample : report.waves()) {
            assertTrue(Float.isFinite(sample.remainingHealth()));
            assertTrue(Float.isFinite(sample.dpsToEnemyHpRatio()));
            assertTrue(Float.isFinite(sample.clearTimeSeconds()));
            assertTrue(sample.clearTimeSeconds() > 0f);
            assertFalse(sample.timedOut(), "Wave " + sample.wave() + " timed out");
        }
        String csv = report.toCsv();
        assertTrue(csv.startsWith("wave,start_hp,start_max_hp,remaining_hp"));
        assertEquals(report.waves().size() + 1L, csv.lines().count());
        System.out.print(csv);
    }
}
