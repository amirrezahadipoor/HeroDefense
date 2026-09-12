package com.amirrezahadipoor.herodefense.model;

/** Exactly six independent Hero equipment positions. */
public enum EquipmentSlot {
    WEAPON,
    HELMET,
    ARMOR,
    BOOTS,
    RING_1,
    RING_2;

    public static EquipmentSlot parse(String value) {
        try {
            return EquipmentSlot.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException error) {
            return null;
        }
    }
}
