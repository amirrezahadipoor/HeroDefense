package com.amirrezahadipoor.herodefense.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

final class HeroDamageTest {
    @Test
    void rollBelowDodgeChanceAvoidsAllDamage() {
        Hero hero = new Hero(1L, 0f, 0f);
        hero.stats.dodge = 10; // 5%

        assertEquals(IncomingHitResult.DODGED, hero.receiveIncomingHit(30f, 0.049f));
        assertEquals(100f, hero.health);
    }

    @Test
    void failedDodgeAppliesDamageAndLethalHitKills() {
        Hero hero = new Hero(1L, 0f, 0f);
        hero.stats.dodge = 10;

        assertEquals(IncomingHitResult.DAMAGED, hero.receiveIncomingHit(25f, 0.05f));
        assertEquals(75f, hero.health);
        assertEquals(IncomingHitResult.KILLED, hero.receiveIncomingHit(100f, 0.9f));
        assertFalse(hero.alive);
        assertFalse(hero.active);
    }
}
