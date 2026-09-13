package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.RewardCardTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.rewards.RewardCardId;
import com.amirrezahadipoor.herodefense.rewards.RewardPowerBudget;

/** Premium post-boss choice surface with three touch-matched permanent reward cards. */
public final class RewardCardOverlayRenderer implements AutoCloseable {
    private static final Color GOLD = Color.valueOf("EAC66D");
    private static final Color IVORY = Color.valueOf("F3E4BC");
    private static final Color SUBTLE = Color.valueOf("AEBCAE");
    private static final Color POSITIVE = Color.valueOf("69C884");

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final RewardPowerBudget powerBudget = new RewardPowerBudget();

    public RewardCardOverlayRenderer() {
        font.getRegion().getTexture().setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );
    }

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
        beginShapes(projection);
        shapes.setColor(0.006f, 0.022f, 0.021f, 0.955f);
        shapes.rect(0f, 0f, 720f, 1280f);
        shapes.end();
        endShapes();

        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 70f, 1000f, 580f, 170f, true, false);
        for (int index = 0; index < BossRewardCardSystem.CHOICE_COUNT; index++) {
            float y = cardY(index);
            frames.draw(
                batch, UiFrameRenderer.Kind.BUTTON,
                RewardCardTouchLayout.CARD_X, y,
                RewardCardTouchLayout.CARD_WIDTH, RewardCardTouchLayout.CARD_HEIGHT,
                true, false
            );
        }
        drawText(batch, "BOSS " + state.pendingRewardBossNumber + " DEFEATED", 110f, 1124f, 0.78f, POSITIVE);
        drawText(batch, "CHOOSE ONE REWARD", 110f, 1073f, 1.42f, GOLD);
        drawText(batch, "The selected boon lasts for this run", 110f, 1029f, 0.72f, SUBTLE);

        for (int index = 0; index < BossRewardCardSystem.CHOICE_COUNT; index++) {
            RewardCardId card = RewardCardId.valueOf(state.pendingRewardCards.get(index));
            float y = cardY(index);
            UiFrameRenderer.State cardState = frames.resolve(
                true, false,
                RewardCardTouchLayout.CARD_X, y,
                RewardCardTouchLayout.CARD_WIDTH, RewardCardTouchLayout.CARD_HEIGHT
            );
            float offset = MainMenuRenderer.pressedOffset(cardState);
            drawText(batch, "0" + (index + 1), 94f, y + 158f + offset, 0.58f, GOLD);
            icons.draw(batch, card.iconKey(), 100f, y + 48f + offset, 94f, cardState);
            drawText(batch, card.title().toUpperCase(), 224f, y + 137f + offset, 1.04f, IVORY);
            drawText(
                batch,
                powerBudget.description(card, state.pendingRewardBossNumber),
                224f,
                y + 91f + offset,
                0.78f,
                POSITIVE
            );
            drawText(batch, "PERMANENT RUN BONUS", 224f, y + 49f + offset, 0.60f, SUBTLE);
            drawText(batch, "TAP TO CLAIM", 510f, y + 49f + offset, 0.56f, GOLD);
        }
        drawText(batch, "Combat resumes immediately after your choice", 178f, 226f, 0.70f, SUBTLE);
        batch.end();
    }

    private static float cardY(int index) {
        return RewardCardTouchLayout.FIRST_CARD_Y - index * RewardCardTouchLayout.CARD_STRIDE;
    }

    private void drawText(
        SpriteBatch batch, String text, float x, float y, float scale, Color color
    ) {
        font.getData().setScale(scale);
        font.setColor(0.003f, 0.010f, 0.009f, color.a);
        font.draw(batch, text, x + 1.5f, y - 2f);
        font.setColor(color);
        font.draw(batch, text, x, y);
    }

    private void beginShapes(Matrix4 projection) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void endShapes() {
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void close() {
        font.dispose();
        shapes.dispose();
    }
}
