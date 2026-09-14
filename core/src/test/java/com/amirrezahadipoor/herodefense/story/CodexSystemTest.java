package com.amirrezahadipoor.herodefense.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CodexSystemTest {
    private final CodexSystem codex = new CodexSystem();

    @Test
    void freshRunUnlocksOnlyTheWaveOneEntry() {
        GameState state = GameState.newRun(7L);
        List<String> unlocked = codex.unlockForWaveReached(state);
        assertEquals(List.of("codex_01"), unlocked);
        assertTrue(codex.isUnlocked(state, "codex_01"));
        assertEquals(1, codex.unlockedCount(state));
    }

    @Test
    void waveUnlocksAreCumulativeAndIdempotent() {
        GameState state = GameState.newRun(7L);
        state.waveNumber = 30;
        assertEquals(List.of("codex_01", "codex_02", "codex_03", "codex_04"), codex.unlockForWaveReached(state));
        assertTrue(codex.unlockForWaveReached(state).isEmpty());
        state.waveNumber = 100;
        assertEquals(
            List.of("codex_05", "codex_06", "codex_07", "codex_08"), codex.unlockForWaveReached(state));
        assertEquals(8, codex.unlockedCount(state));
    }

    @Test
    void firstEliteKillOfEachAffixUnlocksItsOwnEntry() {
        GameState state = GameState.newRun(7L);
        assertEquals(List.of("codex_13"), codex.unlockForEliteKill(state, "blightburst"));
        assertTrue(codex.unlockForEliteKill(state, "blightburst").isEmpty());
        assertEquals(List.of("codex_14"), codex.unlockForEliteKill(state, "rootward_ward"));
        assertEquals(List.of("codex_15"), codex.unlockForEliteKill(state, "weeping_rot"));
        assertTrue(codex.unlockForEliteKill(state, null).isEmpty());
        assertTrue(codex.unlockForEliteKill(state, "unknown_affix").isEmpty());
    }

    @Test
    void secretTwentyEightNeedsAllThreeAffixesKilled() {
        GameState state = GameState.newRun(7L);
        state.eliteKillCounts.put("blightburst", 2);
        state.eliteKillCounts.put("rootward_ward", 1);
        assertEquals(List.of("codex_13"), codex.unlockForEliteKill(state, "blightburst"));
        assertEquals(List.of("codex_14"), codex.unlockForEliteKill(state, "rootward_ward"));
        assertFalse(codex.isUnlocked(state, "codex_28"));
        state.eliteKillCounts.put("weeping_rot", 1);
        assertEquals(
            List.of("codex_15", "codex_28"), codex.unlockForEliteKill(state, "weeping_rot"));
        assertTrue(codex.isUnlocked(state, "codex_28"));
    }

    @Test
    void bossKillUnlocksOnlyItsOwnIdentityEntry() {
        GameState state = GameState.newRun(7L);
        assertEquals(List.of("codex_11"), codex.unlockForBossKill(state, "EMBER_WYRM"));
        assertTrue(codex.unlockForBossKill(state, "EMBER_WYRM").isEmpty());
        assertTrue(codex.unlockForBossKill(state, "ANCIENT_GOLEM").size() == 1);
        assertFalse(codex.isUnlocked(state, "codex_10"));
        assertFalse(codex.isUnlocked(state, "codex_12"));
    }

    @Test
    void eliteKillUnlocksOnlyItsOwnAffixEntry() {
        GameState state = GameState.newRun(7L);
        assertEquals(List.of("codex_14"), codex.unlockForEliteKill(state, "rootward_ward"));
        assertTrue(codex.unlockForEliteKill(state, "rootward_ward").isEmpty());
        assertFalse(codex.isUnlocked(state, "codex_13"));
        assertFalse(codex.isUnlocked(state, "codex_15"));
    }

    @Test
    void ascensionUnlocksFollowCompletedTierThresholds() {
        GameState state = GameState.newRun(7L);
        assertTrue(codex.unlockForAscension(state).isEmpty());
        state.ascensionTier = 1;
        assertEquals(List.of("codex_16"), codex.unlockForAscension(state));
        state.ascensionTier = 5;
        assertEquals(List.of("codex_17", "codex_18", "codex_19"), codex.unlockForAscension(state));
        state.ascensionTier = 10;
        assertEquals(List.of("codex_20"), codex.unlockForAscension(state));
        assertEquals(5, codex.unlockedCount(state));
    }

    @Test
    void unknownIdsAndNullStatesStayLocked() {
        GameState state = GameState.newRun(7L);
        assertFalse(codex.unlock(state, "codex_99"));
        assertFalse(codex.unlock(state, null));
        assertTrue(codex.unlockForWaveReached(null).isEmpty());
        assertTrue(codex.unlockForBossKill(state, null).isEmpty());
        assertTrue(codex.unlockForBossKill(state, "NOPE").isEmpty());
        assertTrue(codex.unlockForEliteKill(state, "nope").isEmpty());
        assertEquals(0, codex.unlockedCount(null));
    }

    @Test
    void bareHandedNeedsWaveFiftyWithNoShopStats() {
        GameState clean = GameState.newRun(11L);
        clean.waveNumber = 50;
        assertEquals(List.of("codex_21"), codex.unlockSecretsForProgress(clean));

        GameState buyer = GameState.newRun(11L);
        buyer.waveNumber = 60;
        buyer.shopStatsBoughtThisRun = 3;
        assertTrue(codex.unlockSecretsForProgress(buyer).stream().noneMatch("codex_21"::equals));
        assertFalse(codex.isUnlocked(buyer, "codex_21"));
    }

    @Test
    void noPotionsNeedsWaveOneHundredOneOnACleanRun() {
        GameState clean = GameState.newRun(12L);
        clean.waveNumber = 101;
        assertTrue(codex.unlockSecretsForProgress(clean).contains("codex_26"));

        GameState user = GameState.newRun(12L);
        user.waveNumber = 150;
        user.noPotionRun = false;
        assertFalse(codex.unlockSecretsForProgress(user).contains("codex_26"));
        assertFalse(codex.isUnlocked(user, "codex_26"));
        GameState early = GameState.newRun(12L);
        early.waveNumber = 100;
        assertFalse(codex.unlockSecretsForProgress(early).contains("codex_26"));
    }

    @Test
    void fastestFallNeedsARecordClearUnderTenSeconds() {
        GameState slow = GameState.newRun(13L);
        slow.fastestWaveClearSeconds = 42f;
        assertFalse(codex.unlockSecretsForProgress(slow).contains("codex_29"));

        GameState fast = GameState.newRun(13L);
        fast.fastestWaveClearSeconds = 9.5f;
        assertEquals(List.of("codex_29"), codex.unlockSecretsForProgress(fast));
    }

    @Test
    void waveTwoHundredTwiceNeedsTwoRecordedReaches() {
        GameState first = GameState.newRun(14L);
        first.waveNumber = 200;
        first.wave200ReachedCount = 1;
        assertFalse(codex.unlockSecretsForProgress(first).contains("codex_30"));

        GameState second = GameState.newRun(14L);
        second.waveNumber = 200;
        second.wave200ReachedCount = 2;
        second.shopStatsBoughtThisRun = 4;
        second.noPotionRun = false;
        assertEquals(List.of("codex_30"), codex.unlockSecretsForProgress(second));
    }

    @Test
    void masteryNeedsAnySkillAtLevelTen() {
        GameState state = GameState.newRun(15L);
        state.skillLevels.put("chain_lightning", 9);
        assertTrue(codex.unlockSecretsForSkillPurchase(state).isEmpty());
        state.skillLevels.put("chain_lightning", 10);
        assertEquals(List.of("codex_23"), codex.unlockSecretsForSkillPurchase(state));
        assertTrue(codex.unlockSecretsForSkillPurchase(state).isEmpty());
    }

    @Test
    void aFullSetNeedsFourPiecesOfOneSetEquipped() {
        GameState state = GameState.newRun(15L);
        equipById(state, "WEAPON", "verdant_recurve");
        equipById(state, "HELMET", "fern_guard");
        equipById(state, "ARMOR", "mossweave_coat");
        assertTrue(codex.unlockSecretsForEquipment(state).isEmpty());

        equipById(state, "BOOTS", "windstep_boots");
        assertEquals(List.of("codex_22"), codex.unlockSecretsForEquipment(state));
        assertTrue(codex.unlockSecretsForEquipment(state).isEmpty());
    }

    @Test
    void sixMythicsNeedsAllSixOwnedAtOnce() {
        GameState state = GameState.newRun(21L);
        for (String id : List.of(
            "sunfall_last_arrow",
            "crown_hollow_eye",
            "bark_first_root",
            "windrunner_last_steps",
            "verdant_oath"
        )) {
            state.inventory.add(EquipmentCatalog.byId(id).createItem());
        }
        assertTrue(codex.unlockSecretsForEquipment(state).isEmpty());

        equipById(state, "RING_2", "emberless_core");
        assertEquals(List.of("codex_25"), codex.unlockSecretsForEquipment(state));
        assertTrue(codex.unlockSecretsForEquipment(state).isEmpty());
    }

    @Test
    void reforgedNeedsAnyItemAtPlusFive() {
        GameState state = GameState.newRun(16L);
        Item close = new Item("starfall_bow", "Starfall Bow", "WEAPON", "RARE");
        close.upgradeLevel = 4;
        state.inventory.add(close);
        assertTrue(codex.unlockSecretsForForge(state).isEmpty());

        close.upgradeLevel = 5;
        assertEquals(List.of("codex_24"), codex.unlockSecretsForForge(state));
    }

    @Test
    void reforgedAlsoSeesEquippedItems() {
        GameState state = GameState.newRun(17L);
        Item equipped = new Item("worldbranch", "Worldbranch", "WEAPON", "LEGENDARY");
        equipped.upgradeLevel = 5;
        state.equippedItems.put("WEAPON", equipped);
        assertEquals(List.of("codex_24"), codex.unlockSecretsForForge(state));
    }

    @Test
    void longPauseNeedsAThreeHundredSecondResume() {
        GameState state = GameState.newRun(18L);
        state.longestPauseSeconds = 299f;
        assertTrue(codex.unlockSecretsForPause(state).isEmpty());
        state.longestPauseSeconds = 300f;
        assertEquals(List.of("codex_27"), codex.unlockSecretsForPause(state));
    }

    @Test
    void futurePhaseSecretsStayLockedWithoutTheirSources() {
        GameState maxed = GameState.newRun(19L);
        maxed.waveNumber = 200;
        maxed.wave200ReachedCount = 2;
        maxed.noPotionRun = true;
        maxed.fastestWaveClearSeconds = 5f;
        maxed.longestPauseSeconds = 999f;
        maxed.skillLevels.put("chain_lightning", 12);
        Item forged = new Item("worldbranch", "Worldbranch", "WEAPON", "LEGENDARY");
        forged.upgradeLevel = 5;
        maxed.inventory.add(forged);
        codex.unlockForWaveReached(maxed);
        codex.unlockSecretsForProgress(maxed);
        codex.unlockSecretsForSkillPurchase(maxed);
        codex.unlockSecretsForForge(maxed);
        codex.unlockSecretsForPause(maxed);
        assertFalse(codex.isUnlocked(maxed, "codex_22"));
        assertFalse(codex.isUnlocked(maxed, "codex_25"));
        assertFalse(codex.isUnlocked(maxed, "codex_28"));
        assertTrue(codex.isUnlocked(maxed, "codex_21"));
        assertTrue(codex.isUnlocked(maxed, "codex_23"));
        assertTrue(codex.isUnlocked(maxed, "codex_24"));
        assertTrue(codex.isUnlocked(maxed, "codex_26"));
        assertTrue(codex.isUnlocked(maxed, "codex_27"));
        assertTrue(codex.isUnlocked(maxed, "codex_29"));
        assertTrue(codex.isUnlocked(maxed, "codex_30"));
    }

    private static void equipById(GameState state, String slot, String id) {
        state.equippedItems.put(
            slot, com.amirrezahadipoor.herodefense.items.EquipmentCatalog.byId(id).createItem()
        );
    }

    @Test
    void waveTwoHundredCountSurvivesResetWhileTheTimerRestarts() {
        GameState state = GameState.newRun(20L);
        state.wave200ReachedCount = 1;
        state.waveElapsedSeconds = 33f;
        state.fastestWaveClearSeconds = 33f;
        state.resetForNewRun(21L);
        assertEquals(1, state.wave200ReachedCount);
        assertEquals(0f, state.waveElapsedSeconds);
    }
}
