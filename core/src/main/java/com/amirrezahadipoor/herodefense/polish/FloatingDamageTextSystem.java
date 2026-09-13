package com.amirrezahadipoor.herodefense.polish;

import com.amirrezahadipoor.herodefense.gameplay.CombatEvent;
import com.amirrezahadipoor.herodefense.polish.FloatingDamageText.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Bounded deterministic queue of damage numbers. Positions come only from the combat event
 * and a rotating drift table, so identical combat produces identical pop-ups on every device.
 */
public final class FloatingDamageTextSystem {
    public static final int MAX_LABELS = 40;
    public static final float LIFETIME_SECONDS = 0.72f;
    public static final float CRITICAL_LIFETIME_SECONDS = 0.95f;
    public static final float RISE_DISTANCE = 46f;
    private static final float[] DRIFTS = {-14f, 9f, -4f, 16f, -11f, 5f, 12f, -8f};

    private final List<FloatingDamageText> labels = new ArrayList<>();
    private int sequence;

    public void emit(CombatEvent event) {
        if (event == null) return;
        switch (event.kind()) {
            case HIT -> add(event.secondary() ? Style.SECONDARY : Style.NORMAL,
                formatDamage(event.amount()), event.x(), event.y(), LIFETIME_SECONDS);
            case CRITICAL_HIT -> add(Style.CRITICAL, formatDamage(event.amount()) + "!",
                event.x(), event.y() + 6f, CRITICAL_LIFETIME_SECONDS);
            case CHAIN_ARC -> add(Style.CHAIN, formatDamage(event.amount()),
                event.x(), event.y(), LIFETIME_SECONDS);
            case STUN -> add(Style.STUN, "STUN", event.x(), event.y(), CRITICAL_LIFETIME_SECONDS);
        }
    }

    /** Gold "+$ n" pop-up above the Hero when auto-sell converts a drop into coins. */
    public void emitCoins(int coins, float x, float y) {
        if (coins <= 0) return;
        add(Style.COIN, "+$ " + coins, x, y, CRITICAL_LIFETIME_SECONDS);
    }

    public void emitAll(List<CombatEvent> events) {
        if (events == null) return;
        for (CombatEvent event : events) emit(event);
    }

    public void update(float deltaSeconds) {
        if (deltaSeconds <= 0f) return;
        for (FloatingDamageText label : labels) label.remainingSeconds -= deltaSeconds;
        labels.removeIf(label -> label.remainingSeconds <= 0f);
    }

    public List<FloatingDamageText> labels() {
        return Collections.unmodifiableList(labels);
    }

    public void clear() {
        labels.clear();
    }

    /** Whole numbers up to 999, then "1.2k" style so late-run damage stays short on screen. */
    public static String formatDamage(float amount) {
        int rounded = Math.max(1, Math.round(amount));
        if (rounded < 1_000) return Integer.toString(rounded);
        if (rounded < 100_000) return String.format(Locale.ROOT, "%.1fk", rounded / 1_000f);
        return (rounded / 1_000) + "k";
    }

    private void add(Style style, String text, float x, float y, float lifetime) {
        if (labels.size() >= MAX_LABELS) labels.remove(0);
        float drift = DRIFTS[sequence++ % DRIFTS.length];
        labels.add(new FloatingDamageText(style, text, x, y, drift, lifetime));
    }
}
