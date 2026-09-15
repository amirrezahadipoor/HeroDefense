package com.amirrezahadipoor.herodefense.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.model.ItemTier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Phase 29.2: 4 bows have own art, borrows unwired. */
final class BowOwnArtContractTest {
    private static final Set<String> BOW_IDS = Set.of(
        "yew_shortbow", "thornwood_bow", "verdant_recurve", "golemsbane_warbow"
    );
    private static final Path REPOSITORY = Path.of("..").normalize();
    private static final Path GENERATED = REPOSITORY.resolve("android/assets/generated");

    @Test
    void bowsDoNotBorrowArt() {
        for (String id : BOW_IDS) {
            EquipmentDefinition def = EquipmentCatalog.byId(id);
            assertTrue(def != null, "missing bow " + id);
            assertEquals(id, def.artId(), id + " still borrows " + def.artId());
            assertEquals("generated/icons/equipment_" + id + ".png", def.iconPath(), id);
            assertEquals(id, def.visualKey(), id);
        }
    }

    @Test
    void bowsHaveDistinctFiles() throws Exception {
        for (String id : BOW_IDS) {
            EquipmentDefinition def = EquipmentCatalog.byId(id);
            Path sheet = GENERATED.resolve("equipment/" + id + ".png");
            Path atlas = GENERATED.resolve("equipment/" + id + ".atlas");
            Path icon = REPOSITORY.resolve("android/assets/" + def.iconPath());
            assertTrue(Files.isRegularFile(sheet), "missing sheet " + sheet);
            assertTrue(Files.isRegularFile(atlas), "missing atlas " + atlas);
            assertTrue(Files.isRegularFile(icon), "missing icon " + icon);
        }
    }

    @Test
    void manifestHasBowAssets() throws Exception {
        String manifest = Files.readString(GENERATED.resolve("asset_manifest.json"));
        for (String id : BOW_IDS) {
            assertTrue(manifest.contains("\"key\": \"equipment_" + id + "\""), id);
        }
    }
}
