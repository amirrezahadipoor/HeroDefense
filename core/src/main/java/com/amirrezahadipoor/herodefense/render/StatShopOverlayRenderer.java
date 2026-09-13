package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.StatShopTouchLayout;
import com.amirrezahadipoor.herodefense.input.StatShopTouchLayout.Tab;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.HeroStat;
import com.amirrezahadipoor.herodefense.shop.StatShopSystem;
import com.amirrezahadipoor.herodefense.skills.SkillEffects;
import com.amirrezahadipoor.herodefense.skills.SkillId;
import com.amirrezahadipoor.herodefense.skills.SkillShopSystem;

import java.util.Locale;

/** Premium coin-only shop: a Stats tab of +1 upgrades and a Skills tab of ten-level powers. */
public final class StatShopOverlayRenderer implements AutoCloseable {
    private static final Color GOLD = Color.valueOf("EAC66D");
    private static final Color IVORY = Color.valueOf("F3E4BC");
    private static final Color SUBTLE = Color.valueOf("AEBCAE");
    private static final Color POSITIVE = Color.valueOf("69C884");
    private static final Color NEGATIVE = Color.valueOf("DF6A65");
    private static final Color MUTED = Color.valueOf("777D76");
    private static final Color ARCANE = Color.valueOf("8FD4F2");

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();

    public StatShopOverlayRenderer() {
    }

