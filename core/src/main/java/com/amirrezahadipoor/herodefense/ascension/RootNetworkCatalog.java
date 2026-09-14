package com.amirrezahadipoor.herodefense.ascension;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data table of 24 permanent Root Network nodes laid along the World Tree. */
public final class RootNetworkCatalog {
    private static final List<RootNodeDefinition> ALL = Collections.unmodifiableList(Arrays.asList(
        // Trunk base - cheap starters
        node("root_strength_1", "Root of Might", "+1 Starting Strength", 15, RootNodeBonusType.STARTING_STRENGTH, 1, 360f, 280f, null),
        node("root_health_1", "Root of Vitality", "+1 Starting Health", 15, RootNodeBonusType.STARTING_HEALTH, 1, 320f, 300f, null),
        node("root_agility_1", "Root of Swiftness", "+1 Starting Agility", 15, RootNodeBonusType.STARTING_AGILITY, 1, 400f, 300f, null),
        node("root_coin_1", "Root of Plenty", "+50 Starting Coins", 20, RootNodeBonusType.STARTING_COIN, 50, 360f, 340f, list("root_strength_1")),

        // Lower branches
        node("root_dodge_1", "Root of Evasion", "+1 Starting Dodge", 25, RootNodeBonusType.STARTING_DODGE, 1, 280f, 380f, list("root_health_1")),
        node("root_luck_1", "Root of Fortune", "+1 Starting Luck", 25, RootNodeBonusType.STARTING_LUCK, 1, 440f, 380f, list("root_agility_1")),
        node("root_health_2", "Deep Vitality", "+2 Starting Health", 30, RootNodeBonusType.STARTING_HEALTH, 2, 300f, 440f, list("root_dodge_1")),
        node("root_strength_2", "Deep Might", "+2 Starting Strength", 30, RootNodeBonusType.STARTING_STRENGTH, 2, 420f, 440f, list("root_luck_1")),
        node("root_talent_1", "Root of Knowledge", "+1 Starting Talent Point", 40, RootNodeBonusType.STARTING_TALENT_POINT, 1, 360f, 480f, list("root_coin_1")),
        node("root_coin_2", "Bountiful Root", "+100 Starting Coins", 35, RootNodeBonusType.STARTING_COIN, 100, 360f, 520f, list("root_talent_1")),

        // Mid trunk
        node("root_agility_2", "Wind Root", "+2 Starting Agility", 45, RootNodeBonusType.STARTING_AGILITY, 2, 260f, 580f, list("root_health_2")),
        node("root_dodge_2", "Shadow Root", "+2 Starting Dodge", 45, RootNodeBonusType.STARTING_DODGE, 2, 460f, 580f, list("root_strength_2")),
        node("root_focus_1", "Root of Focus", "+10% Focus Fill", 50, RootNodeBonusType.FOCUS_FILL_BONUS, 10, 320f, 640f, list("root_agility_2")),
        node("root_luck_2", "Fortune's Deep Root", "+2 Starting Luck", 50, RootNodeBonusType.STARTING_LUCK, 2, 400f, 640f, list("root_dodge_2")),
        node("root_health_3", "Heartwood Health", "+3 Starting Health +20 Max HP", 60, RootNodeBonusType.MAX_HEALTH_BONUS, 20, 360f, 700f, list("root_coin_2")),

        // Upper branches
        node("root_strength_3", "Titan Root", "+3 Starting Strength", 70, RootNodeBonusType.STARTING_STRENGTH, 3, 240f, 780f, list("root_focus_1")),
        node("root_agility_3", "Storm Root", "+3 Starting Agility", 70, RootNodeBonusType.STARTING_AGILITY, 3, 480f, 780f, list("root_luck_2")),
        node("root_coin_3", "Hoard Root", "+200 Starting Coins", 75, RootNodeBonusType.STARTING_COIN, 200, 300f, 840f, list("root_health_3")),
        node("root_talent_2", "Ancient Knowledge", "+2 Starting Talent Points", 80, RootNodeBonusType.STARTING_TALENT_POINT, 2, 420f, 840f, list("root_health_3")),
        node("root_focus_2", "Awakened Focus", "+20% Focus Fill", 85, RootNodeBonusType.FOCUS_FILL_BONUS, 20, 360f, 900f, list("root_coin_3", "root_talent_2")),

        // Crown - expensive capstones
        node("root_all_1", "Root of the First Warden", "+1 All Starting Stats", 120, RootNodeBonusType.STARTING_STRENGTH, 1, 280f, 980f, list("root_strength_3", "root_focus_2")),
        node("root_all_2", "Crown of the Grove", "+2 All Starting Stats +100 Coins", 150, RootNodeBonusType.STARTING_COIN, 100, 440f, 980f, list("root_agility_3", "root_focus_2")),
        node("root_heart_1", "Heart of the Tree", "+50 Max HP +1 Talent", 200, RootNodeBonusType.MAX_HEALTH_BONUS, 50, 360f, 1060f, list("root_all_1", "root_all_2"))
    ));

    private static final Map<String, RootNodeDefinition> BY_ID = index();

    private RootNetworkCatalog() {}

    public static List<RootNodeDefinition> all() { return ALL; }
    public static RootNodeDefinition byId(String id) { return BY_ID.get(id); }

    private static RootNodeDefinition node(String id, String name, String desc, int cost, RootNodeBonusType type, int amount, float x, float y, List<String> req) {
        return new RootNodeDefinition(id, name, desc, cost, type, amount, x, y, req);
    }

    private static List<String> list(String... ids) {
        return Arrays.asList(ids);
    }

    private static Map<String, RootNodeDefinition> index() {
        Map<String, RootNodeDefinition> m = new LinkedHashMap<>();
        for (RootNodeDefinition d : ALL) {
            if (m.put(d.id(), d) != null) throw new IllegalStateException("dup root id " + d.id());
        }
        return Collections.unmodifiableMap(m);
    }
}
