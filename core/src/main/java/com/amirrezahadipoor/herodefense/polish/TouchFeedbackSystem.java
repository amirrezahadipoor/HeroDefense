package com.amirrezahadipoor.herodefense.polish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Real-time visual pulses that continue while combat simulation is paused. */
public final class TouchFeedbackSystem {
    private static final int MAX_PULSES = 12;
    private final List<TouchPulse> pulses = new ArrayList<>();

    public void triggerTap(float x, float y) {
        add(new TouchPulse(x, y, TouchPulse.Kind.TAP, 0.20f));
    }

    public void triggerCardSelection(float x, float y) {
        add(new TouchPulse(x, y, TouchPulse.Kind.CARD_SELECTION, 0.36f));
    }

    public void update(float realDeltaSeconds) {
        if (realDeltaSeconds <= 0f) return;
        for (TouchPulse pulse : pulses) pulse.remainingSeconds -= realDeltaSeconds;
        pulses.removeIf(pulse -> pulse.remainingSeconds <= 0f);
    }

    public List<TouchPulse> pulses() {
        return Collections.unmodifiableList(pulses);
    }

    private void add(TouchPulse pulse) {
        if (pulses.size() >= MAX_PULSES) pulses.remove(0);
        pulses.add(pulse);
    }
}
