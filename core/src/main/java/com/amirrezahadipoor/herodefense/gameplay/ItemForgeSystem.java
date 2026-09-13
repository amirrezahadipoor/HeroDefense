package com.amirrezahadipoor.herodefense.gameplay;

import com.amirrezahadipoor.herodefense.items.EquipmentCatalog;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.model.ItemTier;

import java.util.Map;

/**
 * The Anvil: coin-only reforging of Rare and Legendary items. Every step adds +1 to each of
 * the item's stat bonuses, appends a +N suffix to its name, and raises its sell price by half
 * of what was spent. Steps are persisted on the item itself so saves stay self-describing.
 */
public final class ItemForgeSystem {
    public enum Result { NONE, FORGED, INSUFFICIENT_COINS, NOT_FORGEABLE, MAXED }

    public static final int MAX_UPGRADE = 5;
    public static final int RARE_BASE_COST = 150;
    public static final int LEGENDARY_BASE_COST = 350;
    public static final float COST_GROWTH = 1.6f;
    private static final float FEEDBACK_DURATION_SECONDS = 1.25f;

    private final HeroStatCalculator statCalculator;
    private Result feedbackResult = Result.NONE;
    private String feedbackItemName;
    private int feedbackCoins;
    private float feedbackRemainingSeconds;

    public ItemForgeSystem() {
        this(new HeroStatCalculator());
    }

    public ItemForgeSystem(HeroStatCalculator statCalculator) {
        this.statCalculator = statCalculator;
    }

    /** Only Rare and Legendary catalog items with at least one stat bonus can be reforged. */
    public static boolean isForgeable(Item item) {
        if (item == null || EquipmentCatalog.byId(item.id) == null) return false;
        ItemTier tier = ItemTier.parse(item.tier);
        if (tier != ItemTier.RARE && tier != ItemTier.LEGENDARY) return false;
        return item.statBonuses != null && !item.statBonuses.isEmpty();
    }

    public static int upgradeLevel(Item item) {
        return item == null ? 0 : Math.max(0, Math.min(MAX_UPGRADE, item.upgradeLevel));
    }

    /** Coin cost of the next step, or -1 when the item cannot be forged (any further). */
    public static int nextCost(Item item) {
        if (!isForgeable(item)) return -1;
        int level = upgradeLevel(item);
        if (level >= MAX_UPGRADE) return -1;
        int base = ItemTier.parse(item.tier) == ItemTier.LEGENDARY ? LEGENDARY_BASE_COST : RARE_BASE_COST;
        double cost = base * Math.pow(COST_GROWTH, level);
        return (int) Math.round(cost / 5.0) * 5;
    }

    /** Name with the +N suffix stripped, so the suffix is never stacked. */
    public static String baseName(Item item) {
        if (item == null || item.name == null) return "";
        return item.name.replaceFirst("\\s\\+\\d+$", "");
    }

    public static String displayName(String baseName, int upgradeLevel) {
        return upgradeLevel <= 0 ? baseName : baseName + " +" + upgradeLevel;
    }

    public Result forge(GameState state, Item item) {
        if (state == null || item == null) return Result.NONE;
        boolean owned = state.inventory.contains(item) || state.equippedItems.containsValue(item);
        if (!owned || !isForgeable(item)) {
            showFeedback(Result.NOT_FORGEABLE, item, 0);
            return Result.NOT_FORGEABLE;
        }
        int level = upgradeLevel(item);
        if (level >= MAX_UPGRADE) {
            showFeedback(Result.MAXED, item, 0);
            return Result.MAXED;
        }
        int cost = nextCost(item);
        if (state.coins < cost) {
            showFeedback(Result.INSUFFICIENT_COINS, item, cost - state.coins);
            return Result.INSUFFICIENT_COINS;
        }
        float previousMaxHealth = statCalculator.maxHealth(state);
        state.coins -= cost;
        for (Map.Entry<String, Float> bonus : item.statBonuses.entrySet()) {
            float value = bonus.getValue() == null ? 0f : bonus.getValue();
            bonus.setValue(value + 1f);
        }
        item.upgradeLevel = level + 1;
        item.name = displayName(baseName(item), item.upgradeLevel);
        item.sellPrice += cost / 2;
        if (state.equippedItems.containsValue(item)) {
            float maxHealth = statCalculator.maxHealth(state);
            state.hero.health = Math.min(maxHealth, state.hero.health + maxHealth - previousMaxHealth);
            state.hero.maxHealth = maxHealth;
        }
        showFeedback(Result.FORGED, item, cost);
        return Result.FORGED;
    }

    public void update(float realDeltaSeconds) {
        if (realDeltaSeconds <= 0f || feedbackRemainingSeconds <= 0f) return;
        feedbackRemainingSeconds = Math.max(0f, feedbackRemainingSeconds - realDeltaSeconds);
        if (feedbackRemainingSeconds == 0f) clearFeedback();
    }

    public Result feedbackResult() {
        return feedbackRemainingSeconds > 0f ? feedbackResult : Result.NONE;
    }

    public String feedbackMessage() {
        if (feedbackRemainingSeconds <= 0f || feedbackItemName == null) return null;
        return switch (feedbackResult) {
            case FORGED -> "REFORGED  |  " + feedbackItemName + "  |  -$ " + feedbackCoins;
            case INSUFFICIENT_COINS -> "NEED $ " + feedbackCoins + " MORE  |  ANVIL";
            case NOT_FORGEABLE -> "ANVIL TAKES RARE & LEGENDARY ONLY";
            case MAXED -> "FULLY REFORGED  |  " + feedbackItemName;
            default -> null;
        };
    }

    public float feedbackAlpha() {
        if (feedbackRemainingSeconds <= 0f) return 0f;
        return Math.min(1f, feedbackRemainingSeconds / 0.20f);
    }

    private void showFeedback(Result result, Item item, int coins) {
        feedbackResult = result;
        feedbackItemName = item.name;
        feedbackCoins = Math.max(0, coins);
        feedbackRemainingSeconds = FEEDBACK_DURATION_SECONDS;
    }

    private void clearFeedback() {
        feedbackResult = Result.NONE;
        feedbackItemName = null;
        feedbackCoins = 0;
        feedbackRemainingSeconds = 0f;
    }
}
