package com.amirrezahadipoor.herodefense.polish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amirrezahadipoor.herodefense.gameplay.CombatEvent;
import com.amirrezahadipoor.herodefense.polish.FloatingDamageText.Style;
import java.util.List;
import org.junit.jupiter.api.Test;

final class FloatingDamageTextSystemTest {
    @Test
    void everyEventKindMapsToItsOwnStyleAndText() {
        FloatingDamageTextSystem system = new FloatingDamageTextSystem();
        system.emitAll(List.of(
            CombatEvent.hit(10f, 20f, 12.4f, false, false),
            CombatEvent.hit(10f, 20f, 7f, false, true),
            CombatEvent.hit(10f, 20f, 31.6f, true, false),
            CombatEvent.arc(0f, 0f, 30f, 40f, 6.6f),
            CombatEvent.stun(10f, 20f, 0.6f)
        ));
        List<FloatingDamageText> labels = system.labels();
        assertEquals(5, labels.size());
        assertEquals(Style.NORMAL, labels.get(0).style);
        assertEquals("12", labels.get(0).text);
        assertEquals(Style.SECONDARY, labels.get(1).style);
        assertEquals(Style.CRITICAL, labels.get(2).style);
        assertEquals("32!", labels.get(2).text);
        assertEquals(Style.CHAIN, labels.get(3).style);
        assertEquals("7", labels.get(3).text);
        assertEquals(Style.STUN, labels.get(4).style);
        assertEquals("STUN", labels.get(4).text);
        assertTrue(labels.get(2).scale() > labels.get(0).scale(), "criticals are larger");
    }

    @Test
    void labelsRiseDecelerateAndExpireWithinABoundedPool() {
        FloatingDamageTextSystem system = new FloatingDamageTextSystem();
        for (int index = 0; index < FloatingDamageTextSystem.MAX_LABELS + 5; index++) {
            system.emit(CombatEvent.hit(0f, 100f, 5f, false, false));
        }
        assertEquals(FloatingDamageTextSystem.MAX_LABELS, system.labels().size());
        FloatingDamageText label = system.labels().get(0);
        float startY = label.y();
        system.update(0.2f);
        float earlyRise = label.y() - startY;
        system.update(0.2f);
        float lateRise = label.y() - startY - earlyRise;
        assertTrue(earlyRise > lateRise && lateRise > 0f, "rise decelerates");
        system.update(FloatingDamageTextSystem.CRITICAL_LIFETIME_SECONDS);
        assertTrue(system.labels().isEmpty());
    }

    @Test
    void largeNumbersAreAbbreviated() {
        assertEquals("999", FloatingDamageTextSystem.formatDamage(999.4f));
        assertEquals("1.2k", FloatingDamageTextSystem.formatDamage(1_240f));
        assertEquals("120k", FloatingDamageTextSystem.formatDamage(120_400f));
        assertEquals("1", FloatingDamageTextSystem.formatDamage(0.2f));
    }
}
