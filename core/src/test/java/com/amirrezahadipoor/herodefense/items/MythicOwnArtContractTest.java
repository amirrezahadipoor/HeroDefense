package com.amirrezahadipoor.herodefense.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.ItemTier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Phase 29.1: every Mythic now has its own mesh/material + atlas + icon.
 * No Mythic borrows same-slot Rare/Legendary art any longer.
 */
final class MythicOwnArtContractTest {
    private static final Set<String> MYTHIC_IDS = Set.of(
        "sunfall_last_arrow",
        "crown_hollow_eye",
        "bark_first_root",
        "windrunner_last_steps",
        "verdant_oath",
        "emberless_core"
    );
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path GENERATED = REPOSITORY.resolve("android/assets/generated");

    @Test
    void mythicsDoNotBorrowArt() {
        for (String id : MYTHIC_IDS) {
            EquipmentDefinition def = EquipmentCatalog.byId(id);
            assertTrue(def != null, "missing mythic " + id);
            assertEquals(ItemTier.MYTHIC, def.tier(), id);
            assertEquals(id, def.artId(), id + " still borrows " + def.artId());
            assertEquals("generated/icons/equipment_" + id + ".png", def.iconPath(), id);
            assertEquals(id, def.visualKey(), id);
        }
    }

    @Test
    void mythicsHaveDistinctAtlasAndIconFiles() throws Exception {
        for (String id : MYTHIC_IDS) {
            EquipmentDefinition def = EquipmentCatalog.byId(id);
            Path atlas = GENERATED.resolve("equipment/" + id + ".atlas");
            Path sheet = GENERATED.resolve("equipment/" + id + ".png");
            Path icon = REPOSITORY.resolve("android/assets/" + def.iconPath());
            assertTrue(Files.isRegularFile(atlas), "missing atlas " + atlas);
            assertTrue(Files.isRegularFile(sheet), "missing sheet " + sheet);
            assertTrue(Files.isRegularFile(icon), "missing icon " + icon);
            // Must not be byte-identical to its old borrow target
            String oldBorrow = switch (id) {
                case "sunfall_last_arrow" -> "worldbranch";
                case "crown_hollow_eye" -> "crown_of_first_leaves";
                case "bark_first_root" -> "heartwood_aegis";
                case "windrunner_last_steps" -> "boots_of_three_winds";
                case "verdant_oath" -> "echo_band";
                case "emberless_core" -> "eternal_seed";
                default -> throw new IllegalStateException(id);
            };
            Path oldAtlas = GENERATED.resolve("equipment/" + oldBorrow + ".atlas");
            Path oldSheet = GENERATED.resolve("equipment/" + oldBorrow + ".png");
            if (Files.exists(oldSheet) && Files.exists(sheet)) {
                byte[] mythBytes = Files.readAllBytes(sheet);
                byte[] oldBytes = Files.readAllBytes(oldSheet);
                // Mythic sheet must be distinct art, not a byte-copy of its old borrow target
                assertTrue(!Arrays.equals(mythBytes, oldBytes),
                    id + " sheet is still a copy of " + oldBorrow);
            }
        }
    }

    @Test
    void manifestMarksMythicsAsPremiumV2WithOwnEngineVersion() throws Exception {
        String manifest = Files.readString(GENERATED.resolve("asset_manifest.json"));
        for (String id : MYTHIC_IDS) {
            String key = "equipment_" + id;
            assertTrue(manifest.contains("\"key\": \"" + key + "\""), "manifest missing " + key);
            assertTrue(manifest.contains("\"tier\": \"MYTHIC\""), "manifest tier not MYTHIC for " + key);
            // Phase 75: allow studio-v3, studio-v4-vibrant, studio-v5-hd-pbr as well as premium-v2
            assertTrue(manifest.contains("\"visualQuality\": \"premium-v2\"") || manifest.contains("\"visualQuality\": \"studio-v3\"") || manifest.contains("studio-v4") || manifest.contains("studio-v5"), key);
        }
    }
}
