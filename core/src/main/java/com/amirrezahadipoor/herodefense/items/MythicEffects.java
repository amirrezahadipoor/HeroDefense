package com.amirrezahadipoor.herodefense.items;

import com.amirrezahadipoor.herodefense.model.Enemy;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;

import java.util.List;

/**
 * The six Mythic unique passives (Phase 23.3, narrative in {@code docs/STORY_CONTENT.md}
 * §7). All queries read equipped items by catalog id, so forged, renamed, or heirloom
 * Mythics resolve through their stable id. Every effect is deterministic: no rolls, only
 * counters, timers, and the wave clock.
 */
public final class MythicEffects {
    public static final String SUNFALL_LAST_ARROW = "sunfall_last_arrow";
    public static final String CROWN_HOLLOW_EYE = "crown_hollow_eye";
    public static final String BARK_FIRST_ROOT = "bark_first_root";
    public static final String WINDRUNNER_LAST_STEPS = "windrunner_last_steps";
    public static final String VERDANT_OATH = "verdant_oath";
    public static final String EMBERLESS_CORE = "emberless_core";

    public static final List<String> ALL_IDS = List.of(
        SUNFALL_LAST_ARROW,
        CROWN_HOLLOW_EYE,
        BARK_FIRST_ROOT,
        WINDRUNNER_LAST_STEPS,
        VERDANT_OATH,
        EMBERLESS_CORE
    );

    /** Sunfall: Chain Lightning arcs also stun for this long (bosses resist as usual). */
    public static final float SUNFALL_STUN_SECONDS = 1.0f;
    /** Crown: a crit marks for 4s; marked enemies take +25% from further hits. */
    public static final float CROWN_MARK_SECONDS = 4.0f;
    public static final float CROWN_MARK_DAMAGE_BONUS = 0.25f;
    /** Bark: every 10th landed hit taken heals 20% of max health, potion-free. */
    public static final int BARK_HITS_PER_HEAL = 10;
    public static final float BARK_HEAL_FRACTION = 0.20f;
    /** Windrunner: +1% attack speed per wave-second stood, capped at +25%. */
    public static final float WINDRUNNER_RAMP_PER_SECOND = 0.01f;
    public static final float WINDRUNNER_MAX_BONUS = 0.25f;
    /** Verdant Oath: auto-potions grant +5% lifesteal for 4s. */
    public static final float VERDANT_LIFESTEAL_SECONDS = 4.0f;
    public static final float VERDANT_LIFESTEAL_BONUS = 0.05f;
    /** Emberless Core: crits refund 35% of the shot's cooldown. */
    public static final float EMBERLESS_REFUND_FRACTION = 0.35f;

    private MythicEffects() {
    }

    public static boolean hasSunfall(GameState state) {
        return equipped(state, SUNFALL_LAST_ARROW);
    }

    public static boolean hasCrown(GameState state) {
        return equipped(state, CROWN_HOLLOW_EYE);
    }

    public static boolean hasBark(GameState state) {
        return equipped(state, BARK_FIRST_ROOT);
    }

    public static boolean hasWindrunner(GameState state) {
        return equipped(state, WINDRUNNER_LAST_STEPS);
    }

    public static boolean hasVerdantOath(GameState state) {
        return equipped(state, VERDANT_OATH);
    }

    public static boolean hasEmberless(GameState state) {
        return equipped(state, EMBERLESS_CORE);
    }

    /** How many of the six Mythics are currently equipped (Codex secret #25 watches all). */
    public static int equippedMythicCount(GameState state) {
        int count = 0;
        for (String id : ALL_IDS) {
            if (equipped(state, id)) count++;
        }
        return count;
    }

