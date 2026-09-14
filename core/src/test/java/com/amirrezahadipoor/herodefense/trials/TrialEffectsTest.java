package com.amirrezahadipoor.herodefense.trials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TrialEffectsTest {
    private static List<String> trials(TrialId... ids) {
        String[] names = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            names[i] = ids[i].name();
        }
        return Arrays.asList(names);
    }

    @Test
    void nullOrEmptyTrialsAlwaysYieldIdentity() {
        assertEquals(1f, TrialEffects.enemySpeedMultiplier(null));
        assertEquals(1f, TrialEffects.coinIncomeMultiplier(Collections.emptyList()));
        assertTrue(TrialEffects.potionsDrop(null));
        assertEquals(0, TrialEffects.bonusTalentPointsPerLevel(Collections.emptyList()));
        assertEquals(1f, TrialEffects.bossDamageMultiplier(null));
        assertFalse(TrialEffects.bossAlwaysDropsRarePlus(null));
        assertEquals(1f, TrialEffects.heroMaxHealthMultiplier(Collections.emptyList()));
        assertEquals(1f, TrialEffects.heroDamageMultiplier(null));
        assertEquals(1f, TrialEffects.heroAttackSpeedMultiplier(null));
        assertEquals(0, TrialEffects.extraEnemiesPerWave(null));
        assertEquals(1f, TrialEffects.experienceMultiplier(null));
        assertEquals(1f, TrialEffects.enemyHealthMultiplier(null));
        assertEquals(1f, TrialEffects.itemDropChanceMultiplier(null));
        assertEquals(1f, TrialEffects.bossHealthMultiplier(null));
        assertEquals(1f, TrialEffects.heartwoodMultiplier(null));
        assertEquals(1f, TrialEffects.shopPriceMultiplier(null));
        assertEquals(0, TrialEffects.startingCoinsBonus(null));
        assertEquals(0f, TrialEffects.dodgeChanceBonus(null));
        assertEquals(1f, TrialEffects.damageTakenMultiplier(null));
        assertEquals(0f, TrialEffects.lifestealBonus(null));
        assertEquals(1f, TrialEffects.enemyDamageMultiplier(null));
    }

    @Test
    void everyTrialAppliesItsRiskAndReward() {
        List<String> swift = trials(TrialId.SWIFT_HOLLOW);
        assertEquals(1.25f, TrialEffects.enemySpeedMultiplier(swift));
        assertEquals(1.3f, TrialEffects.coinIncomeMultiplier(swift));

        List<String> dry = trials(TrialId.DRY_VEINS);
        assertFalse(TrialEffects.potionsDrop(dry));
        assertEquals(1, TrialEffects.bonusTalentPointsPerLevel(dry));

        List<String> crowns = trials(TrialId.HEAVY_CROWNS);
        assertEquals(1.3f, TrialEffects.bossDamageMultiplier(crowns));
        assertTrue(TrialEffects.bossAlwaysDropsRarePlus(crowns));

        List<String> thin = trials(TrialId.THIN_BLOOD);
        assertEquals(0.75f, TrialEffects.heroMaxHealthMultiplier(thin));
        assertEquals(1.25f, TrialEffects.heroDamageMultiplier(thin));

        List<String> glass = trials(TrialId.GLASS_ARROWS);
        assertEquals(0.8f, TrialEffects.heroDamageMultiplier(glass));
        assertEquals(1.25f, TrialEffects.heroAttackSpeedMultiplier(glass));

        List<String> tide = trials(TrialId.IRON_TIDE);
        assertEquals(3, TrialEffects.extraEnemiesPerWave(tide));
        assertEquals(1.25f, TrialEffects.experienceMultiplier(tide));

        List<String> stone = trials(TrialId.STONE_SKIN);
        assertEquals(1.3f, TrialEffects.enemyHealthMultiplier(stone));
        assertEquals(1.5f, TrialEffects.itemDropChanceMultiplier(stone));

        List<String> bounty = trials(TrialId.BOSS_BOUNTY);
        assertEquals(1.3f, TrialEffects.bossHealthMultiplier(bounty));
        assertEquals(1.3f, TrialEffects.heartwoodMultiplier(bounty));

        List<String> miser = trials(TrialId.MISERS_PACT);
        assertEquals(1.3f, TrialEffects.shopPriceMultiplier(miser));
        assertEquals(200, TrialEffects.startingCoinsBonus(miser));

        List<String> famished = trials(TrialId.FAMISHED_EARTH);
        assertEquals(0.7f, TrialEffects.coinIncomeMultiplier(famished));
        assertEquals(0.10f, TrialEffects.dodgeChanceBonus(famished));

        List<String> blood = trials(TrialId.BLOOD_PRICE);
        assertEquals(1.2f, TrialEffects.damageTakenMultiplier(blood));
        assertEquals(0.04f, TrialEffects.lifestealBonus(blood));

        List<String> calling = trials(TrialId.HOLLOW_CALLING);
        assertEquals(1.2f, TrialEffects.enemyDamageMultiplier(calling));
        assertEquals(1.15f, TrialEffects.heroMaxHealthMultiplier(calling));
    }

    @Test
    void multipliersStackAcrossCombinedTrials() {
        List<String> pair = trials(TrialId.SWIFT_HOLLOW, TrialId.FAMISHED_EARTH);
        assertEquals(1.3f * 0.7f, TrialEffects.coinIncomeMultiplier(pair), 0.0001f);
        List<String> blood = trials(TrialId.THIN_BLOOD, TrialId.HOLLOW_CALLING);
        assertEquals(0.75f * 1.15f, TrialEffects.heroMaxHealthMultiplier(blood), 0.0001f);
    }
}
