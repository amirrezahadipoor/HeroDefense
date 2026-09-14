package com.amirrezahadipoor.herodefense.items;

import com.amirrezahadipoor.herodefense.gameplay.HeroStatCalculator;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.trials.TrialEffects;

/** Grants the equipment-neutral Hero exactly one visible starter bow per run. */
public final class StarterLoadoutSystem {
    public boolean provisionOnce(GameState state) {
        if (state == null || state.starterLoadoutGranted) {
            return false;
        }
        Item starterBow = EquipmentCatalog.byId("ashwood_bow").createItem();
        state.equippedItems.put(EquipmentSlot.WEAPON.name(), starterBow);
        state.starterLoadoutGranted = true;
        HeroStatCalculator stats = new HeroStatCalculator();
        state.hero.maxHealth = stats.maxHealth(state);
        state.hero.health = Math.min(state.hero.health, state.hero.maxHealth);
        state.coins += TrialEffects.startingCoinsBonus(state.activeTrials);
        return true;
    }
}