    /** Windrunner ramp off the per-wave clock; 1 when the boots are not worn. */
    public static float windrunnerAttackSpeedMultiplier(GameState state) {
        if (state == null || !hasWindrunner(state)) return 1f;
        float elapsed = Float.isFinite(state.waveElapsedSeconds)
            ? Math.max(0f, state.waveElapsedSeconds) : 0f;
        return 1f + Math.min(WINDRUNNER_MAX_BONUS, elapsed * WINDRUNNER_RAMP_PER_SECOND);
    }

    /** Verdant Oath lifesteal while its buff timer runs; 0 otherwise. */
    public static float verdantLifestealBonus(GameState state) {
        if (state == null || state.hero == null) return 0f;
        return state.hero.mythicLifestealRemainingSeconds > 0f ? VERDANT_LIFESTEAL_BONUS : 0f;
    }

    /** Crown mark: further hits on a marked enemy deal +25%. */
    public static float crownMarkDamageMultiplier(Enemy enemy) {
        return enemy != null && enemy.markRemainingSeconds > 0f
            ? 1f + CROWN_MARK_DAMAGE_BONUS : 1f;
    }

    /**
     * Bark of the First Root: counts a LANDED hit and triggers the free heal on every
     * tenth. Never revives: a lethal tenth hit still kills.
     */
    public static void onLandedHitTaken(GameState state) {
        if (state == null || state.hero == null) return;
        state.hero.mythicHitsTaken++;
        if (!hasBark(state) || !state.hero.alive) return;
        if (state.hero.mythicHitsTaken % BARK_HITS_PER_HEAL == 0) {
            state.hero.health = Math.min(
                state.hero.maxHealth,
                state.hero.health + state.hero.maxHealth * BARK_HEAL_FRACTION
            );
        }
    }

    /**
     * One-line passive rules text for the inventory details panel, or null when the id
     * is not a Mythic.
     */
    public static String passiveLine(String itemId) {
        if (itemId == null) return null;
        return switch (itemId) {
            case SUNFALL_LAST_ARROW -> "PASSIVE: Chain arcs also Stun (1.0s)";
            case CROWN_HOLLOW_EYE -> "PASSIVE: Crits Mark 4s; marked take +25%";
            case BARK_FIRST_ROOT -> "PASSIVE: Every 10th hit taken heals 20%";
            case WINDRUNNER_LAST_STEPS -> "PASSIVE: +1% attack speed/s in wave (max +25%)";
            case VERDANT_OATH -> "PASSIVE: Auto-potions grant +5% lifesteal 4s";
            case EMBERLESS_CORE -> "PASSIVE: Crits refund 35% of shot cooldown";
            default -> null;
        };
    }

    /**
     * Verbatim {@code docs/STORY_CONTENT.md} §7 flavor for the inventory details panel,
     * or null when the id is not a Mythic.
     */
    public static String flavorLine(String itemId) {
        if (itemId == null) return null;
        return switch (itemId) {
            case SUNFALL_LAST_ARROW -> "Fired once, centuries ago, at something that fell "
                + "from further up than any bow should reach. It never came back down whole. "
                + "Neither did the thing it hit.";
            case CROWN_HOLLOW_EYE -> "Wear it and you start seeing weakness the way the "
                + "Hollow sees strength — as the one true thing worth aiming for.";
            case BARK_FIRST_ROOT -> "Cut from the World Tree's own outer bark, back when it "
                + "could still spare the wood. It remembers how to close a wound.";
            case WINDRUNNER_LAST_STEPS -> "They were made for running. He never ran again "
                + "after he put them on — he just stopped needing to.";
            case VERDANT_OATH -> "A promise sealed in sap: what heals you should also let "
                + "you keep healing.";
            case EMBERLESS_CORE -> "What's left of the ember that never went out — cooled, "
                + "contained, and finally put to work instead of left to spread.";
            default -> null;
        };
    }

    private static boolean equipped(GameState state, String itemId) {
        if (state == null || state.equippedItems == null) return false;
        for (Item item : state.equippedItems.values()) {
            if (item != null && itemId.equals(item.id)) return true;
        }
        return false;
    }
}
