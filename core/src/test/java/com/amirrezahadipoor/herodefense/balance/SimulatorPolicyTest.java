package com.amirrezahadipoor.herodefense.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.balance.BalanceSimulator.BalanceReport;
import org.junit.jupiter.api.Test;

/**
 * Phase 26.1c: the simulator plays the Ultimate and Evolutions the way the gate
 * assumes — the Ultimate fires on cooldown (the moment Focus fills) and every
 * Evolution purchase takes the higher-DPS fork ({@code SkillEvolution.simPick},
 * whose mapping is locked by SkillEvolutionTest).
 */
final class SimulatorPolicyTest {
    private static final long BASELINE_SEED = 0x4845524F444546L;

    @Test
    void ultimateFiresOnCooldownThroughTheRun() {
        BalanceSimulator sim = new BalanceSimulator();
        BalanceReport report = sim.run(BASELINE_SEED);
        assertTrue(report.reachedFinalWave());
        int fires = sim.lastLedger().ultimateFires;
        System.out.println("ultimate_fires: " + fires);
        assertTrue(fires >= 60, "The sim must fire the Ultimate on cooldown, fired " + fires);
    }

    @Test
    void simulatorBuysEvolutionsThroughTheRun() {
        BalanceSimulator sim = new BalanceSimulator();
        BalanceReport report = sim.run(BASELINE_SEED);
        assertTrue(report.reachedFinalWave());
        int evolutions = sim.lastLedger().evolutionsBought;
        System.out.println("evolutions_bought: " + evolutions);
        System.out.println("ledger: " + sim.lastLedger());
        assertEquals(1, evolutions, "The sim buys exactly one focused Evolution per run");
    }
}
