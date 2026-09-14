package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.TrialDraftTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.trials.TrialDraftSystem;
import com.amirrezahadipoor.herodefense.trials.TrialId;

import java.util.Locale;

/**
 * Pre-run trial draft in the post-boss card-choice presentation: convergence context,
 * four framed trial cards with green reward and red risk lines, and explicit rules.
 */
public final class TrialDraftOverlayRenderer implements AutoCloseable {
    static final float HEADER_PANEL_X = 60f;
    static final float HEADER_PANEL_Y = 1010f;
    static final float HEADER_PANEL_WIDTH = 600f;
    static final float HEADER_PANEL_HEIGHT = 190f;
    static final float FOOTER_PANEL_Y = 140f;
    static final float FOOTER_PANEL_HEIGHT = 104f;
    static final float ICON_SIZE = 84f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        UiIconRenderer icons,
        UiFrameRenderer frames
    ) {
        if (!state.draftPending()
            || state.pendingTrialOffer.size() != TrialDraftSystem.OFFER_COUNT) {
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
        for (int index = 0; index < TrialDraftSystem.OFFER_COUNT; index++) {
            float y = cardY(index);
            boolean picked = isPicked(state, index);
            frames.draw(batch, UiFrameRenderer.Kind.BUTTON, TrialDraftTouchLayout.CARD_X, y,
                TrialDraftTouchLayout.CARD_WIDTH, TrialDraftTouchLayout.CARD_HEIGHT,
                true, picked);
        }
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, HEADER_PANEL_X, FOOTER_PANEL_Y,
            HEADER_PANEL_WIDTH, FOOTER_PANEL_HEIGHT, true, false);

        text.drawCentered(batch, "THE CONVERGENCE OFFERS", 360f, 1168f, 0.78f,
            OverlayText.GOLD);
        text.drawCentered(batch, "CHOOSE TWO TRIALS", 360f, 1122f, 1.62f, OverlayText.GOLD);
        text.drawCentered(batch, picksLabel(state), 360f, 1066f, 0.74f,
            OverlayText.SUBTLE);

        for (int index = 0; index < TrialDraftSystem.OFFER_COUNT; index++) {
            TrialId trial = TrialId.valueOf(state.pendingTrialOffer.get(index));
            float y = cardY(index);
            boolean picked = isPicked(state, index);
            UiFrameRenderer.State cardState = frames.resolve(
                true, picked, TrialDraftTouchLayout.CARD_X, y,
                TrialDraftTouchLayout.CARD_WIDTH, TrialDraftTouchLayout.CARD_HEIGHT
            );
            float offset = MainMenuRenderer.pressedOffset(cardState);
            icons.draw(batch, trial.iconKey(), TrialDraftTouchLayout.CARD_X + 26f,
                y + 40f + offset, ICON_SIZE, cardState);
            text.draw(batch, trial.title().toUpperCase(Locale.ROOT),
                TrialDraftTouchLayout.CARD_X + 130f, y + 126f + offset, 1.12f,
                OverlayText.IVORY);
            text.draw(batch, trial.reward(),
                TrialDraftTouchLayout.CARD_X + 130f, y + 88f + offset, 0.92f,
                OverlayText.POSITIVE);
            text.draw(batch, trial.risk(),
                TrialDraftTouchLayout.CARD_X + 130f, y + 52f + offset, 0.92f,
                OverlayText.NEGATIVE);
            text.drawRightAligned(batch, picked ? "CHOSEN" : "TRIAL",
                TrialDraftTouchLayout.CARD_X + TrialDraftTouchLayout.CARD_WIDTH - 26f,
                y + 126f + offset, 0.60f,
                picked ? OverlayText.GOLD : OverlayText.SUBTLE);
        }

        text.drawCentered(batch, "Two trials bind for this run only. Both bite and bless.",
            360f, FOOTER_PANEL_Y + 66f, 0.70f, OverlayText.IVORY);
        text.drawCentered(batch, "Tap two cards to begin the descent",
            360f, FOOTER_PANEL_Y + 34f, 0.62f, OverlayText.SUBTLE);
        batch.end();
    }

    static float cardY(int index) {
        return TrialDraftTouchLayout.FIRST_CARD_Y - index * TrialDraftTouchLayout.CARD_STRIDE;
    }

    static String picksLabel(GameState state) {
        int picks = state.trialDraftPicks == null ? 0 : state.trialDraftPicks.size();
        return picks + " of " + TrialDraftSystem.PICK_COUNT + " bound  |  rewards green, costs red";
    }

    private static boolean isPicked(GameState state, int index) {
        return state.trialDraftPicks != null
            && state.trialDraftPicks.contains(state.pendingTrialOffer.get(index));
    }

    @Override
    public void close() {
        text.close();
        shapes.dispose();
    }
}
