package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.gameplay.HeroStatCalculator;
import com.amirrezahadipoor.herodefense.input.LevelUpTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.model.HeroStats;

import java.util.Locale;

/** Premium level-up surface: five framed talent rows with explicit current-to-next previews. */
public final class LevelUpOverlayRenderer implements AutoCloseable {
    static final float HEADER_PANEL_X = 60f;
    static final float HEADER_PANEL_Y = 1040f;
    static final float HEADER_PANEL_WIDTH = 600f;
    static final float HEADER_PANEL_HEIGHT = 180f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();
    private final HeroStatCalculator statCalculator = new HeroStatCalculator();

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        UiIconRenderer icons,
        UiFrameRenderer frames
    ) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.065f, 0.140f, 0.120f, 0.88f);
        shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
        shapes.setColor(0.04f, 0.13f, 0.11f, 0.62f);
        shapes.rect(0f, 1040f, 720f, 240f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        float rowWidth = LevelUpTouchLayout.RIGHT - LevelUpTouchLayout.LEFT;
        boolean pointsAvailable = state.unspentTalentPoints > 0;

        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, HEADER_PANEL_X, HEADER_PANEL_Y,
            HEADER_PANEL_WIDTH, HEADER_PANEL_HEIGHT, true, false);
        for (int row = 0; row < HeroStat.values().length; row++) {
            float y = rowY(row);
            frames.draw(batch, UiFrameRenderer.Kind.BUTTON, LevelUpTouchLayout.LEFT, y,
                rowWidth, LevelUpTouchLayout.BUTTON_HEIGHT, pointsAvailable, false);
        }

        text.drawCentered(batch, "LEVEL " + state.heroLevel + " REACHED", 360f, 1188f, 1.62f,
            OverlayText.GOLD);
        text.drawCentered(batch, pointsLabel(state.unspentTalentPoints), 360f, 1132f, 0.96f,
            OverlayText.IVORY);
        text.drawCentered(batch, "Tap a talent to spend one point. Each choice is permanent.",
            360f, 1084f, 0.70f, OverlayText.SUBTLE);

        for (int row = 0; row < HeroStat.values().length; row++) {
            HeroStat stat = HeroStat.values()[row];
            float y = rowY(row);
            UiFrameRenderer.State rowState = frames.resolve(
                pointsAvailable, false, LevelUpTouchLayout.LEFT, y,
                rowWidth, LevelUpTouchLayout.BUTTON_HEIGHT
            );
            float offset = MainMenuRenderer.pressedOffset(rowState);
            icons.draw(batch, stat.name().toLowerCase(Locale.ROOT), 112f, y + 25f + offset, 80f,
                rowState);
            text.draw(batch, pretty(stat).toUpperCase(Locale.ROOT), 214f, y + 104f + offset, 1.08f,
                pointsAvailable ? OverlayText.IVORY : OverlayText.MUTED);
            text.draw(batch, gainPerPoint(stat), 214f, y + 66f + offset, 0.68f,
                OverlayText.SUBTLE);
            text.draw(batch, "POINTS " + points(state, stat), 214f, y + 36f + offset, 0.60f,
                OverlayText.GOLD);
            text.drawRightAligned(batch, "NOW " + currentValue(statCalculator, state, stat),
                606f, y + 92f + offset, 0.68f, OverlayText.SUBTLE);
            text.drawRightAligned(batch, "NEXT " + nextValue(statCalculator, state, stat),
                606f, y + 52f + offset, 0.86f,
                pointsAvailable ? OverlayText.POSITIVE : OverlayText.MUTED);
        }
        batch.end();
    }

    /** Row 0 (Strength) is drawn at the top to match the Shop order. */
    static float rowY(int row) {
        return LevelUpTouchLayout.rowBottom(row);
    }

    static String pointsLabel(int unspentPoints) {
        int points = Math.max(0, unspentPoints);
        return points == 1 ? "1 TALENT POINT TO SPEND" : points + " TALENT POINTS TO SPEND";
    }

    static String gainPerPoint(HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> "+" + fmt(HeroStats.DAMAGE_PER_STRENGTH) + " damage per attack";
            case AGILITY -> "+" + fmt(HeroStats.ATTACK_SPEED_PER_AGILITY) + " attacks per second";
            case LUCK -> "+" + Math.round((HeroStats.DROP_MULTIPLIER_PER_LUCK - 1f) * 100f)
                + "% item-drop multiplier";
            case DODGE -> "+" + fmt(HeroStats.DODGE_CHANCE_PER_POINT * 100f)
                + "% dodge chance (cap " + Math.round(HeroStats.MAX_DODGE_CHANCE * 100f) + "%)";
            case HEALTH -> "+" + Math.round(HeroStats.MAX_HEALTH_PER_POINT) + " maximum HP";
        };
    }

    static String currentValue(HeroStatCalculator calculator, GameState state, HeroStat stat) {
        return formatValue(stat, derived(calculator, state, stat, 0));
    }

    static String nextValue(HeroStatCalculator calculator, GameState state, HeroStat stat) {
        return formatValue(stat, derived(calculator, state, stat, 1));
    }

    private static float derived(
        HeroStatCalculator calculator, GameState state, HeroStat stat, int extraPoints
    ) {
        int total = calculator.points(state, stat) + extraPoints;
        return switch (stat) {
            case STRENGTH -> HeroStats.BASE_DAMAGE + total * HeroStats.DAMAGE_PER_STRENGTH;
            case AGILITY -> HeroStats.BASE_ATTACKS_PER_SECOND
                + total * HeroStats.ATTACK_SPEED_PER_AGILITY;
            case LUCK -> (float) Math.pow(HeroStats.DROP_MULTIPLIER_PER_LUCK, total);
            case DODGE -> Math.min(HeroStats.MAX_DODGE_CHANCE,
                total * HeroStats.DODGE_CHANCE_PER_POINT);
            case HEALTH -> HeroStats.BASE_MAX_HEALTH + total * HeroStats.MAX_HEALTH_PER_POINT;
        };
    }

    private static String formatValue(HeroStat stat, float value) {
        return switch (stat) {
            case STRENGTH -> fmt(value) + " dmg";
            case AGILITY -> fmt(value) + " aps";
            case LUCK -> "x" + String.format(Locale.ROOT, "%.2f", value);
            case DODGE -> fmt(value * 100f) + "%";
            case HEALTH -> Math.round(value) + " HP";
        };
    }

    private static int points(GameState state, HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> state.hero.stats.strength;
            case AGILITY -> state.hero.stats.agility;
            case LUCK -> state.hero.stats.luck;
            case DODGE -> state.hero.stats.dodge;
            case HEALTH -> state.hero.stats.health;
        };
    }

    private static String fmt(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0005f) return Integer.toString(Math.round(value));
        String formatted = String.format(Locale.ROOT, "%.2f", value);
        return formatted.endsWith("0") ? formatted.substring(0, formatted.length() - 1) : formatted;
    }

    private static String pretty(HeroStat stat) {
        String value = stat.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    @Override
    public void close() {
        text.close();
        shapes.dispose();
    }
}
