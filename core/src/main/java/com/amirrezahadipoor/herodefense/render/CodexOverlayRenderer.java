package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.CodexTouchController;
import com.amirrezahadipoor.herodefense.input.CodexTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.story.BossLore;
import com.amirrezahadipoor.herodefense.story.CodexSystem;
import com.amirrezahadipoor.herodefense.story.LoreCatalog;
import com.amirrezahadipoor.herodefense.story.LoreEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

/** Read-only Grove Codex: silhouetted locked rows, full Tree-voice text in leaf-green. */
public final class CodexOverlayRenderer implements AutoCloseable {
    private static final float HEADER_PANEL_X = 40f;
    private static final float HEADER_PANEL_Y = 1052f;
    private static final float HEADER_PANEL_WIDTH = 510f;
    private static final float HEADER_PANEL_HEIGHT = 140f;
    private static final float DETAILS_X = 40f;
    private static final float DETAILS_Y = 60f;
    private static final float DETAILS_WIDTH = 640f;
    private static final float DETAILS_HEIGHT = 380f;
    private static final float DETAILS_PADDING = 24f;
    private static final float BODY_SCALE = 0.72f;
    private static final int BODY_MAX_LINES = 9;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();
    private final CodexSystem codex = new CodexSystem();

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        CodexTouchController controller,
        UiIconRenderer icons,
        UiFrameRenderer frames
    ) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.06f, 0.12f, 0.08f, 0.94f);
        shapes.rect(0f, 0f, 720f, 1280f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(
            batch, UiFrameRenderer.Kind.PANEL,
            HEADER_PANEL_X, HEADER_PANEL_Y, HEADER_PANEL_WIDTH, HEADER_PANEL_HEIGHT,
            true, false
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            CodexTouchLayout.CLOSE_X, CodexTouchLayout.CLOSE_Y,
            CodexTouchLayout.CLOSE_SIZE, CodexTouchLayout.CLOSE_SIZE,
            true, false
        );
        icons.draw(batch, "close", 588f, 1128f, 64f, frames.resolve(
            true, false,
            CodexTouchLayout.CLOSE_X, CodexTouchLayout.CLOSE_Y,
            CodexTouchLayout.CLOSE_SIZE, CodexTouchLayout.CLOSE_SIZE
        ));
        text.draw(batch, "GROVE CODEX", 64f, 1160f, 1.3f, OverlayText.GOLD);
        text.draw(batch, codex.unlockedCount(state) + " / 30 WRITTEN", 64f, 1112f, 0.72f,
            OverlayText.SUBTLE);

        for (int row = 0; row < CodexTouchLayout.VISIBLE_ROWS; row++) {
            int index = controller.firstVisibleIndex() + row;
            if (index >= LoreCatalog.all().size()) break;
            LoreEntry entry = LoreCatalog.all().get(index);
            boolean unlocked = codex.isUnlocked(state, entry.id());
            boolean selected = controller.selectedIndex() == index;
            float bottom = CodexTouchLayout.rowBottom(row);
            UiFrameRenderer.State rowState = frames.resolve(
                unlocked, selected,
                CodexTouchLayout.LIST_X, bottom,
                CodexTouchLayout.LIST_WIDTH, CodexTouchLayout.LIST_ROW_HEIGHT
            );
            frames.draw(
                batch, UiFrameRenderer.Kind.SLOT,
                CodexTouchLayout.LIST_X, bottom,
                CodexTouchLayout.LIST_WIDTH, CodexTouchLayout.LIST_ROW_HEIGHT,
                unlocked, selected
            );
            String label = unlocked
                ? String.format(Locale.ROOT, "%02d. %s", entry.number(), entry.title())
                : "??. ??????";
            text.draw(batch, label, CodexTouchLayout.LIST_X + 20f, bottom + 28f, 0.8f,
                unlocked ? OverlayText.IVORY : OverlayText.MUTED);
            if (selected && unlocked) {
                icons.draw(batch, "continue", 606f, bottom + 15f, 44f, rowState);
            }
        }

        frames.draw(
            batch, UiFrameRenderer.Kind.PANEL,
            DETAILS_X, DETAILS_Y, DETAILS_WIDTH, DETAILS_HEIGHT,
            true, false
        );
        drawDetail(batch, state, controller);
        batch.end();
    }

    private void drawDetail(SpriteBatch batch, GameState state, CodexTouchController controller) {
        LoreEntry detail = detailEntry(state, controller);
        float titleY = DETAILS_Y + DETAILS_HEIGHT - 48f;
        if (detail == null || !codex.isUnlocked(state, detail.id())) {
            text.draw(batch, "??????", DETAILS_X + DETAILS_PADDING, titleY, 1.0f, OverlayText.MUTED);
            text.draw(batch, "The grove has not written this yet.", DETAILS_X + DETAILS_PADDING,
                titleY - 48f, BODY_SCALE, OverlayText.MUTED);
            return;
        }
        text.draw(batch, detail.title(), DETAILS_X + DETAILS_PADDING, titleY, 1.0f, OverlayText.GOLD);
        List<String> lines = wrapLines(
            BossLore.detailFor(detail),
            line -> text.width(line, BODY_SCALE),
            DETAILS_WIDTH - DETAILS_PADDING * 2f
        );
        float step = text.lineHeight(GameFonts.Role.forLegacyScale(BODY_SCALE)) + 6f;
        float y = titleY - 52f;
        int shown = 0;
        for (String line : lines) {
            if (y < DETAILS_Y + 24f || shown >= BODY_MAX_LINES) break;
            if (line.isEmpty()) {
                y -= step * 0.6f;
                continue;
            }
            text.draw(batch, line, DETAILS_X + DETAILS_PADDING, y, BODY_SCALE, OverlayText.POSITIVE);
            y -= step;
            shown++;
        }
    }

    private LoreEntry detailEntry(GameState state, CodexTouchController controller) {
        int selected = controller.selectedIndex();
        if (selected >= 0 && selected < LoreCatalog.all().size()) {
            return LoreCatalog.all().get(selected);
        }
        for (LoreEntry entry : LoreCatalog.all()) {
            if (codex.isUnlocked(state, entry.id())) return entry;
        }
        return null;
    }

    /**
     * Caps wrapped lines for fixed panels, appending "…" to the last kept line when lines
     * are dropped. Pure and headless-safe.
     */
    static List<String> capLines(List<String> lines, int maxLines) {
        List<String> kept = new ArrayList<>();
        if (lines == null || maxLines <= 0) return kept;
        for (int index = 0; index < lines.size() && index < maxLines; index++) {
            kept.add(lines.get(index));
        }
        if (lines.size() > maxLines) {
            int last = kept.size() - 1;
            kept.set(last, kept.get(last) + "…");
        }
        return kept;
    }

    /** Greedy word wrap; pure and headless-safe for tests via the width function. */
    static List<String> wrapLines(String value, ToDoubleFunction<String> widthOf, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (value == null) return lines;
        boolean firstParagraph = true;
        for (String paragraph : value.split("\n\n")) {
            if (!firstParagraph) lines.add("");
            firstParagraph = false;
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (word.isEmpty()) continue;
                String trial = line.length() == 0 ? word : line + " " + word;
                if (widthOf.applyAsDouble(trial) <= maxWidth || line.length() == 0) {
                    line = new StringBuilder(trial);
                } else {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                }
            }
            if (line.length() > 0) lines.add(line.toString());
        }
        return lines;
    }

    @Override
    public void close() {
        text.close();
        shapes.dispose();
    }
}