    /** One row's resolved presentation, shared by both tabs so the layout never diverges. */
    private record Row(
        String iconKey, String title, String benefit, int level, int maxLevel,
        int price, boolean maxed, boolean affordable, Color accent
    ) {
    }

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        StatShopSystem shop,
        SkillShopSystem skills,
        Tab tab,
        UiIconRenderer icons,
        UiFrameRenderer frames,
        boolean returnsToPause
    ) {
        Row[] rows = tab == Tab.SKILLS ? skillRows(state, skills) : statRows(state, shop);

        beginShapes(projection);
        shapes.setColor(0.070f, 0.150f, 0.130f, 0.96f);
        shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
        shapes.setColor(0.07f, 0.19f, 0.16f, 0.82f);
        shapes.rect(0f, 1160f, 720f, 120f);
        shapes.end();
        endShapes();

        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            StatShopTouchLayout.CLOSE_X, StatShopTouchLayout.CLOSE_Y,
            StatShopTouchLayout.CLOSE_SIZE, StatShopTouchLayout.CLOSE_SIZE,
            true, false
        );
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 40f, 1140f, 225f, 12f, true, false);
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            StatShopTouchLayout.TAB_STATS_X, StatShopTouchLayout.TAB_Y,
            StatShopTouchLayout.TAB_WIDTH, StatShopTouchLayout.TAB_HEIGHT,
            true, tab == Tab.STATS
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            StatShopTouchLayout.TAB_SKILLS_X, StatShopTouchLayout.TAB_Y,
            StatShopTouchLayout.TAB_WIDTH, StatShopTouchLayout.TAB_HEIGHT,
            true, tab == Tab.SKILLS
        );
        for (int index = 0; index < rows.length; index++) {
            frames.draw(
                batch, UiFrameRenderer.Kind.BUTTON,
                StatShopTouchLayout.ROW_X, StatShopTouchLayout.rowBottom(index),
                StatShopTouchLayout.ROW_WIDTH, StatShopTouchLayout.ROW_HEIGHT,
                rows[index].affordable(), false
            );
        }
        String feedback = tab == Tab.SKILLS ? skills.feedbackMessage() : shop.feedbackMessage();
        if (feedback != null) {
            frames.draw(batch, UiFrameRenderer.Kind.PANEL, 100f, 150f, 520f, 64f, true, false);
        }
        batch.end();

        beginShapes(projection);
        for (int index = 0; index < rows.length; index++) {
            Row row = rows[index];
            float y = StatShopTouchLayout.rowBottom(index);
            shapes.setColor(row.maxed() ? GOLD : row.affordable() ? POSITIVE : NEGATIVE);
            shapes.rect(StatShopTouchLayout.ROW_X + 6f, y + 16f, 6f,
                StatShopTouchLayout.ROW_HEIGHT - 32f);
            shapes.setColor(0.070f, 0.105f, 0.092f, 0.95f);
            shapes.rect(170f, y + 14f, 205f, 8f);
            shapes.setColor(row.level() > row.maxLevel() ? GOLD : row.accent());
            shapes.rect(170f, y + 14f, 205f * tierProgress(row.level(), row.maxLevel()), 8f);
        }
        shapes.end();
        endShapes();

        batch.begin();
        UiFrameRenderer.State closeState = frames.resolve(
            true, false,
            StatShopTouchLayout.CLOSE_X, StatShopTouchLayout.CLOSE_Y,
            StatShopTouchLayout.CLOSE_SIZE, StatShopTouchLayout.CLOSE_SIZE
        );
        icons.draw(batch, "close", 588f, 1138f, 64f, closeState);
        drawText(batch, "WORLD TREE ARMORY", 40f, 1240f, 1.36f, GOLD);
        drawText(batch, tab == Tab.SKILLS
                ? "Combat skills; costly, permanent, no ceiling"
                : "Permanent upgrades bought only with earned coins",
            40f, 1200f, 0.72f, SUBTLE);
        icons.draw(batch, "coin", 44f, 1152f, 34f);
        drawText(batch, "$ " + Math.max(0, state.coins), 86f, 1178f, 1.00f, IVORY);
        drawText(batch, "COMBAT PAUSED", 300f, 1180f, 0.68f, POSITIVE);
        drawText(
            batch,
            returnsToPause ? "Close returns to Pause" : "Close returns to battle",
            300f, 1152f, 0.62f, SUBTLE
        );
        drawCentered(batch, "STATS", StatShopTouchLayout.TAB_STATS_X + StatShopTouchLayout.TAB_WIDTH * 0.5f,
            StatShopTouchLayout.TAB_Y + 44f, 0.96f, tab == Tab.STATS ? GOLD : SUBTLE);
        drawCentered(batch, "SKILLS", StatShopTouchLayout.TAB_SKILLS_X + StatShopTouchLayout.TAB_WIDTH * 0.5f,
            StatShopTouchLayout.TAB_Y + 44f, 0.96f, tab == Tab.SKILLS ? GOLD : SUBTLE);

        for (int index = 0; index < rows.length; index++) {
            Row row = rows[index];
            float y = StatShopTouchLayout.rowBottom(index);
            UiFrameRenderer.State cardState = frames.resolve(
                row.affordable(), false,
                StatShopTouchLayout.ROW_X, y,
                StatShopTouchLayout.ROW_WIDTH, StatShopTouchLayout.ROW_HEIGHT
            );
            float offset = MainMenuRenderer.pressedOffset(cardState);
            icons.draw(batch, row.iconKey(), 72f, y + 26f + offset, 78f, cardState);
            drawText(batch, row.title(), 170f, y + 102f + offset, 0.98f,
                row.affordable() || row.maxed() ? IVORY : MUTED);
            drawText(batch, row.benefit(), 170f, y + 66f + offset, 0.66f, SUBTLE);
            drawText(batch, levelLabel(row.level(), row.maxLevel()),
                170f, y + 44f + offset, 0.58f, GOLD);
            Color affordabilityColor = row.maxed() ? GOLD : row.affordable() ? POSITIVE : NEGATIVE;
            drawCentered(batch,
                affordabilityLabel(row.maxed(), row.affordable(), row.price(), state.coins),
                532f, y + 91f + offset, 0.62f, affordabilityColor);
            drawCentered(batch, row.maxed() ? "MAX" : "$ " + row.price(),
                532f, y + 54f + offset, 0.92f,
                row.maxed() ? GOLD : row.affordable() ? IVORY : MUTED);
        }

        if (feedback != null) {
            Color base = tab == Tab.SKILLS
                ? switch (skills.feedbackResult()) {
                    case PURCHASED -> POSITIVE;
                    case INSUFFICIENT_COINS -> NEGATIVE;
                    case MAXED -> GOLD;
                    default -> SUBTLE;
                }
                : switch (shop.feedbackResult()) {
                    case PURCHASED -> POSITIVE;
                    case INSUFFICIENT_COINS -> NEGATIVE;
                    case MAXED -> GOLD;
                    default -> SUBTLE;
                };
            Color feedbackColor = new Color(base);
            feedbackColor.a = tab == Tab.SKILLS ? skills.feedbackAlpha() : shop.feedbackAlpha();
            drawCentered(batch, feedback, 360f, 189f, 0.78f, feedbackColor);
        }
        batch.end();
    }

    private static Row[] statRows(GameState state, StatShopSystem shop) {
        HeroStat[] stats = HeroStat.values();
        Row[] rows = new Row[stats.length];
        for (int index = 0; index < stats.length; index++) {
            HeroStat stat = stats[index];
            int purchased = shop.purchasedLevels(state, stat);
            int price = shop.price(state, stat);
            rows[index] = new Row(
                stat.name().toLowerCase(Locale.ROOT),
                pretty(stat).toUpperCase(Locale.ROOT),
                statBenefit(stat),
                purchased, StatShopSystem.CORE_LEVELS,
                price, false, state.coins >= price, POSITIVE
            );
        }
        return rows;
    }

    private static Row[] skillRows(GameState state, SkillShopSystem skills) {
        SkillId[] ids = SkillId.values();
        Row[] rows = new Row[ids.length];
        for (int index = 0; index < ids.length; index++) {
            SkillId skill = ids[index];
            int level = skills.level(state, skill);
            int price = skills.price(state, skill);
            rows[index] = new Row(
                skill.iconKey(),
                skill.displayName().toUpperCase(Locale.ROOT),
                skillBenefit(skill, level),
                level, SkillId.CORE_LEVELS,
                price, false, state.coins >= price, ARCANE
            );
        }
        return rows;
    }

    /**
     * Levels are endless: the label shows "LEVEL n / core" through the core tier and then
     * "LEVEL n  |  ENDLESS" so the player sees both the number and that growth continues.
     */
    static String levelLabel(int level, int coreLevels) {
        if (level <= coreLevels) return "LEVEL " + level + " / " + coreLevels;
        return "LEVEL " + level + "  |  ENDLESS";
    }

    /** Bar fill: fraction of the core tier, then a full gold bar once endless. */
    static float tierProgress(int level, int coreLevels) {
        if (coreLevels <= 0) return 1f;
        return Math.max(0f, Math.min(1f, level / (float) coreLevels));
    }

    static String affordabilityLabel(boolean maxed, boolean affordable, int price, int coins) {
        if (maxed) return "MAXED";
        if (affordable) return "AFFORDABLE";
        return "NEED $ " + Math.max(0, price - coins);
    }

    static String statBenefit(HeroStat stat) {
        return switch (stat) {
            case STRENGTH -> "+1 base damage";
            case AGILITY -> "+1 attack-speed rating";
            case LUCK -> "+1 critical-chance rating";
            case DODGE -> "+1 dodge rating";
            case HEALTH -> "+1 vitality and max health";
        };
    }

    /** Describes the concrete next-level effect so a player knows exactly what a purchase buys. */
    static String skillBenefit(SkillId skill, int level) {
        int next = level + 1;
        return switch (skill) {
            case CHAIN_LIGHTNING -> String.format(Locale.ROOT, "%d%% arc to %d foe%s for %d%% dmg",
                Math.round(SkillEffects.chainChance(next) * 100f), SkillEffects.chainTargets(next),
                SkillEffects.chainTargets(next) == 1 ? "" : "s",
                Math.round(SkillEffects.CHAIN_DAMAGE_SHARE * 100f));
            case MULTI_SHOT -> String.format(Locale.ROOT, "+%.1f arrows per volley at %d%% dmg",
                SkillEffects.extraArrows(next), Math.round(SkillEffects.MULTI_SHOT_DAMAGE_SHARE * 100f));
            case STUN_CHANCE -> String.format(Locale.ROOT, "%d%% chance to stun for %.2fs",
                Math.round(SkillEffects.stunChance(next) * 100f), SkillEffects.stunDuration(next));
            case CRITICAL_MASTERY -> String.format(Locale.ROOT, "%.1f%% crit chance, x%.2f crit dmg",
                SkillEffects.criticalChance(next) * 100f, SkillEffects.criticalMultiplier(next));
            case LONG_RANGE -> String.format(Locale.ROOT, "+%d bow range (%d total)",
                Math.round(SkillEffects.bonusRange(next)), Math.round(420f + SkillEffects.bonusRange(next)));
        };
    }

    private void drawCentered(
        SpriteBatch batch, String label, float centerX, float y, float scale, Color color
    ) {
        text.drawCentered(batch, label, centerX, y, scale, color);
    }

    private void drawText(
        SpriteBatch batch, String label, float x, float y, float scale, Color color
    ) {
        text.draw(batch, label, x, y, scale, color);
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

    private static String pretty(HeroStat stat) {
        String text = stat.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    @Override
    public void close() {
        text.close();
        shapes.dispose();
    }
}
