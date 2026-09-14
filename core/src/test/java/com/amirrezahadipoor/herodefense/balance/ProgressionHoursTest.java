package com.amirrezahadipoor.herodefense.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.ascension.RootNetworkCatalog;
import com.amirrezahadipoor.herodefense.ascension.RootNodeDefinition;
import com.amirrezahadipoor.herodefense.gameplay.EnemyFactory;
import com.amirrezahadipoor.herodefense.gameplay.EnemyWaveSpawner;
import com.amirrezahadipoor.herodefense.gameplay.ItemDropSystem;
import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.items.EquipmentDefinition;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.ItemTier;
import com.amirrezahadipoor.herodefense.story.LoreCatalog;
import com.amirrezahadipoor.herodefense.story.LoreEntry;
import com.amirrezahadipoor.herodefense.story.LoreTrigger;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Phase 26.3: locks the shipped inputs of the 50-hour equation in
 * {@code docs/PROGRESSION_HOURS.md}, so every later balance pass re-checks the
 * target automatically instead of re-deriving it by hand.
 */
final class ProgressionHoursTest {
    @Test
    void rootNetworkCosts1350HeartwoodAcross23Nodes() {
        int total = 0;
        for (RootNodeDefinition node : RootNetworkCatalog.all()) total += node.cost();
        assertEquals(23, RootNetworkCatalog.all().size());
        assertEquals(1350, total);
    }

    @Test
    void nineFlawlessFullClearsExhaustTheNetwork() {
        int income = 0;
        for (int tier = 0; tier <= 8; tier++) {
            income += GameState.calculateHeartwoodReward(GameState.FINAL_WAVE, tier, true);
        }
        assertEquals(1350, income);
    }

    @Test
    void tenNonFlawlessFullClearsExhaustTheNetwork() {
        int income = 0;
        for (int tier = 0; tier <= 9; tier++) {
            income += GameState.calculateHeartwoodReward(GameState.FINAL_WAVE, tier, false);
        }
        assertEquals(1350, income);
    }

    @Test
    void codexHas30EntriesWithSlowestGateAt10Ascensions() {
        assertEquals(30, LoreCatalog.all().size());
        Map<LoreTrigger, Integer> counts = new EnumMap<>(LoreTrigger.class);
        int slowestAscension = 0;
        for (LoreEntry entry : LoreCatalog.all()) {
            counts.merge(entry.trigger(), 1, Integer::sum);
            if (entry.trigger() == LoreTrigger.ASCENSION) {
                slowestAscension = Math.max(slowestAscension, Integer.parseInt(entry.triggerParam()));
            }
        }
        assertEquals(8, counts.get(LoreTrigger.WAVE_MILESTONE));
        assertEquals(4, counts.get(LoreTrigger.BOSS_FIRST_KILL));
        assertEquals(3, counts.get(LoreTrigger.ELITE_KILL));
        assertEquals(5, counts.get(LoreTrigger.ASCENSION));
        assertEquals(10, counts.get(LoreTrigger.SECRET));
        assertEquals(10, slowestAscension);
    }

    @Test
    void sixMythicsExistOnePerSlot() {
        Set<String> slots = new HashSet<>();
        int mythics = 0;
        for (EquipmentDefinition item : EquipmentCatalog.all()) {
            if (item.tier() == ItemTier.MYTHIC) {
                mythics++;
                slots.add(item.slot().name());
            }
        }
        assertEquals(6, mythics);
        assertEquals(EquipmentSlot.values().length, slots.size());
    }

    @Test
    void mythicPerKillRateDerivesFromShippedConstants() {
        assertEquals(0.0015f, ItemDropSystem.LEGENDARY_RATE, 0.0f);
        assertEquals(0.01f, ItemDropSystem.MYTHIC_SHARE_OF_LEGENDARY, 0.0f);
        ItemDropSystem drops = new ItemDropSystem();
        // Per-kill Mythic threshold is LEGENDARY_RATE x share x luck multiplier.
        assertEquals(ItemTier.MYTHIC, drops.tierForRoll(0.000010f, 1.0f));
        assertEquals(ItemTier.LEGENDARY, drops.tierForRoll(0.000020f, 1.0f));
        assertEquals(ItemTier.MYTHIC, drops.tierForRoll(0.000020f, 2.0f));
    }

    @Test
    void fullClearSpawns4440KillableBodies() {
        EnemyWaveSpawner spawner = new EnemyWaveSpawner(new EnemyFactory());
        int regulars = 0;
        for (int wave = 1; wave <= GameState.FINAL_WAVE; wave++) {
            regulars += spawner.regularCountForWave(wave);
        }
        int bosses = GameState.FINAL_WAVE / 5;
        assertEquals(4400, regulars);
        assertEquals(40, bosses);
        assertTrue(regulars + bosses == 4440);
    }
}
