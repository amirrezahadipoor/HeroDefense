package com.amirrezahadipoor.herodefense.ascension;

import com.amirrezahadipoor.herodefense.model.GameState;

import java.util.Map;

/** Purchase and application of permanent Root Network nodes. */
public final class RootNetworkSystem {
    private static final float FEEDBACK_SECONDS = 1.2f;
    private String feedbackId;
    private String feedbackMessage;
    private float feedbackRemaining;

    public void update(float delta) {
        if (feedbackRemaining > 0f) {
            feedbackRemaining = Math.max(0f, feedbackRemaining - delta);
            if (feedbackRemaining == 0f) {
                feedbackId = null;
                feedbackMessage = null;
            }
        }
    }

    public String feedbackMessage() {
        return feedbackRemaining > 0f ? feedbackMessage : null;
    }

    public float feedbackAlpha() {
        if (feedbackRemaining <= 0f) return 0f;
        return Math.min(1f, feedbackRemaining / 0.2f);
    }

    public boolean isPurchased(GameState state, String nodeId) {
        if (state == null || nodeId == null) return false;
        Boolean v = state.rootNodesPurchased.get(nodeId);
        return v != null && v;
    }

    public boolean canPurchase(GameState state, String nodeId) {
        if (state == null || nodeId == null) return false;
        RootNodeDefinition def = RootNetworkCatalog.byId(nodeId);
        if (def == null) return false;
        if (isPurchased(state, nodeId)) return false;
        if (state.heartwood < def.cost()) return false;
        for (String req : def.requires()) {
            if (!isPurchased(state, req)) return false;
        }
        return true;
    }

    public boolean purchase(GameState state, String nodeId) {
        if (!canPurchase(state, nodeId)) {
            RootNodeDefinition def = RootNetworkCatalog.byId(nodeId);
            if (def != null && state != null && state.heartwood < def.cost()) {
                showFeedback(nodeId, "NEED " + (def.cost() - state.heartwood) + " MORE HEARTWOOD");
            }
            return false;
        }
        RootNodeDefinition def = RootNetworkCatalog.byId(nodeId);
        state.heartwood -= def.cost();
        state.rootNodesPurchased.put(nodeId, true);
        showFeedback(nodeId, "ROOT AWAKENED | " + def.name().toUpperCase());
        return true;
    }

    public void applyPermanentBonuses(GameState state) {
        if (state == null || state.hero == null) return;
        float extraMax = 0f;
        for (Map.Entry<String, Boolean> e : state.rootNodesPurchased.entrySet()) {
            if (e.getValue() == null || !e.getValue()) continue;
            String id = e.getKey();
            RootNodeDefinition def = RootNetworkCatalog.byId(id);
            if (def == null) continue;
            if ("root_all_1".equals(id)) {
                state.hero.stats.strength += 1;
                state.hero.stats.agility += 1;
                state.hero.stats.luck += 1;
                state.hero.stats.dodge += 1;
                state.hero.stats.health += 1;
            } else if ("root_all_2".equals(id)) {
                state.hero.stats.strength += 2;
                state.hero.stats.agility += 2;
                state.hero.stats.luck += 2;
                state.hero.stats.dodge += 2;
                state.hero.stats.health += 2;
                state.coins += 100;
            } else if ("root_heart_1".equals(id)) {
                state.unspentTalentPoints += 1;
                extraMax += def.bonusAmount();
            } else {
                applyBonus(state, def);
                if (def.bonusType() == RootNodeBonusType.MAX_HEALTH_BONUS) {
                    extraMax += def.bonusAmount();
                }
            }
        }
        state.hero.maxHealth = state.hero.stats.maxHealth() + extraMax;
        state.hero.health = state.hero.maxHealth;
        state.worldTreeMaxHealth = 1000f + extraMax * 0.5f;
        state.worldTreeHealth = state.worldTreeMaxHealth;
    }

    private void applyBonus(GameState state, RootNodeDefinition def) {
        switch (def.bonusType()) {
            case STARTING_STRENGTH -> state.hero.stats.strength += def.bonusAmount();
            case STARTING_AGILITY -> state.hero.stats.agility += def.bonusAmount();
            case STARTING_LUCK -> state.hero.stats.luck += def.bonusAmount();
            case STARTING_DODGE -> state.hero.stats.dodge += def.bonusAmount();
            case STARTING_HEALTH -> state.hero.stats.health += def.bonusAmount();
            case STARTING_COIN -> state.coins += def.bonusAmount();
            case STARTING_TALENT_POINT -> state.unspentTalentPoints += def.bonusAmount();
            case FOCUS_FILL_BONUS -> state.focusMax += def.bonusAmount();
            case MAX_HEALTH_BONUS -> { /* handled in caller */ }
        }
    }

    private void showFeedback(String id, String msg) {
        feedbackId = id;
        feedbackMessage = msg;
        feedbackRemaining = FEEDBACK_SECONDS;
    }
}
