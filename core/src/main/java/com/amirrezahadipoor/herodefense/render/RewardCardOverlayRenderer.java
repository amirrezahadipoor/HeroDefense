package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.rewards.BossRewardCardSystem;
import com.amirrezahadipoor.herodefense.rewards.RewardCardId;

/** Phone-readable presentation for the three mandatory post-boss choices. */
public final class RewardCardOverlayRenderer implements AutoCloseable {
    public static final float CARD_X = 70f;
    public static final float CARD_WIDTH = 580f;
    public static final float CARD_HEIGHT = 190f;
    public static final float FIRST_CARD_Y = 760f;
    public static final float CARD_STRIDE = 230f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();

    public RewardCardOverlayRenderer() {
        font.getData().setScale(1.8f);
    }

    public void draw(SpriteBatch batch, Matrix4 projection, GameState state) {
        if (!state.awaitingBossReward
            || state.pendingRewardCards.size() != BossRewardCardSystem.CHOICE_COUNT) {
            return;
        }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.03f, 0.06f, 0.075f, 0.88f);
        shapes.rect(0f, 0f, 720f, 1280f);
        for (int index = 0; index < BossRewardCardSystem.CHOICE_COUNT; index++) {
            float y = FIRST_CARD_Y - index * CARD_STRIDE;
            shapes.setColor(0.12f, 0.22f, 0.20f, 1f);
            shapes.rect(CARD_X, y, CARD_WIDTH, CARD_HEIGHT);
            shapes.setColor(0.84f, 0.68f, 0.30f, 1f);
            shapes.rect(CARD_X, y + CARD_HEIGHT - 8f, CARD_WIDTH, 8f);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(Color.valueOf("E7D8B1"));
        font.draw(batch, "Choose one reward", 190f, 1080f);
        for (int index = 0; index < BossRewardCardSystem.CHOICE_COUNT; index++) {
            RewardCardId card = RewardCardId.valueOf(state.pendingRewardCards.get(index));
            float y = FIRST_CARD_Y - index * CARD_STRIDE;
            font.draw(batch, card.title(), CARD_X + 34f, y + 128f);
            font.draw(batch, card.description(), CARD_X + 34f, y + 72f);
        }
        batch.end();
    }

    @Override
    public void close() {
        font.dispose();
        shapes.dispose();
    }
}
