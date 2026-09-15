package com.amirrezahadipoor.herodefense.render;

/** Runtime visual tiers; glow is intentionally absent from generated sprites. */
public enum VisualRarity {
    COMMON(false, 0f, 0f, 0f, 0f),
    UNCOMMON(false, 0f, 0f, 0f, 0f),
    RARE(true, 0.22f, 0.55f, 1.0f, 0.90f), // Phase 52: 0.82->0.90 stronger
    LEGENDARY(true, 1.0f, 0.80f, 0.15f, 1.25f), // Phase 52: 0.67->0.80 gold brighter, 1.0->1.25 double halo
    /** Violet aura matching the Mythic inventory ink, burning hottest of all. */
    MYTHIC(true, 0.85f, 0.45f, 1.0f, 1.55f), // Phase 52: 1.3->1.55 stunning mythic
    /** Sickly chartreuse for blightburst Elites. */
    ELITE_BLIGHTBURST(true, 0.55f, 1.0f, 0.25f, 1.05f),
    /** Deep root-teal for rootward Elites. */
    ELITE_ROOTWARD(true, 0.25f, 0.9f, 0.6f, 1.05f),
    /** Bruised magenta for weeping Elites. */
    ELITE_WEEPING(true, 1.0f, 0.25f, 0.45f, 1.05f);

    private final boolean glowing;
    private final float red;
    private final float green;
    private final float blue;
    private final float intensity;

    VisualRarity(boolean glowing, float red, float green, float blue, float intensity) {
        this.glowing = glowing;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.intensity = intensity;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public float red() {
        return red;
    }

    public float green() {
        return green;
    }

    public float blue() {
        return blue;
    }

    public float intensity() {
        return intensity;
    }

    public static VisualRarity fromTier(String tier) {
        try {
            return VisualRarity.valueOf(tier);
        } catch (IllegalArgumentException | NullPointerException error) {
            return COMMON;
        }
    }
}
