package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.input.GameOverTouchLayout;
import com.amirrezahadipoor.herodefense.input.LevelUpTouchLayout;
import com.amirrezahadipoor.herodefense.input.RewardCardTouchLayout;
import com.amirrezahadipoor.herodefense.input.SettingsTouchLayout;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Locks six coherent premium overlays and their existing generous touch geometry. */
final class PremiumOverlaySurfacesTest {
    private static final Path RENDERERS = Path.of(
        "src/main/java/com/amirrezahadipoor/herodefense/render"
    );

    @Test
    void victoryAndDefeatUseExplicitDistinctOutcomeLanguage() {
        assertEquals("WORLD TREE SAVED", GameOverOverlayRenderer.outcomeTitle(true));
        assertEquals("WORLD TREE FALLEN", GameOverOverlayRenderer.outcomeTitle(false));
    }

    @Test
    void everyTalentStatesItsExactGain() {
        Map<HeroStat, String> expected = Map.of(
            HeroStat.STRENGTH, "+2 damage",
            HeroStat.AGILITY, "+0.03 attacks / second",
            HeroStat.LUCK, "+2% item-drop multiplier",
            HeroStat.DODGE, "+0.5% dodge chance",
            HeroStat.HEALTH, "+10 maximum health"
        );
        for (Map.Entry<HeroStat, String> entry : expected.entrySet()) {
            assertEquals(entry.getValue(), LevelUpOverlayRenderer.description(entry.getKey()));
        }
    }

    @Test
    void allSixSurfacesRetainPhoneSafeTouchTargets() {
        assertTrue(SettingsTouchLayout.ROW_WIDTH >= 520f);
        assertTrue(SettingsTouchLayout.ROW_HEIGHT >= 150f);
        assertTrue(LevelUpTouchLayout.RIGHT - LevelUpTouchLayout.LEFT >= 540f);
        assertTrue(LevelUpTouchLayout.BUTTON_HEIGHT >= 130f);
        assertTrue(RewardCardTouchLayout.CARD_WIDTH >= 580f);
        assertTrue(RewardCardTouchLayout.CARD_HEIGHT >= 190f);
        assertTrue(GameOverTouchLayout.RESTART_WIDTH >= 480f);
        assertTrue(GameOverTouchLayout.RESTART_HEIGHT >= 160f);
    }

    @Test
    void everySurfaceUsesReviewedFramesAndExplicitContext() throws IOException {
        Map<String, String[]> contracts = Map.of(
            "InventoryOverlayRenderer.java", new String[] {
                "COMBAT PAUSED", "RESUME BATTLE", "STAT SHOP", "INVENTORY"
            },
            "SettingsOverlayRenderer.java", new String[] {
                "SETTINGS", "SOUND EFFECTS", "MUSIC", "ON", "OFF"
            },
            "LevelUpOverlayRenderer.java", new String[] {
                "CHOOSE ONE PERMANENT TALENT", "POINTS", "CURRENT", "TAP TO ADD"
            },
            "RewardCardOverlayRenderer.java", new String[] {
                "BOSS ", "CHOOSE ONE REWARD", "PERMANENT RUN BONUS", "TAP TO CLAIM"
            },
            "GameOverOverlayRenderer.java", new String[] {
                "VICTORY", "DEFEAT", "RUN LEDGER", "START A NEW DEFENSE"
            }
        );
        for (Map.Entry<String, String[]> contract : contracts.entrySet()) {
            String source = Files.readString(RENDERERS.resolve(contract.getKey()));
            assertTrue(source.contains("UiFrameRenderer.Kind."), contract.getKey());
            for (String required : contract.getValue()) {
                assertTrue(source.contains(required), contract.getKey() + ": " + required);
            }
        }
    }
}
