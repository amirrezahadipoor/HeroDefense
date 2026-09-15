package com.amirrezahadipoor.herodefense.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.amirrezahadipoor.herodefense.render.UiIconRenderer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/** Every skill the shop can sell must draw a committed, reviewed premium-v2 medallion. */
final class SkillIconContractTest {
    private static final Path ASSETS = Paths.get("..", "android", "assets").normalize();
    private static final String REVIEW = "docs/art_reviews/SKILL_ICONS_PREMIUM_V2_REVIEW.md";

    @Test
    void everySkillHasAReviewedIcon() throws IOException {
        JsonValue manifest = new JsonReader().parse(
            Files.readString(ASSETS.resolve("generated/asset_manifest.json")));
        for (SkillId skill : SkillId.values()) {
            Path icon = ASSETS.resolve(UiIconRenderer.assetPath(skill.iconKey()));
            assertTrue(Files.isRegularFile(icon), icon.toString());
            JsonValue asset = null;
            for (JsonValue candidate = manifest.get("assets").child; candidate != null; candidate = candidate.next) {
                if (("ui_" + skill.iconKey()).equals(candidate.getString("key"))) asset = candidate;
            }
            assertTrue(asset != null, "manifest entry for " + skill);
            assertTrue(java.util.Set.of("premium-v2", "studio-v3", "studio-v4-vibrant", "studio-v5-hd-pbr").contains(asset.getString("visualQuality")), skill.name() + " visualQuality=" + asset.getString("visualQuality"));
            assertEquals(REVIEW, asset.getString("reviewDocument"), skill.name());
            assertEquals("accepted", asset.get("categoryReview").getString("status"), skill.name());
        }
        assertTrue(Files.readString(Paths.get("..", REVIEW)).contains("**Decision:** ACCEPTED"));
    }
}
