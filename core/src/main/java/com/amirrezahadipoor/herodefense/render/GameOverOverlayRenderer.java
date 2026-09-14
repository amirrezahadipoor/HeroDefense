package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.GameOverTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.story.Epilogue;
import com.amirrezahadipoor.herodefense.trials.TrialEffects;

/** Premium end-of-run surface with distinct defeat and victory treatments and a framed restart. */
public final class GameOverOverlayRenderer implements AutoCloseable {
    static final float DESTRUCTION_REVEAL_DELAY_SECONDS = 0.82f;
    static final float REVEAL_FADE_SECONDS = 0.28f;
    static final float TITLE_PANEL_X = 60f;
    static final float TITLE_PANEL_Y = 830f;
    static final float TITLE_PANEL_WIDTH = 600f;
    static final float TITLE_PANEL_HEIGHT = 370f;
    static final float SUMMARY_PANEL_X = 60f;
    static final float SUMMARY_PANEL_Y = 400f;
    static final float SUMMARY_PANEL_WIDTH = 600f;
    static final float SUMMARY_PANEL_HEIGHT = 390f;
    static final int SUMMARY_ROWS = 5;
    static final float EPILOGUE_FIRST_LINE_Y = 1122f;
    static final float EPILOGUE_LINE_STRIDE = 30f;
    static final float EPILOGUE_STANZA_GAP = 12f;
    static final float EPILOGUE_SCALE = 0.72f;
    static final float EPILOGUE_MAX_WIDTH = 540f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        UiIconRenderer icons,
        UiFrameRenderer frames,
        float presentationSeconds
    ) {
        float reveal = revealProgress(presentationSeconds, state.runComplete);
        if (reveal <= 0f) return;
        boolean victory = state.runComplete;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (victory) {
            shapes.setColor(0.065f, 0.140f, 0.105f, 0.84f * reveal);
            shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
            shapes.setColor(0.24f, 0.19f, 0.05f, 0.42f * reveal);
            shapes.rect(0f, 830f, 720f, 450f);
        } else {
            shapes.setColor(0.110f, 0.055f, 0.055f, 0.88f * reveal);
            shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
            shapes.setColor(0.16f, 0.05f, 0.04f, 0.42f * reveal);
            shapes.rect(0f, 830f, 720f, 450f);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        boolean interactive = isInteractive(presentationSeconds, state.runComplete);
        UiFrameRenderer.State restartState = frames.resolve(
            interactive, false,
            GameOverTouchLayout.RESTART_X, GameOverTouchLayout.RESTART_Y,
            GameOverTouchLayout.RESTART_WIDTH, GameOverTouchLayout.RESTART_HEIGHT
        );
        UiFrameRenderer.State ascendState = frames.resolve(
            interactive, false,
            GameOverTouchLayout.ASCEND_X, GameOverTouchLayout.ASCEND_Y,
            GameOverTouchLayout.ASCEND_WIDTH, GameOverTouchLayout.ASCEND_HEIGHT
        );
        UiFrameRenderer.State rootState = frames.resolve(
            interactive, false,
            GameOverTouchLayout.ROOT_X, GameOverTouchLayout.ROOT_Y,
            GameOverTouchLayout.ROOT_WIDTH, GameOverTouchLayout.ROOT_HEIGHT
        );

        batch.setProjectionMatrix(projection);
        batch.setColor(1f, 1f, 1f, reveal);
        batch.begin();
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, TITLE_PANEL_X, TITLE_PANEL_Y,
            TITLE_PANEL_WIDTH, TITLE_PANEL_HEIGHT, true, false);
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, SUMMARY_PANEL_X, SUMMARY_PANEL_Y,
            SUMMARY_PANEL_WIDTH, SUMMARY_PANEL_HEIGHT, true, false);
        frames.draw(batch, UiFrameRenderer.Kind.BUTTON,
            GameOverTouchLayout.RESTART_X, GameOverTouchLayout.RESTART_Y,
            GameOverTouchLayout.RESTART_WIDTH, GameOverTouchLayout.RESTART_HEIGHT,
            interactive, victory);
        frames.draw(batch, UiFrameRenderer.Kind.BUTTON,
            GameOverTouchLayout.ASCEND_X, GameOverTouchLayout.ASCEND_Y,
            GameOverTouchLayout.ASCEND_WIDTH, GameOverTouchLayout.ASCEND_HEIGHT,
            interactive, true);
        frames.draw(batch, UiFrameRenderer.Kind.BUTTON,
            GameOverTouchLayout.ROOT_X, GameOverTouchLayout.ROOT_Y,
            GameOverTouchLayout.ROOT_WIDTH, GameOverTouchLayout.ROOT_HEIGHT,
            interactive, false);

        Color titleColor = victory ? OverlayText.GOLD : OverlayText.NEGATIVE;
        text.drawCentered(batch, victory ? "RUN COMPLETE" : "DEFEAT", 360f, 1168f, 0.78f,
            titleColor, reveal);
        drawEpilogue(batch, state, victory, reveal);

        text.drawCentered(batch, "RUN SUMMARY", 360f, 762f, 0.82f, OverlayText.GOLD, reveal);
        drawRow(batch, icons, 0, "wave", "Wave reached",
            state.waveNumber + " / " + GameState.FINAL_WAVE, reveal);
        drawRow(batch, icons, 1, "health", "Hero level", Integer.toString(state.heroLevel), reveal);
        drawRow(batch, icons, 2, "strength", "Enemies defeated",
            Integer.toString(state.totalKills), reveal);
        drawRow(batch, icons, 3, "general_power", "Bosses defeated",
            state.defeatedBosses + " / 20", reveal);
        drawRow(batch, icons, 4, "coin", "Kill coins earned",
            MainMenuRenderer.coinTotalLabel(state.totalKillCoinsEarned), reveal);

        float offset = MainMenuRenderer.pressedOffset(restartState);
        icons.draw(batch, "restart", 152f, GameOverTouchLayout.RESTART_Y + 24f + offset, 64f,
            restartState);
        text.draw(batch, victory ? "DEFEND AGAIN" : "RESTART AT WAVE 1", 228f,
            GameOverTouchLayout.RESTART_Y + 68f + offset, 1.08f,
            interactive ? OverlayText.IVORY : OverlayText.MUTED, reveal);
        text.draw(batch, "Begin fresh run same tier", 228f,
            GameOverTouchLayout.RESTART_Y + 34f + offset, 0.62f, OverlayText.SUBTLE, reveal);

        float ascOffset = MainMenuRenderer.pressedOffset(ascendState);
        icons.draw(batch, "general_power", 152f, GameOverTouchLayout.ASCEND_Y + 20f + ascOffset, 56f,
            ascendState);
        int heartwoodPreview = Math.round(GameState.calculateHeartwoodReward(state.peakWaveReached, state.ascensionTier, !state.heroDiedThisRun)
            * TrialEffects.heartwoodMultiplier(state.activeTrials));
        text.draw(batch, "ASCEND  |  +" + heartwoodPreview + " HEARTWOOD", 228f,
            GameOverTouchLayout.ASCEND_Y + 60f + ascOffset, 1.0f,
            interactive ? OverlayText.GOLD : OverlayText.MUTED, reveal);
        text.draw(batch, "Tier " + state.ascensionTier + " -> " + (state.ascensionTier + 1) + "  |  Harder foes, permanent roots", 228f,
            GameOverTouchLayout.ASCEND_Y + 30f + ascOffset, 0.58f, OverlayText.SUBTLE, reveal);

        float rootOffset = MainMenuRenderer.pressedOffset(rootState);
        icons.draw(batch, "health", 152f, GameOverTouchLayout.ROOT_Y + 20f + rootOffset, 56f,
            rootState);
        text.draw(batch, "ROOT NETWORK  |  " + state.heartwood + " HW", 228f,
            GameOverTouchLayout.ROOT_Y + 60f + rootOffset, 1.0f,
            interactive ? OverlayText.IVORY : OverlayText.MUTED, reveal);
        text.draw(batch, "Spend Heartwood on permanent growth", 228f,
            GameOverTouchLayout.ROOT_Y + 30f + rootOffset, 0.58f, OverlayText.SUBTLE, reveal);

        batch.end();
        batch.setColor(Color.WHITE);
    }

    private void drawRow(
        SpriteBatch batch,
        UiIconRenderer icons,
        int row,
        String icon,
        String label,
        String value,
        float alpha
    ) {
        float y = summaryRowY(row);
        icons.draw(batch, icon, 96f, y - 20f, 56f);
        text.draw(batch, label, 172f, y + 18f, 0.92f, OverlayText.IVORY, alpha);
        text.drawRightAligned(batch, value, 612f, y + 20f, 1.06f, OverlayText.GOLD, alpha);
    }

    static float summaryRowY(int row) {
        return 726f - row * 62f;
    }

    private void drawEpilogue(
        SpriteBatch batch, GameState state, boolean victory, float reveal
    ) {
        Epilogue epilogue = Epilogue.endingFor(state);
        float y = EPILOGUE_FIRST_LINE_Y;
        for (String beat : epilogue.lines()) {
            for (String line : CodexOverlayRenderer.wrapLines(
                beat, this::epilogueWidth, EPILOGUE_MAX_WIDTH)) {
                text.drawCentered(batch, line, 360f, y, EPILOGUE_SCALE, OverlayText.IVORY, reveal);
                y -= EPILOGUE_LINE_STRIDE;
            }
        }
        if (victory) {
            y -= EPILOGUE_STANZA_GAP;
            for (String beat : Epilogue.TRANSITION) {
                for (String line : CodexOverlayRenderer.wrapLines(
                    beat, this::epilogueWidth, EPILOGUE_MAX_WIDTH)) {
                    text.drawCentered(
                        batch, line, 360f, y, EPILOGUE_SCALE, OverlayText.GOLD, reveal);
                    y -= EPILOGUE_LINE_STRIDE;
                }
            }
        }
    }

    private double epilogueWidth(String line) {
        return text.width(line, EPILOGUE_SCALE);
    }

    static float revealProgress(float presentationSeconds, boolean runComplete) {
        if (runComplete) return 1f;
        if (!Float.isFinite(presentationSeconds)
            || presentationSeconds <= DESTRUCTION_REVEAL_DELAY_SECONDS) {
            return 0f;
        }
        return Math.min(
            1f,
            (presentationSeconds - DESTRUCTION_REVEAL_DELAY_SECONDS) / REVEAL_FADE_SECONDS
        );
    }

    public static boolean isInteractive(float presentationSeconds, boolean runComplete) {
        return revealProgress(presentationSeconds, runComplete) >= 0.95f;
    }

    @Override
    public void close() {
        text.close();
        shapes.dispose();
    }
}
