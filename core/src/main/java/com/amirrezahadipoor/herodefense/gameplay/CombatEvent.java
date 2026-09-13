package com.amirrezahadipoor.herodefense.gameplay;

/**
 * One presentation-relevant combat fact emitted by an attack update. Gameplay never reads
 * these back; renderers, audio, and floating text consume them once per frame.
 *
 * @param kind      what happened
 * @param x         world x of the effect (impact point, or arc destination)
 * @param y         world y of the effect
 * @param fromX     arc origin for {@link Kind#CHAIN_ARC}; equal to {@code x} otherwise
 * @param fromY     arc origin for {@link Kind#CHAIN_ARC}; equal to {@code y} otherwise
 * @param amount    damage dealt, or stun seconds for {@link Kind#STUN}
 * @param secondary true when caused by a Multi Shot extra arrow
 */
public record CombatEvent(
    Kind kind, float x, float y, float fromX, float fromY, float amount, boolean secondary
) {
    public enum Kind { HIT, CRITICAL_HIT, CHAIN_ARC, STUN }

    public static CombatEvent hit(float x, float y, float damage, boolean critical, boolean secondary) {
        return new CombatEvent(critical ? Kind.CRITICAL_HIT : Kind.HIT, x, y, x, y, damage, secondary);
    }

    public static CombatEvent arc(float fromX, float fromY, float toX, float toY, float damage) {
        return new CombatEvent(Kind.CHAIN_ARC, toX, toY, fromX, fromY, damage, false);
    }

    public static CombatEvent stun(float x, float y, float seconds) {
        return new CombatEvent(Kind.STUN, x, y, x, y, seconds, false);
    }
}
