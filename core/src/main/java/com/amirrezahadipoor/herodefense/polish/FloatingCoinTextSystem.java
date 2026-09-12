package com.amirrezahadipoor.herodefense.polish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded presentation queue for visible per-update kill-coin awards. */
public final class FloatingCoinTextSystem {
    public static final int MAX_LABELS = 12;
    public static final float LIFETIME_SECONDS = 0.95f;
    private static final float RISE_SPEED = 58f;

    private final List<FloatingCoinText> labels = new ArrayList<>();

    public void emit(float x, float y, int amount) {
        if (amount <= 0) return;
        if (labels.size() >= MAX_LABELS) labels.remove(0);
        labels.add(new FloatingCoinText(amount, x, y, LIFETIME_SECONDS));
    }

    public void update(float deltaSeconds) {
        if (deltaSeconds <= 0f) return;
        for (FloatingCoinText label : labels) {
            label.remainingSeconds -= deltaSeconds;
            label.y += RISE_SPEED * deltaSeconds;
        }
        labels.removeIf(label -> label.remainingSeconds <= 0f);
    }

    public List<FloatingCoinText> labels() {
        return Collections.unmodifiableList(labels);
    }

    public void clear() {
        labels.clear();
    }
}
