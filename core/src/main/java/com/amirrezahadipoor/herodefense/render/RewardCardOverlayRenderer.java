package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.RewardCardTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.rewards.RewardCardId;
import com.amirrezahadipoor.herodefense.rewards.RewardPowerBudget;

import java.util.Locale;

/** Premium post-boss reward surface: boss context, three framed cards, and explicit rules. */
public final class RewardCardOverlayRenderer implements AutoCloseable {
    static final float HEADER_PANEL_X = 60f;
    static final float HEADER_PANEL_Y = 1010f;
    static final float HEADER_PANEL_WIDTH = 600f;
    static final float HEADER_PANEL_HEIGHT = 190f;
    static final float FOOTER_PANEL_Y = 140f;
    static final float FOOTER_PANEL_HEIGHT = 104f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();
    private final RewardPowerBudget powerBudget = new RewardPowerBudget();

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        UiIconRenderer icons,
        UiFrameRenderer frames
    ) {
        if (!state.awaitingBossReward
            || state.pendingRewardCards.size() != BossRewardCardSystem.CHOICE_COUNT) {
            return;
        }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.065f, 0.140f, 0.120f, 0.84f);
        shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
        shapes.setColor(0.09f, 0.16f, 0.06f, 0.55f);
        shapes.rect(0f, 1010f, 720f, 270f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, HEADER_PANEL_X, HEADER_PANEL_Y,
            HEADER_PANEL_WIDTH, HEADER_PANEL_HEIGHT, true, false);
        for (int index = 0; index < BossRewardCardSystem.CHOICE_COUNT; index++) {
            float y = cardY(index);
            frames.draw(batch, UiFrameRenderer.Kind.BUTTON, RewardCardTouchLayout.CARD_X, y,
                RewardCardTouchLayout.CARD_WIDTH, RewardCardTouchLayout.CARD_HEIGHT, true, false);
        }
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, HEADER_PANEL_X, FOOTER_PANEL_Y,
            HEADER_PANEL_WIDTH, FOOTER_PANEL_HEIGHT, true, false);

        int bossNumber = state.pendingRewardBossNumber;
        text.drawCentered(batch, bossLabel(bossNumber), 360f, 1168f, 0.78f, OverlayText.GOLD);
        text.drawCentered(batch, "CHOOSE ONE REWARD", 360f, 1122f, 1.62f, OverlayText.GOLD);
        text.drawCentered(batch, budgetLabel(powerBudget, bossNumber), 360f, 1066f, 0.74f,
            OverlayText.SUBTLE);

        for (int index = 0; index < BossRewardCardSystem.CHOICE_COUNT; index++) {
            RewardCardId card = RewardCardId.valueOf(state.pendingRewardCards.get(index));
            float y = cardY(index);
            UiFrameRenderer.State cardState = frames.resolve(
                true, false, RewardCardTouchLayout.CARD_X, y,
                RewardCardTouchLayout.CARD_WIDTH, RewardCardTouchLayout.CARD_HEIGHT
            );
            float offset = MainMenuRenderer.pressedOffset(cardState);
            icons.draw(batch, card.iconKey(), RewardCardTouchLayout.CARD_X + 26f, y + 47f + offset,
                96f, cardState);
            text.draw(batch, card.title().toUpperCase(Locale.ROOT),
                RewardCardTouchLayout.CARD_X + 140f, y + 148f + offset, 1.18f, OverlayText.IVORY);
            text.draw(batch, powerBudget.description(card, bossNumber),
                RewardCardTouchLayout.CARD_X + 140f, y + 102f + offset, 0.98f, OverlayText.POSITIVE);
            text.draw(batch, effectKind(card), RewardCardTouchLayout.CARD_X + 140f,
                y + 58f + offset, 0.66f, OverlayText.SUBTLE);
            text.drawRightAligned(batch, "PERMANENT",
                RewardCardTouchLayout.CARD_X + RewardCardTouchLayout.CARD_WIDTH - 26f,
                y + 148f + offset, 0.60f, OverlayText.GOLD);
        }

        text.drawCentered(batch, "Exactly one card applies immediately. The other two are lost.",
            360f, FOOTER_PANEL_Y + 66f, 0.70f, OverlayText.IVORY);
        text.drawCentered(batch, "Wave " + state.waveNumber + " cleared  |  Combat resumes after your choice",
            360f, FOOTER_PANEL_Y + 34f, 0.62f, OverlayText.SUBTLE);
        batch.end();
    }

    static float cardY(int index) {
        return RewardCardTouchLayout.FIRST_CARD_Y - index * RewardCardTouchLayout.CARD_STRIDE;
    }

    static String bossLabel(int bossNumber) {
        int clamped = Math.max(1, Math.min(20, bossNumber));
        return "BOSS " + clamped + " OF 20 DEFEATED";
    }

    static String budgetLabel(RewardPowerBudget budget, int bossNumber) {
        int percent = Math.round(budget.multiplier(bossNumber) * 100f);
        return "Reward power " + percent + "%  |  scales with every boss you defeat";
    }

    static String effectKind(RewardCardId card) {
        return switch (card.effectType()) {
            case BASE_STAT -> "Base talent points, added like a level-up";
            case GENERAL_POWER -> "Multiplies every point of Hero damage";
            case COIN_INCOME -> "Boosts kill and boss coin rewards";
            case LIFESTEAL -> "Heals a share of the damage you deal";
        };
    }

    @Override
    public void close() {
        text.close();
        shapes.dispose();
    }
}
