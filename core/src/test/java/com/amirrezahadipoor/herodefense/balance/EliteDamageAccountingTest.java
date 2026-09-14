package com.amirrezahadipoor.herodefense.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.BalanceReport;
import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.WaveSample;
import com.amirrezahadipoor.herodefense.gameplay.EnemyWaveSpawner;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Phase 26.1b: Elite waves are included in the simulator's gross-damage books,
 * not exempted. The accounting path shipped with the Elites themselves (25.2 /
 * 25.2b): the spawner marks 1-2 elites on Elite waves, and elite melee,
 * blightburst blasts, and weeping rot all land inside the measured HP-delta
 * window that feeds every WaveSample. This test locks the inclusion at the
 * simulator level; the affix mechanics themselves are locked by
 * EliteAffixSystemTest.
 */
final class EliteDamageAccountingTest {
    private static final long BASELINE_SEED = 0x4845524F444546L;

    @Test
    void eliteWavesArePresentAndTheirDamageReachesTheBooks() {
        BalanceReport report = new BalanceSimulator().run(BASELINE_SEED);
        assertTrue(report.reachedFinalWave());

        List<WaveSample> elite = report.waves().stream()
            .filter(sample -> EnemyWaveSpawner.isEliteWave(sample.wave(), 0))
            .toList();
        assertEquals(23, elite.size(), "Elite waves must spawn inside simulated runs");

        float eliteDamage = 0f;
        long pressured = 0;
        for (WaveSample sample : elite) {
            eliteDamage += sample.damageFraction();
            if (sample.damageFraction() >= 0.01f) pressured++;
        }
        System.out.println("elite_waves,elite_damage,pressured: "
            + elite.size() + "," + eliteDamage + "," + pressured);
        assertTrue(eliteDamage > 0f, "Elite waves must threaten through the sim's accounting");
        assertTrue(pressured >= 1, "At least one Elite wave must land pressured damage");

        float total = 0f;
        for (WaveSample sample : report.waves()) total += sample.damageFraction();
        float averageIncludingElites = total / report.waves().size();
        float averageExcludingElites =
            (total - eliteDamage) / (report.waves().size() - elite.size());
        assertTrue(averageIncludingElites > averageExcludingElites,
            "Reported average must include Elite-wave damage");
    }
}
