package com.amirrezahadipoor.herodefense.settings;

import com.amirrezahadipoor.herodefense.model.ItemTier;

/** Device-local accessibility/audio/inventory preferences independent from a run save. */
public final class GameSettings {
    public boolean soundEnabled = true;
    public boolean musicEnabled = true;
    /** Inventory auto-sell: ticked tiers are sold the moment a drop enters the backpack. */
    public boolean autoSellCommon;
    public boolean autoSellUncommon;
    public boolean autoSellRare;

    /** Legendary and Mythic items are never auto-sold; the toggle simply does not exist for them. */
    public boolean autoSells(ItemTier tier) {
        if (tier == null) return false;
        return switch (tier) {
            case COMMON -> autoSellCommon;
            case UNCOMMON -> autoSellUncommon;
            case RARE -> autoSellRare;
            case LEGENDARY -> false;
            case MYTHIC -> false;
        };
    }

    /** Flips the toggle for a sellable tier; returns false for tiers without a toggle. */
    public boolean toggleAutoSell(ItemTier tier) {
        if (tier == null) return false;
        switch (tier) {
            case COMMON -> autoSellCommon = !autoSellCommon;
            case UNCOMMON -> autoSellUncommon = !autoSellUncommon;
            case RARE -> autoSellRare = !autoSellRare;
            default -> {
                return false;
            }
        }
        return true;
    }
}
