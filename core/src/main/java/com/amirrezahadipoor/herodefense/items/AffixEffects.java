package com.amirrezahadipoor.herodefense.items;

import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.model.ItemTier;

/**
 * Pure queries summing the affixes on equipped items. A null state, missing equipment,
 * or unknown affix ids always yield the identity value, so unaffixed builds behave
 * exactly as before. Identical affixes on several pieces stack additively.
 */
public final class AffixEffects {
    private AffixEffects() {}

    private static float sum(GameState state, AffixId affix) {
        if (state == null || state.equippedItems == null || affix == null) {
            return 0f;
        }
        float total = 0f;
        for (Item item : state.equippedItems.values()) {
            if (item != null && affix.name().equals(item.affixId)) {
                total += affix.value();
            }
        }
        return total;
    }

    /**
     * Rolls one uniform affix for a fresh drop of the tier: Rare and Legendary always
     * roll; anything else (including Mythic, which carries a passive instead) rolls none.
     */
    public static String rollForDrop(GameState state, ItemTier tier) {
        if (state == null || (tier != ItemTier.RARE && tier != ItemTier.LEGENDARY)) {
            return "";
        }
        AffixId[] pool = AffixId.values();
        float roll = state.nextAffixRandomFloat();
        int index = Math.min(pool.length - 1, (int) (roll * pool.length));
        return pool[index].name();
    }

    public static float critChanceBonus(GameState state) {
        return sum(state, AffixId.CRIT_CHANCE);
    }

    public static float critDamageBonus(GameState state) {
        return sum(state, AffixId.CRIT_DAMAGE);
    }

    public static float lifestealBonus(GameState state) {
        return sum(state, AffixId.LIFESTEAL);
    }

    public static float coinsOnKill(GameState state) {
        return sum(state, AffixId.COINS_ON_KILL);
    }

    public static float damageMultiplier(GameState state) {
        return 1f + sum(state, AffixId.DAMAGE);
    }

    public static float attackSpeedMultiplier(GameState state) {
        return 1f + sum(state, AffixId.ATTACK_SPEED);
    }

    public static float maxHealthMultiplier(GameState state) {
        return 1f + sum(state, AffixId.MAX_HEALTH);
    }

    public static float dodgeBonus(GameState state) {
        return sum(state, AffixId.DODGE);
    }

    public static float experienceMultiplier(GameState state) {
        return 1f + sum(state, AffixId.EXPERIENCE);
    }

    public static float dropChanceMultiplier(GameState state) {
        return 1f + sum(state, AffixId.ITEM_FIND);
    }

    public static float potionPowerMultiplier(GameState state) {
        return 1f + sum(state, AffixId.POTION_POWER);
    }

    public static float stunChanceBonus(GameState state) {
        return sum(state, AffixId.STUN_CHANCE);
    }

    public static float chainChanceBonus(GameState state) {
        return sum(state, AffixId.CHAIN_CHANCE);
    }

    public static float extraArrowsBonus(GameState state) {
        return sum(state, AffixId.MULTISHOT);
    }

    public static float bossDamageMultiplier(GameState state) {
        return 1f + sum(state, AffixId.BOSS_DAMAGE);
    }
}
