package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.HeroStatCalculator;
import com.amirrezahadipoor.herodefense.input.GameOverTouchLayout;
import com.amirrezahadipoor.herodefense.input.LevelUpTouchLayout;
import com.amirrezahadipoor.herodefense.input.PauseTouchLayout;
import com.amirrezahadipoor.herodefense.input.RewardCardTouchLayout;
import com.amirrezahadipoor.herodefense.input.SettingsTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.rewards.RewardCardId;
import com.amirrezahadipoor.herodefense.rewards.RewardPowerBudget;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks the coherent premium-v2 treatment of Pause, Settings, Level-Up, Reward, and end-of-run. */
final class PremiumFlowSurfacesPresentationTest {
    private static final Path RENDER = Path.of(
        "src/main/java/com/amirrezahadipoor/herodefense/render"
    );

    @Test
    void pauseKeepsGenerousTargetsAndDescribesRunContext() {
        assertTrue(PauseTouchLayout.BUTTON_WIDTH >= 480f);
        assertTrue(PauseTouchLayout.RESUME_HEIGHT >= 200f);
        assertTrue(PauseTouchLayout.SECONDARY_HEIGHT >= 130f);
        assertTrue(PauseTouchLayout.resumeAt(360f, 600f));
        assertTrue(PauseTouchLayout.inventoryAt(360f, 830f));
        assertTrue(PauseTouchLayout.shopAt(360f, 1_000f));
        assertFalse(PauseTouchLayout.resumeAt(360f, PauseOverlayRenderer.CONTEXT_PANEL_Y + 40f));

        GameState state = GameState.newRun(1L);
        state.waveNumber = 5;
        assertEquals("BOSS WAVE", PauseOverlayRenderer.waveLabel(state));
        state.waveNumber = 6;
        state.heroLevel = 12;
        assertEquals("HERO LEVEL 12", PauseOverlayRenderer.waveLabel(state));
    }

    @Test
    void settingsTogglesStateExplicitlyWithoutColorAlone() {
        assertEquals("ON", SettingsOverlayRenderer.toggleLabel(true));
        assertEquals("OFF", SettingsOverlayRenderer.toggleLabel(false));
        assertEquals(SettingsTouchLayout.Action.CLOSE, SettingsTouchLayout.actionAt(
            SettingsOverlayRenderer.CLOSE_X + 50f, SettingsOverlayRenderer.CLOSE_Y + 50f
        ));
        assertEquals(SettingsTouchLayout.Action.TOGGLE_SOUND, SettingsTouchLayout.actionAt(
            360f, SettingsOverlayRenderer.SOUND_ROW_Y + 10f
        ));
        assertEquals(SettingsTouchLayout.Action.TOGGLE_MUSIC, SettingsTouchLayout.actionAt(
            360f, SettingsOverlayRenderer.MUSIC_ROW_Y + 10f
        ));
        assertTrue(SettingsOverlayRenderer.CLOSE_SIZE >= 96f);
    }

    @Test
    void levelUpPreviewsTheExactNextValueForEveryStat() {
        HeroStatCalculator calculator = new HeroStatCalculator();
        GameState state = GameState.newRun(7L);
        assertEquals("10 dmg", LevelUpOverlayRenderer.currentValue(calculator, state, HeroStat.STRENGTH));
        assertEquals("12 dmg", LevelUpOverlayRenderer.nextValue(calculator, state, HeroStat.STRENGTH));
        assertEquals("1 aps", LevelUpOverlayRenderer.currentValue(calculator, state, HeroStat.AGILITY));
        assertEquals("1.03 aps", LevelUpOverlayRenderer.nextValue(calculator, state, HeroStat.AGILITY));
        assertEquals("x1.00", LevelUpOverlayRenderer.currentValue(calculator, state, HeroStat.LUCK));
        assertEquals("x1.02", LevelUpOverlayRenderer.nextValue(calculator, state, HeroStat.LUCK));
        assertEquals("0%", LevelUpOverlayRenderer.currentValue(calculator, state, HeroStat.DODGE));
        assertEquals("0.5%", LevelUpOverlayRenderer.nextValue(calculator, state, HeroStat.DODGE));
        assertEquals("100 HP", LevelUpOverlayRenderer.currentValue(calculator, state, HeroStat.HEALTH));
        assertEquals("110 HP", LevelUpOverlayRenderer.nextValue(calculator, state, HeroStat.HEALTH));

        state.hero.stats.dodge = 200;
        assertEquals("60%", LevelUpOverlayRenderer.nextValue(calculator, state, HeroStat.DODGE));

        assertEquals("1 TALENT POINT TO SPEND", LevelUpOverlayRenderer.pointsLabel(1));
        assertEquals("3 TALENT POINTS TO SPEND", LevelUpOverlayRenderer.pointsLabel(3));
        for (HeroStat stat : HeroStat.values()) {
            assertTrue(LevelUpOverlayRenderer.gainPerPoint(stat).startsWith("+"), stat.name());
        }
        assertEquals(LevelUpTouchLayout.BOTTOM, LevelUpOverlayRenderer.rowY(0));
        assertTrue(LevelUpOverlayRenderer.rowY(4) + LevelUpTouchLayout.BUTTON_HEIGHT
            < LevelUpOverlayRenderer.HEADER_PANEL_Y);
    }

