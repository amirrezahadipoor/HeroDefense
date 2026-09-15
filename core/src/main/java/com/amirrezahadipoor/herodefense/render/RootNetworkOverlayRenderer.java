package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.ascension.RootNetworkCatalog;
import com.amirrezahadipoor.herodefense.ascension.RootNetworkSystem;
import com.amirrezahadipoor.herodefense.ascension.RootNodeDefinition;
import com.amirrezahadipoor.herodefense.ascension.RootNodeBonusType;
import com.amirrezahadipoor.herodefense.input.RootNetworkTouchLayout;
import com.amirrezahadipoor.herodefense.model.GameState;

/** Renders World Tree full-screen with root-node overlays. Reuses existing tree art concept. */
public final class RootNetworkOverlayRenderer implements AutoCloseable {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();

    public void draw(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        RootNetworkSystem system,
        UiIconRenderer icons,
        UiFrameRenderer frames,
        SaplingTreeRenderer treeRenderer,
        float ambientSeconds
    ) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.06f, 0.12f, 0.08f, 0.92f);
        shapes.rect(0f, 0f, 720f, 1280f);
        shapes.setColor(0.12f, 0.18f, 0.11f, 1f);
        shapes.rect(200f, 0f, 320f, 1280f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 20f, 1120f, 680f, 140f, true, false);
        frames.draw(batch, UiFrameRenderer.Kind.PANEL, 460f, 1140f, 200f, 80f, true, false);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (RootNodeDefinition def : RootNetworkCatalog.all()) {
            for (String reqId : def.requires()) {
                RootNodeDefinition req = RootNetworkCatalog.byId(reqId);
                if (req == null) continue;
                boolean purchased = system.isPurchased(state, reqId) && system.isPurchased(state, def.id());
                if (purchased) shapes.setColor(0.74f, 0.76f, 0.35f, 0.9f);
                else shapes.setColor(0.3f, 0.35f, 0.3f, 0.6f);
                shapes.rectLine(req.x(), req.y(), def.x(), def.y(), 4f);
            }
        }
        for (RootNodeDefinition def : RootNetworkCatalog.all()) {
            boolean purchased = system.isPurchased(state, def.id());
            boolean canBuy = system.canPurchase(state, def.id());
            if (purchased) shapes.setColor(0.85f, 0.78f, 0.25f, 1f);
            else if (canBuy) shapes.setColor(0.45f, 0.65f, 0.45f, 0.9f);
            else shapes.setColor(0.25f, 0.25f, 0.25f, 0.7f);
            shapes.circle(def.x(), def.y(), RootNetworkTouchLayout.NODE_RADIUS);
            if (purchased) {
                shapes.setColor(1f, 0.95f, 0.5f, 0.6f);
                shapes.circle(def.x(), def.y(), RootNetworkTouchLayout.NODE_RADIUS + 6f);
            }
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();
        for (RootNodeDefinition def : RootNetworkCatalog.all()) {
            boolean purchased = system.isPurchased(state, def.id());
            String icon = iconKeyFor(def.bonusType());
            icons.draw(batch, icon, def.x() - 16f, def.y() - 16f, 32f);
            if (purchased) {
                text.drawCentered(batch, "OK", def.x(), def.y() + 28f, 0.8f, OverlayText.GOLD, 1f);
            }
        }
        text.draw(batch, "ROOT NETWORK", 40f, 1220f, 1.2f, OverlayText.GOLD, 1f);
        text.draw(batch, "Permanent growth. Never resets.", 40f, 1180f, 0.7f, OverlayText.SUBTLE, 1f);
        text.draw(batch, "HEARTWOOD: " + state.heartwood, 480f, 1190f, 0.9f, OverlayText.IVORY, 1f);
        frames.draw(batch, UiFrameRenderer.Kind.BUTTON,
            RootNetworkTouchLayout.CLOSE_X, RootNetworkTouchLayout.CLOSE_Y,
            RootNetworkTouchLayout.CLOSE_W, RootNetworkTouchLayout.CLOSE_H, true, false);
        text.drawCentered(batch, "CLOSE", 80f, 1190f, 0.8f, OverlayText.IVORY, 1f);
        String feedback = system.feedbackMessage();
        if (feedback != null) {
            float alpha = system.feedbackAlpha();
            text.drawCentered(batch, feedback, 360f, 200f, 1.0f, OverlayText.GOLD, alpha);
        }
        text.drawCentered(batch, "Tap a green node to awaken it with Heartwood", 360f, 140f, 0.7f, OverlayText.SUBTLE, 1f);
        batch.end();
    }

    /** Reviewed icon key per node bonus; every key must resolve in `UiIconRenderer`. */
    static String iconKeyFor(RootNodeBonusType bonusType) {
        return switch (bonusType) {
            case STARTING_STRENGTH -> "strength";
            case STARTING_AGILITY -> "agility";
            case STARTING_LUCK -> "luck";
            case STARTING_DODGE -> "dodge";
            case STARTING_HEALTH -> "health";
            case STARTING_COIN -> "coin";
            case STARTING_TALENT_POINT -> "general_power";
            case FOCUS_FILL_BONUS -> "skill_chain_lightning";
            case MAX_HEALTH_BONUS -> "health";
        };
    }

    @Override
    public void close() {
        text.close();
        shapes.dispose();
    }
}