    @Test
    void rewardCardsExplainBossContextBudgetAndPermanence() {
        RewardPowerBudget budget = new RewardPowerBudget();
        assertEquals("BOSS 1 OF 20 DEFEATED", RewardCardOverlayRenderer.bossLabel(1));
        assertEquals("BOSS 20 OF 20 DEFEATED", RewardCardOverlayRenderer.bossLabel(99));
        assertEquals("Reward power 100%  |  scales with every boss you defeat",
            RewardCardOverlayRenderer.budgetLabel(budget, 1));
        assertEquals("Reward power 195%  |  scales with every boss you defeat",
            RewardCardOverlayRenderer.budgetLabel(budget, 20));
        for (RewardCardId card : RewardCardId.values()) {
            assertTrue(RewardCardOverlayRenderer.effectKind(card).length() >= 20, card.name());
        }
        assertEquals(RewardCardTouchLayout.FIRST_CARD_Y, RewardCardOverlayRenderer.cardY(0));
        assertTrue(RewardCardOverlayRenderer.cardY(0) + RewardCardTouchLayout.CARD_HEIGHT
            < RewardCardOverlayRenderer.HEADER_PANEL_Y);
        assertTrue(RewardCardOverlayRenderer.cardY(2)
            > RewardCardOverlayRenderer.FOOTER_PANEL_Y + RewardCardOverlayRenderer.FOOTER_PANEL_HEIGHT);
    }

    @Test
    void endOfRunDistinguishesVictoryFromDefeatAndKeepsRevealTiming() {
        assertNotEquals(GameOverOverlayRenderer.title(true), GameOverOverlayRenderer.title(false));
        assertTrue(GameOverOverlayRenderer.subtitle(false, 37).contains("wave 37"));
        assertTrue(GameOverOverlayRenderer.subtitle(true, 100).contains("100 waves"));
        assertEquals(0f, GameOverOverlayRenderer.revealProgress(0.5f, false));
        assertTrue(GameOverOverlayRenderer.isInteractive(0f, true));
        float lastRow = GameOverOverlayRenderer.summaryRowY(GameOverOverlayRenderer.SUMMARY_ROWS - 1);
        assertTrue(lastRow - 30f > GameOverOverlayRenderer.SUMMARY_PANEL_Y);
        assertTrue(GameOverOverlayRenderer.summaryRowY(0) + 30f
            < GameOverOverlayRenderer.SUMMARY_PANEL_Y + GameOverOverlayRenderer.SUMMARY_PANEL_HEIGHT);
        assertTrue(GameOverTouchLayout.RESTART_Y + GameOverTouchLayout.RESTART_HEIGHT
            < GameOverOverlayRenderer.SUMMARY_PANEL_Y);
    }

    @Test
    void everyFlowSurfaceBindsReviewedFramesPressedOffsetsAndSharedTypography()
        throws IOException {
        for (String renderer : new String[] {
            "PauseOverlayRenderer", "SettingsOverlayRenderer", "LevelUpOverlayRenderer",
            "RewardCardOverlayRenderer", "GameOverOverlayRenderer"
        }) {
            String source = Files.readString(RENDER.resolve(renderer + ".java"));
            assertTrue(source.contains("UiFrameRenderer.Kind.BUTTON"), renderer);
            assertTrue(source.contains("UiFrameRenderer.Kind.PANEL"), renderer);
            assertTrue(source.contains("MainMenuRenderer.pressedOffset"), renderer);
            assertTrue(source.contains("OverlayText"), renderer);
            assertFalse(source.contains("private void panel("), renderer);
        }
        String inventory = Files.readString(RENDER.resolve("InventoryOverlayRenderer.java"));
        assertFalse(inventory.contains("drawPauseMenu"));
        String game = Files.readString(Path.of(
            "src/main/java/com/amirrezahadipoor/herodefense/HeroDefenseGame.java"
        ));
        assertTrue(game.contains("pauseOverlayRenderer.draw("));
    }
}
